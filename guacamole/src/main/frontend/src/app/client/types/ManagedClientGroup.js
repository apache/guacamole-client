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
 * Provides the ManagedClientGroup class used by the guacClientManager service.
 */
angular.module('client').factory('ManagedClientGroup', ['$injector', function defineManagedClientGroup($injector) {

    /**
     * Object which serves as a grouping of ManagedClients. Each
     * ManagedClientGroup may be attached, detached, and reattached dynamically
     * from different client views, with its contents automatically displayed
     * in a tiled arrangment if needed.
     * 
     * @constructor
     * @param {ManagedClientGroup|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ManagedClientGroup.
     */
    var ManagedClientGroup = function ManagedClientGroup(template) {

        // Use empty object by default
        template = template || {};

        /**
         * Whether this ManagedClientGroup is currently attached to the client
         * interface (true) or is running in the background (false).
         *
         * @type {boolean}
         * @default false
         */
        this.attached = template.attached || false;

        /**
         * The clients that should be displayed within the client interface
         * when this group is attached.
         *
         * @type {ManagedClient[]}
         * @default []
         */
        this.clients = template.clients || [];

        /**
         * The number of rows that should be used when arranging the clients
         * within this group in a grid. By default, this value is automatically
         * calculated from the number of clients.
         *
         * @type {number}
         */
        this.rows = template.rows || ManagedClientGroup.getRows(this);

        /**
         * The number of columns that should be used when arranging the clients
         * within this group in a grid. By default, this value is automatically
         * calculated from the number of clients.
         *
         * @type {number}
         */
        this.columns = template.columns || ManagedClientGroup.getColumns(this);

    };

    /**
     * Updates the number of rows and columns stored within the given
     * ManagedClientGroup such that the clients within the group are evenly
     * distributed. This function should be called whenever the size of a
     * group changes.
     *
     * @param {ManagedClientGroup} group
     *     The ManagedClientGroup that should be updated.
     */
    ManagedClientGroup.recalculateTiles = function recalculateTiles(group) {

        var recalculated = new ManagedClientGroup({
            clients : group.clients
        });

        group.rows = recalculated.rows;
        group.columns = recalculated.columns;

    };

    /**
     * Returns the unique ID representing the given ManagedClientGroup or set
     * of client IDs. The ID of a ManagedClientGroup consists simply of the
     * IDs of all its ManagedClients, separated by periods.
     *
     * @param {ManagedClientGroup|string[]} group
     *     The ManagedClientGroup or array of client IDs to determine the
     *     ManagedClientGroup ID of.
     *
     * @returns {string}
     *     The unique ID representing the given ManagedClientGroup, or the
     *     unique ID that would represent a ManagedClientGroup containing the
     *     clients with the given IDs.
     */
    ManagedClientGroup.getIdentifier = function getIdentifier(group) {

        if (!_.isArray(group))
            group = _.map(group.clients, client => client.id);

        return group.join('.');

    };

    /**
     * Returns an array of client identifiers for all clients contained within
     * the given ManagedClientGroup. Order of the identifiers is preserved
     * with respect to the order of the clients within the group.
     *
     * @param {ManagedClientGroup|string} group
     *     The ManagedClientGroup to retrieve the client identifiers from,
     *     or its ID.
     *
     * @returns {string[]}
     *     The client identifiers of all clients contained within the given
     *     ManagedClientGroup.
     */
    ManagedClientGroup.getClientIdentifiers = function getClientIdentifiers(group) {

        if (_.isString(group))
            return group.split(/\./);

        return group.clients.map(client => client.id);

    };

    /**
     * Returns the number of columns that should be used to evenly arrange
     * all provided clients in a tiled grid.
     *
     * @returns {Number}
     *     The number of columns that should be used for the grid of
     *     clients.
     */
    ManagedClientGroup.getColumns = function getColumns(group) {

        if (!group.clients.length)
            return 0;

        return Math.ceil(Math.sqrt(group.clients.length));

    };

    /**
     * Returns the number of rows that should be used to evenly arrange all
     * provided clients in a tiled grid.
     *
     * @returns {Number}
     *     The number of rows that should be used for the grid of clients.
     */
    ManagedClientGroup.getRows = function getRows(group) {

        if (!group.clients.length)
            return 0;

        return Math.ceil(group.clients.length / ManagedClientGroup.getColumns(group));

    };

    /**
     * Returns the title which should be displayed as the page title if the
     * given client group is attached to the interface.
     *
     * @param {ManagedClientGroup} group
     *     The ManagedClientGroup to determine the title of.
     *
     * @returns {string}
     *     The title of the given ManagedClientGroup.
     */
    ManagedClientGroup.getTitle = function getTitle(group) {

        // Use client-specific title if only one client
        if (group.clients.length === 1)
            return group.clients[0].title;

        // With multiple clients, somehow combining multiple page titles would
        // be confusing. Instead, use the combined names.
        return ManagedClientGroup.getName(group);

    };

    /**
     * Returns the combined names of all clients within the given
     * ManagedClientGroup, as determined by the names of the associated
     * connections or connection groups.
     *
     * @param {ManagedClientGroup} group
     *     The ManagedClientGroup to determine the name of.
     *
     * @returns {string}
     *     The combined names of all clients within the given
     *     ManagedClientGroup.
     */
    ManagedClientGroup.getName = function getName(group) {

        // Generate a name from ONLY the focused clients, unless there are no
        // focused clients
        var relevantClients = _.filter(group.clients, client => client.clientProperties.focused);
        if (!relevantClients.length)
            relevantClients = group.clients;

        return _.filter(relevantClients, (client => !!client.name)).map(client => client.name).join(', ') || '...';

    };

    return ManagedClientGroup;

}]);