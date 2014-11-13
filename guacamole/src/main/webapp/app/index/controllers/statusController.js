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
 * The controller for the status modal.
 */
angular.module('manage').controller('statusController', ['$scope', '$rootScope', '$injector', 
        function statusController($scope, $rootScope, $injector) {
            
    var statusModal = $injector.get('statusModal');

    /**
     * Fires a guacStatusAction event signalling a chosen action. By default,
     * the status modal will be closed, but this can be prevented by calling
     * preventDefault() on the event.
     *
     * @param {String} action The name of the action.
     */
    $scope.fireAction = function fireAction(action) {

        // Fire action event
        var actionEvent = $rootScope.$broadcast('guacStatusAction', action);

        // Close modal unless default is prevented
        if (!actionEvent.defaultPrevented)
            statusModal.deactivate();

    };

}]);



