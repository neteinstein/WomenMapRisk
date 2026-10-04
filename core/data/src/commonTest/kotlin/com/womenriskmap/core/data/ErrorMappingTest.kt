package com.womenriskmap.core.data

import com.womenriskmap.core.data.remote.mapSqlCode
import com.womenriskmap.core.data.remote.remote
import com.womenriskmap.core.data.remote.toDomainException
import com.womenriskmap.core.domain.error.DomainException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class ErrorMappingTest {
    @Test
    fun sql_codes_map_to_domain_errors() {
        assertIs<DomainException.DailyReportLimit>(mapSqlCode("P0001 daily_report_limit"))
        assertIs<DomainException.DuplicateReport>(mapSqlCode("duplicate_report"))
        assertIs<DomainException.InvitesLocked>(mapSqlCode("invites_locked"))
        assertIs<DomainException.InviteLimitReached>(mapSqlCode("invite_limit_reached"))
        assertIs<DomainException.InvalidInvite>(mapSqlCode("invalid_invite"))
        assertIs<DomainException.NotAllowed>(mapSqlCode("permission denied for function x"))
        assertNull(mapSqlCode("something else"))
    }

    @Test
    fun io_errors_are_network_errors() {
        assertIs<DomainException.Network>(IOException("offline").toDomainException())
    }

    @Test
    fun remote_wraps_failures_but_rethrows_cancellation() {
        assertIs<DomainException.Network>(remote { throw IOException("x") }.exceptionOrNull())
        assertFailsWith<CancellationException> { remote { throw CancellationException("stop") } }
    }
}
