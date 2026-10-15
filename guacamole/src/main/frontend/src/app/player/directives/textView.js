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
    const clipboardMediaService = $injector.get('clipboardMediaService');

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
         * The set of image loads currently in flight for thumbnail generation,
         * tracked so their callbacks can be detached on teardown.
         *
         * @type {!Set.<Image>}
         */
        const pendingImages = new Set();

        /**
         * Generate a downscaled thumbnail for the given image clipboard
         * metadata via the shared clipboardMediaService, tracking the in-flight
         * image load so it can be aborted when the directive is destroyed.
         *
         * @param {Object} clipboard
         *     The image clipboard metadata to enrich in place.
         */
        const generateThumbnail = clipboard => {
            const image = clipboardMediaService.generateThumbnail(clipboard,
                fn => $scope.$evalAsync(fn),
                loaded => pendingImages.delete(loaded));
            if (image)
                pendingImages.add(image);
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

        // Wire up the clipboard-image lightbox (openImage/closeImage/
        // lightboxImage + Escape handling + focus management), shared with the
        // clipboard-activity viewer via clipboardMediaService.
        clipboardMediaService.attachLightbox($scope, $element);

        /**
         * @borrows playerTimeService.formatTime
         */
        $scope.formatTime = playerTimeService.formatTime;

    }];

    return config;
}]);
