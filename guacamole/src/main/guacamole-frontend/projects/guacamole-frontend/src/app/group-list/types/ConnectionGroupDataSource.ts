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

import { GroupListItem } from './GroupListItem';
import { ConnectionGroup } from '../../rest/types/ConnectionGroup';
import { FilterService } from '../../list/services/filter.service';
import { map, Observable, of } from 'rxjs';
import cloneDeep from 'lodash/cloneDeep';
import { FilterPattern } from '../../list/types/FilterPattern';

/**
 * A data source which provides a filtered view of a map of
 * the given connection groups.
 */
export class ConnectionGroupDataSource {

    /**
     * An observable which emits the filtered connection groups.
     */
    filteredConnectionGroups!: Observable<Record<string, ConnectionGroup | GroupListItem>>;

    /**
     * The pattern object to use when filtering connections.
     */
    private readonly connectionFilterPattern: FilterPattern;

    /**
     * The pattern object to use when filtering connection groups.
     */
    private readonly connectionGroupFilterPattern: FilterPattern;

    /**
     * Creates a new ConnectionGroupDataSource which provides a filtered view
     * of the given connection groups.
     *
     * @param filterService
     *     The filter service which will be used to filter the connection groups.
     *
     * @param source
     *     The connection groups to filter, as a map of data source
     *     identifier to corresponding root group. The type of each
     *     item within the original map is preserved within the
     *     filtered map.
     *
     * @param searchString
     *     The connection groups to filter, as a map of data source
     *     identifier to corresponding root group. The filtered subset
     *     of this map will be exposed as filteredConnectionGroups.
     *
     * @param connectionProperties
     *     An array of expressions to filter against for each connection in
     *     the hierarchy of connections and groups in the provided map.
     *     These expressions must be Angular expressions which resolve to
     *     properties on the connections in the provided map.
     *
     * @param connectionGroupProperties
     *     An array of expressions to filter against for each connection group
     *     in the hierarchy of connections and groups in the provided map.
     *     These expressions must be Angular expressions which resolve to
     *     properties on the connection groups in the provided map.
     */
    constructor(private filterService: FilterService,
                private source: Record<string, ConnectionGroup | GroupListItem>,
                private searchString: Observable<string> | null,
                private connectionProperties: string[],
                private connectionGroupProperties: string[]) {

        this.connectionFilterPattern = new FilterPattern(this.connectionProperties);
        this.connectionGroupFilterPattern = new FilterPattern(this.connectionGroupProperties);

        this.computeData();
    }

    /**
     * TODO: Document
     */
    private computeData(): void {

        this.filteredConnectionGroups =
            (this.searchString || of(''))
                .pipe(
            map((searchString) => {
                return this.applyFilter(searchString);
            })
        );
    }

    /**
     * Sets the source array and recomputes the data.
     *
     * @param source
     *     The new source array.
     */
    updateSource(source: Record<string, ConnectionGroup | GroupListItem>): void {
        this.source = source;
        this.computeData();
    }

    /**
     * Changes the search string used to filter the connection groups
     * to the given value.
     *
     * @param searchString
     *     The new search string to use when filtering the connection
     *     groups.
     */
    setSearchString(searchString: Observable<string>): void {
        this.searchString = searchString;
        this.computeData();
    }

    /**
     * Applies the current filter predicate, filtering all provided
     * connection groups.
     * TODO: Document
     */
    applyFilter(searchString: string): Record<string, ConnectionGroup | GroupListItem> {

        // Do not apply any filtering (and do not flatten) if no
        // search string is provided
        if (!searchString) {
            return this.source;
        }

        // Compile filter patterns with new search string
        this.connectionFilterPattern.compile(searchString);
        this.connectionGroupFilterPattern.compile(searchString);

        // Re-filter any provided groups
        const connectionGroups = this.source;
        const filteredConnectionGroups: Record<string, ConnectionGroup | GroupListItem> = {};
        if (connectionGroups) {
            for (const dataSource in connectionGroups) {
                const connectionGroup = connectionGroups[dataSource];

                let filteredGroup: ConnectionGroup | GroupListItem;

                // Flatten and filter depending on type
                if (connectionGroup instanceof GroupListItem) {
                    filteredGroup = this.flattenGroupListItem(connectionGroup);
                    this.filterGroupListItem(filteredGroup);
                } else {
                    filteredGroup = this.flattenConnectionGroup(connectionGroup);
                    this.filterConnectionGroup(filteredGroup);
                }

                // Store now-filtered root
                filteredConnectionGroups[dataSource] = filteredGroup;

            }
        }

        return filteredConnectionGroups;

    }

