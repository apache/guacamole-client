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

import { Connection } from '../../rest/types/Connection';
import { ConnectionGroup } from '../../rest/types/ConnectionGroup';
import { GroupListItem } from './GroupListItem';

describe('GroupListItem', () => {

    /**
     * Creates a connection with the given identifier and attributes.
     *
     * @param identifier
     *     The identifier, which is also used as the name of the connection.
     *
     * @param attributes
     *     The attributes of the connection, such as its connection limits.
     *
     * @returns
     *     The new connection.
     */
    function connection(identifier: string, attributes: Record<string, string> = {}): Connection {
        return new Connection({
            identifier: identifier,
            name      : identifier,
            protocol  : 'ssh',
            attributes: attributes
        });
    }

    /**
     * Creates a balancing connection group with the given children and
     * attributes.
     *
     * @param childConnections
     *     The child connections of the group, or null or undefined if they
     *     are unknown.
     *
     * @param attributes
     *     The attributes of the group, such as its connection limits.
     *
     * @param childConnectionGroups
     *     The child connection groups of the group.
     *
     * @returns
     *     The new balancing connection group.
     */
    function balancingGroup(childConnections: Connection[] | null | undefined,
                            attributes: Record<string, string> = {},
                            childConnectionGroups?: ConnectionGroup[]): ConnectionGroup {
        return new ConnectionGroup({
            identifier           : 'balancer',
            name                 : 'balancer',
            type                 : ConnectionGroup.Type.BALANCING,
            childConnections     : childConnections,
            childConnectionGroups: childConnectionGroups,
            attributes           : attributes,
            activeConnections    : 0
        });
    }

    /**
     * Returns a getter for the number of active connections which reads
     * those numbers from the given map.
     *
     * @param active
     *     The number of active connections by connection identifier.
     *     Connections which are not listed are not active.
     *
     * @returns
     *     A getter suitable for the countActiveConnections parameter of
     *     GroupListItem.fromConnectionGroup().
     */
    function activeConnections(active: Record<string, number>): (dataSource: string, connection: Connection) => number {
        return (dataSource, connection) => active[connection.identifier!] ?? 0;
    }

    describe('of a balancing group', () => {

        it('should offer the sum of the limits of free connections', () => {
            const item = GroupListItem.fromConnectionGroup('test', balancingGroup([
                connection('a', { 'max-connections': '2' }),
                connection('b', { 'max-connections': '3' })
            ]), true, true, activeConnections({}), undefined, () => 0);

            expect(item.getEffectiveMaxConnections()).toBe(5);
            expect(item.getAvailableConnections()).toBe(5);
        });

        it('should subtract the active connections of all users', () => {
            const item = GroupListItem.fromConnectionGroup('test', balancingGroup([
                connection('a', { 'max-connections': '2' }),
                connection('b', { 'max-connections': '3' })
            ]), true, true, activeConnections({ a: 1, b: 1 }), undefined, () => 0);

            expect(item.getEffectiveMaxConnections()).toBe(5);
            expect(item.getAvailableConnections()).toBe(3);
        });

        it('should limit each connection by the lower of its global and per-user limit', () => {
            const item = GroupListItem.fromConnectionGroup('test', balancingGroup([
                connection('a', { 'max-connections': '4', 'max-connections-per-user': '1' }),
                connection('b', { 'max-connections-per-user': '2' }),
                connection('c')
            ]), true, true, activeConnections({ c: 1 }), undefined, () => 0);

            expect(item.getEffectiveMaxConnections()).toBe(3);
            expect(item.getAvailableConnections()).toBe(2);
        });

        it('should apply the limits of the group if they are lower', () => {
            const connections = (): Connection[] => [
                connection('a', { 'max-connections': '2' }),
                connection('b', { 'max-connections': '3' })
            ];

            const globalLimit = GroupListItem.fromConnectionGroup('test',
                balancingGroup(connections(), { 'max-connections': '4' }),
                true, true, activeConnections({ a: 1 }), undefined, () => 0);

            const perUserLimit = GroupListItem.fromConnectionGroup('test',
                balancingGroup(connections(), { 'max-connections': '4', 'max-connections-per-user': '1' }),
                true, true, activeConnections({}), undefined, () => 0);

            expect(globalLimit.getEffectiveMaxConnections()).toBe(4);
            expect(globalLimit.getAvailableConnections()).toBe(3);
            expect(perUserLimit.getEffectiveMaxConnections()).toBe(1);
            expect(perUserLimit.getAvailableConnections()).toBe(1);
        });

        it('should apply the limits of the group to unlimited connections', () => {
            const item = GroupListItem.fromConnectionGroup('test',
                balancingGroup([connection('a'), connection('b')], { 'max-connections': '3' }),
                true, true, activeConnections({ b: 1 }), undefined, () => 0);

            expect(item.getEffectiveMaxConnections()).toBe(3);
            expect(item.getAvailableConnections()).toBe(2);
        });

        it('should have no maximum without any limits', () => {
            const item = GroupListItem.fromConnectionGroup('test', balancingGroup([
                connection('a', { 'max-connections': '', 'max-connections-per-user': 'unlimited' }),
                connection('b')
            ]), true, true, activeConnections({ a: 1 }), undefined, () => 0);

            expect(item.getEffectiveMaxConnections()).toBeNull();
            expect(item.getAvailableConnections()).toBeNull();
        });

        it('should subtract the connections of the current user if they exceed those through its own connections', () => {
            const item = GroupListItem.fromConnectionGroup('test', balancingGroup([
                connection('a', { 'max-connections': '2' }),
                connection('b', { 'max-connections': '3' })
            ]), true, true, activeConnections({ a: 1 }), undefined, () => 2);

            expect(item.getAvailableConnections()).toBe(3);
        });

        it('should count each nested balancing group on its own', () => {
            const countUserActiveConnectionGroups = vi.fn((dataSource: string, connectionGroup: ConnectionGroup) =>
                connectionGroup.identifier === 'inner' ? 1 : 0);

            const inner = new ConnectionGroup({
                identifier      : 'inner',
                name            : 'inner',
                type            : ConnectionGroup.Type.BALANCING,
                childConnections: [
                    connection('c', { 'max-connections': '1' }),
                    connection('d', { 'max-connections': '1' })
                ],
                activeConnections: 0
            });

            const outer = GroupListItem.fromConnectionGroup('test', balancingGroup(
                [connection('a', { 'max-connections': '2' })], {}, [inner]),
                true, true, activeConnections({ a: 1 }), undefined, countUserActiveConnectionGroups);

            const innerItem = outer.children.find(child => child.identifier === 'inner')!;

            expect(outer.getEffectiveMaxConnections()).toBe(2);
            expect(outer.getAvailableConnections()).toBe(1);
            expect(innerItem.balancing).toBe(true);
            expect(innerItem.getEffectiveMaxConnections()).toBe(2);
            expect(innerItem.getAvailableConnections()).toBe(1);
            expect(countUserActiveConnectionGroups).toHaveBeenCalledWith('test', inner);
        });

        it('should have no maximum if its child connections are unknown', () => {
            for (const childConnections of [undefined, null]) {
                const item = GroupListItem.fromConnectionGroup('test',
                    balancingGroup(childConnections, { 'max-connections': '2' }),
                    true, true, activeConnections({}), undefined, () => 0);

                expect(item.getEffectiveMaxConnections()).toBeNull();
                expect(item.getAvailableConnections()).toBeNull();
            }
        });

    });

    describe('of a connection', () => {

        it('should not know the number of available connections', () => {
            const item = GroupListItem.fromConnection('test', connection('a', { 'max-connections': '2' }));

            expect(item.getEffectiveMaxConnections()).toBeNull();
            expect(item.getAvailableConnections()).toBeNull();
        });

    });

});
