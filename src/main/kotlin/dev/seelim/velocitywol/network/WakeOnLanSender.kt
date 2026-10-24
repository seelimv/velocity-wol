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

package dev.seelim.velocitywol.network

import org.slf4j.Logger
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class WakeOnLanSender(private val logger: Logger) {

    /**
     * Parses and builds a standard 102-byte Wake-on-LAN magic packet:
     * - 6 bytes of 0xFF
     * - 16 repetitions of the 6-byte target MAC address
     */
    fun createMagicPacket(macAddress: String): ByteArray {
        val macBytes = parseMac(macAddress)
        val packet = ByteArray(6 + 16 * 6)

        for (i in 0 until 6) {
            packet[i] = 0xFF.toByte()
        }

        for (i in 0 until 16) {
            System.arraycopy(macBytes, 0, packet, 6 + i * 6, 6)
        }

        return packet
    }

    /**
     * Sends the magic packet via UDP broadcast.
     */
    fun send(macAddress: String, broadcastIp: String, port: Int): Boolean {
        return try {
            val packetBytes = createMagicPacket(macAddress)
            val targetAddress = InetAddress.getByName(broadcastIp)

            DatagramSocket().use { socket ->
                socket.broadcast = true
                val packet = DatagramPacket(packetBytes, packetBytes.size, targetAddress, port)
                socket.send(packet)
            }

            logger.info("Sent WoL magic packet to {} via {}:{}", macAddress, targetAddress, port)
            true
        } catch (e: Exception) {
            logger.error("Failed to send WoL magic packet to {} via {}:{}; {}", macAddress, broadcastIp, port, e.message)
            false
        }
    }

    /**
     * Parses standard MAC formats like 'AA:BB:CC:DD:EE:FF' or 'AA-BB-CC-DD-EE-FF'.
     */
    fun parseMac(macAddress: String) : ByteArray {
        val cleanMac = macAddress.replace(":", "").replace("-", "").trim()
        require(cleanMac.length == 12) {
            "Invalid MAC address '$macAddress': expected 12 hexadecimal characters"
        }

        return ByteArray(6) {i ->
            val hexByte = cleanMac.substring(i * 2, i * 2 + 2)
            hexByte.toIntOrNull(16)?.toByte()
                ?: throw IllegalArgumentException("Invalid hex character in MAC address: '$macAddress'")
        }
    }
}
