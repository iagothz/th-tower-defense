package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.DailyChallenge

/**
 * Keeps the local save and the cloud's in step: downloads when the game opens or resumes, uploads after every
 * match and deck change, and hands a genuine conflict to the player. The cloud is never applied while a match
 * is on ([inMatch]); the result waits for the next [tick] outside one. An upload only goes out once this session
 * has seen the cloud's version, and the cloud refuses a version it already has, so the other device's progress
 * is never overwritten without a look at it first. Replies from before a disable or a fresh open are dropped.
 */
class CloudController(private val save: SaveData, private val cloud: CloudSave, private val today: () -> Int = DailyChallenge::today) {
    sealed interface Status {
        data object Unavailable : Status
        data object Off : Status
        data object SignedOut : Status
        data object Working : Status
        data class Synced(val day: Int) : Status
        data object Offline : Status
        data class Error(val reason: String) : Status
    }

    var status: Status = when {
        !cloud.available -> Status.Unavailable
        !save.syncEnabled -> Status.Off
        else -> Status.SignedOut
    }
        private set

    /** The game sets this on the match and game-over screens; whatever waited runs on the next tick after. */
    var inMatch = false

    /** A one-line message for the home tab, which clears it once shown. */
    var notice: String? = null

    /** The cloud blob waiting for the player's choice on the conflict screen. */
    var conflict: String? = null
        private set

    /** The game recreates its screens after the cloud save replaces the local one. */
    var onAdopted: () -> Unit = {}

    /** The game opens the conflict screen with the cloud blob; called only outside a match. */
    var onConflict: (String) -> Unit = {}

    private var generation = 0
    private var opening = false
    private var pushAfterOpen = false
    private var uploading = false

    /** The highest version asked for while an upload was in flight; sent once it finishes. */
    private var queued: Int? = null
    private var pending: (() -> Unit)? = null

    /** The cloud version this session last saw (0 for an empty cloud); null until a download or upload succeeds. */
    private var knownCloudVersion: Int? = null

    /** Seconds spent in [Status.Working]; past [TIMEOUT] the call is given up on. */
    private var working = 0f

    private val active get() = cloud.available && save.syncEnabled

    /** Called every frame: runs what waited for the match to end, and times out a call that never answers. */
    fun tick(delta: Float) {
        if (!inMatch) {
            pending?.let {
                pending = null
                it()
            }
        }
        if (status != Status.Working) {
            working = 0f
            return
        }
        working += delta
        if (working < TIMEOUT) return
        working = 0f
        generation++
        opening = false
        uploading = false
        queued = null
        status = Status.Error("timeout")
    }

    /** The opening flow: signed in? → download → decide → act. Also runs when the app comes back to the front. */
    fun open() {
        if (!active) return
        val gen = ++generation
        opening = true
        status = Status.Working
        cloud.isSignedIn { signed ->
            if (gen != generation) return@isSignedIn
            if (!signed) {
                status = Status.SignedOut
                opened()
                return@isSignedIn
            }
            cloud.download { r ->
                if (gen != generation) return@download
                downloaded(r)
                opened()
            }
        }
    }

    fun signIn() {
        if (!cloud.available) return
        val gen = ++generation
        status = Status.Working
        cloud.signIn { ok ->
            if (gen != generation) return@signIn
            if (!ok) {
                status = Status.SignedOut
            } else {
                if (!save.syncEnabled) save.syncEnabled = true
                open()
            }
        }
    }

    fun setEnabled(on: Boolean) {
        save.syncEnabled = on
        generation++
        opening = false
        uploading = false
        queued = null
        pending = null
        conflict = null
        if (on) {
            open()
        } else if (cloud.available) {
            status = Status.Off
        }
    }

    /**
     * Uploads the local save as the next version. Before this session has seen the cloud, it reads it first
     * (and the decision table uploads if the local save moved); while a conflict waits, nothing goes out.
     */
    fun push() {
        if (!active || conflict != null) return
        if (opening) {
            pushAfterOpen = true
            return
        }
        if (knownCloudVersion == null) {
            open()
            return
        }
        upload(save.syncVersion + 1)
    }

    fun keepLocal() {
        val blob = conflict ?: return
        conflict = null
        upload((CloudBlob.version(blob) ?: 0) + 1)
    }

    fun keepCloud() {
        val blob = conflict ?: return
        conflict = null
        adopt(blob)
    }

    private fun opened() {
        opening = false
        if (pushAfterOpen) {
            pushAfterOpen = false
            push()
        }
    }

    private fun downloaded(r: CloudResult<String?>) {
        when (r) {
            is CloudResult.Failed -> status = failure(r.reason)
            is CloudResult.Ok -> {
                val blob = r.value
                // A blob that doesn't parse ranks below any local version, so the local save goes over it.
                val cloudVersion = blob?.let { if (CloudBlob.decode(it) == null) -1 else CloudBlob.version(it) ?: 0 }
                knownCloudVersion = maxOf(0, cloudVersion ?: 0)
                when (val action = CloudSync.decide(save.syncVersion, save.localChanged(), cloudVersion)) {
                    CloudSync.Action.Nothing -> status = Status.Synced(save.syncLastOk ?: today())
                    is CloudSync.Action.Upload -> upload(action.version)
                    CloudSync.Action.Adopt -> defer { adopt(blob!!) }
                    CloudSync.Action.Ask -> defer {
                        conflict = blob
                        onConflict(blob!!)
                    }
                }
            }
        }
    }

    /** Runs [action] on the next tick, or later if a match is on: never inside the caller's screen switch. */
    private fun defer(action: () -> Unit) {
        pending = action
    }

    private fun adopt(blob: String) {
        if (!save.import(blob)) {
            status = Status.Error("blob")
            return
        }
        save.syncVersion = CloudBlob.version(blob) ?: 0
        save.syncHash = CloudBlob.hash(blob)
        save.syncLastOk = today()
        knownCloudVersion = save.syncVersion
        save.clearMatch()
        notice = "Progresso da nuvem aplicado"
        status = Status.Synced(today())
        onAdopted()
    }

    private fun upload(version: Int) {
        if (uploading) {
            queued = maxOf(queued ?: 0, version)
            return
        }
        uploading = true
        status = Status.Working
        val gen = generation
        val blob = save.export() + "\ni:sync.version=$version\ni:sync.at=${today()}"
        val s = CloudBlob.summary(blob)
        cloud.upload(blob, S.f("nível %d · %d partidas", s.level, s.games)) { r ->
            // A success is booked even after a timeout gave up on it: the cloud really holds this version now.
            if (r is CloudResult.Ok && version > save.syncVersion) {
                save.syncVersion = version
                save.syncHash = CloudBlob.hash(blob)
                save.syncLastOk = today()
                knownCloudVersion = version
            }
            if (gen != generation) return@upload
            uploading = false
            when (r) {
                is CloudResult.Ok -> status = Status.Synced(today())
                is CloudResult.Failed -> if (r.reason == "stale") {
                    // The cloud moved past us: read it and let the decision table settle it.
                    queued = null
                    open()
                    return@upload
                } else {
                    status = failure(r.reason)
                }
            }
            queued?.let {
                queued = null
                upload(maxOf(it, save.syncVersion + 1))
            }
        }
    }

    companion object {
        /** Seconds a cloud call may take before the row shows a failure and the next push is allowed. */
        const val TIMEOUT = 20f
    }

    private fun failure(reason: String): Status = when (reason) {
        "offline" -> Status.Offline
        "signed-out" -> Status.SignedOut
        else -> Status.Error(reason)
    }
}
