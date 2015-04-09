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
 * Service which contains all REST API response caches.
 */
angular.module('rest').factory('cacheService', ['$injector',
        function cacheService($injector) {

    // Required services
    var $cacheFactory = $injector.get('$cacheFactory');
    var $rootScope    = $injector.get('$rootScope');

    // Service containing all caches
    var service = {};

    /**
     * Shared cache used by both connectionGroupService and
     * connectionService.
     *
     * @type $cacheFactory.Cache
     */
    service.connections = $cacheFactory('API-CONNECTIONS');

    /**
     * Cache used by protocolService.
     *
     * @type $cacheFactory.Cache
     */
    service.protocols = $cacheFactory('API-PROTOCOLS');

    /**
     * Shared cache used by both userService and permissionService.
     *
     * @type $cacheFactory.Cache
     */
    service.users = $cacheFactory('API-USERS');

    /**
     * Clear all caches defined in this service.
     */
    service.clearCaches = function clearCaches() {
        service.protocols.removeAll();
        service.connections.removeAll();
        service.users.removeAll();
    };

    // Clear caches on logout
    $rootScope.$on('guacLogout', function handleLogout() {
        service.clearCaches();
    });

    return service;

}]);
