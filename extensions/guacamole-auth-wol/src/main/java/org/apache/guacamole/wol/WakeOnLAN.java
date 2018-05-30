/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.guacamole.wol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class to send the Wake-on-LAN (WOL) packets.
 */
public class WakeOnLAN {

    /**
     * The logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(WakeOnLAN.class);

    /**
     * The port on which we send Wake-on-LAN packets.
     */
    public static final int WOL_PORT = 9;

    /**
     * Send the magic wake-up packet for the specified MAC address
     * to the specified network broadcast address.
     * 
     * @param mac
     *     The MAC address of the system to wake up.
     * 
     * @param broadcast
     *     The network broadcast address on which to send the wake-up
     *     packet.
     * 
     * @throws WOLException
     *     If an error occurs establishing the socket or sending the
     *     packet.
     */
    public static void sendMagicPacket(String mac, InetAddress broadcast)
            throws WOLException {

        // Get the MAC address in bytes and then fill up the rest of the packet
        byte[] macBytes = macStringToBytes(mac);
        byte[] magicBytes = new byte[6 + 16 * macBytes.length];

        // Fill out the first bytes.
        for (int i = 0; i < 6; i++)
            macBytes[i] = (byte) 0xff;

        // Copy MAC address into magic packet byte array
        for (int i = 6; i < magicBytes.length; i += macBytes.length)
            System.arraycopy(macBytes, 0, magicBytes, i, macBytes.length);

        // Set up the packet
        DatagramPacket magicPacket = new DatagramPacket(magicBytes,
                magicBytes.length, broadcast, WOL_PORT);

        // Open a socket and send the packet
        try (DatagramSocket magicSocket = new DatagramSocket()) {
                logger.debug("Sending magic packet for MAC {} on address {}", mac,
                        broadcast.toString());
                magicSocket.send(magicPacket);
                magicSocket.close();
        }
        catch (SocketException e) {
            throw new WOLException("Error establishing the socket.", e);
        }
        catch (IOException e) {
            throw new WOLException("Error sending the packet.", e);
        }

    }

    /**
     * Given a MAC address as a String in standard MAC address format, convert
     * it to a byte array and return the array.
     * 
     * @param mac
     *     The MAC address as a String in the format ff:ff:ff:ff:ff:ff.
     * 
     * @return
     *     The MAC address in byte array format.
     */
    private static byte[] macStringToBytes(String mac) {
        byte[] bytes = new byte[6];
        String[] hexMac = mac.split("(\\:|\\-)");

        // Make sure the length is what we expect
        if (hexMac.length != 6)
            return null;

        // Parse the individual array entries into bytes
        for (int i = 0; i < hexMac.length; i++)
            bytes[i] = (byte) Integer.parseInt(hexMac[i], 16);

        return bytes;
    }

}
