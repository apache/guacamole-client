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
 * The controller for editing users.
 */
angular.module('manage').controller('manageUserController', ['$scope', '$injector', 
        function manageUserController($scope, $injector) {
            
    // Required types
    var PageDefinition    = $injector.get('PageDefinition');
    var PermissionFlagSet = $injector.get('PermissionFlagSet');
    var PermissionSet     = $injector.get('PermissionSet');
    var User              = $injector.get('User');

    // Required services
    var $location                = $injector.get('$location');
    var $routeParams             = $injector.get('$routeParams');
    var authenticationService    = $injector.get('authenticationService');
    var dataSourceService        = $injector.get('dataSourceService');
    var guacNotification         = $injector.get('guacNotification');
    var permissionService        = $injector.get('permissionService');
    var requestService           = $injector.get('requestService');
    var schemaService            = $injector.get('schemaService');
    var translationStringService = $injector.get('translationStringService');
    var userService              = $injector.get('userService');

    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    var ACKNOWLEDGE_ACTION = {
        name        : "MANAGE_USER.ACTION_ACKNOWLEDGE",
        // Handle action
        callback    : function acknowledgeCallback() {
            guacNotification.showStatus(false);
        }
    };

    /**
     * The identifiers of all data sources currently available to the
     * authenticated user.
     *
     * @type String[]
     */
    var dataSources = authenticationService.getAvailableDataSources();

    /**
     * The username of the current, authenticated user.
     *
     * @type String
     */
    var currentUsername = authenticationService.getCurrentUsername();

    /**
     * The username of the original user from which this user is
     * being cloned. Only valid if this is a new user.
     *
     * @type String
     */
    var cloneSourceUsername = $location.search().clone;

    /**
     * The username of the user being edited. If a new user is
     * being created, this will not be defined.
     *
     * @type String
     */
    var username = $routeParams.id;

    /**
     * The unique identifier of the data source containing the user being
     * edited.
     *
     * @type String
     */
    $scope.dataSource = $routeParams.dataSource;

    /**
     * The string value representing the user currently being edited within the
     * permission flag set. Note that his may not match the user's actual
     * username - it is a marker that is (1) guaranteed to be associated with
     * the current user's permissions in the permission set and (2) guaranteed
     * not to collide with any user that does not represent the current user
     * within the permission set.
     *
     * @type String
     */
    $scope.selfUsername = '';

    /**
     * All user accounts associated with the same username as the account being
     * created or edited, as a map of data source identifier to the User object
     * within that data source.
     *
     * @type Object.<String, User>
     */
    $scope.users = null;

    /**
     * The user being modified.
     *
     * @type User
     */
    $scope.user = null;

    /**
     * All permissions associated with the user being modified.
     * 
     * @type PermissionFlagSet
     */
    $scope.permissionFlags = null;

    /**
     * A map of data source identifiers to the set of all permissions
     * associated with the current user under that data source, or null if the
     * user's permissions have not yet been loaded.
     *
     * @type Object.<String, PermissionSet>
     */
    $scope.permissions = null;

    /**
     * All available user attributes. This is only the set of attribute
     * definitions, organized as logical groupings of attributes, not attribute
     * values.
     *
     * @type Form[]
     */
    $scope.attributes = null;

    /**
     * The pages associated with each user account having the given username.
     * Each user account will be associated with a particular data source.
     *
     * @type PageDefinition[]
     */
    $scope.accountPages = [];

    /**
     * Returns whether critical data has completed being loaded.
     *
     * @returns {Boolean}
     *     true if enough data has been loaded for the user interface to be
     *     useful, false otherwise.
     */
    $scope.isLoaded = function isLoaded() {

        return $scope.users               !== null
            && $scope.permissionFlags     !== null
            && $scope.rootGroups          !== null
            && $scope.permissions         !== null
            && $scope.attributes          !== null;

    };

    /**
     * Returns whether the user being edited already exists within the data
     * source specified.
     *
     * @param {String} [dataSource]
     *     The identifier of the data source to check. If omitted, this will
     *     default to the currently-selected data source.
     *
     * @returns {Boolean}
     *     true if the user being edited already exists, false otherwise.
     */
    $scope.userExists = function userExists(dataSource) {

        // Do not check if users are not yet loaded
        if (!$scope.users)
            return false;

        // Use currently-selected data source if unspecified
        dataSource = dataSource || $scope.dataSource;

        // Account exists only if it was successfully retrieved
        return (dataSource in $scope.users);

    };

    /**
     * Returns whether the current user can change attributes explicitly
     * associated with the user being edited within the given data source.
     *
     * @param {String} [dataSource]
     *     The identifier of the data source to check. If omitted, this will
     *     default to the currently-selected data source.
     *
     * @returns {Boolean}
     *     true if the current user can change attributes associated with the
     *     user being edited, false otherwise.
     */
    $scope.canChangeAttributes = function canChangeAttributes(dataSource) {

        // Do not check if permissions are not yet loaded
        if (!$scope.permissions)
            return false;

        // Use currently-selected data source if unspecified
        dataSource = dataSource || $scope.dataSource;

        // Attributes can always be set if we are creating the user
        if (!$scope.userExists(dataSource))
            return true;

        // The administrator can always change attributes
        if (PermissionSet.hasSystemPermission($scope.permissions[dataSource],
                PermissionSet.SystemPermissionType.ADMINISTER))
            return true;

        // Otherwise, can change attributes if we have permission to update this user
        return PermissionSet.hasUserPermission($scope.permissions[dataSource],
            PermissionSet.ObjectPermissionType.UPDATE, username);

    };

    /**
     * Returns whether the current user can change/set all user attributes for
     * the user being edited, regardless of whether those attributes are
     * already explicitly associated with that user.
     *
     * @returns {Boolean}
     *     true if the current user can change all attributes for the user
     *     being edited, regardless of whether those attributes are already
     *     explicitly associated with that user, false otherwise.
     */
    $scope.canChangeAllAttributes = function canChangeAllAttributes() {

        // All attributes can be set if we are creating the user
        return !$scope.userExists($scope.dataSource);

    };

    /**
     * Returns whether the current user can change permissions of any kind
     * which are associated with the user being edited within the given data
     * source.
     *
     * @param {String} [dataSource]
     *     The identifier of the data source to check. If omitted, this will
     *     default to the currently-selected data source.
     *
     * @returns {Boolean}
     *     true if the current user can grant or revoke permissions of any kind
     *     which are associated with the user being edited, false otherwise.
     */
    $scope.canChangePermissions = function canChangePermissions(dataSource) {

        // Do not check if permissions are not yet loaded
        if (!$scope.permissions)
            return false;

        // Use currently-selected data source if unspecified
        dataSource = dataSource || $scope.dataSource;

        // Permissions can always be set if we are creating the user
        if (!$scope.userExists(dataSource))
            return true;

        // The administrator can always modify permissions
        if (PermissionSet.hasSystemPermission($scope.permissions[dataSource],
                PermissionSet.SystemPermissionType.ADMINISTER))
            return true;

        // Otherwise, can only modify permissions if we have explicit
        // ADMINISTER permission
        return PermissionSet.hasUserPermission($scope.permissions[dataSource],
            PermissionSet.ObjectPermissionType.ADMINISTER, username);

    };

    /**
     * Returns whether the current user can edit the username of the user being
     * edited within the given data source.
     *
     * @param {String} [dataSource]
     *     The identifier of the data source to check. If omitted, this will
     *     default to the currently-selected data source.
     *
     * @returns {Boolean}
     *     true if the current user can edit the username of the user being
     *     edited, false otherwise.
     */
    $scope.canEditUsername = function canEditUsername(dataSource) {
        return !username;
    };

    /**
     * Returns whether the current user can save the user being edited within
     * the given data source. Saving will create or update that user depending
     * on whether the user already exists.
     *
     * @param {String} [dataSource]
     *     The identifier of the data source to check. If omitted, this will
     *     default to the currently-selected data source.
     *
     * @returns {Boolean}
     *     true if the current user can save changes to the user being edited,
     *     false otherwise.
     */
    $scope.canSaveUser = function canSaveUser(dataSource) {

        // Do not check if permissions are not yet loaded
        if (!$scope.permissions)
            return false;

        // Use currently-selected data source if unspecified
        dataSource = dataSource || $scope.dataSource;

        // The administrator can always save users
        if (PermissionSet.hasSystemPermission($scope.permissions[dataSource],
                PermissionSet.SystemPermissionType.ADMINISTER))
            return true;

        // If user does not exist, can only save if we have permission to create users
        if (!$scope.userExists(dataSource))
           return PermissionSet.hasSystemPermission($scope.permissions[dataSource],
               PermissionSet.SystemPermissionType.CREATE_USER);

        // Otherwise, can only save if we have permission to update this user
        return PermissionSet.hasUserPermission($scope.permissions[dataSource],
            PermissionSet.ObjectPermissionType.UPDATE, username);

    };

    /**
     * Returns whether the current user can clone the user being edited within
     * the given data source.
     *
     * @param {String} [dataSource]
     *     The identifier of the data source to check. If omitted, this will
     *     default to the currently-selected data source.
     *
     * @returns {Boolean}
     *     true if the current user can clone the user being edited, false
     *     otherwise.
     */
    $scope.canCloneUser = function canCloneUser(dataSource) {

        // Do not check if permissions are not yet loaded
        if (!$scope.permissions)
            return false;

        // Use currently-selected data source if unspecified
        dataSource = dataSource || $scope.dataSource;

        // If we are not editing an existing user, we cannot clone
        if (!$scope.userExists($scope.dataSource))
            return false;

        // The administrator can always clone users
        if (PermissionSet.hasSystemPermission($scope.permissions[dataSource],
                PermissionSet.SystemPermissionType.ADMINISTER))
            return true;

        // Otherwise we need explicit CREATE_USER permission
        return PermissionSet.hasSystemPermission($scope.permissions[dataSource],
            PermissionSet.SystemPermissionType.CREATE_USER);

    };

    /**
     * Returns whether the current user can delete the user being edited from
     * the given data source.
     *
     * @param {String} [dataSource]
     *     The identifier of the data source to check. If omitted, this will
     *     default to the currently-selected data source.
     *
     * @returns {Boolean}
     *     true if the current user can delete the user being edited, false
     *     otherwise.
     */
    $scope.canDeleteUser = function canDeleteUser(dataSource) {

        // Do not check if permissions are not yet loaded
        if (!$scope.permissions)
            return false;

        // Use currently-selected data source if unspecified
        dataSource = dataSource || $scope.dataSource;

        // Can't delete what doesn't exist
        if (!$scope.userExists(dataSource))
            return false;

        // The administrator can always delete users
        if (PermissionSet.hasSystemPermission($scope.permissions[dataSource],
                PermissionSet.SystemPermissionType.ADMINISTER))
            return true;

        // Otherwise, require explicit DELETE permission on the user
        return PermissionSet.hasUserPermission($scope.permissions[dataSource],
            PermissionSet.ObjectPermissionType.DELETE, username);

    };

    /**
     * Returns whether the user being edited within the given data source is
     * read-only, and thus cannot be modified by the current user.
     *
     * @param {String} [dataSource]
     *     The identifier of the data source to check. If omitted, this will
     *     default to the currently-selected data source.
     *
     * @returns {Boolean}
     *     true if the user being edited is actually read-only and cannot be
     *     edited at all, false otherwise.
     */
    $scope.isReadOnly = function isReadOnly(dataSource) {

        // Use currently-selected data source if unspecified
        dataSource = dataSource || $scope.dataSource;

        // User is read-only if they cannot be saved
        return !$scope.canSaveUser(dataSource);

    };

    // Update visible account pages whenever available users/permissions changes
    $scope.$watchGroup(['users', 'permissions'], function updateAccountPages() {

        // Generate pages for each applicable data source
        $scope.accountPages = [];
        angular.forEach(dataSources, function addAccountPage(dataSource) {

            // Determine whether data source contains this user
            var linked   = $scope.userExists(dataSource);
            var readOnly = $scope.isReadOnly(dataSource);

            // Account is not relevant if it does not exist and cannot be
            // created
            if (!linked && readOnly)
                return;

            // Only the selected data source is relevant when cloning
            if (cloneSourceUsername && dataSource !== $scope.dataSource)
                return;

            // Determine class name based on read-only / linked status
            var className;
            if (readOnly)    className = 'read-only';
            else if (linked) className = 'linked';
            else             className = 'unlinked';

            // Add page entry
            $scope.accountPages.push(new PageDefinition({
                name      : translationStringService.canonicalize('DATA_SOURCE_' + dataSource) + '.NAME',
                url       : '/manage/' + encodeURIComponent(dataSource) + '/users/' + encodeURIComponent(username || ''),
                className : className
            }));

        });

    });

    // Pull user attribute schema
    schemaService.getUserAttributes($scope.dataSource).then(function attributesReceived(attributes) {
        $scope.attributes = attributes;
    }, requestService.WARN);

    // Pull user data and permissions if we are editing an existing user
    if (username) {

        // Pull user data
        dataSourceService.apply(userService.getUser, dataSources, username)
        .then(function usersReceived(users) {

            // Get user for currently-selected data source
            $scope.users = users;
            $scope.user  = users[$scope.dataSource];

            // Create skeleton user if user does not exist
            if (!$scope.user)
                $scope.user = new User({
                    'username' : username
                });

        }, requestService.WARN);

        // The current user will be associated with username of the existing
        // user in the retrieved permission set
        $scope.selfUsername = username;

        // Pull user permissions
        permissionService.getPermissions($scope.dataSource, username).then(function gotPermissions(permissions) {
            $scope.permissionFlags = PermissionFlagSet.fromPermissionSet(permissions);
        })

        // If permissions cannot be retrieved, use empty permissions
        ['catch'](requestService.createErrorCallback(function permissionRetrievalFailed() {
            $scope.permissionFlags = new PermissionFlagSet();
        }));
    }

    // If we are cloning an existing user, pull his/her data instead
    else if (cloneSourceUsername) {

        dataSourceService.apply(userService.getUser, dataSources, cloneSourceUsername)
        .then(function usersReceived(users) {

            // Get user for currently-selected data source
            $scope.users = {};
            $scope.user  = users[$scope.dataSource];

        }, requestService.WARN);

        // The current user will be associated with cloneSourceUsername in the
        // retrieved permission set
        $scope.selfUsername = cloneSourceUsername;

        // Pull user permissions
        permissionService.getPermissions($scope.dataSource, cloneSourceUsername)
        .then(function gotPermissions(permissions) {
            $scope.permissionFlags = PermissionFlagSet.fromPermissionSet(permissions);
            $scope.permissionsAdded = permissions;
        })

        // If permissions cannot be retrieved, use empty permissions
        ['catch'](requestService.createErrorCallback(function permissionRetrievalFailed() {
            $scope.permissionFlags = new PermissionFlagSet();
        }));
    }

    // Use skeleton data if we are creating a new user
    else {

        // No users exist regardless of data source if there is no username
        $scope.users = {};

        // Use skeleton user object with no associated permissions
        $scope.user = new User();
        $scope.permissionFlags = new PermissionFlagSet();

        // As no permissions are yet associated with the user, it is safe to
        // use any non-empty username as a placeholder for self-referential
        // permissions
        $scope.selfUsername = 'SELF';

    }

    // Query the user's permissions for the current user
    dataSourceService.apply(
        permissionService.getEffectivePermissions,
        dataSources,
        currentUsername
    )
    .then(function permissionsReceived(permissions) {
        $scope.permissions = permissions;
    }, requestService.WARN);

    /**
     * The set of permissions that will be added to the user when the user is
     * saved. Permissions will only be present in this set if they are
     * manually added, and not later manually removed before saving.
     *
     * @type PermissionSet
     */
    $scope.permissionsAdded = new PermissionSet();

    /**
     * The set of permissions that will be removed from the user when the user 
     * is saved. Permissions will only be present in this set if they are
     * manually removed, and not later manually added before saving.
     *
     * @type PermissionSet
     */
    $scope.permissionsRemoved = new PermissionSet();

    /**
     * Cancels all pending edits, returning to the management page.
     */
    $scope.cancel = function cancel() {
        $location.url('/settings/users');
    };

    /**
     * Cancels all pending edits, opening an edit page for a new user
     * which is prepopulated with the data from the user currently being edited.
     */
    $scope.cloneUser = function cloneUser() {
        $location.path('/manage/' + encodeURIComponent($scope.dataSource) + '/users').search('clone', username);
    };
            
    /**
     * Saves the user, updating the existing user only.
     */
    $scope.saveUser = function saveUser() {

        // Verify passwords match
        if ($scope.passwordMatch !== $scope.user.password) {
            guacNotification.showStatus({
                'className'  : 'error',
                'title'      : 'MANAGE_USER.DIALOG_HEADER_ERROR',
                'text'       : {
                    key : 'MANAGE_USER.ERROR_PASSWORD_MISMATCH'
                },
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
            return;
        }

        // Save or create the user, depending on whether the user exists
        var saveUserPromise;
        if ($scope.userExists($scope.dataSource))
            saveUserPromise = userService.saveUser($scope.dataSource, $scope.user);
        else
            saveUserPromise = userService.createUser($scope.dataSource, $scope.user);

        saveUserPromise.then(function savedUser() {

            // Move permission flags if username differs from marker
            if ($scope.selfUsername !== $scope.user.username) {

                // Rename added permission
                if ($scope.permissionsAdded.userPermissions[$scope.selfUsername]) {
                    $scope.permissionsAdded.userPermissions[$scope.user.username] = $scope.permissionsAdded.userPermissions[$scope.selfUsername];
                    delete $scope.permissionsAdded.userPermissions[$scope.selfUsername];
                }

                // Rename removed permission
                if ($scope.permissionsRemoved.userPermissions[$scope.selfUsername]) {
                    $scope.permissionsRemoved.userPermissions[$scope.user.username] = $scope.permissionsRemoved.userPermissions[$scope.selfUsername];
                    delete $scope.permissionsRemoved.userPermissions[$scope.selfUsername];
                }
                
            }

            // Upon success, save any changed permissions
            permissionService.patchPermissions($scope.dataSource, $scope.user.username, $scope.permissionsAdded, $scope.permissionsRemoved)
            .then(function patchedUserPermissions() {
                $location.url('/settings/users');
            }, guacNotification.SHOW_REQUEST_ERROR);

        }, guacNotification.SHOW_REQUEST_ERROR);

    };
    
    /**
     * An action to be provided along with the object sent to showStatus which
     * immediately deletes the current user.
     */
    var DELETE_ACTION = {
        name        : "MANAGE_USER.ACTION_DELETE",
        className   : "danger",
        // Handle action
        callback    : function deleteCallback() {
            deleteUserImmediately();
            guacNotification.showStatus(false);
        }
    };

    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    var CANCEL_ACTION = {
        name        : "MANAGE_USER.ACTION_CANCEL",
        // Handle action
        callback    : function cancelCallback() {
            guacNotification.showStatus(false);
        }
    };

    /**
     * Immediately deletes the current user, without prompting the user for
     * confirmation.
     */
    var deleteUserImmediately = function deleteUserImmediately() {

        // Delete the user 
        userService.deleteUser($scope.dataSource, $scope.user)
        .then(function deletedUser() {
            $location.path('/settings/users');
        }, guacNotification.SHOW_REQUEST_ERROR);

    };

    /**
     * Deletes the user, prompting the user first to confirm that deletion is
     * desired.
     */
    $scope.deleteUser = function deleteUser() {

        // Confirm deletion request
        guacNotification.showStatus({
            'title'      : 'MANAGE_USER.DIALOG_HEADER_CONFIRM_DELETE',
            'text'       : {
                key : 'MANAGE_USER.TEXT_CONFIRM_DELETE'
            },
            'actions'    : [ DELETE_ACTION, CANCEL_ACTION]
        });

    };

}]);
