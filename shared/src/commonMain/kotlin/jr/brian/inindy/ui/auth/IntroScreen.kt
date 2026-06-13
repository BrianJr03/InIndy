package jr.brian.inindy.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.inindy.resources.Res
import jr.brian.inindy.resources.intro_1_subtitle
import jr.brian.inindy.resources.intro_1_title
import jr.brian.inindy.resources.intro_2_subtitle
import jr.brian.inindy.resources.intro_2_title
import jr.brian.inindy.resources.intro_3_subtitle
import jr.brian.inindy.resources.intro_3_title
import jr.brian.inindy.resources.intro_get_started
import jr.brian.inindy.resources.intro_next
import jr.brian.inindy.resources.intro_skip
import jr.brian.inindy.ui.auth.animations.AnimatedBrandMarkBackground
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class IntroPage(
    val title: StringResource,
    val subtitle: StringResource,
    val accent: Color
)

@Composable
fun IntroScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = listOf(
        IntroPage(Res.string.intro_1_title, Res.string.intro_1_subtitle, MaterialTheme.colorScheme.primary),
        IntroPage(Res.string.intro_2_title, Res.string.intro_2_subtitle, MaterialTheme.colorScheme.tertiary),
        IntroPage(Res.string.intro_3_title, Res.string.intro_3_subtitle, MaterialTheme.colorScheme.secondary)
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    AuthBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            AuthTopBar(
                onBack = null,
                trailing = {
                    if (pagerState.currentPage < pages.lastIndex) {
                        TextButton(onClick = onFinish) {
                            Text(
                                text = stringResource(Res.string.intro_skip),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                IntroPageContent(pages[page])
            }

            PageIndicator(
                count = pages.size,
                selectedIndex = pagerState.currentPage,
                accent = pages[pagerState.currentPage].accent,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Button(
                onClick = {
                    if (pagerState.currentPage < pages.lastIndex) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinish()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = pages[pagerState.currentPage].accent,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage < pages.lastIndex)
                        stringResource(Res.string.intro_next)
                    else
                        stringResource(Res.string.intro_get_started),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun IntroPageContent(page: IntroPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedBrandMarkBackground(
            colors = listOf(
                page.accent.copy(alpha = 0.30f),
                page.accent.copy(alpha = 0.12f)
            )
        )
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(page.subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PageIndicator(
    count: Int,
    selectedIndex: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = if (selected) 24.dp else 8.dp, height = 8.dp)
                    .background(
                        color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Preview
@Composable
private fun IntroScreenPreview() {
    MaterialTheme {
        IntroScreen(onFinish = {})
    }
}

@Preview
@Composable
private fun IntroScreenDarkPreview() {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
        IntroScreen(onFinish = {})
    }
}
