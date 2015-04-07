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
 * A service for generating all the important pages a user can visit.
 */
angular.module('navigation').factory('userPageService', ['$injector',
        function userPageService($injector) {

    // Get required types
    var ConnectionGroup = $injector.get('ConnectionGroup');
    var PermissionSet   = $injector.get('PermissionSet');

    // Get required services
    var $q                     = $injector.get('$q');
    var authenticationService  = $injector.get('authenticationService');
    var connectionGroupService = $injector.get("connectionGroupService");
    var permissionService      = $injector.get("permissionService");
    
    var service = {};
    
    /**
     * Construct a new Page object with the given name and url.
     * @constructor
     * 
     * @param {String} name
     *     The i18n key for the name of the page.
     * 
     * @param {String} url
     *     The url to the page.
     *     
     * @returns {PageDefinition} 
     *     The newly created PageDefinition object.
     */
    var Page = function Page(name, url) {
        this.name = name;
        this.url  = url;
    };

    /**
     * Returns an appropriate home page for the current user.
     *
     * @param {ConnectionGroup} rootGroup
     *     The root of the connection group tree for the current user.
     *
     * @returns {Page}
     *     The user's home page.
     */
    var generateHomePage = function generateHomePage(rootGroup) {

        // Get children
        var connections      = rootGroup.childConnections      || [];
        var connectionGroups = rootGroup.childConnectionGroups || [];

        // Use main connection list screen as home if multiple connections
        // are available
        if (connections.length + connectionGroups.length === 1) {

            var connection      = connections[0];
            var connectionGroup = connectionGroups[0];

            // Only one connection present, use as home page
            if (connection) {
                return new Page(
                    connection.name,
                    '/client/c/' + connection.identifier
                );
            }

            // Only one connection present, use as home page
            if (connectionGroup
                    && connectionGroup.type === ConnectionGroup.Type.BALANCING
                    && _.isEmpty(connectionGroup.childConnections)
                    && _.isEmpty(connectionGroup.childConnectionGroups)) {
                return new Page(
                    connectionGroup.name,
                    '/client/g/' + connectionGroup.identifier
                );
            }

        }

        // Resolve promise with default home page
        return new Page(
            'USER_MENU.ACTION_NAVIGATE_HOME',
            '/'
        );

    };

    /**
     * Returns a promise which resolves with an appropriate home page for the
     * current user.
     *
     * @returns {Promise.<Page>}
     *     A promise which resolves with the user's default home page.
     */
    service.getHomePage = function getHomePage() {

        var deferred = $q.defer();

        // Resolve promise using home page derived from root connection group
        connectionGroupService.getConnectionGroupTree(ConnectionGroup.ROOT_IDENTIFIER)
        .success(function rootConnectionGroupRetrieved(rootGroup) {
            deferred.resolve(generateHomePage(rootGroup));
        });

        return deferred.promise;

    };

    /**
     * Returns all the main pages that the current user can visit. This can 
     * include the home page, manage pages, etc. In the case that there are no 
     * applicable pages of this sort, it may return a client page.
     * 
     * @param {ConnectionGroup} rootGroup
     *     The root of the connection group tree for the current user.
     *     
     * @param {PermissionSet} permissions
     *     The permissions for the current user.
     * 
     * @returns {Page[]} 
     *     An array of all main pages that the current user can visit.
     */
    var generateMainPages = function generateMainPages(rootGroup, permissions) {
        
        var pages = [];
        
        permissions = angular.copy(permissions);

        // Ignore permission to update root group
        PermissionSet.removeConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE, ConnectionGroup.ROOT_IDENTIFIER);

        // Ignore permission to update self
        PermissionSet.removeUserPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE, authenticationService.getCurrentUserID());

        // Determine whether the current user needs access to the user management UI
        var canManageUsers =

                // System permissions
                   PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
                || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_USER)

                // Permission to update users
                || PermissionSet.hasUserPermission(permissions,            PermissionSet.ObjectPermissionType.UPDATE)

                // Permission to delete users
                || PermissionSet.hasUserPermission(permissions,            PermissionSet.ObjectPermissionType.DELETE)

                // Permission to administer users
                || PermissionSet.hasUserPermission(permissions,            PermissionSet.ObjectPermissionType.ADMINISTER);

        // Determine whether the current user needs access to the connection management UI
        var canManageConnections =

                // System permissions
                   PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
                || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_CONNECTION)
                || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_CONNECTION_GROUP)

                // Permission to update connections or connection groups
                || PermissionSet.hasConnectionPermission(permissions,      PermissionSet.ObjectPermissionType.UPDATE)
                || PermissionSet.hasConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE)

                // Permission to delete connections or connection groups
                || PermissionSet.hasConnectionPermission(permissions,      PermissionSet.ObjectPermissionType.DELETE)
                || PermissionSet.hasConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.DELETE)

                // Permission to administer connections or connection groups
                || PermissionSet.hasConnectionPermission(permissions,      PermissionSet.ObjectPermissionType.ADMINISTER)
                || PermissionSet.hasConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.ADMINISTER);

        var canManageSessions = 

                // A user must be a system administrator to manage sessions
                PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER);
        
        // Add home page
        pages.push(generateHomePage(rootGroup));
        
        // If user can manage users, add link to user management page
        if (canManageUsers) {
            pages.push(new Page(
                'USER_MENU.ACTION_MANAGE_USERS',
                '/manage/modules/users/'
            ));
        }

        // If user can manage connections, add link to connections management page
        if (canManageConnections) {
            pages.push(new Page(
                'USER_MENU.ACTION_MANAGE_CONNECTIONS',
                '/manage/modules/connections/'
            ));
        }
        
        // If user can manage sessions, add link to sessions management page
        if (canManageSessions) {
            pages.push(new Page(
                'USER_MENU.ACTION_MANAGE_SESSIONS',
                '/manage/modules/sessions/'
            ));
        }
        
        return pages;
    };

    /**
     * Returns a promise which resolves to an array of all main pages that the
     * current user can visit. This can include the home page, manage pages,
     * etc. In the case that there are no applicable pages of this sort, it may
     * return a client page.
     *
     * @returns {Promise.<Page[]>} 
     *     A promise which resolves to an array of all main pages that the
     *     current user can visit.
     */
    service.getMainPages = function getMainPages() {

        var deferred = $q.defer();

        var rootGroup   = null;
        var permissions = null;

        /**
         * Resolves the main pages retrieval promise, if possible. If
         * insufficient data is available, this function does nothing.
         */
        var resolveMainPages = function resolveMainPages() {
            if (rootGroup && permissions)
                deferred.resolve(generateMainPages(rootGroup, permissions));
        };

        // Retrieve root group, resolving main pages if possible
        connectionGroupService.getConnectionGroupTree(ConnectionGroup.ROOT_IDENTIFIER)
        .success(function rootConnectionGroupRetrieved(retrievedRootGroup) {
            rootGroup = retrievedRootGroup;
            resolveMainPages();
        });

        // Retrieve current permissions, resolving main pages if possible
        permissionService.getPermissions(authenticationService.getCurrentUserID())
        .success(function permissionsRetrieved(retrievedPermissions) {
            permissions = retrievedPermissions;
            resolveMainPages();
        });
        
        return deferred.promise;

    };
   
    return service;
    
}]);