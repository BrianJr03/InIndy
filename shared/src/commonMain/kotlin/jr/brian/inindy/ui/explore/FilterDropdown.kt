package jr.brian.inindy.ui.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jr.brian.inindy.domain.model.ExploreFilter
import jr.brian.inindy.domain.model.Group
import jr.brian.inindy.domain.model.toFilterLabel
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.explore_filter_check_cd
import jr.brian.inindy.resources.explore_filter_option_all
import jr.brian.inindy.resources.explore_filter_option_search_groups
import jr.brian.inindy.resources.explore_filter_recent_group_icon_cd
import jr.brian.inindy.ui.icons.CheckIcon
import jr.brian.inindy.ui.icons.GroupIcon
import jr.brian.inindy.ui.icons.SearchIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun FilterDropdown(
    expanded: Boolean,
    activeFilter: ExploreFilter,
    neighborhoodName: String,
    lastSelectedGroup: Group?,
    onSelectAll: () -> Unit,
    onSelectNeighborhood: () -> Unit,
    onSelectLastGroup: (Group) -> Unit,
    onSearchGroups: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        DropdownMenuItem(
            text = {
                FilterOptionRow(
                    label = stringResource(Res.string.explore_filter_option_all),
                    isSelected = activeFilter is ExploreFilter.All
                )
            },
            onClick = onSelectAll
        )
        DropdownMenuItem(
            text = {
                FilterOptionRow(
                    label = "In${neighborhoodName.toFilterLabel()}",
                    isSelected = activeFilter is ExploreFilter.Neighborhood
                )
            },
            onClick = onSelectNeighborhood
        )

        if (lastSelectedGroup != null) {
            val isActive = activeFilter is ExploreFilter.Group &&
                activeFilter.groupId == lastSelectedGroup.id
            DropdownMenuItem(
                text = {
                    FilterOptionRow(
                        label = lastSelectedGroup.name,
                        isSelected = isActive
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = GroupIcon,
                        contentDescription = stringResource(Res.string.explore_filter_recent_group_icon_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = { onSelectLastGroup(lastSelectedGroup) }
            )
        }

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text(stringResource(Res.string.explore_filter_option_search_groups)) },
            leadingIcon = {
                Icon(
                    imageVector = SearchIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            },
            onClick = onSearchGroups
        )
    }
}

@Composable
private fun FilterOptionRow(
    label: String,
    isSelected: Boolean
) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.width(220.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        if (isSelected) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = CheckIcon,
                contentDescription = stringResource(Res.string.explore_filter_check_cd),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Preview
@Composable
private fun FilterDropdownPreview() {
    MaterialTheme {
        FilterDropdown(
            expanded = true,
            activeFilter = ExploreFilter.All,
            neighborhoodName = "Broad Ripple",
            lastSelectedGroup = null,
            onSelectAll = {},
            onSelectNeighborhood = {},
            onSelectLastGroup = {},
            onSearchGroups = {},
            onDismiss = {}
        )
    }
}
