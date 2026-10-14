package com.danielealbano.androidremotecontrolmcp.data.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("BindAddressValidator")
class BindAddressValidatorTest {
    @Nested
    @DisplayName("accepts")
    inner class Accepts {
        @ParameterizedTest
        @ValueSource(
            strings = [
                "100.64.0.5",
                "100.127.255.254",
                "192.168.1.10",
                "10.0.0.1",
                "127.0.0.1",
                "fd7a:115c:a1e0::1",
                "2001:db8::42",
                "::1",
            ],
        )
        fun `numeric non-wildcard addresses`(address: String) {
            assertEquals(address, BindAddressValidator.validate(address).getOrNull())
        }

        @Test
        fun `trims surrounding whitespace`() {
            assertEquals("100.64.0.5", BindAddressValidator.validate("  100.64.0.5 ").getOrNull())
        }
    }

    @Nested
    @DisplayName("rejects")
    inner class Rejects {
        @ParameterizedTest
        @ValueSource(
            strings = [
                "",
                "   ",
                "localhost",
                "phone.tailnet.ts.net",
                "256.1.1.1",
                "1.2.3",
                "1.2.3.4.5",
                "01.2.3.4",
                "1.2.3.4:8080",
                "1:2:3:4:5:6:7:8:9",
                "fe80::1%wlan0",
                "http://100.64.0.5",
            ],
        )
        fun `non-literal or malformed input`(address: String) {
            assertTrue(BindAddressValidator.validate(address).isFailure, "expected failure for '$address'")
        }

        @ParameterizedTest
        @ValueSource(strings = ["0.0.0.0", "::", "0:0:0:0:0:0:0:0"])
        fun `wildcard addresses`(address: String) {
            val result = BindAddressValidator.validate(address)
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("Network option"))
        }

        @Test
        fun `IPv6 link-local addresses`() {
            assertTrue(BindAddressValidator.validate("fe80::1").isFailure)
        }
    }

    @Nested
    @DisplayName("toUrlHost")
    inner class ToUrlHost {
        @Test
        fun `leaves IPv4 unchanged`() {
            assertEquals("100.64.0.5", BindAddressValidator.toUrlHost("100.64.0.5"))
        }

        @Test
        fun `brackets IPv6`() {
            assertEquals("[fd7a:115c:a1e0::1]", BindAddressValidator.toUrlHost("fd7a:115c:a1e0::1"))
        }
    }
}
