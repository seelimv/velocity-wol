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

import org.slf4j.helpers.NOPLogger
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WakeOnLanSenderTest {

    private val sender = WakeOnLanSender(NOPLogger.NOP_LOGGER)

    @Test
    fun `parseMac parses colon and hyphen separated MAC addresses correctly`() {
        val expected = byteArrayOf(
            0x12.toByte(), 0x34.toByte(), 0x56.toByte(),
            0x78.toByte(), 0x9A.toByte(), 0xBC.toByte(),
        )

        assertContentEquals(expected, sender.parseMac("12:34:56:78:9A:BC"))
        assertContentEquals(expected, sender.parseMac("12-34-56-78-9a-bc"))
        assertContentEquals(expected, sender.parseMac("123456789abc"))
    }

    @Test
    fun `parseMac rejects invalid MAC address`() {
        assertFailsWith<IllegalArgumentException> {
            sender.parseMac("00:11:22:33:44")
        }

        assertFailsWith<IllegalArgumentException> {
            sender.parseMac("00:11:22:33:44:55:66")
        }

        assertFailsWith<IllegalArgumentException> {
            sender.parseMac("ZZ:11:22:33:44:55")
        }
    }

    @Test
    fun `createMagicPacket creates 102 byte payload with preamble and 16 repetitions`() {
        val mac = "AA:BB:CC:DD:EE:FF"
        val packet = sender.createMagicPacket(mac)

        assertEquals(102, packet.size)

        for (i in 0 until 6) {
            assertEquals(0xFF.toByte(), packet[i], "Byte at index $i should be 0xFF")
        }

        val expectedMacBytes = byteArrayOf(
            0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(),
            0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte(),
        )

        for (repetition in 0 until 16) {
            val offset = 6 + repetition * 6
            val slice = packet.copyOfRange(offset, offset + 6)
            assertContentEquals(expectedMacBytes, slice, "MAC mismatch at repetition $repetition")
        }
    }

    @Test
    fun `send transmits UDP packet over local socket successfully`() {
        DatagramSocket(0, InetAddress.getByName("127.0.0.1")).use { receiver ->
            val localPort = receiver.localPort

            val success = sender.send(
                macAddress = "01:02:03:04:05:06",
                broadcastIp = "127.0.0.1",
                port = localPort,
            )

            assertTrue(success, "send() should return true")

            val buffer = ByteArray(128)
            val receivedPacket = DatagramPacket(buffer, buffer.size)
            receiver.soTimeout = 2000
            receiver.receive(receivedPacket)

            assertEquals(102, receivedPacket.length)
        }
    }
}
