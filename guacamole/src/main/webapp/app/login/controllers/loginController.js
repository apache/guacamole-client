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

angular.module('login').controller('loginController', ['$scope', '$injector',
        function loginController($scope, $injector) {
            
    // Get the dependencies commonJS style
    var authenticationService   = $injector.get("authenticationService");
    var localStorageUtility     = $injector.get("localStorageUtility");
    var $location               = $injector.get("$location");
            
    // Clear the auth token and userID to log out the user
    localStorageUtility.clear("authToken");
    localStorageUtility.clear("userID");
        
    $scope.loginError = false;
    
    $scope.login = function login() {
        authenticationService.login($scope.username, $scope.password)
            .success(function success(data, status, headers, config) {
                localStorageUtility.set('authToken', data.authToken);
                localStorageUtility.set('userID', data.userID);
                
                // Set up the basic permissions for the user
                $scope.loadBasicPermissions();
                $location.path('/');
            }).error(function error(data, status, headers, config) {
                $scope.loginError = true;
            });
    };
}]);
