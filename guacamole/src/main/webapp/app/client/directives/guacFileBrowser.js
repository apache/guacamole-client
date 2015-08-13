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
 * A directive which displays the contents of a filesystem received through the
 * Guacamole client.
 */
angular.module('client').directive('guacFileBrowser', [function guacFileBrowser() {

    return {
        restrict: 'E',
        replace: true,
        scope: {

            /**
             * The client whose file transfers should be managed by this
             * directive.
             *
             * @type ManagedClient
             */
            client : '=',

            /**
             * @type ManagedFilesystem
             */
            filesystem : '='

        },

        templateUrl: 'app/client/templates/guacFileBrowser.html',
        controller: ['$scope', '$element', '$injector', function guacFileBrowserController($scope, $element, $injector) {

            // Required types
            var ManagedFilesystem = $injector.get('ManagedFilesystem');

            // Required services
            var $interpolate     = $injector.get('$interpolate');
            var $templateRequest = $injector.get('$templateRequest');

            /**
             * The jQuery-wrapped element representing the contents of the
             * current directory within the file browser.
             *
             * @type Element[]
             */
            var currentDirectoryContents = $element.find('.current-directory-contents');

            /**
             * Statically-cached template HTML used to render each file within
             * a directory. Once available, this will be used through
             * createFileElement() to generate the DOM elements which make up
             * a directory listing.
             *
             * @type String
             */
            var fileTemplate = null;

            /**
             * Returns whether the given file is a normal file.
             *
             * @param {ManagedFilesystem.File} file
             *     The file to test.
             *
             * @returns {Boolean}
             *     true if the given file is a normal file, false otherwise.
             */
            $scope.isNormalFile = function isNormalFile(file) {
                return file.type === ManagedFilesystem.File.Type.NORMAL;
            };

            /**
             * Returns whether the given file is a directory.
             *
             * @param {ManagedFilesystem.File} file
             *     The file to test.
             *
             * @returns {Boolean}
             *     true if the given file is a directory, false otherwise.
             */
            $scope.isDirectory = function isDirectory(file) {
                return file.type === ManagedFilesystem.File.Type.DIRECTORY;
            };

            /**
             * Changes the currently-displayed directory to the given
             * directory.
             *
             * @param {ManagedFilesystem.File} file
             *     The directory to change to.
             */
            $scope.changeDirectory = function changeDirectory(file) {
                ManagedFilesystem.changeDirectory($scope.filesystem, file);
            };

            /**
             * Initiates a download of the given file. The progress of the
             * download can be observed through guacFileTransferManager.
             *
             * @param {ManagedFilesystem.File} file
             *     The file to download.
             */
            $scope.downloadFile = function downloadFile(file) {
                ManagedFilesystem.downloadFile($scope.client, $scope.filesystem, file.streamName);
            };

            /**
             * Creates a new element representing the given file and properly
             * handling user events, bypassing the overhead incurred through
             * use of ngRepeat and related techniques.
             *
             * Note that this function depends on the availability of the
             * statically-cached fileTemplate.
             *
             * @param {ManagedFilesystem.File} file
             *     The file to generate an element for.
             *
             * @returns {Element[]}
             *     A jQuery-wrapped array containing a single DOM element
             *     representing the given file.
             */
            var createFileElement = function createFileElement(file) {

                // Create from internal template
                var element = angular.element($interpolate(fileTemplate)(file));

                // Change current directory when directories are clicked
                if ($scope.isDirectory(file)) {
                    element.addClass('directory');
                    element.on('dblclick', function changeDirectory() {
                        $scope.changeDirectory(file);
                    });
                }

                // Initiate downloads when normal files are clicked
                else if ($scope.isNormalFile(file)) {
                    element.addClass('normal-file');
                    element.on('dblclick', function downloadFile() {
                        $scope.downloadFile(file);
                    });
                }

                // Mark file as focused upon click
                element.on('click', function focusFile() {
                    element.parent().children().removeClass('focused');
                    element.addClass('focused');
                });

                // Prevent text selection during navigation
                element.on('selectstart', function avoidSelect(e) {
                    e.preventDefault();
                    e.stopPropagation();
                });

                return element;

            };

            /**
             * Sorts the given map of files, returning an array of those files
             * grouped by file type (directories first, followed by non-
             * directories) and sorted lexicographically.
             *
             * @param {Object.<String, ManagedFilesystem.File>} files
             *     The map of files to sort.
             *
             * @returns {ManagedFilesystem.File[]}
             *     An array of all files in the given map, sorted
             *     lexicographically with directories first, followed by non-
             *     directories.
             */
            var sortFiles = function sortFiles(files) {

                // Get all given files as an array
                var unsortedFiles = [];
                for (var name in files)
                    unsortedFiles.push(files[name]);

                // Sort files - directories first, followed by all other files
                // sorted by name
                return unsortedFiles.sort(function fileComparator(a, b) {

                    // Directories come before non-directories
                    if ($scope.isDirectory(a) && !$scope.isDirectory(b))
                        return -1;

                    // Non-directories come after directories
                    if (!$scope.isDirectory(a) && $scope.isDirectory(b))
                        return 1;

                    // All other combinations are sorted by name
                    return a.name.localeCompare(b.name);

                });

            };

            // Watch directory contents once file template is available
            $templateRequest('app/client/templates/file.html').then(function fileTemplateRetrieved(html) {

                // Store file template statically
                fileTemplate = html;

                // Update the contents of the file browser whenever the current directory (or its contents) changes
                $scope.$watch('filesystem.currentDirectory.files', function currentDirectoryChanged(files) {

                    // Clear current content
                    currentDirectoryContents.html('');

                    // Display all files within current directory, sorted
                    angular.forEach(sortFiles(files), function displayFile(file) {
                        currentDirectoryContents.append(createFileElement(file));
                    });

                });

            }); // end retrieve file template

        }]

    };
}]);
