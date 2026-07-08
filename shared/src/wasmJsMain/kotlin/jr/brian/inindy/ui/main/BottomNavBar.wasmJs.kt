package jr.brian.inindy.ui.main

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.ic_explore
import jr.brian.inindy.resources.ic_person
import jr.brian.inindy.resources.nav_create
import jr.brian.inindy.resources.nav_explore
import jr.brian.inindy.resources.nav_me
import jr.brian.inindy.ui.icons.AddIcon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun BottomNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onCreateClick: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedIndex == 0,
            onClick = { onItemSelected(0) },
            icon = { Icon(painter = painterResource(Res.drawable.ic_explore), contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_explore)) }
        )
        NavigationBarItem(
            selected = selectedIndex == 1,
            onClick = { onItemSelected(1) },
            icon = { Icon(painter = painterResource(Res.drawable.ic_person), contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_me)) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onCreateClick,
            icon = { Icon(imageVector = AddIcon, contentDescription = null) },
            label = { Text(stringResource(Res.string.nav_create)) }
        )
    }
}
