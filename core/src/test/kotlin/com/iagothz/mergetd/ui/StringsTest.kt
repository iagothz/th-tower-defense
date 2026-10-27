package com.iagothz.mergetd.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringsTest {
    @Test
    fun tableHasAnEnglishLineForEveryEntryAndNoEmptyOnes() {
        assertTrue(S.keys().size > 150, "${S.keys().size} entries")
        for (key in S.keys()) {
            assertTrue(key.isNotBlank() && S.has(Language.EN, key) && S.english(key).isNotBlank(), "'$key'")
        }
    }

    @Test
    fun lookupFollowsTheCurrentLanguageAndFormats() {
        S.language = Language.PT
        assertEquals("Jogar", S["Jogar"])
        S.language = Language.EN
        assertEquals("Play", S["Jogar"])
        assertEquals("Wave 7", S.f("Onda %d", 7))
        assertEquals("unknown text stays", S["unknown text stays"])
        S.language = Language.PT
        assertEquals("Onda 7", S.f("Onda %d", 7))
    }

    @Test
    fun autoPicksEnglishForAnythingButPortuguese() {
        assertEquals(Language.PT, Language.resolve(Language.AUTO, "pt"))
        assertEquals(Language.EN, Language.resolve(Language.AUTO, "en"))
        assertEquals(Language.EN, Language.resolve(Language.AUTO, "es"))
        assertEquals(Language.PT, Language.resolve(Language.PT, "en"))
        assertEquals(Language.EN, Language.resolve(Language.EN, "pt"))
    }
}
