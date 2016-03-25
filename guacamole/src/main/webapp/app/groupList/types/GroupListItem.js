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
 * Provides the GroupListItem class definition.
 */
angular.module('groupList').factory('GroupListItem', ['ConnectionGroup', function defineGroupListItem(ConnectionGroup) {

    /**
     * Creates a new GroupListItem, initializing the properties of that
     * GroupListItem with the corresponding properties of the given template.
     *
     * @constructor
     * @param {GroupListItem|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     GroupListItem.
     */
    var GroupListItem = function GroupListItem(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The identifier of the data source associated with the connection or
         * connection group this item represents.
         *
         * @type String
         */
        this.dataSource = template.dataSource;

        /**
         * The unique identifier associated with the connection or connection
         * group this item represents.
         *
         * @type String
         */
        this.identifier = template.identifier;

        /**
         * The human-readable display name of this item.
         * 
         * @type String
         */
        this.name = template.name;

        /**
         * The unique identifier of the protocol, if this item represents a
         * connection. If this item does not represent a connection, this
         * property is not applicable.
         * 
         * @type String
         */
        this.protocol = template.protocol;

        /**
         * All children items of this item. If this item contains no children,
         * this will be an empty array.
         *
         * @type GroupListItem[]
         */
        this.children = template.children || [];

        /**
         * Whether this item represents a connection. If this item represents
         * a connection group, this MUST be false.
         *
         * @type Boolean
         */
        this.isConnection = template.isConnection;

        /**
         * Whether this item represents a connection group. If this item
         * represents a connection, this MUST be false.
         *
         * @type Boolean
         */
        this.isConnectionGroup = template.isConnectionGroup;

        /**
         * Whether this item represents a balancing connection group.
         *
         * @type Boolean
         */
        this.isBalancing = template.isBalancing;

        /**
         * Whether the children items should be displayed.
         *
         * @type Boolean
         */
        this.isExpanded = template.isExpanded;
        
        /**
         * Returns the number of currently active users for this connection or
         * connection group, if known.
         * 
         * @type Number
         */
        this.getActiveConnections = template.getActiveConnections || (function getActiveConnections() {
            return null;
        });

        /**
         * The connection or connection group whose data is exposed within
         * this GroupListItem.
         *
         * @type Connection|ConnectionGroup
         */
        this.wrappedItem = template.wrappedItem;

    };

    /**
     * Creates a new GroupListItem using the contents of the given connection.
     *
     * @param {String} dataSource
     *     The identifier of the data source containing the given connection
     *     group.
     *
     * @param {ConnectionGroup} connection
     *     The connection whose contents should be represented by the new
     *     GroupListItem.
     *
     * @param {Function} [countActiveConnections]
     *     A getter which returns the current number of active connections for
     *     the given connection. If omitted, the number of active connections
     *     known at the time this function was called is used instead. This
     *     function will be passed, in order, the data source identifier and
     *     the connection in question.
     *
     * @returns {GroupListItem}
     *     A new GroupListItem which represents the given connection.
     */
    GroupListItem.fromConnection = function fromConnection(dataSource,
        connection, countActiveConnections) {

        // Return item representing the given connection
        return new GroupListItem({

            // Identifying information
            name       : connection.name,
            identifier : connection.identifier,
            protocol   : connection.protocol,
            dataSource : dataSource,

            // Type information
            isConnection      : true,
            isConnectionGroup : false,
            
            // Count of currently active connections using this connection
            getActiveConnections : function getActiveConnections() {

                // Use getter, if provided
                if (countActiveConnections)
                    return countActiveConnections(dataSource, connection);

                return connection.activeConnections;

            },

            // Wrapped item
            wrappedItem : connection

        });

    };

    /**
     * Creates a new GroupListItem using the contents and descendants of the
     * given connection group.
     *
     * @param {String} dataSource
     *     The identifier of the data source containing the given connection
     *     group.
     *
     * @param {ConnectionGroup} connectionGroup
     *     The connection group whose contents and descendants should be
     *     represented by the new GroupListItem and its descendants.
     *     
     * @param {Boolean} [includeConnections=true]
     *     Whether connections should be included in the contents of the
     *     resulting GroupListItem. By default, connections are included.
     *
     * @param {Function} [countActiveConnections]
     *     A getter which returns the current number of active connections for
     *     the given connection. If omitted, the number of active connections
     *     known at the time this function was called is used instead. This
     *     function will be passed, in order, the data source identifier and
     *     the connection group in question.
     *
     * @param {Function} [countActiveConnectionGroups]
     *     A getter which returns the current number of active connections for
     *     the given connection group. If omitted, the number of active
     *     connections known at the time this function was called is used
     *     instead. This function will be passed, in order, the data source
     *     identifier and the connection group in question.
     *
     * @returns {GroupListItem}
     *     A new GroupListItem which represents the given connection group,
     *     including all descendants.
     */
    GroupListItem.fromConnectionGroup = function fromConnectionGroup(dataSource,
        connectionGroup, includeConnections, countActiveConnections,
        countActiveConnectionGroups) {

        var children = [];

        // Add any child connections
        if (connectionGroup.childConnections && includeConnections !== false) {
            connectionGroup.childConnections.forEach(function addChildConnection(child) {
                children.push(GroupListItem.fromConnection(dataSource, child,
                    countActiveConnections));
            });
        }

        // Add any child groups 
        if (connectionGroup.childConnectionGroups) {
            connectionGroup.childConnectionGroups.forEach(function addChildGroup(child) {
                children.push(GroupListItem.fromConnectionGroup(dataSource,
                    child, includeConnections, countActiveConnections,
                    countActiveConnectionGroups));
            });
        }

        // Return item representing the given connection group
        return new GroupListItem({

            // Identifying information
            name       : connectionGroup.name,
            identifier : connectionGroup.identifier,
            dataSource : dataSource,

            // Type information
            isConnection      : false,
            isConnectionGroup : true,
            isBalancing       : connectionGroup.type === ConnectionGroup.Type.BALANCING,

            // Already-converted children
            children : children,

            // Count of currently active connection groups using this connection
            getActiveConnections : function getActiveConnections() {

                // Use getter, if provided
                if (countActiveConnectionGroups)
                    return countActiveConnectionGroups(dataSource, connectionGroup);

                return connectionGroup.activeConnections;

            },


            // Wrapped item
            wrappedItem : connectionGroup

        });

    };

    return GroupListItem;

}]);