    /**
     * Replaces the set of children within the given GroupListItem such
     * that only children which match the filter predicate for the
     * current search string are present.
     *
     * @param item
     *     The GroupListItem whose children should be filtered.
     */
    private filterGroupListItem(item: GroupListItem): void {
        item.children = item.children.filter(child => {

            // Filter connections and connection groups by
            // given pattern
            switch (child.type) {

                case GroupListItem.Type.CONNECTION:
                    return this.connectionFilterPattern?.predicate(child.wrappedItem);

                case GroupListItem.Type.CONNECTION_GROUP:
                    return this.connectionGroupFilterPattern?.predicate(child.wrappedItem);

            }

            // Include all other children
            return true;

        });
    }

    /**
     * Replaces the set of child connections and connection groups
     * within the given connection group such that only children which
     * match the filter predicate for the current search string are
     * present.
     *
     * @param connectionGroup
     *     The connection group whose children should be filtered.
     */
    private filterConnectionGroup(connectionGroup: ConnectionGroup): void {
        connectionGroup.childConnections = this.filterService
            .filterByPredicate(connectionGroup.childConnections, this.connectionFilterPattern.predicate as any);

        connectionGroup.childConnectionGroups = this.filterService
            .filterByPredicate(connectionGroup.childConnectionGroups, this.connectionGroupFilterPattern.predicate as any);
    }

    /**
     * Flattens the connection group hierarchy of the given connection
     * group such that all descendants are copied as immediate
     * children. The hierarchy of nested connection groups is otherwise
     * completely preserved. A connection or connection group nested
     * two or more levels deep within the hierarchy will thus appear
     * within the returned connection group in two places: in its
     * original location AND as an immediate child.
     *
     * @param connectionGroup
     *     The connection group whose descendents should be copied as
     *     first-level children.
     *
     * @returns
     *     A new connection group completely identical to the provided
     *     connection group, except that absolutely all descendents
     *     have been copied into the first level of children.
     */
    private flattenConnectionGroup(connectionGroup: ConnectionGroup): ConnectionGroup {

        // Replace connection group with shallow copy
        connectionGroup = new ConnectionGroup(connectionGroup);

        // Ensure child arrays are defined and independent copies
        connectionGroup.childConnections = cloneDeep(connectionGroup.childConnections) || [];
        connectionGroup.childConnectionGroups = cloneDeep(connectionGroup.childConnectionGroups) || [];

        // Flatten all children to the top-level group
        connectionGroup.childConnectionGroups?.forEach(child => {

            const flattenedChild = this.flattenConnectionGroup(child);

            // Merge all child connections
            Array.prototype.push.apply(
                connectionGroup.childConnections,
                flattenedChild.childConnections!
            );

            // Merge all child connection groups
            Array.prototype.push.apply(
                connectionGroup.childConnectionGroups,
                flattenedChild.childConnectionGroups!
            );

        });

        return connectionGroup;

    }

    /**
     * Flattens the connection group hierarchy of the given
     * GroupListItem such that all descendants are copied as immediate
     * children. The hierarchy of nested items is otherwise completely
     * preserved. A connection or connection group nested two or more
     * levels deep within the hierarchy will thus appear within the
     * returned item in two places: in its original location AND as an
     * immediate child.
     *
     * @param item
     *     The GroupListItem whose descendents should be copied as
     *     first-level children.
     *
     * @returns
     *     A new GroupListItem completely identical to the provided
     *     item, except that absolutely all descendents have been
     *     copied into the first level of children.
     */
    private flattenGroupListItem(item: GroupListItem): GroupListItem {

        // Replace item with shallow copy
        item = new GroupListItem(item);

        // Ensure children are defined and independent copies
        item.children = cloneDeep(item.children) || [];

        // Flatten all children to the top-level group
        item.children.forEach(child => {
            if (child.type === GroupListItem.Type.CONNECTION_GROUP) {

                const flattenedChild = this.flattenGroupListItem(child);

                // Merge all children
                Array.prototype.push.apply(
                    item.children,
                    flattenedChild.children
                );

            }
        });

        return item;

    }

}
