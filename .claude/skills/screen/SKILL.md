---
name: screen
description: Scaffold a new CMP screen with ViewModel, UiState, and navigation hookup. Use when creating any new Compose Multiplatform screen for InIndy.
argument-hint: <ScreenName> [tab: catchup|explore]
---

# New Screen Scaffold

Scaffold a complete new screen for InIndy using the name and optional tab provided in $ARGUMENTS.

## Steps

1. **Parse arguments** — extract `ScreenName` (PascalCase) and `tab` if provided
2. **Create UiState** in `shared/commonMain/presentation/<tab>/<ScreenName>UiState.kt`
   - Sealed class with `Loading`, `Success(data: ...)`, `Error(message: String)`
3. **Create ViewModel** in `shared/commonMain/presentation/<tab>/<ScreenName>ViewModel.kt`
   - Extend `CommonViewModel`
   - Expose `StateFlow<ScreenNameUiState>`
   - Inject repository via Koin constructor
4. **Create Composable** in `shared/commonMain/ui/<tab>/<ScreenName>Screen.kt`
   - Stateless composable receiving UiState + event lambdas
   - Collect ViewModel state with `collectAsState()`
   - Handle all three UiState cases
5. **Register ViewModel in Koin** — add to the appropriate Koin module in `shared/commonMain/di/`
6. **Add navigation route** — add to the nav graph in `shared/commonMain/navigation/NavGraph.kt`

## Conventions
- File naming: `<ScreenName>Screen.kt`, `<ScreenName>ViewModel.kt`, `<ScreenName>UiState.kt`
- No business logic in Composables — ViewModels only
- All strings go in a `strings` resource file, not hardcoded
- Preview functions required for every Composable
