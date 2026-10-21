package org.example.memosm.model

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

/** The design language the app is drawn with: the original Material Design styling or Miuix. */
enum class ThemeStyle { MD, MIUIX }

/** Whether the bottom navigation bar slides away while the list scrolls. */
enum class NavBarScrollMode { HIDE_ON_SCROLL, ALWAYS_VISIBLE }

/** Which part of a feed header hides while scrolling: nothing, the search bar, or the whole header. */
enum class TopBarScrollMode { NEVER, SEARCH_ONLY, ALL }

/** Device preferences, shared by every account and activity. */
data class AppearancePreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorTheme: ColorTheme = ColorTheme.SYSTEM,
    val customHue: Float? = null,
    val font: AppFont = AppFont.SYSTEM,
    val themeStyle: ThemeStyle = ThemeStyle.MIUIX,
    val navBarScrollMode: NavBarScrollMode = NavBarScrollMode.HIDE_ON_SCROLL,
    val topBarScrollMode: TopBarScrollMode = TopBarScrollMode.ALL,
    /** Enabled bottom-bar destinations in display order (MainDestination names); empty = default. */
    val navDestinations: List<String> = emptyList()
)
