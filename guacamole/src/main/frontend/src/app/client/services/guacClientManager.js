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
 * A service for managing several active Guacamole clients.
 */
angular.module('client').factory('guacClientManager', ['$injector',
        function guacClientManager($injector) {

    // Required types
    var ManagedClient      = $injector.get('ManagedClient');
    var ManagedClientGroup = $injector.get('ManagedClientGroup');

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
     * Getter/setter which retrieves or sets the array of all active managed
     * client groups.
     *
     * @type Function
     */
    var storedManagedClientGroups = sessionStorageFactory.create([], function destroyClientGroupStorage() {

        // Disconnect all clients when storage is destroyed
        service.clear();

    });

    /**
     * Returns an array of all managed client groups.
     *
     * @returns {ManagedClientGroup[]>}
     *     An array of all active managed client groups.
     */
    service.getManagedClientGroups = function getManagedClientGroups() {
        return storedManagedClientGroups();
    };

    /**
     * Removes the ManagedClient with the given ID from all
     * ManagedClientGroups, automatically adjusting the tile size of the
     * clients that remain in each group. All client groups that are empty
     * after the client is removed will also be removed.
     *
     * @param {string} id
     *     The ID of the ManagedClient to remove.
     */
    var ungroupManagedClient = function ungroupManagedClient(id) {

        var managedClientGroups = storedManagedClientGroups();

        // Remove client from all groups
        managedClientGroups.forEach(group => {
            _.remove(group.clients, client => (client.id === id));
            ManagedClientGroup.recalculateTiles(group);
        });

        // Remove any groups that are now empty
        _.remove(managedClientGroups, group => !group.clients.length);

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
    service.removeManagedClient = function removeManagedClient(id) {
        
        var managedClients = storedManagedClients();

        // Remove client if it exists
        if (id in managedClients) {

            // Pull client out of any containing groups
            ungroupManagedClient(id);

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
     * @returns {ManagedClient}
     *     The ManagedClient associated with the connection having the given
     *     ID.
     */
    service.replaceManagedClient = function replaceManagedClient(id) {

        var managedClients = storedManagedClients();
        var managedClientGroups = storedManagedClientGroups();

        // Remove client if it exists
        if (id in managedClients) {

            var hadFocus = managedClients[id].clientProperties.focused;
            managedClients[id].client.disconnect();
            delete managedClients[id];
            
            // Remove client from all groups
            managedClientGroups.forEach(group => {

                var index = _.findIndex(group.clients, client => (client.id === id));
                if (index === -1)
                    return;

                group.clients[index] = managedClients[id] = ManagedClient.getInstance(id);
                managedClients[id].clientProperties.focused = hadFocus;

            });

        }

        return managedClients[id];

    };

    /**
     * Returns the ManagedClient associated with the connection having the
     * given ID. If no such ManagedClient exists, a new ManagedClient is
     * created.
     *
     * @param {String} id
     *     The ID of the connection whose ManagedClient should be retrieved.
     *     
     * @returns {ManagedClient}
     *     The ManagedClient associated with the connection having the given
     *     ID.
     */
    service.getManagedClient = function getManagedClient(id) {

        var managedClients = storedManagedClients();

        // Ensure any existing client is removed from its containing group
        // prior to being returned
        ungroupManagedClient(id);

        // Create new managed client if it doesn't already exist
        if (!(id in managedClients))
            managedClients[id] = ManagedClient.getInstance(id);

        // Return existing client
        return managedClients[id];

    };

    /**
     * Returns the ManagedClientGroup having the given ID. If no such
     * ManagedClientGroup exists, a new ManagedClientGroup is created by
     * extracting the relevant connections from the ID.
     *
     * @param {String} id
     *     The ID of the ManagedClientGroup to retrieve or create.
     *
     * @returns {ManagedClientGroup}
     *     The ManagedClientGroup having the given ID.
     */
    service.getManagedClientGroup = function getManagedClientGroup(id) {

        var clients = [];
        var clientIds = ManagedClientGroup.getClientIdentifiers(id);

        // Separate active clients by whether they should be displayed within
        // the current view
        clientIds.forEach(function groupClients(id) {
            clients.push(service.getManagedClient(id));
        });

        // Focus the first client if there are no clients focused
        if (clients.length >= 1 && _.findIndex(clients, client => client.clientProperties.focused) === -1) {
            clients[0].clientProperties.focused = true;
        }

        var group = new ManagedClientGroup({
            clients : clients
        });

        var managedClientGroups = storedManagedClientGroups();
        managedClientGroups.push(group);
        return group;

    };

    /**
     * Removes the existing ManagedClientGroup having the given ID, if any,
     * disconnecting and removing all ManagedClients associated with that
     * group. If no such a ManagedClientGroup currently exists, this function
     * has no effect.
     *
     * @param {String} id
     *     The ID of the ManagedClientGroup to remove.
     * 
     * @returns {Boolean}
     *     true if a ManagedClientGroup was removed, false otherwise.
     */
    service.removeManagedClientGroup = function removeManagedClientGroup(id) {

        var managedClients = storedManagedClients();
        var managedClientGroups = storedManagedClientGroups();

        // Remove all matching groups (there SHOULD only be one)
        var removed = _.remove(managedClientGroups, (group) => ManagedClientGroup.getIdentifier(group) === id);

        // Disconnect all clients associated with the removed group(s)
        removed.forEach((group) => {
            group.clients.forEach((client) => {

                var id = client.id;
                if (managedClients[id]) {
                    managedClients[id].client.disconnect();
                    delete managedClients[id];
                }

            });
        });

        return !!removed.length;

    };

    /**
     * Disconnects and removes all currently-connected clients and client
     * groups.
     */
    service.clear = function clear() {

        var managedClients = storedManagedClients();

        // Disconnect each managed client
        for (var id in managedClients)
            managedClients[id].client.disconnect();

        // Clear managed clients and client groups
        storedManagedClients({});
        storedManagedClientGroups([]);

    };

    // Disconnect all clients when window is unloaded
    $window.addEventListener('unload', service.clear);

    return service;

}]);
