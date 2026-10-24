package org.example.moxia.model

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK -> true
    }
}

enum class ColorTheme { SYSTEM, MONOCHROME, MEMOS, CUSTOM }

enum class AppFont { SYSTEM, SANS_SERIF, SERIF, MONOSPACE }

/** Whether the bottom navigation bar slides away while the list scrolls. */
enum class NavBarScrollMode { HIDE_ON_SCROLL, ALWAYS_VISIBLE }

/** Whether the feed header slides away while the list scrolls. */
enum class TopBarScrollMode { ALWAYS, ON_SCROLL }

/** The gesture on a memo card that opens its editor; the card menu always offers 编辑 too. */
enum class EditGesture { NONE, SINGLE_TAP, DOUBLE_TAP, LONG_PRESS }

/**
 * Bottom-bar destinations a fresh install starts with, as [org.example.moxia.ui.MainDestination]
 * names: just the notes feed and the profile. Every other destination stays available in the
 * editor, so this only decides what the bar shows before the user changes anything.
 */
val DefaultNavDestinations: List<String> = listOf("MEMOS", "PROFILE")

/** Device preferences, shared by every account and activity. */
data class AppearancePreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorTheme: ColorTheme = ColorTheme.SYSTEM,
    val customHue: Float? = null,
    val font: AppFont = AppFont.SYSTEM,
    val navBarScrollMode: NavBarScrollMode = NavBarScrollMode.ALWAYS_VISIBLE,
    val topBarScrollMode: TopBarScrollMode = TopBarScrollMode.ON_SCROLL,
    /** Frosts the floating feed header (API 33+ only). Off by default: the header is a thin row. */
    val topBarBlur: Boolean = false,
    /** Frosts the bottom bar and the compose button that shares its material (API 33+ only). */
    val bottomBarBlur: Boolean = true,
    /** Draws the bottom bar as a floating capsule with a centred compose button. */
    val floatingNavBar: Boolean = true,
    /** Opens the editor straight from a card, bypassing the detail page and the menu. */
    val editGesture: EditGesture = EditGesture.NONE,
    /** Enabled bottom-bar destinations in display order; see [DefaultNavDestinations]. */
    val navDestinations: List<String> = DefaultNavDestinations
)
