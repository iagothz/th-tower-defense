package com.iagothz.mergetd

import com.iagothz.mergetd.ui.MergeTdGame
import java.io.File
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuildInfoTest {
    private val props = Properties().apply { File("../gradle.properties").inputStream().use { load(it) } }

    @Test
    fun versionComesFromGradleProperties() {
        assertEquals(props.getProperty("appVersion"), BuildInfo.VERSION)
        assertEquals(BuildInfo.VERSION, MergeTdGame.VERSION)
    }

    @Test
    fun versionIsSemanticAndHasAChangelogEntry() {
        assertTrue(Regex("""\d+\.\d+\.\d+""").matches(BuildInfo.VERSION), BuildInfo.VERSION)
        assertTrue(props.getProperty("appVersionCode").toInt() > 0)
        assertTrue(File("../CHANGELOG.md").readText().contains("## [${BuildInfo.VERSION}]"))
    }
}
