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

import { ClientIdentifierService } from '../../navigation/service/client-identifier.service';
import { ClientIdentifier } from '../../navigation/types/ClientIdentifier';
import { Connection } from '../../rest/types/Connection';
import { ConnectionGroup } from '../../rest/types/ConnectionGroup';
import { SharingProfile } from '../../rest/types/SharingProfile';
import { Optional } from '../../util/utility-types';

/**
 * Provides the GroupListItem class definition.
 */
export class GroupListItem {

    /**
     * The identifier of the data source associated with the connection,
     * connection group, or sharing profile this item represents.
     */
    dataSource: string;

    /**
     * The unique identifier associated with the connection, connection
     * group, or sharing profile this item represents.
     */
    identifier?: string;

    /**
     * The human-readable display name of this item.
     */
    name?: string;

    /**
     * The unique identifier of the protocol, if this item represents a
     * connection. If this item does not represent a connection, this
     * property is not applicable.
     */
    protocol?: string;

    /**
     * All children items of this item. If this item contains no children,
     * this will be an empty array.
     */
    children: GroupListItem[];

    /**
     * The type of object represented by this GroupListItem. Standard types
     * are defined by GroupListItem.Type, but custom types are also legal.
     */
    type: string;

    /**
     * Whether this item, or items of the same type, can contain children.
     * This may be true even if this particular item does not presently
     * contain children.
     */
    expandable: boolean;

    /**
     * Whether this item represents a balancing connection group.
     */
    balancing: boolean;

    /**
     * Whether the children items should be displayed.
     */
    expanded: boolean;

    /**
     * Returns the number of currently active users for this connection,
     * connection group, or sharing profile, if known. If unknown, null may
     * be returned.
     *
     * @returns
     *     The number of currently active users for this connection,
     *     connection group, or sharing profile.
     */
    getActiveConnections: () => number | null;

    /**
     * Returns the unique string identifier that must be used when
     * connecting to a connection or connection group represented by this
     * GroupListItem.
     *
     * @returns
     *     The client identifier associated with the connection or
     *     connection group represented by this GroupListItem, or null if
     *     this GroupListItem cannot have an associated client identifier.
     */
    getClientIdentifier: () => string | null;


    /**
     * Returns the relative URL of the client page that connects to the
     * connection or connection group represented by this GroupListItem.
     *
     * @returns
     *     The relative URL of the client page that connects to the
     *     connection or connection group represented by this GroupListItem,
     *     or null if this GroupListItem cannot be connected to.
     */
    getClientURL: () => string | null;

    /**
     * The connection, connection group, or sharing profile whose data is
     * exposed within this GroupListItem. If the type of this GroupListItem
     * is not one of the types defined by GroupListItem.Type, then this
     * value may be anything.
     */
    wrappedItem: Connection | ConnectionGroup | SharingProfile | any;

    /**
     * The sorting weight to apply when displaying this GroupListItem. This
     * weight is relative only to other sorting weights. If two items have
     * the same weight, they will be sorted based on their names.
     *
     * @default 0
     */
    weight: number;

    /**
     * Creates a new GroupListItem, initializing the properties of that
     * GroupListItem with the corresponding properties of the given template.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     GroupListItem.
     */
    constructor(template: Optional<GroupListItem, 'name' | 'identifier' | 'protocol' | 'children' | 'expandable'
        | 'balancing' | 'expanded' | 'getActiveConnections' | 'getClientIdentifier' | 'getClientURL' | 'weight'>) {

        this.dataSource = template.dataSource;
        this.identifier = template.identifier;
        this.name = template.name;
        this.protocol = template.protocol;
        this.children = template.children || [];
        this.type = template.type;
        this.expandable = template.expandable || false;
        this.balancing = template.balancing || false;
        this.expanded = template.expanded || false;

        this.getActiveConnections = template.getActiveConnections || (() => null);

        this.getClientIdentifier = template.getClientIdentifier || (() => {

            // If the item is a connection, generate a connection identifier
            if (this.type === GroupListItem.Type.CONNECTION)
                return ClientIdentifierService.getString({
                    dataSource: this.dataSource,
                    type      : ClientIdentifier.Types.CONNECTION,
                    id        : this.identifier
                });

            // If the item is a connection group, generate a connection group identifier
            if (this.type === GroupListItem.Type.CONNECTION_GROUP && this.balancing)
                return ClientIdentifierService.getString({
                    dataSource: this.dataSource,
                    type      : ClientIdentifier.Types.CONNECTION_GROUP,
                    id        : this.identifier
                });

            // Otherwise, no such identifier can exist
            return null;

        });

        this.getClientURL = template.getClientURL || (() => {

            // There is a client page for this item only if it has an
            // associated client identifier
            const identifier = this.getClientIdentifier();
            if (identifier)
                return '/client/' + encodeURIComponent(identifier);

            return null;

        });

        this.wrappedItem = template.wrappedItem;
        this.weight = template.weight || 0;
    }

