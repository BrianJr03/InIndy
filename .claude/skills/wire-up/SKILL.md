---
name: wire-up
description: Wire up the full InIndy app — session persistence, user profile data flow, local DataStore persistence for neighborhood and interests, Koin module assembly, root navigation routing, and bottom nav connected to real screens. Use when the UI is built and needs to be connected end to end.
---

# InIndy Wire-Up

Connect the full InIndy app end to end based on $ARGUMENTS.
This skill covers local persistence and app wiring only — no Supabase or real backend required.

## What this skill wires up

1. **Session persistence** — fake token saved to `TokenStorage` on first login, read on restart
2. **User profile in app** — name, photo, neighborhood, interests flow from onboarding into the app
3. **Local DataStore** — neighborhood + interests persisted across sessions
4. **Koin module assembly** — all modules registered in correct order
5. **Root navigation** — session gate → onboarding gate → correct tab on launch
6. **Bottom nav** — `Me`, `Explore`, `Events` connected to real screens
7. **Me tab** — loads real fake data on launch
8. **Explore tab** — loads feed on launch with default filter

---

## Phase 1 — Local persistence with DataStore

### Dependency
Add to `gradle/libs.versions.toml`:
```toml
[versions]
datastore = "1.1.1"

[libraries]
datastore-preferences = { module = "androidx.datastore:datastore-preferences-core", version.ref = "datastore" }
```
Add to `shared/commonMain` dependencies in `build.gradle.kts`.

### UserPreferences DataStore
File: `shared/commonMain/data/local/UserPreferencesStore.kt`
```kotlin
data class UserPreferences(
    val userId: String?,
    val fullName: String?,
    val avatarUrl: String?,
    val neighborhoodId: String?,
    val neighborhoodName: String?,
    val interests: List<String>,      // Interest enum names
    val onboardingComplete: Boolean
)

interface UserPreferencesStore {
    val preferences: Flow<UserPreferences>
    suspend fun saveUserId(id: String)
    suspend fun saveProfile(fullName: String, avatarUrl: String?)
    suspend fun saveNeighborhood(id: String, name: String)
    suspend fun saveInterests(interests: List<Interest>)
    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun clear()                // call on sign out
}
```

### expect/actual implementation
File: `shared/commonMain/data/local/UserPreferencesStoreImpl.kt`
- Use `DataStore<Preferences>` from `androidx.datastore:datastore-preferences-core`
- This is multiplatform — works on both Android and iOS via the KMP DataStore artifact
- Keys: `userId`, `fullName`, `avatarUrl`, `neighborhoodId`, `neighborhoodName`, `interests` (comma-separated), `onboardingComplete`

---

## Phase 2 — Session persistence

### FakeAuthRepository — persist session
Update `FakeAuthRepository` to use `TokenStorage`:
```kotlin
class FakeAuthRepository(
    private val tokenStorage: TokenStorage,
    private val userPreferencesStore: UserPreferencesStore
) : AuthRepository {

    override suspend fun isSessionValid(): Boolean {
        // Session is valid if a token exists in TokenStorage
        return tokenStorage.getToken() != null
    }

    override suspend fun getCurrentUser(): User? {
        val prefs = userPreferencesStore.preferences.first()
        val userId = prefs.userId ?: return null
        return User(
            id = userId,
            fullName = prefs.fullName,
            avatarUrl = prefs.avatarUrl,
            phoneVerified = true,
            neighborhoodId = prefs.neighborhoodId,
            interests = prefs.interests.mapNotNull {
                runCatching { Interest.valueOf(it) }.getOrNull()
            }
        )
    }

    // On any successful sign in — save fake token + userId
    override suspend fun verifyOtp(phone: String, code: String): Result<User> {
        delay(1000)
        val fakeUser = User(
            id = "fake_user_001",
            fullName = null,           // null triggers onboarding gate
            avatarUrl = null,
            phoneVerified = true,
            neighborhoodId = null,
            interests = emptyList()
        )
        tokenStorage.saveToken("fake_jwt_token_${System.currentTimeMillis()}")
        userPreferencesStore.saveUserId(fakeUser.id)
        return Result.success(fakeUser)
    }

    override suspend fun signOut(): Result<Unit> {
        tokenStorage.clearToken()
        userPreferencesStore.clear()
        return Result.success(Unit)
    }
}
```

---

