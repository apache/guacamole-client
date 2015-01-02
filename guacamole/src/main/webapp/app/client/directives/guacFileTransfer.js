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
 * Directive which displays an active file transfer, providing links for
 * downloads, if applicable.
 */
angular.module('client').directive('guacFileTransfer', [function guacFileTransfer() {

    return {
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The file transfer to display.
             * 
             * @type ManagedFileUpload|ManagedFileDownload
             */
            transfer : '='

        },

        templateUrl: 'app/client/templates/guacFileTransfer.html',
        controller: ['$scope', '$injector', function guacFileTransferController($scope, $injector) {

            // Required types
            var ManagedFileTransferState = $injector.get('ManagedFileTransferState');

            /**
             * All upload error codes handled and passed off for translation.
             * Any error code not present in this list will be represented by
             * the "DEFAULT" translation.
             */
            var UPLOAD_ERRORS = {
                0x0100: true,
                0x0201: true,
                0x0202: true,
                0x0203: true,
                0x0204: true,
                0x0205: true,
                0x0301: true,
                0x0303: true,
                0x0308: true,
                0x031D: true
            };

            /**
             * Returns the unit string that is most appropriate for the
             * number of bytes transferred thus far - either 'gb', 'mb', 'kb',
             * or 'b'.
             *
             * @returns {String}
             *     The unit string that is most appropriate for the number of
             *     bytes transferred thus far.
             */
            $scope.getProgressUnit = function getProgressUnit() {

                var bytes = $scope.transfer.progress;

                // Gigabytes
                if (bytes > 1000000000)
                    return 'gb';

                // Megabytes
                if (bytes > 1000000)
                    return 'mb';

                // Kilobytes
                if (bytes > 1000)
                    return 'kb';

                // Bytes
                return 'b';

            };

            /**
             * Returns the amount of data transferred thus far, in the units
             * returned by getProgressUnit().
             *
             * @returns {Number}
             *     The amount of data transferred thus far, in the units
             *     returned by getProgressUnit().
             */
            $scope.getProgressValue = function getProgressValue() {

                var bytes = $scope.transfer.progress;
                if (!bytes)
                    return bytes;

                // Convert bytes to necessary units
                switch ($scope.getProgressUnit()) {

                    // Gigabytes
                    case 'gb':
                        return (bytes / 1000000000).toFixed(1);

                    // Megabytes
                    case 'mb':
                        return (bytes / 1000000).toFixed(1);

                    // Kilobytes
                    case 'kb':
                        return (bytes / 1000).toFixed(1);

                    // Bytes
                    case 'b':
                    default:
                        return bytes;

                }

            };

            /**
             * Returns the percentage of bytes transferred thus far, if the
             * overall length of the file is known.
             *
             * @returns {Number}
             *     The percentage of bytes transferred thus far, if the
             *     overall length of the file is known.
             */
            $scope.getPercentDone = function getPercentDone() {
                return $scope.transfer.progress / $scope.transfer.length * 100;
            };

            /**
             * Determines whether the associated file transfer is in progress.
             *
             * @returns {Boolean}
             *     true if the file transfer is in progress, false othherwise.
             */
            $scope.isInProgress = function isInProgress() {

                // Not in progress if there is no transfer
                if (!$scope.transfer)
                    return false;

                // Determine in-progress status based on stream state
                switch ($scope.transfer.transferState.streamState) {

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
             * Returns whether the file associated with this file transfer can
             * be saved locally via a call to save().
             *
             * @returns {Boolean}
             *     true if a call to save() will result in the file being
             *     saved, false otherwise.
             */
            $scope.isSavable = function isSavable() {
                return !!$scope.transfer.blob;
            };

            /**
             * Saves the downloaded file, if any. If this transfer is an upload
             * or the download is not yet complete, this function has no
             * effect.
             */
            $scope.save = function save() {

                // Ignore if no blob exists
                if (!$scope.transfer.blob)
                    return;

                // Save file
                saveAs($scope.transfer.blob, $scope.transfer.filename); 

            };

            /**
             * Returns whether an error has occurred. If an error has occurred,
             * the transfer is no longer active, and the text of the error can
             * be read from getErrorText().
             *
             * @returns {Boolean}
             *     true if an error has occurred during transfer, false
             *     otherwise.
             */
            $scope.hasError = function hasError() {
                return $scope.transfer.transferState.streamState === ManagedFileTransferState.StreamState.ERROR;
            };

            /**
             * Returns the text of the current error as a translation string.
             *
             * @returns {String}
             *     The name of the translation string containing the text
             *     associated with the current error.
             */
            $scope.getErrorText = function getErrorText() {

                // Determine translation name of error
                var status = $scope.transfer.transferState.statusCode;
                var errorName = (status in UPLOAD_ERRORS) ? status.toString(16).toUpperCase() : "DEFAULT";

                // Return translation string
                return 'CLIENT.ERROR_UPLOAD_' + errorName;

            };

        }] // end file transfer controller

    };
}]);
