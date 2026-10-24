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

import org.slf4j.LoggerFactory
import org.slf4j.helpers.NOPLogger
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigManagerTest {

    private val logger = LoggerFactory.getLogger(ConfigManagerTest::class.java)
    private lateinit var tempDir: Path

    @BeforeTest
    fun setUp() {
        tempDir = createTempDirectory("velocity-wol-test")
    }

    @AfterTest
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `creates default config when file does not exist`() {
        val manager = ConfigManager(tempDir, logger)
        val config = manager.load()

        val expectedConfigFile = tempDir.resolve("config.conf")
        assertTrue(expectedConfigFile.exists(), "config.conf should be created automatically")

        assertEquals("limbo", config.holdingServer)
        assertEquals(2, config.pingIntervalSeconds)
        assertEquals(10, config.wolPacketIntervalSeconds)
        assertEquals(60, config.wakeTimeoutSeconds)

        assertEquals("⌛ Waking up the server, please wait...", config.messages.wakingActionBar)
        assertEquals("<red>⌚ The server is currently closed for the night.</red>", config.messages.downtimeActionBar)

        val survival = config.servers["survival"]
        assertNotNull(survival)
        assertEquals("AA:BB:CC:DD:EE:FF", survival.mac)
        assertEquals("192.168.1.255", survival.broadcastIp)
        assertEquals(9, survival.port)
        assertFalse(survival.downtime.enabled)
        assertEquals("00:00", survival.downtime.startTime)
        assertEquals("08:00", survival.downtime.endTime)
        assertEquals("UTC", survival.downtime.timezone)
    }

    @Test
    fun `load custom configuration from existing file`() {
        val configFile = tempDir.resolve("config.conf")
        configFile.writeText(
            """
                holding-server = "hub-limbo"
                ping-interval-seconds = 5
                wol-packet-interval-seconds = 15
                wake-timeout-seconds = 120
                
                messages {
                    waking-action-bar = "Starting up..."
                    downtime-action-bar = "Closed!"
                }
                
                servers {
                    survival {
                        mac = "AA:BB:CC:DD:EE:FF"
                        broadcast-ip = "192.168.1.255"
                        port = 7
                        downtime {
                            enabled = true
                            start-time = "22:00"
                            end-time = "06:00"
                            timezone = "Europe/Berlin"
                        }
                    }
                }
            """.trimIndent()
        )

        val manager = ConfigManager(tempDir, logger)
        val config = manager.load()

        assertEquals("hub-limbo", config.holdingServer)
        assertEquals(5, config.pingIntervalSeconds)
        assertEquals(15, config.wolPacketIntervalSeconds)
        assertEquals(120, config.wakeTimeoutSeconds)

        assertEquals("Starting up...", config.messages.wakingActionBar)
        assertEquals("Closed!", config.messages.downtimeActionBar)

        val survivalServer = config.servers["survival"]
        assertNotNull(survivalServer, "Survival server entry should exist")
        assertEquals("AA:BB:CC:DD:EE:FF", survivalServer.mac)
        assertEquals("192.168.1.255", survivalServer.broadcastIp)
        assertEquals(7, survivalServer.port)

        assertTrue(survivalServer.downtime.enabled)
        assertEquals("22:00", survivalServer.downtime.startTime)
        assertEquals("06:00", survivalServer.downtime.endTime)
        assertEquals("Europe/Berlin", survivalServer.downtime.timezone)
    }

    @Test
    fun `reload updates configuration when file is modified`() {
        val configFile = tempDir.resolve("config.conf")
        configFile.writeText("""holding-server = "first-limbo"""")

        val manager = ConfigManager(tempDir, logger)
        assertEquals("first-limbo", manager.load().holdingServer)

        configFile.writeText("""holding-server = "second-limbo"""")
        val reloaded = manager.reload()

        assertEquals("second-limbo", reloaded.holdingServer)
        assertEquals("second-limbo", manager.config.holdingServer)
    }

    @Test
    fun `partial configuration falls back to defaults for missing fields`() {
        val configFile = tempDir.resolve("config.conf")
        configFile.writeText(
            """
                holding-server = "custom-limbo"
                servers {
                    creative {
                        mac = "11:22:33:44:55:66"
                    }
                }
            """.trimIndent()
        )

        val manager = ConfigManager(tempDir, logger)
        val config = manager.load()

        assertEquals("custom-limbo", config.holdingServer)
        assertEquals(60, config.wakeTimeoutSeconds)

        val creative = config.servers["creative"]
        assertNotNull(creative)
        assertEquals("11:22:33:44:55:66", creative.mac)
        assertEquals("255.255.255.255", creative.broadcastIp)
        assertEquals(9, creative.port)
        assertFalse(creative.downtime.enabled)
    }

    @Test
    fun `falls back to default config when file syntax is malformed`() {
        val configFile = tempDir.resolve("config.conf")
        configFile.writeText("{ malformed json / hocon :::")

        val manager = ConfigManager(tempDir, NOPLogger.NOP_LOGGER)
        val config = manager.load()

        assertEquals("limbo", config.holdingServer)
        assertEquals(60, config.wakeTimeoutSeconds)
    }
}