## Phase 3 — Onboarding data flow

### FakeOnboardingRepository — persist to DataStore
```kotlin
class FakeOnboardingRepository(
    private val userPreferencesStore: UserPreferencesStore
) : OnboardingRepository {

    override suspend fun updateProfile(fullName: String, avatarUrl: String?): Result<Unit> {
        delay(800)
        userPreferencesStore.saveProfile(fullName, avatarUrl)
        return Result.success(Unit)
    }

    override suspend fun updateNeighborhood(neighborhoodId: String): Result<Unit> {
        delay(500)
        // Resolve neighborhood name from id
        val name = fakeNeighborhoods.find { it.id == neighborhoodId }?.name ?: neighborhoodId
        userPreferencesStore.saveNeighborhood(neighborhoodId, name)
        return Result.success(Unit)
    }

    override suspend fun updateInterests(interests: List<Interest>): Result<Unit> {
        delay(500)
        userPreferencesStore.saveInterests(interests)
        return Result.success(Unit)
    }

    override suspend fun completeOnboarding(): Result<Unit> {
        userPreferencesStore.setOnboardingComplete(true)
        return Result.success(Unit)
    }

    override suspend fun getNeighborhoods(): Result<List<Neighborhood>> {
        delay(300)
        return Result.success(fakeNeighborhoods)
    }

    private val fakeNeighborhoods = listOf(
        Neighborhood("broad_ripple", "Broad Ripple"),
        Neighborhood("fountain_square", "Fountain Square"),
        Neighborhood("irvington", "Irvington"),
        Neighborhood("downtown", "Downtown"),
        Neighborhood("mass_ave", "Mass Ave"),
        Neighborhood("bates_hendricks", "Bates-Hendricks"),
        Neighborhood("cottage_home", "Cottage Home"),
        Neighborhood("herron_morton", "Herron-Morton Place"),
        Neighborhood("butler_tarkington", "Butler-Tarkington"),
        Neighborhood("meridian_kessler", "Meridian-Kessler")
    )
}
```

### User.isOnboardingComplete — update to use DataStore flag
```kotlin
// In User domain model — keep extension but also check DataStore flag
val User.isOnboardingComplete: Boolean
    get() = fullName != null && neighborhoodId != null && interests.isNotEmpty()
```
The `UserPreferences.onboardingComplete` flag is the authoritative gate — set to true only after all three onboarding steps complete.

---

## Phase 4 — User state in app

### AppViewModel (root level)
File: `shared/commonMain/presentation/AppViewModel.kt`
```kotlin
data class AppUiState(
    val isLoading: Boolean = true,
    val destination: AppDestination = AppDestination.Auth
)

sealed class AppDestination {
    object Auth : AppDestination()
    object Onboarding : AppDestination()
    object Main : AppDestination()
}

class AppViewModel(
    private val authRepository: AuthRepository,
    private val userPreferencesStore: UserPreferencesStore
) : CommonViewModel() {

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val sessionValid = authRepository.isSessionValid()
            if (!sessionValid) {
                _state.update { it.copy(isLoading = false, destination = AppDestination.Auth) }
                return@launch
            }
            // Session valid — check onboarding
            val prefs = userPreferencesStore.preferences.first()
            val destination = if (prefs.onboardingComplete) {
                AppDestination.Main
            } else {
                AppDestination.Onboarding
            }
            _state.update { it.copy(isLoading = false, destination = destination) }
        }
    }
}
```

### CurrentUserProvider
File: `shared/commonMain/domain/CurrentUserProvider.kt`
```kotlin
// Singleton in Koin — provides current user to any ViewModel that needs it
class CurrentUserProvider(
    private val userPreferencesStore: UserPreferencesStore
) {
    val user: Flow<UserPreferences> = userPreferencesStore.preferences

    suspend fun get(): UserPreferences = userPreferencesStore.preferences.first()
}
```
Inject `CurrentUserProvider` into any ViewModel that needs the user's name, neighborhood, or interests — `MeViewModel`, `ExploreViewModel`, `CreatePostViewModel`.

---

## Phase 5 — Root navigation

File: `shared/commonMain/navigation/NavGraph.kt`

