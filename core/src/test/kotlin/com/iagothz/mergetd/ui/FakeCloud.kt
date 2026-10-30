package com.iagothz.mergetd.ui

/** An in-memory cloud: one blob, flags for the failure modes, and optionally held-back replies. */
class FakeCloud : CloudSave {
    override var available = true
    var signedIn = true
    var offline = false
    var blob: String? = null
    val uploads = mutableListOf<String>()
    val descriptions = mutableListOf<String>()

    /** When true, replies wait in [pending] until [deliver] runs them, one per call. */
    var deliverLater = false
    private val pending = ArrayDeque<() -> Unit>()

    fun deliver() {
        pending.removeFirstOrNull()?.invoke()
    }

    private fun reply(f: () -> Unit) {
        if (deliverLater) pending.addLast(f) else f()
    }

    override fun signIn(done: (Boolean) -> Unit) = reply { done(signedIn) }

    override fun isSignedIn(done: (Boolean) -> Unit) = reply { done(signedIn) }

    override fun download(done: (CloudResult<String?>) -> Unit) = reply {
        done(
            when {
                !signedIn -> CloudResult.Failed("signed-out")
                offline -> CloudResult.Failed("offline")
                else -> CloudResult.Ok(blob)
            },
        )
    }

    override fun upload(blob: String, description: String, done: (CloudResult<Unit>) -> Unit) = reply {
        when {
            !signedIn -> done(CloudResult.Failed("signed-out"))
            offline -> done(CloudResult.Failed("offline"))
            else -> {
                this.blob = blob
                uploads += blob
                descriptions += description
                done(CloudResult.Ok(Unit))
            }
        }
    }
}
