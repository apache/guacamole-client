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
 * A directive which allows multiple files to be uploaded. Clicking on the
 * associated element will result in a file selector dialog, which then calls
 * the provided callback function with any chosen files.
 */
angular.module('element').directive('guacUpload', ['$document', function guacUpload($document) {

    return {
        restrict: 'A',

        link: function linkGuacUpload($scope, $element, $attrs) {

            /**
             * The function to call whenever files are chosen. The callback is
             * provided a single parameter: the FileList containing all chosen
             * files.
             *
             * @type Function 
             */
            var guacUpload = $scope.$eval($attrs.guacUpload);

            /**
             * The element which will register the drag gesture.
             *
             * @type Element
             */
            var element = $element[0];

            /**
             * Internal form, containing a single file input element.
             *
             * @type HTMLFormElement
             */
            var form = $document[0].createElement('form');

            /**
             * Internal file input element.
             *
             * @type HTMLInputElement
             */
            var input = $document[0].createElement('input');

            // Init input element
            input.type = 'file';
            input.multiple = true;

            // Add input element to internal form
            form.appendChild(input);

            // Notify of any chosen files
            input.addEventListener('change', function filesSelected() {
                $scope.$apply(function setSelectedFiles() {

                    // Only set chosen files selection is not canceled
                    if (guacUpload && input.files.length > 0)
                        guacUpload(input.files);

                    // Reset selection
                    form.reset();

                });
            });

            // Open file chooser when element is clicked
            element.addEventListener('click', function elementClicked() {
                input.click();
            });

        } // end guacUpload link function

    };

}]);
