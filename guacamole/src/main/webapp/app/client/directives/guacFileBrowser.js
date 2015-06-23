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
        controller: ['$scope', '$injector', function guacFileBrowserController($scope, $injector) {

            // Required types
            var ManagedFilesystem = $injector.get('ManagedFilesystem');

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
             * Toggles the expanded state of the given file between expanded
             * and collapsed, showing or hiding the file's children. This is
             * only applicable to directories.
             *
             * @param {ManagedFilesystem.File} file
             *     The file to expand or collapse.
             */
            $scope.toggleExpanded = function toggleExpanded(file) {

                // Toggle expanded state
                file.expanded = !file.expanded;

                // If now expanded, refresh contents
                if (file.expanded)
                    ManagedFilesystem.refresh($scope.filesystem, file);

            };

        }]

    };
}]);
