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
 * The controller for editing users.
 */
angular.module('manage').controller('manageUserController', ['$scope', '$injector', 
        function manageUserController($scope, $injector) {
            
    // Required types
    var ConnectionGroup   = $injector.get('ConnectionGroup');
    var PermissionFlagSet = $injector.get('PermissionFlagSet');
    var PermissionSet     = $injector.get('PermissionSet');

    // Required services
    var $location              = $injector.get('$location');
    var $routeParams           = $injector.get('$routeParams');
    var authenticationService  = $injector.get('authenticationService');
    var connectionGroupService = $injector.get('connectionGroupService');
    var guacNotification       = $injector.get('guacNotification');
    var permissionService      = $injector.get('permissionService');
    var schemaService          = $injector.get('schemaService');
    var userService            = $injector.get('userService');

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
     * The username of the user being edited.
     *
     * @type String
     */
    var username = $routeParams.id;

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
     * The root connection group of the connection group hierarchy.
     *
     * @type ConnectionGroup
     */
    $scope.rootGroup = null;
    
    /**
     * Whether the authenticated user has UPDATE permission for the user being edited.
     * 
     * @type Boolean
     */
    $scope.hasUpdatePermission = null;
    
    /**
     * Whether the authenticated user has DELETE permission for the user being edited.
     * 
     * @type Boolean
     */
    $scope.hasDeletePermission = null;

    /**
     * All permissions associated with the current user, or null if the user's
     * permissions have not yet been loaded.
     *
     * @type PermissionSet
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
     * Returns whether critical data has completed being loaded.
     *
     * @returns {Boolean}
     *     true if enough data has been loaded for the user interface to be
     *     useful, false otherwise.
     */
    $scope.isLoaded = function isLoaded() {

        return $scope.user                !== null
            && $scope.permissionFlags     !== null
            && $scope.rootGroup           !== null
            && $scope.permissions         !== null
            && $scope.attributes          !== null
            && $scope.canSaveUser         !== null
            && $scope.canDeleteUser       !== null;

    };

    // Pull user attribute schema
    schemaService.getUserAttributes().success(function attributesReceived(attributes) {
        $scope.attributes = attributes;
    });

    // Pull user data
    userService.getUser(username).success(function userReceived(user) {
        $scope.user = user;
    });

    // Pull user permissions
    permissionService.getPermissions(username).success(function gotPermissions(permissions) {
        $scope.permissionFlags = PermissionFlagSet.fromPermissionSet(permissions);
    });

    // Retrieve all connections for which we have ADMINISTER permission
    connectionGroupService.getConnectionGroupTree(ConnectionGroup.ROOT_IDENTIFIER, [PermissionSet.ObjectPermissionType.ADMINISTER])
    .success(function connectionGroupReceived(rootGroup) {
        $scope.rootGroup = rootGroup;
    });
    
    // Query the user's permissions for the current connection
    permissionService.getPermissions(authenticationService.getCurrentUsername())
            .success(function permissionsReceived(permissions) {

        $scope.permissions = permissions;
                        
        // Check if the user is new or if the user has UPDATE permission
        $scope.canSaveUser =
              !username
           || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
           || PermissionSet.hasUserPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE, username);

        // Check if user is not new and the user has DELETE permission
        $scope.canDeleteUser =
           !!username && (
                  PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
              ||  PermissionSet.hasUserPermission(permissions, PermissionSet.ObjectPermissionType.DELETE, username)
           );

    });

    /**
     * Available system permission types, as translation string / internal
     * value pairs.
     * 
     * @type Object[]
     */
    $scope.systemPermissionTypes = [
        {
            label: "MANAGE_USER.FIELD_HEADER_ADMINISTER_SYSTEM",
            value: PermissionSet.SystemPermissionType.ADMINISTER
        },
        {
            label: "MANAGE_USER.FIELD_HEADER_CREATE_NEW_USERS",
            value: PermissionSet.SystemPermissionType.CREATE_USER
        },
        {
            label: "MANAGE_USER.FIELD_HEADER_CREATE_NEW_CONNECTIONS",
            value: PermissionSet.SystemPermissionType.CREATE_CONNECTION
        },
        {
            label: "MANAGE_USER.FIELD_HEADER_CREATE_NEW_CONNECTION_GROUPS",
            value: PermissionSet.SystemPermissionType.CREATE_CONNECTION_GROUP
        }
    ];

    /**
     * The set of permissions that will be added to the user when the user is
     * saved. Permissions will only be present in this set if they are
     * manually added, and not later manually removed before saving.
     *
     * @type PermissionSet
     */
    var permissionsAdded = new PermissionSet();

    /**
     * The set of permissions that will be removed from the user when the user 
     * is saved. Permissions will only be present in this set if they are
     * manually removed, and not later manually added before saving.
     *
     * @type PermissionSet
     */
    var permissionsRemoved = new PermissionSet();

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets to
     * reflect the addition of the given system permission.
     * 
     * @param {String} type
     *     The system permission to add, as defined by
     *     PermissionSet.SystemPermissionType.
     */
    var addSystemPermission = function addSystemPermission(type) {

        // If permission was previously removed, simply un-remove it
        if (PermissionSet.hasSystemPermission(permissionsRemoved, type))
            PermissionSet.removeSystemPermission(permissionsRemoved, type);

        // Otherwise, explicitly add the permission
        else
            PermissionSet.addSystemPermission(permissionsAdded, type);

    };

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets to
     * reflect the removal of the given system permission.
     *
     * @param {String} type
     *     The system permission to remove, as defined by
     *     PermissionSet.SystemPermissionType.
     */
    var removeSystemPermission = function removeSystemPermission(type) {

        // If permission was previously added, simply un-add it
        if (PermissionSet.hasSystemPermission(permissionsAdded, type))
            PermissionSet.removeSystemPermission(permissionsAdded, type);

        // Otherwise, explicitly remove the permission
        else
            PermissionSet.addSystemPermission(permissionsRemoved, type);

    };

    /**
     * Notifies the controller that a change has been made to the given
     * system permission for the user being edited.
     *
     * @param {String} type
     *     The system permission that was changed, as defined by
     *     PermissionSet.SystemPermissionType.
     */
    $scope.systemPermissionChanged = function systemPermissionChanged(type) {

        // Determine current permission setting
        var value = $scope.permissionFlags.systemPermissions[type];

        // Add/remove permission depending on flag state
        if (value)
            addSystemPermission(type);
        else
            removeSystemPermission(type);

    };

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets to
     * reflect the addition of the given user permission.
     * 
     * @param {String} type
     *     The user permission to add, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param {String} identifier
     *     The identifier of the user affected by the permission being added.
     */
    var addUserPermission = function addUserPermission(type, identifier) {

        // If permission was previously removed, simply un-remove it
        if (PermissionSet.hasUserPermission(permissionsRemoved, type, identifier))
            PermissionSet.removeUserPermission(permissionsRemoved, type, identifier);

        // Otherwise, explicitly add the permission
        else
            PermissionSet.addUserPermission(permissionsAdded, type, identifier);

    };

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets to
     * reflect the removal of the given user permission.
     *
     * @param {String} type
     *     The user permission to remove, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param {String} identifier
     *     The identifier of the user affected by the permission being removed.
     */
    var removeUserPermission = function removeUserPermission(type, identifier) {

        // If permission was previously added, simply un-add it
        if (PermissionSet.hasUserPermission(permissionsAdded, type, identifier))
            PermissionSet.removeUserPermission(permissionsAdded, type, identifier);

        // Otherwise, explicitly remove the permission
        else
            PermissionSet.addUserPermission(permissionsRemoved, type, identifier);

    };

    /**
     * Notifies the controller that a change has been made to the given user
     * permission for the user being edited.
     *
     * @param {String} type
     *     The user permission that was changed, as defined by
     *     PermissionSet.ObjectPermissionType.
     *
     * @param {String} identifier
     *     The identifier of the user affected by the changed permission.
     */
    $scope.userPermissionChanged = function userPermissionChanged(type, identifier) {

        // Determine current permission setting
        var value = $scope.permissionFlags.userPermissions[type][identifier];

        // Add/remove permission depending on flag state
        if (value)
            addUserPermission(type, identifier);
        else
            removeUserPermission(type, identifier);

    };

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets to
     * reflect the addition of the given connection permission.
     * 
     * @param {String} identifier
     *     The identifier of the connection to add READ permission for.
     */
    var addConnectionPermission = function addConnectionPermission(identifier) {

        // If permission was previously removed, simply un-remove it
        if (PermissionSet.hasConnectionPermission(permissionsRemoved, PermissionSet.ObjectPermissionType.READ, identifier))
            PermissionSet.removeConnectionPermission(permissionsRemoved, PermissionSet.ObjectPermissionType.READ, identifier);

        // Otherwise, explicitly add the permission
        else
            PermissionSet.addConnectionPermission(permissionsAdded, PermissionSet.ObjectPermissionType.READ, identifier);

    };

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets to
     * reflect the removal of the given connection permission.
     *
     * @param {String} identifier
     *     The identifier of the connection to remove READ permission for.
     */
    var removeConnectionPermission = function removeConnectionPermission(identifier) {

        // If permission was previously added, simply un-add it
        if (PermissionSet.hasConnectionPermission(permissionsAdded, PermissionSet.ObjectPermissionType.READ, identifier))
            PermissionSet.removeConnectionPermission(permissionsAdded, PermissionSet.ObjectPermissionType.READ, identifier);

        // Otherwise, explicitly remove the permission
        else
            PermissionSet.addConnectionPermission(permissionsRemoved, PermissionSet.ObjectPermissionType.READ, identifier);

    };

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets to
     * reflect the addition of the given connection group permission.
     * 
     * @param {String} identifier
     *     The identifier of the connection group to add READ permission for.
     */
    var addConnectionGroupPermission = function addConnectionGroupPermission(identifier) {

        // If permission was previously removed, simply un-remove it
        if (PermissionSet.hasConnectionGroupPermission(permissionsRemoved, PermissionSet.ObjectPermissionType.READ, identifier))
            PermissionSet.removeConnectionGroupPermission(permissionsRemoved, PermissionSet.ObjectPermissionType.READ, identifier);

        // Otherwise, explicitly add the permission
        else
            PermissionSet.addConnectionGroupPermission(permissionsAdded, PermissionSet.ObjectPermissionType.READ, identifier);

    };

    /**
     * Updates the permissionsAdded and permissionsRemoved permission sets to
     * reflect the removal of the given connection permission.
     *
     * @param {String} identifier
     *     The identifier of the connection to remove READ permission for.
     */
    var removeConnectionGroupPermission = function removeConnectionGroupPermission(identifier) {

        // If permission was previously added, simply un-add it
        if (PermissionSet.hasConnectionGroupPermission(permissionsAdded, PermissionSet.ObjectPermissionType.READ, identifier))
            PermissionSet.removeConnectionGroupPermission(permissionsAdded, PermissionSet.ObjectPermissionType.READ, identifier);

        // Otherwise, explicitly remove the permission
        else
            PermissionSet.addConnectionGroupPermission(permissionsRemoved, PermissionSet.ObjectPermissionType.READ, identifier);

    };

    // Expose permission query and modification functions to group list template
    $scope.groupListContext = {

        /**
         * Returns the PermissionFlagSet that contains the current state of
         * granted permissions.
         *
         * @returns {PermissionFlagSet}
         *     The PermissionFlagSet describing the current state of granted
         *     permissions for the user being edited.
         */
        getPermissionFlags : function getPermissionFlags() {
            return $scope.permissionFlags;
        },

        /**
         * Notifies the controller that a change has been made to the given
         * connection permission for the user being edited. This only applies
         * to READ permissions.
         *
         * @param {String} identifier
         *     The identifier of the connection affected by the changed
         *     permission.
         */
        connectionPermissionChanged : function connectionPermissionChanged(identifier) {

            // Determine current permission setting
            var value = $scope.permissionFlags.connectionPermissions.READ[identifier];

            // Add/remove permission depending on flag state
            if (value)
                addConnectionPermission(identifier);
            else
                removeConnectionPermission(identifier);

        },

        /**
         * Notifies the controller that a change has been made to the given
         * connection group permission for the user being edited. This only
         * applies to READ permissions.
         *
         * @param {String} identifier
         *     The identifier of the connection group affected by the changed
         *     permission.
         */
        connectionGroupPermissionChanged : function connectionGroupPermissionChanged(identifier) {

            // Determine current permission setting
            var value = $scope.permissionFlags.connectionGroupPermissions.READ[identifier];

            // Add/remove permission depending on flag state
            if (value)
                addConnectionGroupPermission(identifier);
            else
                removeConnectionGroupPermission(identifier);

        }

    };

    /**
     * Cancels all pending edits, returning to the management page.
     */
    $scope.cancel = function cancel() {
        $location.path('/settings/users');
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
                'text'       : 'MANAGE_USER.ERROR_PASSWORD_MISMATCH',
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
            return;
        }

        // Save the user
        userService.saveUser($scope.user)
        .success(function savedUser() {

            // Upon success, save any changed permissions
            permissionService.patchPermissions($scope.user.username, permissionsAdded, permissionsRemoved)
            .success(function patchedUserPermissions() {
                $location.path('/settings/users');
            })

            // Notify of any errors
            .error(function userPermissionsPatchFailed(error) {
                guacNotification.showStatus({
                    'className'  : 'error',
                    'title'      : 'MANAGE_USER.DIALOG_HEADER_ERROR',
                    'text'       : error.message,
                    'actions'    : [ ACKNOWLEDGE_ACTION ]
                });
            });

        })

        // Notify of any errors
        .error(function userSaveFailed(error) {
            guacNotification.showStatus({
                'className'  : 'error',
                'title'      : 'MANAGE_USER.DIALOG_HEADER_ERROR',
                'text'       : error.message,
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
        });

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
        userService.deleteUser($scope.user)
        .success(function deletedUser() {
            $location.path('/settings/users');
        })

        // Notify of any errors
        .error(function userDeletionFailed(error) {
            guacNotification.showStatus({
                'className'  : 'error',
                'title'      : 'MANAGE_USER.DIALOG_HEADER_ERROR',
                'text'       : error.message,
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
        });

    };

    /**
     * Deletes the user, prompting the user first to confirm that deletion is
     * desired.
     */
    $scope.deleteUser = function deleteUser() {

        // Confirm deletion request
        guacNotification.showStatus({
            'title'      : 'MANAGE_USER.DIALOG_HEADER_CONFIRM_DELETE',
            'text'       : 'MANAGE_USER.TEXT_CONFIRM_DELETE',
            'actions'    : [ DELETE_ACTION, CANCEL_ACTION]
        });

    };

}]);
