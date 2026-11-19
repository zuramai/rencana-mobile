package com.zuramai.rencana.data

import kotlin.test.Test
import kotlin.test.assertEquals


class PkceTest {
    @Test
    fun rfc7636AppendixB() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", Pkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
    }

    @Test
    fun randomTokenShape() {
        val t = Pkce.randomToken()
        assertEquals(43, t.length)
        assertEquals(false, t == Pkce.randomToken())
    }
}
