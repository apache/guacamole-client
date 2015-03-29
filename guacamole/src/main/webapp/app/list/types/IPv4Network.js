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
 * A service for defining the IPv4Network class.
 */
angular.module('list').factory('IPv4Network', [
    function defineIPv4Network() {

    /**
     * Represents an IPv4 network as a pairing of base address and netmask,
     * both of which are in binary form. To obtain an IPv4Network from
     * standard CIDR or dot-decimal notation, use IPv4Network.parse().
     *
     * @constructor 
     * @param {Number} address
     *     The IPv4 address of the network in binary form.
     *
     * @param {Number} netmask
     *     The IPv4 netmask of the network in binary form.
     */
    var IPv4Network = function IPv4Network(address, netmask) {

        /**
         * Reference to this IPv4Network.
         *
         * @type IPv4Network
         */
        var network = this;

        /**
         * The binary address of this network. This will be a 32-bit quantity.
         *
         * @type Number
         */
        this.address = address;

        /**
         * The binary netmask of this network. This will be a 32-bit quantity.
         *
         * @type Number
         */
        this.netmask = netmask;

        /**
         * Tests whether the given network is entirely within this network,
         * taking into account the base addresses and netmasks of both.
         *
         * @param {IPv4Network} other
         *     The network to test.
         *
         * @returns {Boolean}
         *     true if the other network is entirely within this network, false
         *     otherwise.
         */
        this.contains = function contains(other) {
            return network.address === (other.address & other.netmask & network.netmask);
        };

    };

    /**
     * Parses the given string as an IPv4 address or subnet, returning an
     * IPv4Network object which describes that address or subnet.
     *
     * @param {String} str
     *     The string to parse.
     *
     * @returns {IPv4Network}
     *     The parsed network, or null if the given string is not valid.
     */
    IPv4Network.parse = function parse(str) {

        // Regex which matches the general form of IPv4 addresses
        var pattern = /^([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3})(?:\/([0-9]{1,2}))?$/;

        // Parse IPv4 address via regex
        var match = pattern.exec(str);
        if (!match)
            return null;

        // Parse netmask, if given
        var netmask = 0xFFFFFFFF;
        if (match[5]) {
            var bits = parseInt(match[5]);
            if (bits > 0 && bits <= 32)
                netmask = 0xFFFFFFFF << (32 - bits);
        }

        // Read each octet onto address
        var address = 0;
        for (var i=1; i <= 4; i++) {

            // Validate octet range
            var octet = parseInt(match[i]);
            if (octet > 255)
                return null;

            // Shift on octet
            address = (address << 8) | octet;

        }

        return new IPv4Network(address, netmask);

    };

    return IPv4Network;

}]);
