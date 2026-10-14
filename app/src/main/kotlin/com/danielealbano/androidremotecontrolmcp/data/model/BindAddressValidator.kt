package com.danielealbano.androidremotecontrolmcp.data.model

import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Validates a user-supplied address for [BindingAddress.SPECIFIC].
 *
 * Only numeric IP literals are accepted. Hostnames are rejected outright, so validation never
 * performs a DNS lookup and a hostname can never silently resolve to an unexpected interface.
 * Wildcard addresses (`0.0.0.0`, `::`) are rejected because they bind every interface — that is
 * what [BindingAddress.NETWORK] is for. IPv6 zone IDs (`fe80::1%wlan0`) are not supported.
 *
 * Pure JVM (no Android APIs) so it can be unit tested directly.
 */
object BindAddressValidator {
    private const val IPV4_OCTET = "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)"
    private val IPV4_REGEX = Regex("^$IPV4_OCTET(\\.$IPV4_OCTET){3}$")
    private val IPV6_GROUP_REGEX = Regex("^[0-9A-Fa-f]{1,4}$")
    private const val IPV6_GROUPS = 8
    private const val MAX_IPV6_LENGTH = 45

    /**
     * @return [Result.success] with the trimmed address, or [Result.failure] with an
     *   [IllegalArgumentException] whose message is suitable for display.
     */
    @Suppress("ReturnCount")
    fun validate(raw: String): Result<String> {
        val address = raw.trim()
        if (address.isEmpty()) {
            return failure("Enter the IP address to bind to")
        }
        val isLiteral = IPV4_REGEX.matches(address) || isIpv6Literal(address)
        val parsed: InetAddress =
            (if (isLiteral) parseLiteral(address) else null)
                ?: return failure("Must be a numeric IPv4 or IPv6 address (hostnames are not supported)")

        if (parsed.isAnyLocalAddress) {
            return failure("Wildcard addresses bind every interface; use the Network option instead")
        }
        if (parsed is Inet6Address && parsed.isLinkLocalAddress) {
            return failure("IPv6 link-local addresses are not supported; use a global or ULA address")
        }
        return Result.success(address)
    }

    /**
     * Strict textual IPv6 check (RFC 4291 section 2.2 forms, optional trailing dotted IPv4, no zone ID).
     * Done by hand rather than via [InetAddress] because Android's [InetAddress.getByName] falls back
     * to DNS for strings that are not valid literals, and this must never trigger a lookup.
     */
    @Suppress("ReturnCount")
    private fun isIpv6Literal(address: String): Boolean {
        if (address.length > MAX_IPV6_LENGTH || !address.contains(':')) return false
        val halves = address.split("::")
        if (halves.size > 2) return false
        val groups = halves.map { half -> if (half.isEmpty()) emptyList() else half.split(':') }
        val all = groups.flatten()
        var groupCount = all.size
        all.forEachIndexed { index, group ->
            val isLast = index == all.lastIndex
            if (isLast && group.contains('.')) {
                if (!IPV4_REGEX.matches(group)) return false
                groupCount += 1 // an embedded IPv4 occupies two 16-bit groups
            } else if (!IPV6_GROUP_REGEX.matches(group)) {
                return false
            }
        }
        return if (halves.size == 2) groupCount < IPV6_GROUPS else groupCount == IPV6_GROUPS
    }

    /**
     * Parses an IP literal. Callers must have already checked [address] is a strict numeric literal,
     * which guarantees [InetAddress.getByName] parses it without a DNS lookup on any platform.
     */
    private fun parseLiteral(address: String): InetAddress? =
        try {
            InetAddress.getByName(address)
        } catch (_: UnknownHostException) {
            null
        }

    private fun failure(message: String): Result<String> = Result.failure(IllegalArgumentException(message))

    /** Formats [address] for use as a URL host, bracketing IPv6 literals. */
    fun toUrlHost(address: String): String = if (address.contains(':')) "[$address]" else address
}
