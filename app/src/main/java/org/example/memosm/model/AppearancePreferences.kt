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

/** Whether the feed header slides away while the list scrolls. */
enum class TopBarScrollMode { ALWAYS, ON_SCROLL }

/** Device preferences, shared by every account and activity. */
data class AppearancePreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorTheme: ColorTheme = ColorTheme.SYSTEM,
    val customHue: Float? = null,
    val font: AppFont = AppFont.SYSTEM,
    val themeStyle: ThemeStyle = ThemeStyle.MIUIX,
    val navBarScrollMode: NavBarScrollMode = NavBarScrollMode.HIDE_ON_SCROLL,
    val topBarScrollMode: TopBarScrollMode = TopBarScrollMode.ON_SCROLL,
    /** Frosts the top bar, the bottom bar and the compose button (API 33+ only). */
    val blurEnabled: Boolean = true,
    /** Draws the bottom bar as a floating capsule with a centred compose button. */
    val floatingNavBar: Boolean = false,
    /** Enabled bottom-bar destinations in display order (MainDestination names); empty = default. */
    val navDestinations: List<String> = emptyList()
)
