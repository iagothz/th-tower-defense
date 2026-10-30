package com.iagothz.mergetd.ui

import com.iagothz.mergetd.ui.CloudSync.Action.Adopt
import com.iagothz.mergetd.ui.CloudSync.Action.Ask
import com.iagothz.mergetd.ui.CloudSync.Action.Nothing
import com.iagothz.mergetd.ui.CloudSync.Action.Upload
import com.iagothz.mergetd.ui.CloudSync.decide
import kotlin.test.Test
import kotlin.test.assertEquals

class CloudSyncTest {
    @Test
    fun noCloudUploadsVersionOne() {
        assertEquals(Upload(1), decide(0, false, null))
        assertEquals(Upload(1), decide(3, true, null))
    }

    @Test
    fun sameVersionUploadsOnlyWhenLocalChanged() {
        assertEquals(Nothing, decide(4, false, 4))
        assertEquals(Upload(5), decide(4, true, 4))
    }

    @Test
    fun cloudAheadAdoptsWhenLocalUnchangedElseAsks() {
        assertEquals(Adopt, decide(4, false, 6))
        assertEquals(Ask, decide(4, true, 6))
    }

    @Test
    fun cloudBehindIsOverwritten() {
        assertEquals(Upload(5), decide(4, false, 2))
        assertEquals(Upload(5), decide(4, true, 0))
    }
}
