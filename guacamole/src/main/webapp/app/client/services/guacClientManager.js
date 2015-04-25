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
angular.module('client').factory('guacClientManager', ['$injector',
        function guacClientManager($injector) {

    // Required types
    var ManagedClient = $injector.get('ManagedClient');

    // Required services
    var $window               = $injector.get('$window');
    var sessionStorageFactory = $injector.get('sessionStorageFactory');

    var service = {};

    /**
     * Getter/setter which retrieves or sets the map of all active managed
     * clients. Each key is the ID of the connection used by that client.
     *
     * @type Function
     */
    var storedManagedClients = sessionStorageFactory.create({}, function destroyClientStorage() {

        // Disconnect all clients when storage is destroyed
        service.clear();

    });

    /**
     * Returns a map of all active managed clients. Each key is the ID of the
     * connection used by that client.
     *
     * @returns {Object.<String, ManagedClient>}
     *     A map of all active managed clients.
     */
    service.getManagedClients = function getManagedClients() {
        return storedManagedClients();
    };

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

        var managedClients = storedManagedClients();

        // Remove client if it exists
        if (id in managedClients) {

            // Disconnect and remove
            managedClients[id].client.disconnect();
            delete managedClients[id];

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
        return storedManagedClients()[id] = ManagedClient.getInstance(id, connectionParameters);

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

        var managedClients = storedManagedClients();

        // Create new managed client if it doesn't already exist
        if (!(id in managedClients))
            managedClients[id] = ManagedClient.getInstance(id, connectionParameters);

        // Return existing client
        return managedClients[id];

    };

    /**
     * Disconnects and removes all currently-connected clients.
     */
    service.clear = function clear() {

        var managedClients = storedManagedClients();

        // Disconnect each managed client
        for (var id in managedClients)
            managedClients[id].client.disconnect();

        // Clear managed clients
        storedManagedClients({});

    };

    // Disconnect all clients when window is unloaded
    $window.addEventListener('unload', service.clear);

    return service;

}]);
