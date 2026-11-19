package com.zuramai.rencana.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackendUrlTest {
    @Test
    fun stripsTrailingSlashAndTrims() {
        assertEquals("https://api.example.com", BackendUrl.parse(" https://api.example.com/ ", false).getOrThrow())
        assertEquals("https://api.example.com/v1", BackendUrl.parse("https://api.example.com/v1/", false).getOrThrow())
    }

    @Test
    fun httpOnlyWhenAllowed() {
        assertTrue(BackendUrl.parse("http://10.0.2.2:8787", false).isFailure)
        assertEquals("http://10.0.2.2:8787", BackendUrl.parse("http://10.0.2.2:8787", true).getOrThrow())
    }

    @Test
    fun rejectsInvalid() {
        for (bad in listOf("ftp://x", "https://user@x", "https://user:pw@x.com", "https://x?a=1", "https://x#frag", "", "   ", "api.example.com", "https://")) {
            assertTrue(BackendUrl.parse(bad, true).isFailure, "should reject '$bad'")
        }
    }
}
