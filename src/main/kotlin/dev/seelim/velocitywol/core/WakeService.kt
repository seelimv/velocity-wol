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

package dev.seelim.velocitywol.core

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import dev.seelim.velocitywol.config.ConfigManager
import dev.seelim.velocitywol.config.DowntimeEvaluator
import dev.seelim.velocitywol.network.WakeOnLanSender
import net.kyori.adventure.text.minimessage.MiniMessage
import org.slf4j.Logger
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Clock
import kotlin.time.Instant

class WakeService(
    private val proxyServer: ProxyServer,
    private val configManager: ConfigManager,
    private val wolSender: WakeOnLanSender,
    private val logger: Logger,
) {
    private val miniMessage = MiniMessage.miniMessage()

    /** Tracks active wake sessions by target server name (e.g. "survival" -> WakeSession) */
    private val activeSessions = ConcurrentHashMap<String, WakeSession>()

    /** Maps waiting player UUID to their target server name */
    private val waitingPlayers = ConcurrentHashMap<UUID, String>()

    data class WakeSession(
        val serverName: String,
        val startedAt: Instant = Clock.System.now(),
        var lastWolSentAt: Instant = Clock.System.now(),
        val playerUuids: MutableSet<UUID> = ConcurrentHashMap.newKeySet(),
        val transferred: AtomicBoolean = AtomicBoolean(false),
        )

    /**
     * Enqueues a player in the holding server to wait for a target server.
     * Starts the wake cycle and sends the initial WoL packet if not in downtime.
     */
    fun enqueuePlayer(player: Player, targetServerName: String) {
        removePlayer(player.uniqueId)

        val config = configManager.config
        val serverConfig = config.servers[targetServerName] ?: return

        waitingPlayers[player.uniqueId] = targetServerName

        var initialWolNeeded = false
        val session = activeSessions.computeIfAbsent(targetServerName) {
            initialWolNeeded = !DowntimeEvaluator.isDowntime(serverConfig.downtime)
            WakeSession(serverName = targetServerName)
        }

        if (initialWolNeeded) {
            wolSender.send(serverConfig.mac, serverConfig.broadcastIp, serverConfig.port)
        }

        session.playerUuids.add(player.uniqueId)
        logger.info("Player {} queued for server '{}' (Waiting: {})", player.username, targetServerName, session.playerUuids.size)
    }

    /**
     * Removes a player when they disconnect from the proxy or leave the holding server.
     */
    fun removePlayer(playerUuid: UUID) {
        val targetServerName = waitingPlayers.remove(playerUuid) ?: return
        val session = activeSessions[targetServerName] ?: return

        session.playerUuids.remove(playerUuid)

        if (session.playerUuids.isEmpty()) {
            activeSessions.remove(targetServerName)
                logger.info("No players waiting for '{}'. Canceled wake cycle.", targetServerName)
        }
    }

    fun isWaiting(playerUuid: UUID): Boolean = waitingPlayers.containsKey(playerUuid)

    fun getTargetServer(playerUuid: UUID): String? = waitingPlayers[playerUuid]

    /**
     * Periodically pings all servers with active sessions.
     */
    fun tickPings() {
        val config = configManager.config

        for ((serverName, session) in activeSessions) {
            val registeredServer = proxyServer.getServer(serverName).orElse(null)
            if (registeredServer == null) {
                logger.warn("Configured WoL server '{}' is not registered in Velocity proxy!", serverName)
                continue
            }

            logger.info("Pinging target server '{}'...", serverName)

            registeredServer.ping().whenComplete { pingResult, error ->
                if (error == null && pingResult != null) {
                    if (session.transferred.compareAndSet(false, true)) {
                        logger.info("Server '{}' is now online! Transferring {} waiting players...", serverName, session.playerUuids.size)
                        transferWaitingPlayers(session, registeredServer)
                    }
                } else {
                    handleServerStillOffline(session)
                }
            }
        }
    }

    private fun handleServerStillOffline(session: WakeSession) {
        val config = configManager.config
        val serverConfig = config.servers[session.serverName] ?: return
        val now = Clock.System.now()

        val elapsedSeconds = (now - session.startedAt).inWholeSeconds
        if (elapsedSeconds >= config.wakeTimeoutSeconds) {
            logger.warn("Wake cycle for '{}' timed out after {}s without response.", session.serverName, elapsedSeconds)
            activeSessions.remove(session.serverName)
            session.playerUuids.forEach { waitingPlayers.remove(it) }
            return
        }

        val isDowntime = DowntimeEvaluator.isDowntime(serverConfig.downtime)
        val timeSinceLastWol = (now - session.lastWolSentAt).inWholeSeconds

        if (!isDowntime && timeSinceLastWol >= config.wolPacketIntervalSeconds && session.playerUuids.isNotEmpty()) {
            wolSender.send(serverConfig.mac, serverConfig.broadcastIp, serverConfig.port)
            session.lastWolSentAt = now
        }
    }

    private fun transferWaitingPlayers(
        session: WakeSession,
        targetServer: RegisteredServer,
    ) {
        activeSessions.remove(session.serverName)

        for (uuid in session.playerUuids) {
            waitingPlayers.remove(uuid)
            proxyServer.getPlayer(uuid).ifPresent { player ->
                player.createConnectionRequest(targetServer).connect().thenAccept { result ->
                    if (!result.isSuccessful) {
                        logger.warn("Failed to connect {} to {}: {}", player.username, session.serverName, result.reasonComponent)
                    }
                }
            }
        }
    }

    fun tickActionBar() {
        val config = configManager.config

        for ((uuid, serverName) in waitingPlayers) {
            val player = proxyServer.getPlayer(uuid).orElse(null) ?: continue
            val serverConfig = config.servers[serverName] ?: continue

            val isDowntime = DowntimeEvaluator.isDowntime(serverConfig.downtime)
            val messageString = if (isDowntime) {
                config.messages.downtimeActionBar
            } else {
                config.messages.wakingActionBar
            }

            player.sendActionBar(miniMessage.deserialize(messageString))
        }
    }
}