package com.iagothz.mergetd.ui

/** A cloud call's outcome; [Failed.reason] is "offline", "signed-out" or "error:<code>". */
sealed interface CloudResult<out T> {
    data class Ok<T>(val value: T) : CloudResult<T>
    data class Failed(val reason: String) : CloudResult<Nothing>
}

/**
 * Where the account's save lives online. Every callback is delivered on the GL thread; nothing here blocks.
 * The Android module implements it with Play Games Saved Games; everything else uses [NoCloud].
 */
interface CloudSave {
    /** False where there is no cloud at all (desktop, simulator): the settings row is hidden. */
    val available: Boolean

    /** Opens the platform's sign-in; false when the player refused or it failed. */
    fun signIn(done: (Boolean) -> Unit)

    fun isSignedIn(done: (Boolean) -> Unit)

    /** The stored blob, or null when the account has none yet. */
    fun download(done: (CloudResult<String?>) -> Unit)

    fun upload(blob: String, description: String, done: (CloudResult<Unit>) -> Unit)
}

object NoCloud : CloudSave {
    override val available = false
    override fun signIn(done: (Boolean) -> Unit) = done(false)
    override fun isSignedIn(done: (Boolean) -> Unit) = done(false)
    override fun download(done: (CloudResult<String?>) -> Unit) = done(CloudResult.Failed("unavailable"))
    override fun upload(blob: String, description: String, done: (CloudResult<Unit>) -> Unit) = done(CloudResult.Failed("unavailable"))
}
