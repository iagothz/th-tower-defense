package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.DailyChallenge

/**
 * Keeps the local save and the cloud's in step: downloads when the game opens, uploads after every match and
 * deck change, and hands a genuine conflict to the player. The cloud is never applied while a match is on
 * ([inMatch]); the result waits for the menu. Replies from before a disable or a fresh open are dropped.
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

    /** The game sets this on the match and game-over screens; clearing it applies whatever waited. */
    var inMatch = false
        set(value) {
            field = value
            if (!value) pending?.let { pending = null; it() }
        }

    /** A one-line message for the home tab, which clears it once shown. */
    var notice: String? = null

    /** The cloud blob waiting for the player's choice on the conflict screen. */
    var conflict: String? = null
        private set

    /** The game recreates its screens after the cloud save replaces the local one. */
    var onAdopted: () -> Unit = {}

    private var generation = 0
    private var uploading = false
    private var uploadAgain = false
    private var pending: (() -> Unit)? = null

    private val active get() = cloud.available && save.syncEnabled

    /** The opening flow: signed in? → download → decide → act. */
    fun open() {
        if (!active) return
        val gen = ++generation
        status = Status.Working
        cloud.isSignedIn { signed ->
            if (gen != generation) return@isSignedIn
            if (!signed) {
                status = Status.SignedOut
                return@isSignedIn
            }
            cloud.download { r ->
                if (gen == generation) downloaded(r)
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
        pending = null
        conflict = null
        if (on) open() else if (cloud.available) status = Status.Off
    }

    /** Uploads the local save as the next version; a second call during an upload runs once more after it. */
    fun push() {
        if (!active) return
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

    private fun downloaded(r: CloudResult<String?>) {
        when (r) {
            is CloudResult.Failed -> status = failure(r.reason)
            is CloudResult.Ok -> {
                val blob = r.value
                // A blob that doesn't parse ranks below any local version, so the local save goes over it.
                val cloudVersion = blob?.let { if (CloudBlob.decode(it) == null) -1 else CloudBlob.version(it) ?: 0 }
                when (val action = CloudSync.decide(save.syncVersion, save.localChanged(), cloudVersion)) {
                    CloudSync.Action.Nothing -> status = Status.Synced(save.syncLastOk ?: today())
                    is CloudSync.Action.Upload -> upload(action.version)
                    CloudSync.Action.Adopt -> defer { adopt(blob!!) }
                    CloudSync.Action.Ask -> defer { conflict = blob }
                }
            }
        }
    }

    private fun defer(action: () -> Unit) {
        if (inMatch) pending = action else action()
    }

    private fun adopt(blob: String) {
        if (!save.import(blob)) {
            status = Status.Error("blob")
            return
        }
        save.syncVersion = CloudBlob.version(blob) ?: 0
        save.syncHash = CloudBlob.hash(blob)
        save.syncLastOk = today()
        save.clearMatch()
        notice = "Progresso da nuvem aplicado"
        status = Status.Synced(today())
        onAdopted()
    }

    private fun upload(version: Int) {
        if (uploading) {
            uploadAgain = true
            return
        }
        uploading = true
        status = Status.Working
        val gen = generation
        val blob = save.export() + "\ni:sync.version=$version\ni:sync.at=${today()}"
        val s = CloudBlob.summary(blob)
        cloud.upload(blob, S.f("nível %d · %d partidas", s.level, s.games)) { r ->
            uploading = false
            if (gen != generation) {
                uploadAgain = false
                return@upload
            }
            when (r) {
                is CloudResult.Ok -> {
                    save.syncVersion = version
                    save.syncHash = CloudBlob.hash(blob)
                    save.syncLastOk = today()
                    status = Status.Synced(today())
                }
                is CloudResult.Failed -> status = failure(r.reason)
            }
            if (uploadAgain) {
                uploadAgain = false
                push()
            }
        }
    }

    private fun failure(reason: String): Status = when (reason) {
        "offline" -> Status.Offline
        "signed-out" -> Status.SignedOut
        else -> Status.Error(reason)
    }
}
