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
 * Directive which displays all active file transfers.
 */
angular.module('client').directive('guacFileTransferManager', [function guacFileTransferManager() {

    return {
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The client whose file transfers should be managed by this
             * directive.
             * 
             * @type ManagerClient
             */
            client : '='

        },

        templateUrl: 'app/client/templates/guacFileTransferManager.html',
        controller: ['$scope', '$injector', function guacFileTransferManagerController($scope, $injector) {

            // Required types
            var ManagedClient            = $injector.get('ManagedClient');
            var ManagedFileTransferState = $injector.get('ManagedFileTransferState');

            /**
             * Determines whether the attached client has associated file
             * transfers, regardless of those file transfers' state.
             *
             * @returns {Boolean}
             *     true if there are any file transfers associated with the
             *     attached client, false otherise.
             */
            $scope.hasTransfers = function hasTransfers() {

                // There are no file transfers if there is no client
                if (!$scope.client)
                    return false;

                return !!($scope.client.uploads.length || $scope.client.downloads.length);

            };

            /**
             * Determines whether the given file transfer state indicates an
             * in-progress transfer.
             *
             * @param {ManagedFileTransferState} transferState
             *     The file transfer state to check.
             *
             * @returns {Boolean}
             *     true if the given file transfer state indicates an in-
             *     progress transfer, false otherwise.
             */
            var isInProgress = function isInProgress(transferState) {
                switch (transferState.streamState) {

                    // IDLE or OPEN file transfers are active
                    case ManagedFileTransferState.StreamState.IDLE:
                    case ManagedFileTransferState.StreamState.OPEN:
                        return true;

                    // All others are not active
                    default:
                        return false;

                }
            };

            /**
             * Removes all file transfers which are not currently in-progress.
             */
            $scope.clearCompletedTransfers = function clearCompletedTransfers() {

                // Nothing to clear if no client attached
                if (!$scope.client)
                    return;

                // Remove completed uploads
                $scope.client.uploads = $scope.client.uploads.filter(function isUploadInProgress(upload) {
                    return isInProgress(upload.transferState);
                });

                // Remove completed downloads
                $scope.client.downloads = $scope.client.downloads.filter(function isDownloadInProgress(download) {
                    return isInProgress(download.transferState);
                });

            };

            /**
             * Begins a file upload through the attached Guacamole client for
             * each file in the given FileList.
             *
             * @param {FileList} files
             *     The files to upload.
             */
            $scope.uploadFiles = function uploadFiles(files) {

                // Ignore file uploads if no attached client
                if (!$scope.client)
                    return;

                // Upload each file 
                for (var i = 0; i < files.length; i++)
                    ManagedClient.uploadFile($scope.client, files[i]);

            };

        }]

    };
}]);
