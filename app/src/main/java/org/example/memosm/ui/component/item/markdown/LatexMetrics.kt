package org.example.memosm.ui.component.item.markdown

import android.content.Context
import android.view.View.MeasureSpec
import androidx.collection.LruCache
import com.agog.mathdisplay.MTMathView

/**
 * Inline math is sized by laying out a throwaway [MTMathView], which is expensive enough to show up
 * in notes that repeat a formula (or when a card recomposes while scrolling). Results are kept per
 * `(latex, font size)` and reused; the view itself is never retained.
 */
internal object LatexMetrics {
    private data class Key(val latex: String, val fontSizePx: Float)

    private val cache = object : LruCache<Key, Pair<Double, Double>>(512) {}

    /** Width and height of the rendered formula in `em`, or null when it cannot be measured. */
    fun measure(context: Context, latex: String, fontSizePx: Float): Pair<Double, Double>? {
        if (latex.isEmpty() || fontSizePx <= 0f) return null
        val key = Key(latex, fontSizePx)
        cache[key]?.let { return it }
        return try {
            val view = MTMathView(context, null)
            view.fontSize = fontSizePx
            view.latex = latex
            view.measure(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            val em = (view.measuredWidth / fontSizePx).toDouble() to
                (view.measuredHeight / fontSizePx).toDouble()
            cache.put(key, em)
            em
        } catch (e: Exception) {
            null
        }
    }
}
