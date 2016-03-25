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
 * Provides the ManagedFileDownload class used by the guacClientManager service.
 */
angular.module('client').factory('ManagedFileDownload', ['$rootScope', '$injector',
    function defineManagedFileDownload($rootScope, $injector) {

    // Required types
    var ManagedFileTransferState = $injector.get('ManagedFileTransferState');

    /**
     * Object which serves as a surrogate interface, encapsulating a Guacamole
     * file download while it is active, allowing it to be detached and
     * reattached from different client views.
     * 
     * @constructor
     * @param {ManagedFileDownload|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ManagedFileDownload.
     */
    var ManagedFileDownload = function ManagedFileDownload(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The current state of the file transfer stream.
         *
         * @type ManagedFileTransferState
         */
        this.transferState = template.transferState || new ManagedFileTransferState();

        /**
         * The mimetype of the file being transferred.
         *
         * @type String
         */
        this.mimetype = template.mimetype;

        /**
         * The filename of the file being transferred.
         *
         * @type String
         */
        this.filename = template.filename;

        /**
         * The number of bytes transferred so far.
         *
         * @type Number
         */
        this.progress = template.progress;

        /**
         * A blob containing the complete downloaded file. This is available
         * only after the download has finished.
         *
         * @type Blob
         */
        this.blob = template.blob;

    };

    /**
     * Creates a new ManagedFileDownload which downloads the contents of the
     * given stream as a file having the given mimetype and filename.
     *
     * @param {Guacamole.InputStream} stream
     *     The stream whose contents should be downloaded as a file.
     *
     * @param {String} mimetype
     *     The mimetype of the stream contents.
     *
     * @param {String} filename
     *     The filename of the file being received over the steram.
     *
     * @return {ManagedFileDownload}
     *     A new ManagedFileDownload object which can be used to track the
     *     progress of the download.
     */
    ManagedFileDownload.getInstance = function getInstance(stream, mimetype, filename) {

        // Init new file download object
        var managedFileDownload = new ManagedFileDownload({
            mimetype : mimetype,
            filename : filename,
            progress : 0,
            transferState : new ManagedFileTransferState({
                streamState : ManagedFileTransferState.StreamState.OPEN
            })
        });

        // Begin file download
        var blob_reader = new Guacamole.BlobReader(stream, mimetype);

        // Update progress as data is received
        blob_reader.onprogress = function onprogress() {

            // Update progress
            $rootScope.$apply(function downloadStreamProgress() {
                managedFileDownload.progress = blob_reader.getLength();
            });

            // Signal server that data was received
            stream.sendAck("Received", Guacamole.Status.Code.SUCCESS);

        };

        // Save blob and close stream when complete
        blob_reader.onend = function onend() {
            $rootScope.$apply(function downloadStreamEnd() {

                // Save blob
                managedFileDownload.blob = blob_reader.getBlob();

                // Mark stream as closed
                ManagedFileTransferState.setStreamState(managedFileDownload.transferState,
                    ManagedFileTransferState.StreamState.CLOSED);

                // Notify of upload completion
                $rootScope.$broadcast('guacDownloadComplete', filename);

            });
        };

        // Signal server that data is ready to be received
        stream.sendAck("Ready", Guacamole.Status.Code.SUCCESS);
        
        return managedFileDownload;

    };

    return ManagedFileDownload;

}]);