```kotlin
@Composable
fun RootNavGraph(
    appViewModel: AppViewModel = koinViewModel(),
    navController: NavHostController = rememberNavController()
) {
    val state by appViewModel.state.collectAsState()

    // Show splash/loading while checking session
    if (state.isLoading) {
        SplashScreen()
        return
    }

    val startDestination = when (state.destination) {
        AppDestination.Auth -> "auth_graph"
        AppDestination.Onboarding -> "onboarding_graph"
        AppDestination.Main -> "main_graph"
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // Auth graph
        navigation(startDestination = "intro", route = "auth_graph") {
            composable("intro") { IntroScreen(navController) }
            composable("welcome") { WelcomeScreen(navController) }
            composable("signup_phone") { SignUpPhoneScreen(navController) }
            composable("signup_email") { SignUpEmailScreen(navController) }
            composable("otp_verify/{phone}") { OtpVerifyScreen(navController) }
            composable("email_link_sent") { EmailLinkSentScreen(navController) }
            composable("signin") { SignInScreen(navController) }
        }

        // Onboarding graph
        navigation(startDestination = "onboarding_profile", route = "onboarding_graph") {
            composable("onboarding_profile") { OnboardingProfileScreen(navController) }
            composable("onboarding_neighborhood") { OnboardingNeighborhoodScreen(navController) }
            composable("onboarding_interests") { OnboardingInterestsScreen(navController) }
        }

        // Main graph
        navigation(startDestination = "main", route = "main_graph") {
            composable("main") { MainScreen(navController) }
            composable("create_post") { CreatePostScreen(navController) }
            composable("create_group") { CreateGroupScreen(navController) }
            composable("post_detail/{postId}") { PostDetailScreen(navController) }
            composable("group_management/{groupId}") { GroupManagementScreen(navController) }
        }
    }
}
```

### After onboarding completes → navigate to main
In `OnboardingViewModel` on `CompleteOnboarding`:
```kotlin
// After saving interests + setting onboardingComplete = true
navController.navigate("main_graph") {
    popUpTo("onboarding_graph") { inclusive = true }
}
```

### After sign in → check onboarding
In `AuthViewModel` on `Authenticated`:
```kotlin
val destination = if (user.isOnboardingComplete) "main_graph" else "onboarding_graph"
navController.navigate(destination) {
    popUpTo("auth_graph") { inclusive = true }
}
```

---

## Phase 6 — Bottom nav + MainScreen

File: `shared/commonMain/ui/main/MainScreen.kt`

```kotlin
@Composable
fun MainScreen(rootNavController: NavHostController) {
    val tabNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavBar(
                navController = tabNavController,
                items = listOf(
                    BottomNavItem("me", Icons.Rounded.Person, "Me"),
                    BottomNavItem("explore", Icons.Rounded.Explore, "Explore"),
                    BottomNavItem("events", Icons.Rounded.CalendarMonth, "Events")
                )
            )
        }
    ) { padding ->
        NavHost(
            navController = tabNavController,
            startDestination = "explore",         // Explore is default on first launch
            modifier = Modifier.padding(padding)
        ) {
            composable("me") {
                MeScreen(
                    onCreatePost = { rootNavController.navigate("create_post") },
                    onCreateGroup = { rootNavController.navigate("create_group") },
                    onPostClick = { rootNavController.navigate("post_detail/$it") },
                    onGroupClick = { rootNavController.navigate("group_management/$it") }
                )
            }
            composable("explore") {
                ExploreScreen(
                    onPostClick = { rootNavController.navigate("post_detail/$it") }
                )
            }
            composable("events") {
                EventsScreen()  // placeholder until Eventbrite is connected
            }
        }
    }
}
```

### BottomNavBar — selected state + back stack
```kotlin
@Composable
fun BottomNavBar(
    navController: NavHostController,
    items: List<BottomNavItem>,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
```

---

## Phase 7 — User data in ViewModels

### MeViewModel — load from CurrentUserProvider
```kotlin
class MeViewModel(
    private val currentUserProvider: CurrentUserProvider,
    private val postRepository: PostRepository,
    private val groupRepository: GroupRepository,
    private val attendanceRepository: AttendanceRepository
) : CommonViewModel() {

    init {
        onIntent(MeIntent.Load)
    }

    private fun load() {
        viewModelScope.launch {
            val prefs = currentUserProvider.get()
            // Use prefs.fullName, prefs.avatarUrl, prefs.neighborhoodName in MeScreen header
            val posts = async { postRepository.getUserPosts() }
            val groups = async { groupRepository.getUserGroups() }
            val attendance = async { attendanceRepository.getAttendanceHistory() }
            // ...
        }
    }
}
```

