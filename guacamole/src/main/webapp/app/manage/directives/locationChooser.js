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
            item: '=item',
            root: '=root',
        },
        templateUrl: 'app/manage/templates/locationChooser.html',
        controller: ['$scope', '$injector', function locationChooserController($scope, $injector) {
            // The dropdown should start closed
            $scope.showDropDown = false;
            
            // Map of ID to name for all connection groups
            $scope.connectionGroupNameMap = {};
            
            // Set up the group for display and search
            mapConnectionGroupNames($scope.root);
            $scope.connectionGroups = [$scope.root];

            // Should be in the root group by default
            if(!$scope.item.parentIdentifier)
                $scope.item.parentIdentifier = $scope.root.parentIdentifier;

            setCurrentParentName();
    
            // Add the name of all connection groups under group to the group name map
            function mapConnectionGroupNames(group) {
                $scope.connectionGroupNameMap[group.identifier] = group.name;
                for(var i = 0; i < group.children.length; i++) {
                    var child = group.children[i];
                    if(!child.isConnection)
                        mapConnectionGroupNames(child);
                }
            }
    
            //Set the current connection group name to the name of the connection group with the currently chosen ID
            function setCurrentParentName() {
                $scope.currentConnectionGroupName = $scope.connectionGroupNameMap[$scope.item.parentIdentifier];
            }
            
            // Watch for changes to the parentID, and update the current name as needed
            $scope.currentConnectionGroupName = "";
            $scope.$watch('item.parentIdentifier', function watchParentID() {
                setCurrentParentName();
            });
            
            /**
             * Toggle the drop down - open or closed.
             */
            $scope.toggleDropDown = function toggleDropDown() {
                $scope.showDropDown = !$scope.showDropDown;
            }
            
            /**
             * Choose a new parent ID for the item.
             * @param {type} parentID The new parentID.
             */
            $scope.chooseParentID = function chooseParentID(parentID) {
                $scope.item.parentIdentifier = parentID;
            }
        }]
    };
    
}]);