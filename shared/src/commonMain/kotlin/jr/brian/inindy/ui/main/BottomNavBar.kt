package jr.brian.inindy.ui.main

import androidx.compose.runtime.Composable

const val ROUTE_TAB_ME = "tab_me"
const val ROUTE_TAB_EXPLORE = "tab_explore"

@Composable
expect fun BottomNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
)
