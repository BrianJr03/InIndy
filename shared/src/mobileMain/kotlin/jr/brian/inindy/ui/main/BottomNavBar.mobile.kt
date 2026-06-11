package jr.brian.inindy.ui.main

import androidx.compose.runtime.Composable
import io.github.narendraanjana09.adaptivenavbar.AdaptiveNavigationBar
import io.github.narendraanjana09.adaptivenavbar.NavigationItem
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.ic_explore
import jr.brian.inindy.resources.ic_person
import jr.brian.inindy.resources.nav_explore
import jr.brian.inindy.resources.nav_me
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun BottomNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    val items = listOf(
        NavigationItem(
            title = stringResource(Res.string.nav_explore),
            icon = Res.drawable.ic_explore,
            selectedIcon = Res.drawable.ic_explore,
            systemIcon = "map",
            selectedSystemIcon = "map.fill"
        ),
        NavigationItem(
            title = stringResource(Res.string.nav_me),
            icon = Res.drawable.ic_person,
            selectedIcon = Res.drawable.ic_person,
            systemIcon = "person",
            selectedSystemIcon = "person.fill"
        ),
    )

    AdaptiveNavigationBar(
        items = items,
        selectedIndex = selectedIndex,
        onItemSelected = onItemSelected
    )
}
