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

package dev.seelim.velocitywol.listener

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.proxy.ProxyServer
import dev.seelim.velocitywol.config.ConfigManager
import dev.seelim.velocitywol.core.WakeService
import org.slf4j.Logger

class ConnectionListener(
    private val proxyServer: ProxyServer,
    private val configManager: ConfigManager,
    private val wakeService: WakeService,
    private val logger: Logger,
    ) {

    /**
     * Fires whenever a connection to a backend server fails or when a backend server disconnects a player.
     */
    @Subscribe
    fun onKickedFromServer(event: KickedFromServerEvent) {
        val config = configManager.config
        val kickedServerName = event.server.serverInfo.name

        if (kickedServerName.equals(config.holdingServer, ignoreCase = true)) {
            return
        }

        if (!config.servers.containsKey(kickedServerName)) {
            return
        }

        val holdingServer = proxyServer.getServer(config.holdingServer).orElse(null)
        if (holdingServer == null) {
            logger.error("Configured server '{}' does not exist in Velocity.", config.holdingServer)
            return
        }

        event.result = KickedFromServerEvent.RedirectPlayer.create(holdingServer)

        wakeService.enqueuePlayer(event.player, kickedServerName)
    }

    /**
     * Cleans up the queue if a player leaves the proxy entirely.
     */
    @Subscribe
    fun onDisconnect(event: DisconnectEvent) {
        wakeService.removePlayer(event.player.uniqueId)
    }

    /**
     * If a player manually switches to another normal server instead of waiting in holding,
     * remove them from the queue so they aren't unexpectedly pulled back.
     */
    @Subscribe
    fun onServerConnected(event: ServerConnectedEvent) {
        val connectedServerName = event.server.serverInfo.name

        if (wakeService.isWaiting(event.player.uniqueId) && !connectedServerName.equals(configManager.config.holdingServer, ignoreCase = true)) {
            wakeService.removePlayer(event.player.uniqueId)
        }
    }
}
