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
 * A directive for choosing the location of a connection or connection group.
 */
angular.module('manage').directive('locationChooser', [function locationChooser() {
    
    return {
        // Element only
        restrict: 'E',
        replace: true,

        scope: {

            /**
             * The identifier of the data source from which the given root
             * connection group was retrieved.
             *
             * @type String
             */
            dataSource : '=',

            /**
             * The root connection group of the connection group hierarchy to
             * display.
             *
             * @type ConnectionGroup
             */
            rootGroup : '=',

            /**
             * The unique identifier of the currently-selected connection
             * group. If not specified, the root group will be used.
             *
             * @type String
             */
            value : '='

        },

        templateUrl: 'app/manage/templates/locationChooser.html',
        controller: ['$scope', function locationChooserController($scope) {

            /**
             * Map of unique identifiers to their corresponding connection
             * groups.
             *
             * @type Object.<String, GroupListItem>
             */
            var connectionGroups = {};

            /**
             * Recursively traverses the given connection group and all
             * children, storing each encountered connection group within the
             * connectionGroups map by its identifier.
             *
             * @param {GroupListItem} group
             *     The connection group to traverse.
             */
            var mapConnectionGroups = function mapConnectionGroups(group) {

                // Map given group
                connectionGroups[group.identifier] = group;

                // Map all child groups
                if (group.childConnectionGroups)
                    group.childConnectionGroups.forEach(mapConnectionGroups);

            };

            /**
             * Whether the group list menu is currently open.
             * 
             * @type Boolean
             */
            $scope.menuOpen = false;
            
            /**
             * The human-readable name of the currently-chosen connection
             * group.
             * 
             * @type String
             */
            $scope.chosenConnectionGroupName = null;
            
            /**
             * Toggle the current state of the menu listing connection groups.
             * If the menu is currently open, it will be closed. If currently
             * closed, it will be opened.
             */
            $scope.toggleMenu = function toggleMenu() {
                $scope.menuOpen = !$scope.menuOpen;
            };

            // Update the root group map when data source or root group change
            $scope.$watchGroup(['dataSource', 'rootGroup'], function updateRootGroups() {

                // Abort if the root group is not set
                if (!$scope.dataSource || !$scope.rootGroup)
                    return null;

                // Wrap root group in map
                $scope.rootGroups = {};
                $scope.rootGroups[$scope.dataSource] = $scope.rootGroup;

            });

            // Expose selection function to group list template
            $scope.groupListContext = {
                
                /**
                 * Selects the given group item.
                 *
                 * @param {GroupListItem} item
                 *     The chosen item.
                 */
                chooseGroup : function chooseGroup(item) {

                    // Record new parent
                    $scope.value = item.identifier;
                    $scope.chosenConnectionGroupName = item.name;

                    // Close menu
                    $scope.menuOpen = false;

                }

            };

            $scope.$watch('rootGroup', function setRootGroup(rootGroup) {

                connectionGroups = {};

                if (!rootGroup)
                    return;

                // Map all known groups
                mapConnectionGroups(rootGroup);

                // If no value is specified, default to the root identifier
                if (!$scope.value || !($scope.value in connectionGroups))
                    $scope.value = rootGroup.identifier;

                $scope.chosenConnectionGroupName = connectionGroups[$scope.value].name; 

            });

        }]
    };
    
}]);