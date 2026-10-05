package com.womenriskmap.feature.profile.domain

/** Spec §4 Ecrã 9: export and delete. Feature-local contract (only Settings needs it). */
interface AccountRepository {
    /** Everything stored about the user, as pretty-printed JSON. */
    suspend fun exportMyData(): Result<String>

    /** Deletes the account and all personal data; reports stay on the map, unlinked (spec §7). */
    suspend fun deleteMyAccount(): Result<Unit>
}
