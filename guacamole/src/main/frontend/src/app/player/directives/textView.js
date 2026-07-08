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

const fuzzysort = require('fuzzysort')

/**
 * Directive which plays back session recordings.
 */
angular.module('player').directive('guacPlayerTextView',
        ['$injector', function guacPlayer($injector) {

    // Required services
    const playerTimeService = $injector.get('playerTimeService');

    const config = {
        restrict : 'E',
        templateUrl : 'app/player/templates/textView.html'
    };

    config.scope = {

        /**
         * All the batches of text extracted from this recording.
         *
         * @type {!keyEventDisplayService.TextBatch[]}
         */
        textBatches : '=',

        /**
         * A callback that accepts a timestamp, and seeks the recording to
         * that provided timestamp.
         *
         * @type {!Function}
         */
        seek: '&',

        /**
         * The current position within the recording.
         *
         * @type {!Number}
         */
        currentPosition: '='

    };

    config.controller = ['$scope', '$element', '$injector',
            function guacPlayerController($scope, $element) {

        /**
         * The phrase to search within the text batches in order to produce the
         * filtered list for display.
         *
         * @type {String}
         */
        $scope.searchPhrase = '';

        /**
         * The text batches that match the current search phrase, or all
         * batches if no search phrase is set.
         *
         * @type {!keyEventDisplayService.TextBatch[]}
         */
        $scope.filteredBatches = $scope.textBatches;

        /**
         * Whether or not the key log viewer should be full-screen. False by
         * default unless explicitly enabled by user interaction.
         *
         * @type {boolean}
         */
        $scope.fullscreenKeyLog = false;

        /**
         * Toggle whether the key log viewer should take up the whole screen.
         */
        $scope.toggleKeyLogFullscreen = function toggleKeyLogFullscreen() {
            $element.toggleClass("fullscreen");
        };

        /**
         * Filter the provided text batches using the provided search phrase to
         * generate the list of filtered batches, or set to all provided
         * batches if no search phrase is provided.
         *
         * @param {String} searchPhrase
         *     The phrase to search the text batches for. If no phrase is
         *     provided, the list of batches will not be filtered.
         */
        const applyFilter = searchPhrase => {

            // If there's search phrase entered, search the text within the
            // batches for it
            if (searchPhrase)
                $scope.filteredBatches = fuzzysort.go(
                    searchPhrase, $scope.textBatches, {key: 'simpleValue'})
                .map(result => result.obj);

            // Otherwise, do not filter the batches
            else
                $scope.filteredBatches = $scope.textBatches;

        };

        /**
         * The maximum width/height, in pixels, of a generated clipboard image
         * thumbnail. The full-resolution image is retained only for the
         * lightbox; the inline chip uses this downscaled copy to keep the
         * key-log DOM light even when a recording contains many or large
         * clipboard images.
         *
         * @type {!Number}
         */
        const THUMBNAIL_MAX_DIMENSION = 96;

        /**
         * Generate a downscaled thumbnail, and capture the natural dimensions,
         * for the given image clipboard metadata - asynchronously populating
         * its thumbURL, width, and height fields. Falls back to the full data
         * URL if the browser cannot rasterize a thumbnail. No-op if the
         * thumbnail already exists or is in progress.
         *
         * @param {Object} clipboard
         *     The image clipboard metadata to enrich in place.
         */
        const pendingImages = new Set();

        const generateThumbnail = clipboard => {

            // Only process each image event once
            if (!clipboard || !clipboard.isImage || !clipboard.dataURL
                    || clipboard.thumbURL || clipboard.thumbPending)
                return;

            clipboard.thumbPending = true;

            const image = new Image();
            pendingImages.add(image);

            image.onload = function thumbnailLoaded() {

                pendingImages.delete(image);

                const w = image.naturalWidth;
                const h = image.naturalHeight;
                const scale = Math.min(1, THUMBNAIL_MAX_DIMENSION / Math.max(w, h));
                const tw = Math.max(1, Math.round(w * scale));
                const th = Math.max(1, Math.round(h * scale));

                let thumb = clipboard.dataURL;
                try {
                    const canvas = document.createElement('canvas');
                    canvas.width = tw;
                    canvas.height = th;
                    canvas.getContext('2d').drawImage(image, 0, 0, tw, th);
                    thumb = canvas.toDataURL('image/png');
                }
                catch (ignore) {
                    // Keep the full data URL as the thumbnail fallback
                }

                $scope.$evalAsync(function applyThumbnail() {
                    clipboard.width = w;
                    clipboard.height = h;
                    clipboard.thumbURL = thumb;
                    clipboard.thumbPending = false;
                });
            };

            image.onerror = function thumbnailFailed() {
                pendingImages.delete(image);
                $scope.$evalAsync(function applyFailure() {
                    clipboard.thumbPending = false;
                });
            };

            image.src = clipboard.dataURL;

        };

        // Abort any in-flight thumbnail loads when the directive is destroyed
        // so their callbacks don't run against a detached scope and their
        // closures don't outlive the viewer
        $scope.$on('$destroy', function abortPendingThumbnails() {
            pendingImages.forEach(function detach(image) {
                image.onload = null;
                image.onerror = null;
                image.src = '';
            });
            pendingImages.clear();
        });

        /**
         * Kick off thumbnail generation for every image clipboard event across
         * all current text batches that has not yet been processed.
         */
        const generateThumbnails = () => {
            ($scope.textBatches || []).forEach(batch =>
                (batch.events || []).forEach(event => {
                    if (event.clipboard && event.clipboard.isImage)
                        generateThumbnail(event.clipboard);
                }));
        };

        // Reapply the current filter and refresh thumbnails when batches change
        $scope.$watch('textBatches', () => {
            applyFilter($scope.searchPhrase);
            generateThumbnails();
        });

        // Reapply the filter whenever the search phrase is updated
        $scope.$watch('searchPhrase', applyFilter);

        /**
         * The clipboard image currently shown enlarged in the lightbox, or
         * null if the lightbox is closed. Objects here are the `clipboard`
         * metadata attached to an image clipboard event (see
         * keyEventDisplayService).
         *
         * @type {Object}
         */
        $scope.lightboxImage = null;

        /**
         * Handler which dismisses the lightbox when the Escape key is pressed.
         * Bound to the document only while the lightbox is open.
         *
         * @param {KeyboardEvent} e
         *     The keydown event.
         */
        const dismissOnEscape = function dismissOnEscape(e) {
            if (e.keyCode === 27) // Escape
                $scope.$apply(function applyClose() {
                    $scope.closeImage();
                });
        };

        /**
         * Opens the clipboard-image lightbox for the given image clipboard
         * event metadata.
         *
         * @param {Object} clipboard
         *     The clipboard metadata (including dataURL) to display enlarged.
         */
        $scope.openImage = function openImage(clipboard) {
            $scope.lightboxImage = clipboard;
            angular.element(document).on('keydown', dismissOnEscape);
        };

        /**
         * Closes the clipboard-image lightbox, if open.
         */
        $scope.closeImage = function closeImage() {
            $scope.lightboxImage = null;
            angular.element(document).off('keydown', dismissOnEscape);
        };

        // Ensure the document-level listener never outlives the directive
        $scope.$on('$destroy', function unbindEscape() {
            angular.element(document).off('keydown', dismissOnEscape);
        });

        /**
         * @borrows playerTimeService.formatTime
         */
        $scope.formatTime = playerTimeService.formatTime;

    }];

    return config;
}]);
