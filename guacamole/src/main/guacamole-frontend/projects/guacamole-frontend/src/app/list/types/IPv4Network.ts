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

/**
 * Represents an IPv4 network as a pairing of base address and netmask,
 * both of which are in binary form. To obtain an IPv4Network from
 * standard CIDR or dot-decimal notation, use IPv4Network.parse().
 */
export class IPv4Network {

    /**
     * The binary address of this network. This will be a 32-bit quantity.
     */
    address: number;

    /**
     * The binary netmask of this network. This will be a 32-bit quantity.
     */
    netmask: number;

    /**
     * Creates a new IPv4Network.
     *
     * @param address
     *     The IPv4 address of the network in binary form.
     *
     * @param netmask
     *     The IPv4 netmask of the network in binary form.
     */
    constructor(address: number, netmask: number) {
        this.address = address;
        this.netmask = netmask;
    }

    /**
     * Tests whether the given network is entirely within this network,
     * taking into account the base addresses and netmasks of both.
     *
     * @param other
     *     The network to test.
     *
     * @returns
     *     true if the other network is entirely within this network, false
     *     otherwise.
     */
    contains(other: IPv4Network): boolean {
        return this.address === (other.address & other.netmask & this.netmask);
    }

    /**
     * Parses the given string as an IPv4 address or subnet, returning an
     * IPv4Network object which describes that address or subnet.
     *
     * @param str
     *     The string to parse.
     *
     * @returns
     *     The parsed network, or null if the given string is not valid.
     */
    static parse(str: string): IPv4Network | null {

        // Regex which matches the general form of IPv4 addresses
        const pattern = /^([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3})(?:\/([0-9]{1,2}))?$/;

        // Parse IPv4 address via regex
        const match = pattern.exec(str);
        if (!match)
            return null;

        // Parse netmask, if given
        let netmask = 0xFFFFFFFF;
        if (match[5]) {
            const bits = parseInt(match[5]);
            if (bits > 0 && bits <= 32)
                netmask = 0xFFFFFFFF << (32 - bits);
        }

        // Read each octet onto address
        let address = 0;
        for (let i = 1; i <= 4; i++) {

            // Validate octet range
            const octet = parseInt(match[i]);
            if (octet > 255)
                return null;

            // Shift on octet
            address = (address << 8) | octet;

        }

        return new IPv4Network(address, netmask);

    }
}
