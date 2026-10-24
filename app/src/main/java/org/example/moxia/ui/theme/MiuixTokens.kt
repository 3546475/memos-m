package org.example.moxia.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Visual constants for the Miuix (HyperOS) rendering path.
 *
 * Keep material-specific sizes out of screens: a component should read a token from here so the
 * whole MIUIX theme can be tuned in one place. These values are only used when [LocalThemeStyle]
 * is `ThemeStyle.MIUIX`; the MD path keeps its own Material defaults.
 */
object MiuixTokens {
    /** Horizontal page padding used by settings, feeds and full-screen pages. */
    val PageHorizontalPadding = 16.dp

    /** Vertical padding above and below a page's content. */
    val PageVerticalPadding = 12.dp

    /** Distance between two grouped sections. */
    val SectionSpacing = 16.dp

    /** Distance between rows inside one grouped card. */
    val RowSpacing = 2.dp

    /** Corner radius of a row inside a grouped card. */
    val RowCornerRadius = 4.dp

    /** Corner radius of a content card (memo card, settings group, panel). */
    val CardCornerRadius = 16.dp

    /** Corner radius of dialogs and bottom sheets. */
    val DialogCornerRadius = 28.dp

    /** Corner radius for capsules such as filter chips and segmented controls. */
    val PillCornerRadius = 50.dp

    /** Horizontal and vertical padding inside a preference row. */
    val RowHorizontalPadding = 20.dp
    val RowVerticalPadding = 12.dp

    val IconSmall = 20.dp
    val IconMedium = 24.dp
    val IconLarge = 28.dp

    /** Hairline divider thickness. */
    val DividerThickness = 0.5.dp

    /** Chevron on a row that opens another page; quiet enough not to compete with the title. */
    val ChevronColor = androidx.compose.ui.graphics.Color(0xFFADADAD)

    /**
     * Material [Shapes] used while MIUIX is active. Components that cannot be replaced (Material 3
     * dialogs, sliders, text fields) still read corners from here, so they match Miuix cards.
     */
    val Shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(CardCornerRadius),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(DialogCornerRadius)
    )
}
