package org.example.memosm.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * Guards the HyperOS structure of the MIUIX palette: the neutral scale must stay Miuix's (white
 * cards on a grey page, black page when dark) instead of collapsing into Material You tones.
 */
class MiuixColorMappingTest {
    @Test
    fun `light cards are muiix surfaces on the page color`() {
        val colors = lightColorScheme()
        val scheme = colors.toMaterialColorScheme(isDark = false)

        assertEquals(colors.surface, scheme.background)
        assertEquals(colors.surface, scheme.surface)
        assertEquals(colors.surfaceContainer, scheme.surfaceContainer)
        assertEquals(colors.surfaceContainer, scheme.surfaceContainerHigh)
        assertEquals(colors.surfaceContainer, scheme.surfaceContainerHighest)
        assertEquals(colors.surface, scheme.surfaceContainerLow)
        assertEquals(Color(0xFFFFFFFF), scheme.surfaceContainer)
        assertEquals(Color(0xFFF7F7F7), scheme.background)
    }

    @Test
    fun `dark keeps a black page and a lighter card`() {
        val colors = darkColorScheme()
        val scheme = colors.toMaterialColorScheme(isDark = true)

        assertEquals(colors.surface, scheme.background)
        assertEquals(Color(0xFF000000), scheme.background)
        assertEquals(Color(0xFF242424), scheme.surfaceContainer)
    }

    @Test
    fun `accent and text roles follow the muiix palette`() {
        val colors = lightColorScheme()
        val scheme = colors.toMaterialColorScheme(isDark = false)

        assertEquals(colors.primary, scheme.primary)
        assertEquals(colors.onPrimary, scheme.onPrimary)
        assertEquals(colors.tertiaryContainer, scheme.primaryContainer)
        assertEquals(colors.onTertiaryContainer, scheme.onPrimaryContainer)
        assertEquals(colors.onSurfaceSecondary, scheme.onSurfaceVariant)
        assertEquals(colors.dividerLine, scheme.outlineVariant)
    }
}
