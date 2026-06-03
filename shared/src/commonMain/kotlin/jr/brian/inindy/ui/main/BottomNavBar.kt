package jr.brian.inindy.ui.main

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.nav_events
import jr.brian.inindy.resources.nav_explore
import jr.brian.inindy.resources.nav_me
import jr.brian.inindy.ui.icons.DateRangeIcon
import jr.brian.inindy.ui.icons.PersonIcon
import jr.brian.inindy.ui.icons.SearchIcon
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class MainTab(
    val route: String,
    val icon: ImageVector,
    val label: StringResource
) {
    ME(
        route = "tab_me",
        icon = PersonIcon,
        label = Res.string.nav_me
    ),
    EXPLORE(
        route = "tab_explore",
        icon = SearchIcon,
        label = Res.string.nav_explore
    ),
    EVENTS(
        route = "tab_events",
        icon = DateRangeIcon,
        label = Res.string.nav_events
    )
}

@Composable
fun BottomNavBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    tabs: List<MainTab> = listOf(
        MainTab.EXPLORE,
        MainTab.ME,
        /*MainTab.EVENTS*/
    )
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar(modifier = modifier) {
        tabs.forEach { tab ->
            TabItem(
                selected = currentRoute == tab.route,
                onClick = {
                    if (currentRoute == tab.route) return@TabItem
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = tab.icon,
                label = tab.label
            )
        }
    }
}

@Composable
private fun RowScope.TabItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: StringResource
) {
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedColor = MaterialTheme.colorScheme.primary
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) selectedColor else unselectedColor,
                modifier = Modifier.size(22.dp)
            )
        },
        label = {
            Text(
                text = stringResource(label),
                color = if (selected) selectedColor else unselectedColor,
                fontSize = if (selected) 13.sp else 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = selectedColor,
            unselectedIconColor = unselectedColor,
            selectedTextColor = selectedColor,
            unselectedTextColor = unselectedColor,
            indicatorColor = Color.Transparent
        )
    )
}
