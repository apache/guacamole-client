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
 * A directive which provides a filtering text input field which automatically
 * produces a filtered subset of the given connection groups.
 */
angular.module('groupList').directive('guacGroupListFilter', [function guacGroupListFilter() {

    return {
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The property to which a subset of the provided map of connection
             * groups will be assigned.
             *
             * @type Array
             */
            filteredConnectionGroups : '=',

            /**
             * The placeholder text to display within the filter input field
             * when no filter has been provided.
             * 
             * @type String
             */
            placeholder : '&',

            /**
             * The connection groups to filter, as a map of data source
             * identifier to corresponding root group. A subset of this map
             * will be exposed as filteredConnectionGroups.
             *
             * @type Object.<String, ConnectionGroup>
             */
            connectionGroups : '&',

            /**
             * An array of expressions to filter against for each connection in
             * the hierarchy of connections and groups in the provided map.
             * These expressions must be Angular expressions which resolve to
             * properties on the connections in the provided map.
             *
             * @type String[]
             */
            connectionProperties : '&',

            /**
             * An array of expressions to filter against for each connection group
             * in the hierarchy of connections and groups in the provided map.
             * These expressions must be Angular expressions which resolve to
             * properties on the connection groups in the provided map.
             *
             * @type String[]
             */
            connectionGroupProperties : '&'

        },

        templateUrl: 'app/groupList/templates/guacGroupListFilter.html',
        controller: ['$scope', '$injector', function guacGroupListFilterController($scope, $injector) {

            // Required types
            var ConnectionGroup = $injector.get('ConnectionGroup');
            var FilterPattern   = $injector.get('FilterPattern');

            /**
             * The pattern object to use when filtering connections.
             *
             * @type FilterPattern
             */
            var connectionFilterPattern = new FilterPattern($scope.connectionProperties());

            /**
             * The pattern object to use when filtering connection groups.
             *
             * @type FilterPattern
             */
            var connectionGroupFilterPattern = new FilterPattern($scope.connectionGroupProperties());

            /**
             * The filter search string to use to restrict the displayed
             * connection groups.
             *
             * @type String
             */
            $scope.searchString = null;

            /**
             * Flattens the connection group hierarchy of the given connection
             * group such that all descendants are copied as immediate
             * children. The hierarchy of nested connection groups is otherwise
             * completely preserved. A connection or connection group nested
             * two or more levels deep within the hierarchy will thus appear
             * within the returned connection group in two places: in its
             * original location AND as an immediate child.
             *
             * @param {ConnectionGroup} connectionGroup
             *     The connection group whose descendents should be copied as
             *     first-level children.
             *
             * @returns {ConnectionGroup}
             *     A new connection group completely identical to the provided
             *     connection group, except that absolutely all descendents
             *     have been copied into the first level of children.
             */
            var flattenConnectionGroup = function flattenConnectionGroup(connectionGroup) {

                // Replace connection group with shallow copy
                connectionGroup = new ConnectionGroup(connectionGroup);

                // Ensure child arrays are defined and independent copies
                connectionGroup.childConnections = angular.copy(connectionGroup.childConnections) || [];
                connectionGroup.childConnectionGroups = angular.copy(connectionGroup.childConnectionGroups) || [];

                // Flatten all children to the top-level group
                angular.forEach(connectionGroup.childConnectionGroups, function flattenChild(child) {

                    var flattenedChild = flattenConnectionGroup(child);

                    // Merge all child connections
                    Array.prototype.push.apply(
                        connectionGroup.childConnections,
                        flattenedChild.childConnections
                    );

                    // Merge all child connection groups
                    Array.prototype.push.apply(
                        connectionGroup.childConnectionGroups,
                        flattenedChild.childConnectionGroups
                    );

                });

                return connectionGroup;

            };

            /**
             * Applies the current filter predicate, filtering all provided
             * connection groups and storing the result in
             * filteredConnectionGroups.
             */
            var updateFilteredConnectionGroups = function updateFilteredConnectionGroups() {

                // Do not apply any filtering (and do not flatten) if no
                // search string is provided
                if (!$scope.searchString) {
                    $scope.filteredConnectionGroups = $scope.connectionGroups() || {};
                    return;
                }

                // Clear all current filtered groups
                $scope.filteredConnectionGroups = {};

                // Re-filter any provided groups
                var connectionGroups = $scope.connectionGroups();
                if (connectionGroups) {
                    angular.forEach(connectionGroups, function updateFilteredConnectionGroup(connectionGroup, dataSource) {

                        // Flatten hierarchy of connection group
                        var filteredGroup = flattenConnectionGroup(connectionGroup);

                        // Filter all direct children
                        filteredGroup.childConnections = filteredGroup.childConnections.filter(connectionFilterPattern.predicate);
                        filteredGroup.childConnectionGroups = filteredGroup.childConnectionGroups.filter(connectionGroupFilterPattern.predicate);

                        // Store now-filtered root
                        $scope.filteredConnectionGroups[dataSource] = filteredGroup;

                    });
                }

            };

            // Recompile and refilter when pattern is changed
            $scope.$watch('searchString', function searchStringChanged(searchString) {
                connectionFilterPattern.compile(searchString);
                connectionGroupFilterPattern.compile(searchString);
                updateFilteredConnectionGroups();
            });

            // Refilter when items change
            $scope.$watchCollection($scope.connectionGroups, function itemsChanged() {
                updateFilteredConnectionGroups();
            });

        }]

    };
}]);
