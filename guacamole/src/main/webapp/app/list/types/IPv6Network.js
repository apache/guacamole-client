/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * A service for defining the IPv6Network class.
 */
angular.module('list').factory('IPv6Network', [
    function defineIPv6Network() {

    /**
     * Represents an IPv6 network as a pairing of base address and netmask,
     * both of which are in binary form. To obtain an IPv6Network from
     * standard CIDR or dot-decimal notation, use IPv6Network.parse().
     *
     * @constructor 
     * @param {Number[]} addressGroups
     *     Array of eight IPv6 address groups in binary form, each group being 
     *     16-bit number.
     *
     * @param {Number[]} netmaskGroups
     *     Array of eight IPv6 netmask groups in binary form, each group being 
     *     16-bit number.
     */
    var IPv6Network = function IPv6Network(addressGroups, netmaskGroups) {

        /**
         * Reference to this IPv6Network.
         *
         * @type IPv6Network
         */
        var network = this;

        /**
         * The 128-bit binary address of this network as an array of eight
         * 16-bit numbers.
         *
         * @type Number[]
         */
        this.addressGroups = addressGroups;

        /**
         * The 128-bit binary netmask of this network as an array of eight
         * 16-bit numbers.
         *
         * @type Number
         */
        this.netmaskGroups = netmaskGroups;

        /**
         * Tests whether the given network is entirely within this network,
         * taking into account the base addresses and netmasks of both.
         *
         * @param {IPv6Network} other
         *     The network to test.
         *
         * @returns {Boolean}
         *     true if the other network is entirely within this network, false
         *     otherwise.
         */
        this.contains = function contains(other) {

            // Test that each masked 16-bit quantity matches the address
            for (var i=0; i < 8; i++) {
                if (network.addressGroups[i] !== (other.addressGroups[i]
                                                & other.netmaskGroups[i]
                                                & network.netmaskGroups[i]))
                    return false;
            }

            // All 16-bit numbers match
            return true;

        };

    };

    /**
     * Generates a netmask having the given number of ones on the left side.
     * All other bits within the netmask will be zeroes. The resulting netmask
     * will be an array of eight numbers, where each number corresponds to a
     * 16-bit group of an IPv6 netmask.
     *
     * @param {Number} bits
     *     The number of ones to include on the left side of the netmask. All
     *     other bits will be zeroes.
     *
     * @returns {Number[]}
     *     The generated netmask, having the given number of ones.
     */
    var generateNetmask = function generateNetmask(bits) {

        var netmask = [];

        // Only generate up to 128 bits
        bits = Math.min(128, bits);

        // Add any contiguous 16-bit sections of 1's
        while (bits >= 16) {
            netmask.push(0xFFFF);
            bits -= 16;
        }

        // Add remaining 1's
        if (bits > 0 && bits <= 16)
            netmask.push(0xFFFF & (0xFFFF << (16 - bits)));

        // Add remaining zeroes
        while (netmask.length < 8)
            netmask.push(0);

        return netmask;

    };

    /**
     * Splits the given IPv6 address or partial address into its corresponding
     * 16-bit groups.
     *
     * @param {String} str
     *     The IPv6 address or partial address to split.
     * 
     * @returns Number[]
     *     The numeric values of all 16-bit groups within the given IPv6
     *     address.
     */
    var splitAddress = function splitAddress(str) {

        var address = [];

        // Split address into groups
        var groups = str.split(':');

        // Parse the numeric value of each group
        angular.forEach(groups, function addGroup(group) {
            var value = parseInt(group || '0', 16);
            address.push(value);
        });

        return address;

    };

    /**
     * Parses the given string as an IPv6 address or subnet, returning an
     * IPv6Network object which describes that address or subnet.
     *
     * @param {String} str
     *     The string to parse.
     *
     * @returns {IPv6Network}
     *     The parsed network, or null if the given string is not valid.
     */
    IPv6Network.parse = function parse(str) {

        // Regex which matches the general form of IPv6 addresses
        var pattern = /^([0-9a-f]{0,4}(?::[0-9a-f]{0,4}){0,7})(?:\/([0-9]{1,3}))?$/;

        // Parse rudimentary IPv6 address via regex
        var match = pattern.exec(str);
        if (!match)
            return null;

        // Extract address and netmask from parse results
        var unparsedAddress = match[1];
        var unparsedNetmask = match[2];

        // Parse netmask
        var netmask;
        if (unparsedNetmask)
            netmask = generateNetmask(parseInt(unparsedNetmask));
        else
            netmask = generateNetmask(128);

        var address;

        // Separate based on the double-colon, if present
        var doubleColon = unparsedAddress.indexOf('::');

        // If no double colon, just split into groups
        if (doubleColon === -1)
            address = splitAddress(unparsedAddress);

        // Otherwise, split either side of the double colon and pad with zeroes
        else {

            // Parse either side of the double colon
            var leftAddress  = splitAddress(unparsedAddress.substring(0, doubleColon));
            var rightAddress = splitAddress(unparsedAddress.substring(doubleColon + 2));

            // Pad with zeroes up to address length
            var remaining = 8 - leftAddress.length - rightAddress.length;
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

    };

    return IPv6Network;

}]);
