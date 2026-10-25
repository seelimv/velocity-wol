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

package dev.seelim.velocitywol

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import dev.seelim.velocitywol.config.ConfigManager
import dev.seelim.velocitywol.core.WakeService
import dev.seelim.velocitywol.network.WakeOnLanSender
import org.slf4j.Logger
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class VelocityWoL @Inject constructor(
    val server: ProxyServer,
    val logger: Logger,
    @DataDirectory val dataDirectory: Path
) {
    val configManager = ConfigManager(dataDirectory, logger)
    val wolSender = WakeOnLanSender(logger)

    lateinit var wakeService: WakeService
        private set

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        val config = configManager.load()
        wakeService = WakeService(server, configManager, wolSender, logger)

        server.scheduler.buildTask(this, Runnable { wakeService.tickPings() })
            .repeat(config.pingIntervalSeconds, TimeUnit.SECONDS)
            .schedule()

        server.scheduler
            .buildTask(this, Runnable { wakeService.tickActionBar() })
            .repeat(1L, TimeUnit.SECONDS)
            .schedule()

        logger.info("Velocity-WoL initialized successfully!")
    }
}
