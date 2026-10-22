package com.jetbrains.otp.freeze

import com.intellij.diagnostic.ThreadDump
import org.junit.Assert.assertEquals
import org.junit.Test

class FreezeDumpSelectionTest {
    @Test
    fun `keeps all dumps when there are at most four`() {
        for (count in 0..4) {
            val dumps = dumps(count)
            assertEquals("Dump count: $count", dumps, selectFreezeDumps(dumps))
        }
    }

    @Test
    fun `selects first last and evenly spaced middle dumps`() {
        val dumps = dumps(10)
        assertEquals(listOf(dumps[0], dumps[3], dumps[6], dumps[9]), selectFreezeDumps(dumps))
    }

    @Test
    fun `rounds intermediate positions for six dumps`() {
        val dumps = dumps(6)
        assertEquals(listOf(dumps[0], dumps[2], dumps[3], dumps[5]), selectFreezeDumps(dumps))
    }

    @Test
    fun `keeps distinct samples just above the limit`() {
        val dumps = dumps(5)
        assertEquals(listOf(dumps[0], dumps[1], dumps[3], dumps[4]), selectFreezeDumps(dumps))
    }

    @Test
    fun `supports collections that are not lists`() {
        val dumps = dumps(10)
        assertEquals(listOf(dumps[0], dumps[3], dumps[6], dumps[9]), selectFreezeDumps(LinkedHashSet(dumps)))
    }

    private fun dumps(count: Int): List<ThreadDump> =
        List(count) { ThreadDump("dump ${it + 1}", emptyArray(), emptyArray()) }
}
