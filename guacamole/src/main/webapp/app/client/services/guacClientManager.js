/*
 * Copyright (C) 2014 Glyptodon LLC
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
 * A service for managing several active Guacamole clients.
 */
angular.module('client').factory('guacClientManager', ['ManagedClient',
        function guacClientManager(ManagedClient) {

    var service = {};

    /**
     * Map of all active managed clients. Each key is the ID of the connection
     * used by that client.
     *
     * @type Object.<String, ManagedClient>
     */
    service.managedClients = {};

    /**
     * Removes the existing ManagedClient associated with the connection having
     * the given ID, if any. If no such a ManagedClient already exists, this
     * function has no effect.
     *
     * @param {String} id
     *     The ID of the connection whose ManagedClient should be removed.
     * 
     * @returns {Boolean}
     *     true if an existing client was removed, false otherwise.
     */
    service.removeManagedClient = function replaceManagedClient(id) {
        
        // Remove client if it exists
        if (id in service.managedClients) {

            // Disconnect and remove
            service.managedClients[id].client.disconnect();
            delete service.managedClients[id];

            // A client was removed
            return true;

        }

        // No client was removed
        return false;

    };

    /**
     * Creates a new ManagedClient associated with the connection having the
     * given ID. If such a ManagedClient already exists, it is disconnected and
     * replaced.
     *
     * @param {String} id
     *     The ID of the connection whose ManagedClient should be retrieved.
     *     
     * @param {String} [connectionParameters]
     *     Any additional HTTP parameters to pass while connecting. This
     *     parameter only has an effect if a new connection is established as
     *     a result of this function call.
     * 
     * @returns {ManagedClient}
     *     The ManagedClient associated with the connection having the given
     *     ID.
     */
    service.replaceManagedClient = function replaceManagedClient(id, connectionParameters) {

        // Disconnect any existing client
        service.removeManagedClient(id);

        // Set new client
        return service.managedClients[id] = ManagedClient.getInstance(id, connectionParameters);

    };

    /**
     * Returns the ManagedClient associated with the connection having the
     * given ID. If no such ManagedClient exists, a new ManagedClient is
     * created.
     *
     * @param {String} id
     *     The ID of the connection whose ManagedClient should be retrieved.
     *     
     * @param {String} [connectionParameters]
     *     Any additional HTTP parameters to pass while connecting. This
     *     parameter only has an effect if a new connection is established as
     *     a result of this function call.
     * 
     * @returns {ManagedClient}
     *     The ManagedClient associated with the connection having the given
     *     ID.
     */
    service.getManagedClient = function getManagedClient(id, connectionParameters) {

        // Create new managed client if it doesn't already exist
        if (!(id in service.managedClients))
            service.managedClients[id] = ManagedClient.getInstance(id, connectionParameters);

        // Return existing client
        return service.managedClients[id];

    };

    return service;

}]);
