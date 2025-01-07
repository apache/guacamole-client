

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
 * Represents an IPv6 network as a pairing of base address and netmask,
 * both of which are in binary form. To obtain an IPv6Network from
 * standard CIDR notation, use IPv6Network.parse().
 */
export class IPv6Network {

    /**
     * The 128-bit binary address of this network as an array of eight
     * 16-bit numbers.
     */
    addressGroups: number[];

    /**
     * The 128-bit binary netmask of this network as an array of eight
     * 16-bit numbers.
     */
    netmaskGroups: number[];

    /**
     * Creates a new IPv6Network.
     *
     * @param addressGroups
     *     Array of eight IPv6 address groups in binary form, each group being
     *     16-bit number.
     *
     * @param netmaskGroups
     *     Array of eight IPv6 netmask groups in binary form, each group being
     *     16-bit number.
     */
    constructor(addressGroups: number[], netmaskGroups: number[]) {
        this.addressGroups = addressGroups;
        this.netmaskGroups = netmaskGroups;
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
    contains(other: IPv6Network): boolean {

        // Test that each masked 16-bit quantity matches the address
        for (let i = 0; i < 8; i++) {
            if (this.addressGroups[i] !== (other.addressGroups[i]
                & other.netmaskGroups[i]
                & this.netmaskGroups[i]))
                return false;
        }

        // All 16-bit numbers match
        return true;

    }

    /**
     * Generates a netmask having the given number of ones on the left side.
     * All other bits within the netmask will be zeroes. The resulting netmask
     * will be an array of eight numbers, where each number corresponds to a
     * 16-bit group of an IPv6 netmask.
     *
     * @param bits
     *     The number of ones to include on the left side of the netmask. All
     *     other bits will be zeroes.
     *
     * @returns
     *     The generated netmask, having the given number of ones.
     */
    private static generateNetmask(bits: number): number[] {

        const netmask: number[] = [];

        // Only generate up to 128 bits
        bits = Math.min(128, bits);

        // Add any contiguous 16-bit sections of ones
        while (bits >= 16) {
            netmask.push(0xFFFF);
            bits -= 16;
        }

        // Add remaining ones
        if (bits > 0 && bits <= 16)
            netmask.push(0xFFFF & (0xFFFF << (16 - bits)));

        // Add remaining zeroes
        while (netmask.length < 8)
            netmask.push(0);

        return netmask;

    }

    /**
     * Splits the given IPv6 address or partial address into its corresponding
     * 16-bit groups.
     *
     * @param str
     *     The IPv6 address or partial address to split.
     *
     * @returns
     *     The numeric values of all 16-bit groups within the given IPv6
     *     address.
     */
    private static splitAddress(str: string): number[] {

        const address: number[] = [];

        // Split address into groups
        const groups = str.split(':');

        // Parse the numeric value of each group
        groups.forEach(group => {
            const value = parseInt(group || '0', 16);
            address.push(value);
        });

        return address;

    }

    /**
     * Parses the given string as an IPv6 address or subnet, returning an
     * IPv6Network object which describes that address or subnet.
     *
     * @param str
     *     The string to parse.
     *
     * @returns
     *     The parsed network, or null if the given string is not valid.
     */
    static parse(str: string): IPv6Network | null {

        // Regex which matches the general form of IPv6 addresses
        const pattern = /^([0-9a-f]{0,4}(?::[0-9a-f]{0,4}){0,7})(?:\/([0-9]{1,3}))?$/;

        // Parse rudimentary IPv6 address via regex
        const match = pattern.exec(str);
        if (!match)
            return null;

        // Extract address and netmask from parse results
        const unparsedAddress = match[1];
        const unparsedNetmask = match[2];

        // Parse netmask
        let netmask;
        if (unparsedNetmask)
            netmask = IPv6Network.generateNetmask(parseInt(unparsedNetmask));
        else
            netmask = IPv6Network.generateNetmask(128);

        let address;

        // Separate based on the double-colon, if present
        const doubleColon = unparsedAddress.indexOf('::');

        // If no double colon, just split into groups
        if (doubleColon === -1)
            address = IPv6Network.splitAddress(unparsedAddress);

        // Otherwise, split either side of the double colon and pad with zeroes
        else {

            // Parse either side of the double colon
            const leftAddress = IPv6Network.splitAddress(unparsedAddress.substring(0, doubleColon));
            const rightAddress = IPv6Network.splitAddress(unparsedAddress.substring(doubleColon + 2));

            // Pad with zeroes up to address length
            let remaining = 8 - leftAddress.length - rightAddress.length;
            while (remaining > 0) {
                leftAddress.push(0);
                remaining--;
            }

            address = leftAddress.concat(rightAddress);

        }

        // Validate length of address
        if (address.length !== 8)
            return null;

        return new IPv6Network(address, netmask);

    }
}
