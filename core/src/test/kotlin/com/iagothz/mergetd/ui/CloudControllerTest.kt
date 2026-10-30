package com.iagothz.mergetd.ui

import com.iagothz.mergetd.ui.CloudController.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CloudControllerTest {
    private val today = 20300
    private val fake = FakeCloud()
    private val save = SaveData(SaveDataTest.memoryPrefs()).apply { xp = 100 }
    private var adopted = 0
    private val c by lazy { CloudController(save, fake) { today }.also { it.onAdopted = { adopted++ } } }

    private val cloudAhead = "i:sync.at=20290\ni:sync.version=3\ni:xp=900"

    private fun bothChanged() {
        save.syncVersion = 2
        save.syncHash = "stale"
        fake.blob = cloudAhead
        c.open()
    }

    @Test
    fun openingWithNoCloudUploadsVersionOne() {
        c.open()
        assertEquals(1, fake.uploads.size)
        assertEquals(1, CloudBlob.version(fake.blob!!))
        assertEquals(Status.Synced(today), c.status)
        assertEquals(1, save.syncVersion)
        assertEquals(CloudBlob.hash(fake.blob!!), save.syncHash)
        assertEquals("nível 1 · 0 partidas", fake.descriptions.single())
    }

    @Test
    fun openingWithCloudAheadAdoptsSilently() {
        fake.blob = cloudAhead
        c.open()
        assertEquals(900, save.xp)
        assertEquals(3, save.syncVersion)
        assertEquals("Progresso da nuvem aplicado", c.notice)
        assertEquals(1, adopted)
        assertEquals(Status.Synced(today), c.status)
        assertTrue(fake.uploads.isEmpty())
    }

    @Test
    fun openingUnchangedAtTheSameVersionDoesNothing() {
        c.open()
        fake.uploads.clear()
        c.open()
        assertTrue(fake.uploads.isEmpty())
        assertEquals(Status.Synced(today), c.status)
    }

    @Test
    fun openingWithBothChangedAsks() {
        bothChanged()
        assertEquals(cloudAhead, c.conflict)
        assertEquals(100, save.xp)
        assertTrue(fake.uploads.isEmpty())
    }

    @Test
    fun conflictCallbackFiresOnlyOutsideAMatch() {
        val seen = mutableListOf<String>()
        c.onConflict = { seen += it }
        c.inMatch = true
        bothChanged()
        assertTrue(seen.isEmpty())
        c.inMatch = false
        assertEquals(listOf(cloudAhead), seen)
        assertEquals(cloudAhead, c.conflict)
    }

    @Test
    fun keepLocalUploadsAboveTheCloudVersion() {
        bothChanged()
        c.keepLocal()
        assertEquals(4, CloudBlob.version(fake.blob!!))
        assertEquals(100, save.xp)
        assertNull(c.conflict)
    }

    @Test
    fun keepCloudImportsIt() {
        bothChanged()
        c.keepCloud()
        assertEquals(900, save.xp)
        assertEquals(3, save.syncVersion)
        assertNull(c.conflict)
        assertEquals(1, adopted)
    }

    @Test
    fun cloudResultDuringAMatchWaitsForTheMenu() {
        fake.blob = cloudAhead
        fake.deliverLater = true
        c.open()
        c.inMatch = true
        fake.deliver() // isSignedIn
        fake.deliver() // download
        assertEquals(100, save.xp)
        c.inMatch = false
        assertEquals(900, save.xp)
        assertEquals(1, adopted)
    }

    @Test
    fun pushWhileUploadingRunsOnceMoreAfterwards() {
        fake.deliverLater = true
        c.push()
        c.push()
        c.push()
        fake.deliver()
        fake.deliver()
        assertEquals(2, fake.uploads.size)
        assertEquals(2, save.syncVersion)
        assertEquals(2, CloudBlob.version(fake.blob!!))
    }

    @Test
    fun aNewDeviceThatPlayedOfflineAsksBeforeTakingTheCloud() {
        save.recordMatch(com.iagothz.mergetd.logic.GameMap.DEFAULT, emptyList(), 5, 1, 0, 0)
        fake.blob = cloudAhead
        c.open()
        assertEquals(cloudAhead, c.conflict)
    }

    @Test
    fun offlinePushLeavesHashDivergentSoNextOpenUploads() {
        fake.offline = true
        c.push()
        assertEquals(Status.Offline, c.status)
        fake.offline = false
        c.open()
        assertEquals(1, fake.uploads.size)
    }

    @Test
    fun disabledDoesNothingAndDropsLateResults() {
        fake.blob = cloudAhead
        fake.deliverLater = true
        c.open()
        c.setEnabled(false)
        fake.deliver()
        fake.deliver()
        assertEquals(100, save.xp)
        assertEquals(Status.Off, c.status)
        c.push()
        assertTrue(fake.uploads.isEmpty())
    }

    @Test
    fun enablingAgainOpens() {
        c.setEnabled(false)
        fake.deliverLater = false
        c.setEnabled(true)
        assertEquals(1, fake.uploads.size)
    }

    @Test
    fun signedOutShowsSignedOutAndSignInOpens() {
        fake.signedIn = false
        c.open()
        assertEquals(Status.SignedOut, c.status)
        fake.signedIn = true
        c.signIn()
        assertEquals(1, fake.uploads.size)
    }

    @Test
    fun signInRefusedStaysSignedOut() {
        fake.signedIn = false
        c.signIn()
        assertEquals(Status.SignedOut, c.status)
    }

    @Test
    fun unavailableCloudIsUnavailable() {
        fake.available = false
        assertEquals(Status.Unavailable, c.status)
        c.open()
        c.push()
        assertTrue(fake.uploads.isEmpty())
    }

    @Test
    fun corruptCloudBlobIsOverwrittenByLocal() {
        fake.blob = "garbage"
        c.open()
        assertEquals(1, CloudBlob.version(fake.blob!!))
        assertEquals(100, save.xp)
    }

    @Test
    fun adoptingDropsTheMatchInProgress() {
        save.saveMatch("snapshot", 3)
        fake.blob = cloudAhead
        c.open()
        assertNull(save.savedMatch)
    }

    @Test
    fun aReplyThatNeverComesTimesOutAndFreesTheNextPush() {
        fake.deliverLater = true
        c.open()
        c.tick(CloudController.TIMEOUT - 1f)
        assertEquals(Status.Working, c.status)
        c.tick(2f)
        assertEquals(Status.Error("timeout"), c.status)
        fake.deliver()
        fake.deliver()
        assertEquals(100, save.xp, "the late reply is dropped")
        fake.deliverLater = true
        c.push()
        c.tick(CloudController.TIMEOUT + 1f)
        fake.deliverLater = false
        c.push()
        assertEquals(1, fake.uploads.size, "a hung upload no longer blocks the next one")
    }

    @Test
    fun errorsFromTheCloudShowAsErrors() {
        fake.blob = null
        fake.signedIn = true
        val broken = object : CloudSave by fake {
            override fun download(done: (CloudResult<String?>) -> Unit) = done(CloudResult.Failed("error:7"))
        }
        val c2 = CloudController(save, broken) { today }
        c2.open()
        assertEquals(Status.Error("error:7"), c2.status)
    }
}
