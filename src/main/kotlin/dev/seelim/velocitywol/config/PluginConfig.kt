/*
 * Copyright (c) 2026. Seedim <hello@seelim.dev>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.seelim.velocitywol.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class PluginConfig(
    @Comment("Holding server where players wait while target wakes up")
    val holdingServer: String = "limbo",

    @Comment("Frequency in seconds to ping the target server to check if it has come online")
    val pingIntervalSeconds: Long = 2,

    @Comment("Frequency in seconds to re-send WoL magic packets while players are in holding server")
    val wolPacketIntervalSeconds: Long = 10,

    @Comment("Timeout in seconds before giving up waiting for the server")
    val wakeTimeoutSeconds: Long = 60,

    @Comment("Action bar messages notification settings")
    val messages: MessageConfig = MessageConfig(),

    @Comment("Servers configured for Wake-on-LAN")
    val servers: Map<String, ServerWoLConfig> = mapOf(
        "survival" to ServerWoLConfig(
            "AA:BB:CC:DD:EE:FF",
            "192.168.1.255",
            9)
    )
)

@ConfigSerializable
data class MessageConfig(

    @Comment("Action bar message shown while waiting for the server to wake up. Supports MiniMessage formatting.")
    val wakingActionBar: String = "⌛ Waking up the server, please wait...",

    @Comment("Action bar message shown during downtime when WoL is disabled")
    val downtimeActionBar: String = "<red>⌚ The server is currently closed for the night.</red>",
)

@ConfigSerializable
data class ServerWoLConfig(
    @Comment("Target machine MAC address (e.g. 00:11:22:33:44:55)")
    val mac: String = "00:00:00:00:00:00",

    @Comment("Broadcast IP address of the local subnet")
    val broadcastIp: String = "255.255.255.255",

    @Comment("WoL UDP target port (usually 7 or 9)")
    val port: Int = 9,

    @Comment("Optional downtime schedule when WoL will not be sent")
    val downtime: DowntimeConfig = DowntimeConfig()
)

@ConfigSerializable
data class DowntimeConfig(
    @Comment("Enable downtime / restricted hours")
    val enabled: Boolean = false,

    @Comment("Timezone for downtime schedule (e.g., 'UTC', 'Europe/Berlin')")
    val timezone: String = "UTC",

    @Comment("Downtime start time in 24h format (HH:mm)")
    val startTime: String = "00:00",

    @Comment("Downtime end time in 24h format (HH:mm)")
    val endTime: String = "08:00",
)
