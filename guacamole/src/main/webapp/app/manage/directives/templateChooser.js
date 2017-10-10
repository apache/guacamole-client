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
 * A directive for choosing the template of a connection from a list of
 * available and applicable connections.
 */
angular.module('manage').directive('templateChooser', [function templateChooser() {
    
    return {
        // Element only
        restrict: 'E',
        replace: true,

        scope: {

            /**
             * The identifier of the connection being manipulated, if this is
             * an existing connection.
             *
             * @type String
             */
            connectionId : '=',

            /**
             * The identifier of the data source from which the given root
             * connection group was retrieved.
             *
             * @type String
             */
            dataSource : '=',

            /**
             * The protocol of the ocnnection.
             *
             * @type String
             */
            protocol : '=',

            /**
             * The root connection group of the connection group hierarchy to
             * display.
             *
             * @type ConnectionGroup
             */
            rootGroup : '=',

            /**
             * The Connection object of the currently-selected template
             * connection. If not specified, the root group will be used.
             *
             * @type Connection
             */
            value : '='

        },

        templateUrl: 'app/manage/templates/templateChooser.html',
        controller: ['$scope', '$log', function templateChooserController($scope,$log) {

            /**
             * Map of unique identifiers to their corresponding connection
             * groups.
             *
             * @type Object.<String, GroupListItem>
             */
            var connectionGroups = {};

            /**
             * Flat list of all possible template connections.
             *
             * @type [Connection]
             */
            var templateConnections = [];

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
                for (var templConn in group.childConnections) {
                    var thisId = group.childConnections[templConn].identifier;
                    templateConnections[thisId] = group.childConnections[templConn];
                }

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
            $scope.chosenTemplateName = null;

            /**
             *
             */
            $scope.clearTemplate = function clearTemplate() {

                $scope.value = null;

            };
            
            /**
             * Toggle the current state of the menu listing connection groups.
             * If the menu is currently open, it will be closed. If currently
             * closed, it will be opened.
             */
            $scope.toggleMenu = function toggleMenu() {
                $scope.menuOpen = !$scope.menuOpen;
                if ($scope.menuOpen)
                    mapConnectionGroups($scope.rootGroup);
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
            $scope.templateListContext = {
                
                /**
                 * Selects the given group item.
                 *
                 * @param {ConnectionListItem} item
                 *     The chosen item.
                 */
                chooseTemplate : function chooseTemplate(item) {

                    if (item.identifier == $scope.connectionId)
                        return false;
                    if (item.protocol != $scope.protocol)
                        return false;

                    // Record new parent
                    $scope.value = item.identifier;
                    $scope.chosenTemplateName = item.name;

                    // Close menu
                    $scope.menuOpen = false;

                    return true;

                },

                templateEnabled : function templateEnabled(item) {
                    if (item.identifier == $scope.connectionId)
                        return false;
                    if (item.protocol != $scope.protocol)
                        return false;

                    return true;
                }

            };

            $scope.$watch('rootGroup', function setRootGroup(rootGroup) {

                connectionGroups = {};

                if (!rootGroup)
                    return;

                // Map all known groups
                mapConnectionGroups(rootGroup);

                // If no value is specified, default to a null value.
                if (!$scope.value || !$scope.value in templateConnections)
                    $scope.value = null;
                else
                    $scope.chosenTemplateName = templateConnections[$scope.value].name;

            });

            $scope.$watch('value', function valueChanged(value) {
                if (!value)
                    $scope.chosenTemplateName = 'No Template Selected';
                else
                    $scope.chosenTemplateName = templateConnections[value].name;
            });

            $scope.$watch('protocol', function protocolChanged(protocol) {

                connectionGroups = {};

                if (!protocol || !$scope.rootGroup)
                    return;

                mapConnectionGroups($scope.rootGroup);

                // If no value is specified, default to a null value.
                if (!$scope.value || !$scope.value in templateConnections) {
                    $scope.value = null;
                    $scope.chosenTemplateName = 'No Template Selected.';
                }
                else if (protocol != templateConnections[$scope.value].protocol) {
                    $scope.value = null;
                    $scope.chosenTemplateName = 'No Template Selected.';
                }
                else
                    $scope.chosenTemplateName = templateConnections[$scope.value].name;

            });

        }]
    };
    
}]);
