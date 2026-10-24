package org.example.moxia.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * True while a row is rendered inside a grouped card.
 *
 * Miuix's row primitive ([top.yukonga.miuix.kmp.basic.BasicComponent]) carries no surface of its
 * own: the surrounding card paints the background and the rows are plain rectangles separated by
 * hairlines. Rows that live outside such a card keep their own rounded, tinted block.
 */
val LocalGroupedRow = staticCompositionLocalOf { false }