### ExploreViewModel — neighborhood from CurrentUserProvider
```kotlin
init {
    viewModelScope.launch {
        val prefs = currentUserProvider.get()
        _state.update { it.copy(
            user = prefs,
            brandMarkText = "InIndy"
        )}
        loadFeed()
    }
}
```

---

## Phase 8 — Koin module assembly

File: `shared/commonMain/di/AppModule.kt`

```kotlin
// Assemble all modules in correct dependency order
val appModules = listOf(
    coreModule,         // TokenStorage, UserPreferencesStore, CurrentUserProvider
    authModule,         // FakeAuthRepository, FakeOnboardingRepository, AuthViewModel, OnboardingViewModel
    postModule,         // FakePostRepository, FakeGroupRepository, FakeAttendanceRepository, LocationProvider
    mediaModule,        // FakeMediaRepository, ImageCompressor, ImagePicker
    appViewModelModule  // AppViewModel
)
```

File: `shared/commonMain/di/CoreModule.kt`
```kotlin
val coreModule = module {
    single { TokenStorage() }
    single { UserPreferencesStoreImpl(get()) as UserPreferencesStore }
    single { CurrentUserProvider(get()) }
}
```

File: `shared/commonMain/di/AppViewModelModule.kt`
```kotlin
val appViewModelModule = module {
    viewModel { AppViewModel(get(), get()) }
}
```

### Android — start Koin in Application class
File: `androidApp/src/main/kotlin/InIndyApplication.kt`
```kotlin
class InIndyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@InIndyApplication)
            modules(appModules)
        }
    }
}
```
Register in `AndroidManifest.xml`: `android:name=".InIndyApplication"`

### iOS — start Koin in AppDelegate / @main
File: `iosApp/iosApp/InIndyApp.swift`
```swift
@main
struct InIndyApp: App {
    init() {
        KoinKt.doInitKoin()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

File: `shared/commonMain/di/KoinInit.kt`
```kotlin
fun initKoin() {
    startKoin {
        modules(appModules)
    }
}
```

---

## Phase 9 — EventsScreen placeholder

File: `shared/commonMain/ui/events/EventsScreen.kt`
```kotlin
@Composable
fun EventsScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Events",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Eventbrite events coming soon",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

---

## Implementation order
1. `UserPreferencesStore` interface + `UserPreferencesStoreImpl` (DataStore)
2. `CurrentUserProvider`
3. `CoreModule` Koin module
4. Update `FakeAuthRepository` — use `TokenStorage` + `UserPreferencesStore` for persistence
5. Update `FakeOnboardingRepository` — persist to `UserPreferencesStore`
6. `AppViewModel` — session check on init
7. `AppViewModelModule` Koin module
8. Assemble `appModules` list in `AppModule.kt`
9. Wire Koin in `InIndyApplication` (Android) + `KoinInit.kt` (iOS)
10. `RootNavGraph` — session → onboarding → main routing
11. `MainScreen` — `Scaffold` + `BottomNavBar` + tab `NavHost`
12. `BottomNavBar` — `saveState`/`restoreState` pattern
13. Update `MeViewModel` to load from `CurrentUserProvider`
14. Update `ExploreViewModel` to load neighborhood from `CurrentUserProvider`
15. `EventsScreen` placeholder
16. Update `OnboardingViewModel` — navigate to `main_graph` on complete
17. Update `AuthViewModel` — navigate to correct graph on sign in

## What NOT to do
- Don't use `SharedPreferences` or `NSUserDefaults` directly — use `UserPreferencesStore`
- Don't store JWT in `UserPreferencesStore` — JWT lives in `TokenStorage` (encrypted) only
- Don't hardcode `startDestination` to auth — always derive from `AppViewModel` state
- Don't navigate inside Composables directly — emit navigation events from ViewModels
- Don't use a single global `NavController` for both root and tab navigation — keep them separate
- Don't call `loadFeed()` before user preferences are loaded — neighborhood id must be available first
- Don't skip `saveState = true` + `restoreState = true` in `BottomNavBar` — without these, tab state is lost on switch
- Don't call `startKoin` more than once — guard with `try/catch` or `KoinApplication` check on iOS
  EOF