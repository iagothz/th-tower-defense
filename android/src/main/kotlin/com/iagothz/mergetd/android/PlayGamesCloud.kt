package com.iagothz.mergetd.android

import android.app.Activity
import com.badlogic.gdx.Gdx
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import com.google.android.gms.tasks.Task
import com.iagothz.mergetd.ui.CloudResult
import com.iagothz.mergetd.ui.CloudSave

/**
 * The account's save as one Play Games snapshot named [SNAPSHOT]. Google's own conflict between two devices
 * writing at once is settled by "most recently modified"; the game's notion of conflict lives in the core.
 * Every callback is posted to the GL thread.
 */
class PlayGamesCloud(private val activity: Activity) : CloudSave {
    override val available = true

    override fun signIn(done: (Boolean) -> Unit) {
        PlayGames.getGamesSignInClient(activity).signIn().addOnCompleteListener { t ->
            post { done(t.isSuccessful && t.result.isAuthenticated) }
        }
    }

    override fun isSignedIn(done: (Boolean) -> Unit) {
        PlayGames.getGamesSignInClient(activity).isAuthenticated.addOnCompleteListener { t ->
            post { done(t.isSuccessful && t.result.isAuthenticated) }
        }
    }

    override fun download(done: (CloudResult<String?>) -> Unit) {
        val client = PlayGames.getSnapshotsClient(activity)
        open(client).addOnCompleteListener { t ->
            val snapshot = t.snapshot()
            if (snapshot == null) {
                post { done(failure(t.exception)) }
                return@addOnCompleteListener
            }
            val bytes = runCatching { snapshot.snapshotContents.readFully() }.getOrNull()
            client.discardAndClose(snapshot)
            post { done(CloudResult.Ok(bytes?.takeIf { it.isNotEmpty() }?.toString(Charsets.UTF_8))) }
        }
    }

    override fun upload(blob: String, description: String, done: (CloudResult<Unit>) -> Unit) {
        val client = PlayGames.getSnapshotsClient(activity)
        open(client).addOnCompleteListener { t ->
            val snapshot = t.snapshot()
            if (snapshot == null) {
                post { done(failure(t.exception)) }
                return@addOnCompleteListener
            }
            snapshot.snapshotContents.writeBytes(blob.toByteArray(Charsets.UTF_8))
            val change = SnapshotMetadataChange.Builder().setDescription(description).build()
            client.commitAndClose(snapshot, change).addOnCompleteListener { c ->
                post { done(if (c.isSuccessful) CloudResult.Ok(Unit) else failure(c.exception)) }
            }
        }
    }

    private fun open(client: SnapshotsClient): Task<SnapshotsClient.DataOrConflict<Snapshot>> =
        client.open(SNAPSHOT, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)

    /** The opened snapshot, or null when the call failed or Google still reports a conflict after its policy. */
    private fun Task<SnapshotsClient.DataOrConflict<Snapshot>>.snapshot(): Snapshot? =
        if (isSuccessful) result.data else null

    private fun failure(e: Exception?): CloudResult.Failed {
        val code = (e as? ApiException)?.statusCode
        return when (code) {
            CommonStatusCodes.NETWORK_ERROR, CommonStatusCodes.TIMEOUT -> CloudResult.Failed("offline")
            CommonStatusCodes.SIGN_IN_REQUIRED -> CloudResult.Failed("signed-out")
            null -> CloudResult.Failed("error:${e?.javaClass?.simpleName ?: "unknown"}")
            else -> CloudResult.Failed("error:$code")
        }
    }

    private fun post(f: () -> Unit) = Gdx.app.postRunnable(f)

    private companion object {
        const val SNAPSHOT = "save"
    }
}
