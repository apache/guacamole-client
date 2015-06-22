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
 * Provides the ManagedFilesystem class used by ManagedClient to represent
 * available remote filesystems.
 */
angular.module('client').factory('ManagedFilesystem', ['$rootScope', '$injector',
    function defineManagedFilesystem($rootScope, $injector) {

    /**
     * Object which serves as a surrogate interface, encapsulating a Guacamole
     * filesystem object while it is active, allowing it to be detached and
     * reattached from different client views.
     * 
     * @constructor
     * @param {ManagedFilesystem|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ManagedFilesystem.
     */
    var ManagedFilesystem = function ManagedFilesystem(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The Guacamole filesystem object, as received via a "filesystem"
         * instruction.
         *
         * @type Guacamole.Object
         */
        this.object = template.object;

        /**
         * The declared, human-readable name of the filesystem
         *
         * @type String
         */
        this.name = template.name;

        /**
         * The root directory of the filesystem.
         *
         * @type ManagedFilesystem.File
         */
        this.root = template.root;

    };

    /**
     * Refreshes the contents of the given file, if that file is a directory.
     * Only the immediate children of the file are refreshed. Files further
     * down the directory tree are not refreshed.
     *
     * @param {ManagedFilesystem} filesystem
     *     The filesystem associated with the file being refreshed.
     *
     * @param {ManagedFilesystem.File} file
     *     The file being refreshed.
     */
    ManagedFilesystem.refresh = function updateDirectory(filesystem, file) {

        // Do not attempt to refresh the contents of directories
        if (file.mimetype !== Guacamole.Object.STREAM_INDEX_MIMETYPE)
            return;

        // Request contents of given file
        filesystem.object.requestInputStream(file.streamName, function handleStream(stream, mimetype) {

            // Ignore stream if mimetype is wrong
            if (mimetype !== Guacamole.Object.STREAM_INDEX_MIMETYPE) {
                stream.sendAck('Unexpected mimetype', Guacamole.Status.Code.UNSUPPORTED);
                return;
            }

            // Signal server that data is ready to be received
            stream.sendAck('Ready', Guacamole.Status.Code.SUCCESS);

            // Read stream as JSON
            var reader = new Guacamole.JSONReader(stream);

            // Acknowledge received JSON blobs
            reader.onprogress = function onprogress() {
                stream.sendAck("Received", Guacamole.Status.Code.SUCCESS);
            };

            // Reset contents of directory
            reader.onend = function jsonReady() {
                $rootScope.$evalAsync(function updateFileContents() {

                    // Empty contents
                    file.files = {};

                    // Determine the expected filename prefix of each stream
                    var expectedPrefix = file.streamName;
                    if (expectedPrefix.charAt(expectedPrefix.length - 1) !== '/')
                        expectedPrefix += '/';

                    // For each received stream name
                    var mimetypes = reader.getJSON();
                    for (var name in mimetypes) {

                        // Assert prefix is correct
                        if (name.substring(0, expectedPrefix.length) !== expectedPrefix)
                            continue;

                        // Extract filename from stream name
                        var filename = name.substring(expectedPrefix.length);

                        // Deduce type from mimetype
                        var type = ManagedFilesystem.File.Type.NORMAL;
                        if (mimetypes[name] === Guacamole.Object.STREAM_INDEX_MIMETYPE)
                            type = ManagedFilesystem.File.Type.DIRECTORY;

                        // Add file entry
                        file.files[filename] = new ManagedFilesystem.File({
                            mimetype   : mimetypes[name],
                            streamName : name,
                            type       : type,
                            name       : filename
                        });

                    }

                });
            };

        });

    };

    /**
     * Creates a new ManagedFilesystem instance from the given Guacamole.Object
     * and human-readable name. Upon creation, a request to populate the
     * contents of the root directory will be automatically dispatched.
     *
     * @param {Guacamole.Object} object
     *     The Guacamole.Object defining the filesystem.
     *
     * @param {String} name
     *     A human-readable name for the filesystem.
     *
     * @returns {ManagedFilesystem}
     *     The newly-created ManagedFilesystem.
     */
    ManagedFilesystem.getInstance = function getInstance(object, name) {

        // Init new filesystem object
        var managedFilesystem = new ManagedFilesystem({
            object : object,
            name   : name,
            root   : new ManagedFilesystem.File({
                mimetype   : Guacamole.Object.STREAM_INDEX_MIMETYPE,
                streamName : Guacamole.Object.ROOT_STREAM,
                type       : ManagedFilesystem.File.Type.DIRECTORY
            })
        });

        // Retrieve contents of root
        ManagedFilesystem.refresh(managedFilesystem, managedFilesystem.root);

        return managedFilesystem;

    };

    /**
     * A file within a ManagedFilesystem. Each ManagedFilesystem.File provides
     * sufficient information for retrieval or replacement of the file's
     * contents, as well as the file's name and type.
     *
     * @param {ManagedFilesystem|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ManagedFilesystem.File.
     */
    ManagedFilesystem.File = function File(template) {

        /**
         * The mimetype of the data contained within this file.
         *
         * @type String
         */
        this.mimetype = template.mimetype;

        /**
         * The name of the stream representing this files contents within its
         * associated filesystem object.
         *
         * @type String
         */
        this.streamName = template.streamName;

        /**
         * The type of this file. All legal file type strings are defined
         * within ManagedFilesystem.File.Type.
         *
         * @type String
         */
        this.type = template.type;

        /**
         * The name of this file.
         *
         * @type String
         */
        this.name = template.name;

        /**
         * Map of all known files containined within this file by name. This is
         * only applicable to directories.
         *
         * @type Object.<String, ManagedFilesystem.File>
         */
        this.files = template.files || {};

    };

    /**
     * All legal type strings for a ManagedFilesystem.File.
     *
     * @type Object.<String, String>
     */
    ManagedFilesystem.File.Type = {

        /**
         * A normal file. As ManagedFilesystem does not currently represent any
         * other non-directory types of files, like symbolic links, this type
         * string may be used for any non-directory file.
         *
         * @type String
         */
        NORMAL : 'NORMAL',

        /**
         * A directory.
         *
         * @type String
         */
        DIRECTORY : 'DIRECTORY'

    };

    return ManagedFilesystem;

}]);
