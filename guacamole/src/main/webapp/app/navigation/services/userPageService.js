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
 * A service for generating all the important pages a user can visit.
 */
angular.module('navigation').factory('userPageService', ['$injector',
        function userPageService($injector) {

    // Get required types
    var ClientIdentifier = $injector.get('ClientIdentifier');
    var ConnectionGroup  = $injector.get('ConnectionGroup');
    var PageDefinition   = $injector.get('PageDefinition');
    var PermissionSet    = $injector.get('PermissionSet');

    // Get required services
    var $q                       = $injector.get('$q');
    var authenticationService    = $injector.get('authenticationService');
    var connectionGroupService   = $injector.get('connectionGroupService');
    var dataSourceService        = $injector.get('dataSourceService');
    var permissionService        = $injector.get('permissionService');
    var translationStringService = $injector.get('translationStringService');
    
    var service = {};
    
    /**
     * The home page to assign to a user if they can navigate to more than one
     * page.
     * 
     * @type PageDefinition
     */
    var SYSTEM_HOME_PAGE = new PageDefinition({
        name : 'USER_MENU.ACTION_NAVIGATE_HOME',
        url  : '/'
    });

    /**
     * Returns an appropriate home page for the current user.
     *
     * @param {Object.<String, ConnectionGroup>} rootGroups
     *     A map of all root connection groups visible to the current user,
     *     where each key is the identifier of the corresponding data source.
     *
     * @param {Object.<String, PermissionSet>} permissions
     *     A map of all permissions granted to the current user, where each
     *     key is the identifier of the corresponding data source.
     *
     * @returns {PageDefinition}
     *     The user's home page.
     */
    var generateHomePage = function generateHomePage(rootGroups, permissions) {

        var homePage = null;
        var settingsPages = generateSettingsPages(permissions);

        // If user has access to settings pages, return home page and skip
        // evaluation for automatic connections.  The Preferences page is
        // a Settings page and is always visible, so we look for more than
        // one to indicate access to administrative pages.
        if (settingsPages.length > 1)
            return SYSTEM_HOME_PAGE;

        // Determine whether a connection or balancing group should serve as
        // the home page
        for (var dataSource in rootGroups) {

            // Get corresponding root group
            var rootGroup = rootGroups[dataSource];

            // Get children
            var connections      = rootGroup.childConnections      || [];
            var connectionGroups = rootGroup.childConnectionGroups || [];

            // Calculate total number of root-level objects
            var totalRootObjects = connections.length + connectionGroups.length;

            // If exactly one connection or balancing group is available, use
            // that as the home page
            if (homePage === null && totalRootObjects === 1) {

                var connection      = connections[0];
                var connectionGroup = connectionGroups[0];

                // Only one connection present, use as home page
                if (connection) {
                    homePage = new PageDefinition({
                        name : connection.name,
                        url  : '/client/' + ClientIdentifier.toString({
                            dataSource : dataSource,
                            type       : ClientIdentifier.Types.CONNECTION,
                            id         : connection.identifier
                        })
                    });
                }

                // Only one balancing group present, use as home page
                if (connectionGroup
                        && connectionGroup.type === ConnectionGroup.Type.BALANCING
                        && _.isEmpty(connectionGroup.childConnections)
                        && _.isEmpty(connectionGroup.childConnectionGroups)) {
                    homePage = new PageDefinition({
                        name : connectionGroup.name,
                        url  : '/client/' + ClientIdentifier.toString({
                            dataSource : dataSource,
                            type       : ClientIdentifier.Types.CONNECTION_GROUP,
                            id         : connectionGroup.identifier
                        })
                    });
                }

            }

            // Otherwise, a connection or balancing group cannot serve as the
            // home page
            else if (totalRootObjects >= 1) {
                homePage = null;
                break;
            }

        } // end for each data source

        // Use default home page if no other is available
        return homePage || SYSTEM_HOME_PAGE;

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

        // Resolve promise using home page derived from root connection groups
        var getRootGroups = dataSourceService.apply(
            connectionGroupService.getConnectionGroupTree,
            authenticationService.getAvailableDataSources(),
            ConnectionGroup.ROOT_IDENTIFIER
        );
        var getPermissionSets = dataSourceService.apply(
            permissionService.getPermissions,
            authenticationService.getAvailableDataSources(),
            authenticationService.getCurrentUsername()
        );

        $q.all({
            rootGroups : getRootGroups,
            permissionsSets : getPermissionSets
        })
        .then(function rootConnectionGroupsPermissionsRetrieved(data) {
            deferred.resolve(generateHomePage(data.rootGroups,data.permissionsSets));
        });

        return deferred.promise;

    };

    /**
     * Returns all settings pages that the current user can visit. This can
     * include any of the various manage pages.
     * 
     * @param {Object.<String, PermissionSet>} permissionSets
     *     A map of all permissions granted to the current user, where each
     *     key is the identifier of the corresponding data source.
     * 
     * @returns {Page[]} 
     *     An array of all settings pages that the current user can visit.
     */
    var generateSettingsPages = function generateSettingsPages(permissionSets) {
        
        var pages = [];
        
        var canManageUsers = [];
        var canManageConnections = [];
        var canViewConnectionRecords = [];
        var canManageSessions = [];

        // Inspect the contents of each provided permission set
        angular.forEach(authenticationService.getAvailableDataSources(), function inspectPermissions(dataSource) {

            // Get permissions for current data source, skipping if non-existent
            var permissions = permissionSets[dataSource];
            if (!permissions)
                return;

            // Do not modify original object
            permissions = angular.copy(permissions);

            // Ignore permission to update root group
            PermissionSet.removeConnectionGroupPermission(permissions,
                PermissionSet.ObjectPermissionType.UPDATE,
                ConnectionGroup.ROOT_IDENTIFIER);

            // Ignore permission to update self
            PermissionSet.removeUserPermission(permissions,
                PermissionSet.ObjectPermissionType.UPDATE,
                authenticationService.getCurrentUsername());

            // Determine whether the current user needs access to the user management UI
            if (
                    // System permissions
                       PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
                    || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_USER)

                    // Permission to update users
                    || PermissionSet.hasUserPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE)

                    // Permission to delete users
                    || PermissionSet.hasUserPermission(permissions, PermissionSet.ObjectPermissionType.DELETE)

                    // Permission to administer users
                    || PermissionSet.hasUserPermission(permissions, PermissionSet.ObjectPermissionType.ADMINISTER)
            ) {
                canManageUsers.push(dataSource);
            }

            // Determine whether the current user needs access to the connection management UI
            if (
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
                    || PermissionSet.hasConnectionGroupPermission(permissions, PermissionSet.ObjectPermissionType.ADMINISTER)
            ) {
                canManageConnections.push(dataSource);
            }

            // Determine whether the current user needs access to the session management UI or view connection history
            if (
                    // A user must be a system administrator to manage sessions
                    PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
            ) {
                canManageSessions.push(dataSource);
                canViewConnectionRecords.push(dataSource);
            }

        });

        // If user can manage sessions, add link to sessions management page
        if (canManageSessions.length) {
            pages.push(new PageDefinition({
                name : 'USER_MENU.ACTION_MANAGE_SESSIONS',
                url  : '/settings/sessions'
            }));
        }

        // If user can manage connections, add links for connection management pages
        angular.forEach(canViewConnectionRecords, function addConnectionHistoryLink(dataSource) {
            pages.push(new PageDefinition({
                name : [
                    'USER_MENU.ACTION_VIEW_HISTORY',
                    translationStringService.canonicalize('DATA_SOURCE_' + dataSource) + '.NAME'
                ],
                url  : '/settings/' + encodeURIComponent(dataSource) + '/history'
            }));
        });

        // If user can manage users, add link to user management page
        if (canManageUsers.length) {
            pages.push(new PageDefinition({
                name : 'USER_MENU.ACTION_MANAGE_USERS',
                url  : '/settings/users'
            }));
        }

        // If user can manage connections, add links for connection management pages
        angular.forEach(canManageConnections, function addConnectionManagementLink(dataSource) {
            pages.push(new PageDefinition({
                name : [
                    'USER_MENU.ACTION_MANAGE_CONNECTIONS',
                    translationStringService.canonicalize('DATA_SOURCE_' + dataSource) + '.NAME'
                ],
                url  : '/settings/' + encodeURIComponent(dataSource) + '/connections'
            }));
        });

        // Add link to user preferences (always accessible)
        pages.push(new PageDefinition({
            name : 'USER_MENU.ACTION_MANAGE_PREFERENCES',
            url  : '/settings/preferences'
        }));

        return pages;
    };

    /**
     * Returns a promise which resolves to an array of all settings pages that
     * the current user can visit. This can include any of the various manage
     * pages.
     *
     * @returns {Promise.<Page[]>} 
     *     A promise which resolves to an array of all settings pages that the
     *     current user can visit.
     */
    service.getSettingsPages = function getSettingsPages() {

        var deferred = $q.defer();

        // Retrieve current permissions
        dataSourceService.apply(
            permissionService.getPermissions,
            authenticationService.getAvailableDataSources(),
            authenticationService.getCurrentUsername() 
        )

        // Resolve promise using settings pages derived from permissions
        .then(function permissionsRetrieved(permissions) {
            deferred.resolve(generateSettingsPages(permissions));
        });
        
        return deferred.promise;

    };
   
    /**
     * Returns all the main pages that the current user can visit. This can 
     * include the home page, manage pages, etc. In the case that there are no 
     * applicable pages of this sort, it may return a client page.
     * 
     * @param {Object.<String, ConnectionGroup>} rootGroups
     *     A map of all root connection groups visible to the current user,
     *     where each key is the identifier of the corresponding data source.
     *     
     * @param {Object.<String, PermissionSet>} permissions
     *     A map of all permissions granted to the current user, where each
     *     key is the identifier of the corresponding data source.
     * 
     * @returns {Page[]} 
     *     An array of all main pages that the current user can visit.
     */
    var generateMainPages = function generateMainPages(rootGroups, permissions) {
        
        var pages = [];

        // Get home page and settings pages
        var homePage = generateHomePage(rootGroups, permissions);
        var settingsPages = generateSettingsPages(permissions);

        // Only include the home page in the list of main pages if the user
        // can navigate elsewhere.
        if (homePage === SYSTEM_HOME_PAGE || settingsPages.length)
            pages.push(homePage);

        // Add generic link to the first-available settings page
        if (settingsPages.length) {
            pages.push(new PageDefinition({
                name : 'USER_MENU.ACTION_MANAGE_SETTINGS',
                url  : settingsPages[0].url
            }));
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

        var rootGroups  = null;
        var permissions = null;

        /**
         * Resolves the main pages retrieval promise, if possible. If
         * insufficient data is available, this function does nothing.
         */
        var resolveMainPages = function resolveMainPages() {
            if (rootGroups && permissions)
                deferred.resolve(generateMainPages(rootGroups, permissions));
        };

        // Retrieve root group, resolving main pages if possible
        dataSourceService.apply(
            connectionGroupService.getConnectionGroupTree,
            authenticationService.getAvailableDataSources(),
            ConnectionGroup.ROOT_IDENTIFIER
        )
        .then(function rootConnectionGroupsRetrieved(retrievedRootGroups) {
            rootGroups = retrievedRootGroups;
            resolveMainPages();
        });

        // Retrieve current permissions
        dataSourceService.apply(
            permissionService.getPermissions,
            authenticationService.getAvailableDataSources(),
            authenticationService.getCurrentUsername()
        )

        // Resolving main pages if possible
        .then(function permissionsRetrieved(retrievedPermissions) {
            permissions = retrievedPermissions;
            resolveMainPages();
        });
        
        return deferred.promise;

    };
   
    return service;
    
}]);
