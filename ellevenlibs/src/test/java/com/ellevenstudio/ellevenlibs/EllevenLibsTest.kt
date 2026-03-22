package com.ellevenstudio.ellevenlibs

import org.junit.Assert.assertEquals
import org.junit.Test

class EllevenLibsTest {
    @Test
    fun testVersion() {
        assertEquals("1.0.0", EllevenLibs.VERSION)
    }
}
