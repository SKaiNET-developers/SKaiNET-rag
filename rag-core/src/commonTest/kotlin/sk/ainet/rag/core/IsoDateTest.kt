package sk.ainet.rag.core

import sk.ainet.rag.core.document.IsoDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IsoDateTest {

    @Test
    fun `the epoch itself is day zero`() {
        assertEquals(0L, IsoDate.epochDay("1970-01-01"))
    }

    @Test
    fun `known days match the civil calendar`() {
        assertEquals(19_358L, IsoDate.epochDay("2023-01-01"))
        assertEquals(-1L, IsoDate.epochDay("1969-12-31"))
        // Across the 2024 leap day: 2024-03-01 minus 2024-02-28 is two days.
        assertEquals(2L, IsoDate.epochDay("2024-03-01")!! - IsoDate.epochDay("2024-02-28")!!)
    }

    @Test
    fun `leap rules admit and reject the right february days`() {
        assertEquals(1L, IsoDate.epochDay("2024-02-29")!! - IsoDate.epochDay("2024-02-28")!!)
        assertNull(IsoDate.epochDay("2023-02-29"))
        assertNull(IsoDate.epochDay("1900-02-29")) // century, not divisible by 400
        assertEquals(1L, IsoDate.epochDay("2000-02-29")!! - IsoDate.epochDay("2000-02-28")!!)
    }

    @Test
    fun `malformed input reads as null, not garbage`() {
        assertNull(IsoDate.epochDay("not a date"))
        assertNull(IsoDate.epochDay("2026-13-01"))
        assertNull(IsoDate.epochDay("2026-04-31"))
        assertNull(IsoDate.epochDay("2026-1-01"))
    }
}
