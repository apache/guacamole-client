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
 * The controller for editing or creating sharing profiles.
 */
angular.module('manage').controller('manageSharingProfileController', ['$scope', '$injector',
        function manageSharingProfileController($scope, $injector) {

    // Required types
    var SharingProfile = $injector.get('SharingProfile');
    var PermissionSet  = $injector.get('PermissionSet');

    // Required services
    var $location                = $injector.get('$location');
    var $routeParams             = $injector.get('$routeParams');
    var authenticationService    = $injector.get('authenticationService');
    var connectionService        = $injector.get('connectionService');
    var guacNotification         = $injector.get('guacNotification');
    var permissionService        = $injector.get('permissionService');
    var schemaService            = $injector.get('schemaService');
    var sharingProfileService    = $injector.get('sharingProfileService');
    var translationStringService = $injector.get('translationStringService');

    /**
     * An action which can be provided along with the object sent to showStatus
     * to allow the user to acknowledge (and close) the currently-shown status
     * dialog.
     */
    var ACKNOWLEDGE_ACTION = {
        name        : "MANAGE_SHARING_PROFILE.ACTION_ACKNOWLEDGE",
        callback    : function acknowledgeCallback() {
            guacNotification.showStatus(false);
        }
    };

    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog, effectively canceling the
     * operation which was pending user confirmation.
     */
    var CANCEL_ACTION = {
        name        : "MANAGE_SHARING_PROFILE.ACTION_CANCEL",
        callback    : function cancelCallback() {
            guacNotification.showStatus(false);
        }
    };

    /**
     * The unique identifier of the data source containing the sharing profile
     * being edited.
     *
     * @type String
     */
    $scope.selectedDataSource = $routeParams.dataSource;

    /**
     * The identifier of the original sharing profile from which this sharing
     * profile is being cloned. Only valid if this is a new sharing profile.
     *
     * @type String
     */
    var cloneSourceIdentifier = $location.search().clone;

    /**
     * The identifier of the sharing profile being edited. If a new sharing
     * profile is being created, this will not be defined.
     *
     * @type String
     */
    var identifier = $routeParams.id;

    /**
     * Map of protocol name to corresponding Protocol object.
     *
     * @type Object.<String, Protocol>
     */
    $scope.protocols = null;

    /**
     * The sharing profile being modified.
     *
     * @type SharingProfile
     */
    $scope.sharingProfile = null;

    /**
     * The parameter name/value pairs associated with the sharing profile being
     * modified.
     *
     * @type Object.<String, String>
     */
    $scope.parameters = null;

    /**
     * Whether the user can save the sharing profile being edited. This could be
     * updating an existing sharing profile, or creating a new sharing profile.
     *
     * @type Boolean
     */
    $scope.canSaveSharingProfile = null;

    /**
     * Whether the user can delete the sharing profile being edited.
     *
     * @type Boolean
     */
    $scope.canDeleteSharingProfile = null;

    /**
     * Whether the user can clone the sharing profile being edited.
     *
     * @type Boolean
     */
    $scope.canCloneSharingProfile = null;

    /**
     * All permissions associated with the current user, or null if the user's
     * permissions have not yet been loaded.
     *
     * @type PermissionSet
     */
    $scope.permissions = null;

    /**
     * All available sharing profile attributes. This is only the set of
     * attribute definitions, organized as logical groupings of attributes, not
     * attribute values.
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

        return $scope.protocols               !== null
            && $scope.sharingProfile          !== null
            && $scope.primaryConnection       !== null
            && $scope.parameters              !== null
            && $scope.permissions             !== null
            && $scope.attributes              !== null
            && $scope.canSaveSharingProfile   !== null
            && $scope.canDeleteSharingProfile !== null
            && $scope.canCloneSharingProfile  !== null;

    };

    // Pull sharing profile attribute schema
    schemaService.getSharingProfileAttributes($scope.selectedDataSource)
    .success(function attributesReceived(attributes) {
        $scope.attributes = attributes;
    });

    // Query the user's permissions for the current sharing profile
    permissionService.getPermissions($scope.selectedDataSource, authenticationService.getCurrentUsername())
    .success(function permissionsReceived(permissions) {

        $scope.permissions = permissions;

        // The sharing profile can be saved if it is new or if the user has
        // UPDATE permission for that sharing profile (either explicitly or by
        // virtue of being an administrator)
        $scope.canSaveSharingProfile =
               !identifier
            || PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
            || PermissionSet.hasSharingProfilePermission(permissions, PermissionSet.ObjectPermissionType.UPDATE, identifier);

        // The sharing profile can be saved only if it exists and the user has
        // DELETE permission (either explicitly or by virtue of being an
        // administrator)
        $scope.canDeleteSharingProfile =
            !!identifier && (
                   PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER)
               ||  PermissionSet.hasSharingProfilePermission(permissions, PermissionSet.ObjectPermissionType.DELETE, identifier)
            );

        // The sharing profile can be cloned only if it exists, the user has
        // UPDATE permission on the sharing profile being cloned (is able to
        // read parameters), and the user can create new sharing profiles
        $scope.canCloneSharingProfile =
            !!identifier && (
               PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.ADMINISTER) || (
                       PermissionSet.hasSharingProfilePermission(permissions, PermissionSet.ObjectPermissionType.UPDATE, identifier)
                   &&  PermissionSet.hasSystemPermission(permissions, PermissionSet.SystemPermissionType.CREATE_SHARING_PROFILE)
               )
            );

    });

    // Get protocol metadata
    schemaService.getProtocols($scope.selectedDataSource)
    .success(function protocolsReceived(protocols) {
        $scope.protocols = protocols;
    });

    // If we are editing an existing sharing profile, pull its data
    if (identifier) {

        // Pull data from existing sharing profile
        sharingProfileService.getSharingProfile($scope.selectedDataSource, identifier)
        .success(function sharingProfileRetrieved(sharingProfile) {
            $scope.sharingProfile = sharingProfile;
        });

        // Pull sharing profile parameters
        sharingProfileService.getSharingProfileParameters($scope.selectedDataSource, identifier)
        .success(function parametersReceived(parameters) {
            $scope.parameters = parameters;
        });

    }

    // If we are cloning an existing sharing profile, pull its data instead
    else if (cloneSourceIdentifier) {

        // Pull data from cloned sharing profile
        sharingProfileService.getSharingProfile($scope.selectedDataSource, cloneSourceIdentifier)
        .success(function sharingProfileRetrieved(sharingProfile) {

            // Store data of sharing profile being cloned
            $scope.sharingProfile = sharingProfile;

            // Clear the identifier field because this sharing profile is new
            delete $scope.sharingProfile.identifier;

        });

        // Pull sharing profile parameters from cloned sharing profile
        sharingProfileService.getSharingProfileParameters($scope.selectedDataSource, cloneSourceIdentifier)
        .success(function parametersReceived(parameters) {
            $scope.parameters = parameters;
        });

    }

    // If we are creating a new sharing profile, populate skeleton sharing
    // profile data
    else {

        $scope.sharingProfile = new SharingProfile({
            primaryConnectionIdentifier : $location.search().parent
        });

        $scope.parameters = {};

    }

    // Populate primary connection once its identifier is known
    $scope.$watch('sharingProfile.primaryConnectionIdentifier',
        function retrievePrimaryConnection(identifier) {

        // Pull data from existing sharing profile
        connectionService.getConnection($scope.selectedDataSource, identifier)
        .success(function connectionRetrieved(connection) {
            $scope.primaryConnection = connection;
        });

    });

    /**
     * Returns whether the current user can change/set all sharing profile
     * attributes for the sharing profile being edited, regardless of whether
     * those attributes are already explicitly associated with that sharing
     * profile.
     *
     * @returns {Boolean}
     *     true if the current user can change all attributes for the sharing
     *     profile being edited, regardless of whether those attributes are
     *     already explicitly associated with that sharing profile, false
     *     otherwise.
     */
    $scope.canChangeAllAttributes = function canChangeAllAttributes() {

        // All attributes can be set if we are creating the sharing profile
        return !identifier;

    };

    /**
     * Returns the translation string namespace for the protocol having the
     * given name. The namespace will be of the form:
     *
     * <code>PROTOCOL_NAME</code>
     *
     * where <code>NAME</code> is the protocol name transformed via
     * translationStringService.canonicalize().
     *
     * @param {String} protocolName
     *     The name of the protocol.
     *
     * @returns {String}
     *     The translation namespace for the protocol specified, or null if no
     *     namespace could be generated.
     */
    $scope.getNamespace = function getNamespace(protocolName) {

        // Do not generate a namespace if no protocol is selected
        if (!protocolName)
            return null;

        return 'PROTOCOL_' + translationStringService.canonicalize(protocolName);

    };

    /**
     * Cancels all pending edits, returning to the management page.
     */
    $scope.cancel = function cancel() {
        $location.url('/settings/' + encodeURIComponent($scope.selectedDataSource) + '/connections');
    };

    /**
     * Cancels all pending edits, opening an edit page for a new sharing profile
     * which is prepopulated with the data from the sharing profile currently
     * being edited.
     */
    $scope.cloneSharingProfile = function cloneSharingProfile() {
        $location.path('/manage/' + encodeURIComponent($scope.selectedDataSource) + '/sharingProfiles').search('clone', identifier);
    };

    /**
     * Saves the sharing profile, creating a new sharing profile or updating
     * the existing sharing profile.
     */
    $scope.saveSharingProfile = function saveSharingProfile() {

        $scope.sharingProfile.parameters = $scope.parameters;

        // Save the sharing profile
        sharingProfileService.saveSharingProfile($scope.selectedDataSource, $scope.sharingProfile)
        .success(function savedSharingProfile() {
            $location.url('/settings/' + encodeURIComponent($scope.selectedDataSource) + '/connections');
        })

        // Notify of any errors
        .error(function sharingProfileSaveFailed(error) {
            guacNotification.showStatus({
                'className'  : 'error',
                'title'      : 'MANAGE_SHARING_PROFILE.DIALOG_HEADER_ERROR',
                'text'       : error.translatableMessage,
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
        });

    };

    /**
     * An action to be provided along with the object sent to showStatus which
     * immediately deletes the current sharing profile.
     */
    var DELETE_ACTION = {
        name        : "MANAGE_SHARING_PROFILE.ACTION_DELETE",
        className   : "danger",
        // Handle action
        callback    : function deleteCallback() {
            deleteSharingProfileImmediately();
            guacNotification.showStatus(false);
        }
    };

    /**
     * Immediately deletes the current sharing profile, without prompting the
     * user for confirmation.
     */
    var deleteSharingProfileImmediately = function deleteSharingProfileImmediately() {

        // Delete the sharing profile
        sharingProfileService.deleteSharingProfile($scope.selectedDataSource, $scope.sharingProfile)
        .success(function deletedSharingProfile() {
            $location.path('/settings/' + encodeURIComponent($scope.selectedDataSource) + '/connections');
        })

        // Notify of any errors
        .error(function sharingProfileDeletionFailed(error) {
            guacNotification.showStatus({
                'className'  : 'error',
                'title'      : 'MANAGE_SHARING_PROFILE.DIALOG_HEADER_ERROR',
                'text'       : error.translatableMessage,
                'actions'    : [ ACKNOWLEDGE_ACTION ]
            });
        });

    };

    /**
     * Deletes the sharing profile, prompting the user first to confirm that
     * deletion is desired.
     */
    $scope.deleteSharingProfile = function deleteSharingProfile() {

        // Confirm deletion request
        guacNotification.showStatus({
            'title'      : 'MANAGE_SHARING_PROFILE.DIALOG_HEADER_CONFIRM_DELETE',
            'text'       : {
                'key' : 'MANAGE_SHARING_PROFILE.TEXT_CONFIRM_DELETE'
            },
            'actions'    : [ DELETE_ACTION, CANCEL_ACTION]
        });

    };

}]);
