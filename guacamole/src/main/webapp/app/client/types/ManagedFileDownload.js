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