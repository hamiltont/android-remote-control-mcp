package com.danielealbano.androidremotecontrolmcp.data.model

/**
 * Represents the network binding address for the MCP server.
 *
 * @property address The IP address string to bind to. Empty for [SPECIFIC], whose address is
 *   user-supplied and stored separately in [ServerConfig.customBindingAddress]; always resolve the
 *   address to bind through [ServerConfig.bindHost] rather than reading this property directly.
 */
enum class BindingAddress(
    val address: String,
) {
    /** Bind to localhost only. Requires ADB port forwarding for access. */
    LOCALHOST("127.0.0.1"),

    /** Bind to all network interfaces. Accessible on the local network. */
    NETWORK("0.0.0.0"),

    /**
     * Bind to one user-supplied IP address (e.g. a VPN or Tailscale interface address), so the server
     * is reachable only on the interface that owns that address instead of on every interface.
     */
    SPECIFIC(""),
    ;

    companion object {
        /**
         * Returns the fixed-address [BindingAddress] matching the given [address] string, or [LOCALHOST]
         * if not found. Never returns [SPECIFIC], which has no fixed address.
         */
        fun fromAddress(address: String): BindingAddress {
            val match = entries.firstOrNull { it != SPECIFIC && it.address == address }
            return match ?: LOCALHOST
        }
    }
}
