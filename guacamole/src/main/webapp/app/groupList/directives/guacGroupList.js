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
 * A directive which displays the contents of a connection group within an
 * automatically-paginated view.
 */
angular.module('groupList').directive('guacGroupList', [function guacGroupList() {

    return {
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The connection group to display.
             *
             * @type ConnectionGroup|Object 
             */
            connectionGroup : '=',

            /**
             * Arbitrary object which shall be made available to the connection
             * and connection group templates within the scope as
             * <code>context</code>.
             * 
             * @type Object
             */
            context : '=',

            /**
             * The URL or ID of the Angular template to use when rendering a
             * connection. The @link{GroupListItem} associated with that
             * connection will be exposed within the scope of the template
             * as <code>item</code>, and the arbitrary context object, if any,
             * will be exposed as <code>context</code>.
             *
             * @type String
             */
            connectionTemplate : '=',

            /**
             * The URL or ID of the Angular template to use when rendering a
             * connection group. The @link{GroupListItem} associated with that
             * connection group will be exposed within the scope of the
             * template as <code>item</code>, and the arbitrary context object,
             * if any, will be exposed as <code>context</code>.
             *
             * @type String
             */
            connectionGroupTemplate : '=',

            /**
             * Whether the root of the connection group hierarchy given should
             * be shown. If false (the default), only the descendants of the
             * given connection group will be listed.
             * 
             * @type Boolean
             */
            showRootGroup : '=',

            /**
             * The maximum number of connections or groups to show per page.
             *
             * @type Number
             */
            pageSize : '='

        },

        templateUrl: 'app/groupList/templates/guacGroupList.html',
        controller: ['$scope', '$injector', '$interval', function guacGroupListController($scope, $injector, $interval) {

            // Get required types
            var GroupListItem = $injector.get('GroupListItem');

            /**
             * Returns whether the given item represents a connection that can
             * be displayed. If there is no connection template, then no
             * connection is visible.
             * 
             * @param {GroupListItem} item
             *     The item to check.
             *
             * @returns {Boolean}
             *     true if the given item is a connection that can be
             *     displayed, false otherwise.
             */
            $scope.isVisibleConnection = function isVisibleConnection(item) {
                return item.isConnection && !!$scope.connectionTemplate;
            };

            /**
             * Returns whether the given item represents a connection group
             * that can be displayed. If there is no connection group template,
             * then no connection group is visible.
             * 
             * @param {GroupListItem} item
             *     The item to check.
             *
             * @returns {Boolean}
             *     true if the given item is a connection group that can be
             *     displayed, false otherwise.
             */
            $scope.isVisibleConnectionGroup = function isVisibleConnectionGroup(item) {
                return item.isConnectionGroup && !!$scope.connectionGroupTemplate;
            };

            // Set contents whenever the connection group is assigned or changed
            $scope.$watch("connectionGroup", function setContents(connectionGroup) {

                if (connectionGroup) {

                    // Create item hierarchy, including connections only if they will be visible
                    var rootItem = GroupListItem.fromConnectionGroup(connectionGroup, !!$scope.connectionTemplate);

                    // If root group is to be shown, wrap that group as the child of a fake root group
                    if ($scope.showRootGroup)
                        $scope.rootItem = new GroupListItem({
                            isConnectionGroup : true,
                            isBalancing       : false,
                            children          : [ rootItem ]
                        });

                    // If not wrapped, only the descendants of the root will be shown
                    else
                        $scope.rootItem = rootItem;

                }
                else
                    $scope.rootItem = null;

            });

            /**
             * Toggle the open/closed status of a group list item.
             * 
             * @param {GroupListItem} groupListItem
             *     The list item to expand, which should represent a
             *     connection group.
             */
            $scope.toggleExpanded = function toggleExpanded(groupListItem) {
                groupListItem.isExpanded = !groupListItem.isExpanded;
            };
            
        }]

    };
}]);