    /**
     * Creates a new GroupListItem using the contents of the given connection.
     *
     * @param dataSource
     *     The identifier of the data source containing the given connection
     *     group.
     *
     * @param connection
     *     The connection whose contents should be represented by the new
     *     GroupListItem.
     *
     * @param includeSharingProfiles
     *     Whether sharing profiles should be included in the contents of the
     *     resulting GroupListItem. By default, sharing profiles are included.
     *
     * @param countActiveConnections
     *     A getter which returns the current number of active connections for
     *     the given connection. If omitted, the number of active connections
     *     known at the time this function was called is used instead. This
     *     function will be passed, in order, the data source identifier and
     *     the connection in question.
     *
     * @returns
     *     A new GroupListItem which represents the given connection.
     */
    static fromConnection(dataSource: string,
                          connection: Connection,
                          includeSharingProfiles = true,
                          countActiveConnections?: (dataSource: string, connection: Connection) => number): GroupListItem {

        const children: GroupListItem[] = [];

        // Add any sharing profiles
        if (connection.sharingProfiles && includeSharingProfiles !== false) {
            connection.sharingProfiles.forEach(function addSharingProfile(child) {
                children.push(GroupListItem.fromSharingProfile(dataSource,
                    child)); // TODO additional third parameter 'countActiveConnections'?
            });
        }

        // Return item representing the given connection
        return new GroupListItem({

            // Identifying information
            name      : connection.name,
            identifier: connection.identifier,
            protocol  : connection.protocol,
            dataSource: dataSource,

            // Type information
            expandable: includeSharingProfiles !== false,
            type      : GroupListItem.Type.CONNECTION,

            // Already-converted children
            children: children,

            // Count of currently active connections using this connection
            getActiveConnections: function getActiveConnections() {

                // Use getter, if provided
                if (countActiveConnections)
                    return countActiveConnections(dataSource, connection);

                return connection.activeConnections === undefined ? null : connection.activeConnections;

            },

            // Wrapped item
            wrappedItem: connection

        });

    }

    /**
     * Creates a new GroupListItem using the contents and descendants of the
     * given connection group.
     *
     * @param dataSource
     *     The identifier of the data source containing the given connection
     *     group.
     *
     * @param connectionGroup
     *     The connection group whose contents and descendants should be
     *     represented by the new GroupListItem and its descendants.
     *
     * @param includeConnections
     *     Whether connections should be included in the contents of the
     *     resulting GroupListItem. By default, connections are included.
     *
     * @param includeSharingProfiles
     *     Whether sharing profiles should be included in the contents of the
     *     resulting GroupListItem. By default, sharing profiles are included.
     *
     * @param countActiveConnections
     *     A getter which returns the current number of active connections for
     *     the given connection. If omitted, the number of active connections
     *     known at the time this function was called is used instead. This
     *     function will be passed, in order, the data source identifier and
     *     the connection group in question.
     *
     * @param countActiveConnectionGroups
     *     A getter which returns the current number of active connections for
     *     the given connection group. If omitted, the number of active
     *     connections known at the time this function was called is used
     *     instead. This function will be passed, in order, the data source
     *     identifier and the connection group in question.
     *
     * @returns
     *     A new GroupListItem which represents the given connection group,
     *     including all descendants.
     */
    static fromConnectionGroup(dataSource: string,
                               connectionGroup: ConnectionGroup,
                               includeConnections     = true,
                               includeSharingProfiles = true,
                               countActiveConnections?: (dataSource: string, connection: Connection) => number,
                               countActiveConnectionGroups?: (dataSource: string, connectionGroup: ConnectionGroup) => number): GroupListItem {

        const children: GroupListItem[] = [];

        // Add any child connections
        if (connectionGroup.childConnections && includeConnections !== false) {
            connectionGroup.childConnections.forEach(child => {
                children.push(GroupListItem.fromConnection(dataSource, child,
                    includeSharingProfiles, countActiveConnections));
            });
        }

        // Add any child groups
        if (connectionGroup.childConnectionGroups) {
            connectionGroup.childConnectionGroups.forEach(child => {
                children.push(GroupListItem.fromConnectionGroup(dataSource,
                    child, includeConnections, includeSharingProfiles,
                    countActiveConnections, countActiveConnectionGroups));
            });
        }

        // Return item representing the given connection group
        return new GroupListItem({

            // Identifying information
            name      : connectionGroup.name,
            identifier: connectionGroup.identifier,
            dataSource: dataSource,

            // Type information
            type      : GroupListItem.Type.CONNECTION_GROUP,
            balancing : connectionGroup.type === ConnectionGroup.Type.BALANCING,
            expandable: true,

            // Already-converted children
            children: children,

            // Count of currently active connection groups using this connection
            getActiveConnections: function getActiveConnections() {

                // Use getter, if provided
                if (countActiveConnectionGroups)
                    return countActiveConnectionGroups(dataSource, connectionGroup);

                return connectionGroup.activeConnections;

            },


            // Wrapped item
            wrappedItem: connectionGroup

        });

    }

    /**
     * Creates a new GroupListItem using the contents of the given sharing
     * profile.
     *
     * @param dataSource
     *     The identifier of the data source containing the given sharing
     *     profile.
     *
     * @param sharingProfile
     *     The sharing profile whose contents should be represented by the new
     *     GroupListItem.
     *
     * @returns
     *     A new GroupListItem which represents the given sharing profile.
     */
    static fromSharingProfile(dataSource: string, sharingProfile: SharingProfile): GroupListItem {

        // Return item representing the given sharing profile
        return new GroupListItem({

            // Identifying information
            name      : sharingProfile.name,
            identifier: sharingProfile.identifier,
            dataSource: dataSource,

            // Type information
            type: GroupListItem.Type.SHARING_PROFILE,

            // Wrapped item
            wrappedItem: sharingProfile

        });

    }

}

export namespace GroupListItem {

    /**
     * All pre-defined types of GroupListItems. Note that, while these are the
     * standard types supported by GroupListItem and the related guacGroupList
     * directive, the type string is otherwise arbitrary and custom types are
     * legal.
     */
    export enum Type {
        /**
         * The standard type string of a GroupListItem which represents a
         * connection.
         */
        CONNECTION       = 'connection',

        /**
         * The standard type string of a GroupListItem which represents a
         * connection group.
         */
        CONNECTION_GROUP = 'connection-group',

        /**
         * The standard type string of a GroupListItem which represents a
         * sharing profile.
         */
        SHARING_PROFILE  = 'sharing-profile'
    }
}
