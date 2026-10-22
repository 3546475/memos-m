package org.example.memosm.ui.component.item.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

class CodeLanguageLabelTest {
    @Test
    fun `known aliases are shown with their canonical name`() {
        assertEquals("Kotlin", CodeLanguageLabel.displayName("kt"))
        assertEquals("Kotlin", CodeLanguageLabel.displayName(" KOTLIN "))
        assertEquals("Shell", CodeLanguageLabel.displayName("bash"))
        assertEquals("JavaScript", CodeLanguageLabel.displayName("js"))
        assertEquals("C++", CodeLanguageLabel.displayName("cpp"))
        assertEquals("YAML", CodeLanguageLabel.displayName("yml"))
    }

    @Test
    fun `a fence without a language is labelled as plain text`() {
        assertEquals(CodeLanguageLabel.PlainTextLabel, CodeLanguageLabel.displayName(""))
        assertEquals(CodeLanguageLabel.PlainTextLabel, CodeLanguageLabel.displayName("   "))
    }

    @Test
    fun `unknown or decorated languages keep the author's word`() {
        assertEquals("mermaid", CodeLanguageLabel.displayName("Mermaid"))
        // A fence may carry more than the language; only the first word is the language.
        assertEquals("Kotlin", CodeLanguageLabel.displayName("kotlin title=Main.kt"))
        assertEquals("unknown-thing", CodeLanguageLabel.displayName("unknown-thing"))
    }
}
