package dev.havlicektomas.notemark.auth.domain

/**
 * Persists the session across launches. Backed by encrypted storage so the
 * tokens are never written in plaintext. Survives logout-without-sync per the
 * offline-first rules in later milestones.
 */
interface SessionStorage {
    suspend fun get(): AuthInfo?
    suspend fun set(info: AuthInfo?)
    suspend fun clear()
}
