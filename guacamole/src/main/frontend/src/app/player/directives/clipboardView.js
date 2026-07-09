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
 * Directive which displays the clipboard activity captured within a session
 * recording as a scrollable, chronological list of cards. Each card seeks the
 * recording to the moment the clipboard event occurred, annotates the transfer
 * direction, and allows the underlying text or image payload to be inspected
 * and downloaded. The whole log can additionally be exported as CSV.
 */
angular.module('player').directive('guacPlayerClipboardView',
        ['$injector', function guacPlayerClipboardView($injector) {

    // Required services
    const playerTimeService = $injector.get('playerTimeService');
    const clipboardMediaService = $injector.get('clipboardMediaService');

    const config = {
        restrict : 'E',
        templateUrl : 'app/player/templates/clipboardView.html'
    };

    config.scope = {

        /**
         * All clipboard events extracted from this recording.
         *
         * @type {!Guacamole.ClipboardEventInterpreter.ClipboardEvent[]}
         */
        clipboardEvents : '=',

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
        currentPosition: '=',

        /**
         * The number of clipboard transfers that were recorded incompletely
         * (streams opened but never terminated). When greater than zero, a
         * warning banner is shown.
         *
         * @type {Number}
         */
        incompleteCount: '='

    };

    config.controller = ['$scope', '$element', '$injector',
            function guacPlayerClipboardController($scope, $element) {

        /**
         * The maximum number of characters of a text clipboard event shown
         * before it is truncated behind a "show more" toggle.
         *
         * @type {!Number}
         */
        const TEXT_TRUNCATE_LENGTH = 500;

        /**
         * Display metadata describing how each recognized clipboard transfer
         * direction should be presented, keyed by the raw server-annotated
         * direction. Every entry carries a text label (never colour alone) and
         * a CSS class reused from the key-log viewer's colour treatment.
         *
         * @type {!Object.<String, {labelKey: String, directionClass: String}>}
         */
        const DIRECTION_META = {
            'guest-to-client' : {
                labelKey       : 'PLAYER.LABEL_CLIPBOARD_COPIED_OUT',
                directionClass : 'from-guest'
            },
            'client-to-guest' : {
                labelKey       : 'PLAYER.LABEL_CLIPBOARD_PASTED_IN',
                directionClass : 'to-guest'
            }
        };

        /**
         * The clipboard events, sorted chronologically and enriched with
         * display metadata for rendering.
         *
         * @type {!Object[]}
         */
        $scope.sortedEvents = [];

        /**
         * The clustered, display-ready list derived from $scope.sortedEvents.
         * Runs of consecutive events sharing the same direction and identical
         * content are collapsed into a single cluster card. This is the list
         * rendered in the template; $scope.sortedEvents remains the canonical
         * flat list used for CSV export and per-item download.
         *
         * @type {!Object[]}
         */
        $scope.displayItems = [];

        /**
         * The set of image loads currently in flight for thumbnail generation,
         * tracked so their callbacks can be detached on teardown.
         *
         * @type {!Set.<Image>}
         */
        const pendingImages = new Set();

        /**
         * Returns the number of bytes required to encode the given string as
         * UTF-8.
         *
         * @param {String} text
         *     The string to measure.
         *
         * @returns {!Number}
         *     The size, in bytes, of the UTF-8 encoding of the string.
         */
        const byteLength = text => text ? new Blob([text]).size : 0;

        /**
         * Formats a size in bytes as a short human-readable string.
         *
         * @param {Number} bytes
         *     The size, in bytes.
         *
         * @returns {!String}
         *     A human-readable representation of the size.
         */
        const formatBytes = bytes => {
            if (!bytes && bytes !== 0)
                return '';
            if (bytes < 1024)
                return bytes + ' B';
            if (bytes < 1024 * 1024)
                return Math.round(bytes / 1024) + ' KB';
            return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
        };

        /**
         * Builds the sorted, display-ready list of clipboard events from the
         * raw events bound to this directive, generating image thumbnails as
         * needed.
         */
        const rebuild = () => {

            const events = ($scope.clipboardEvents || []).slice()
                    .sort((a, b) => a.timestamp - b.timestamp);

            $scope.sortedEvents = events.map((event, index) => {

                const meta = (event.direction && DIRECTION_META[event.direction])
                        || { labelKey : 'PLAYER.LABEL_CLIPBOARD', directionClass : '' };

                const bytes = event.isImage ? event.size : byteLength(event.data);

                return {
                    raw            : event,
                    index          : index,
                    timestamp      : event.timestamp,
                    formattedTime  : playerTimeService.formatTime(event.timestamp),
                    isImage        : event.isImage,
                    mimetype       : event.mimetype,
                    text           : event.data,
                    truncatable    : !event.isImage && event.data
                                        && event.data.length > TEXT_TRUNCATE_LENGTH,
                    expanded       : false,
                    bytes          : bytes,
                    sizeLabel      : formatBytes(bytes),
                    directionLabelKey : meta.labelKey,
                    directionClass    : meta.directionClass
                };
            });

            // Kick off thumbnail generation for every image event
            $scope.sortedEvents.forEach(item => {
                if (item.isImage) {
                    const image = clipboardMediaService.generateThumbnail(item.raw,
                        fn => $scope.$evalAsync(fn),
                        loaded => pendingImages.delete(loaded));
                    if (image)
                        pendingImages.add(image);
                }
            });

            buildDisplayItems();

        };

        /**
         * Collapses runs of consecutive clipboard events sharing the same
         * transfer direction and identical content into cluster entries, and
         * stores the result in $scope.displayItems. A run of length one becomes
         * a singleton cluster (clusterCount === 1). The canonical flat list in
         * $scope.sortedEvents is left untouched.
         */
        const buildDisplayItems = () => {

            const clusters = [];

            $scope.sortedEvents.forEach(item => {

                const last = clusters.length ? clusters[clusters.length - 1] : null;

                const sameContent = last
                        && last.raw.direction === item.raw.direction
                        && last.isImage === item.isImage
                        && (item.isImage
                                ? last.raw.dataURL === item.raw.dataURL
                                : last.text === item.text);

                if (sameContent) {
                    last.clusterCount++;
                    last.clusterMembers.push(item);
                    last.lastTime = item.timestamp;
                    last.formattedLastTime = item.formattedTime;
                }
                else {
                    clusters.push(angular.extend({}, item, {
                        clusterCount      : 1,
                        clusterMembers    : [item],
                        clusterExpanded   : false,
                        firstTime         : item.timestamp,
                        lastTime          : item.timestamp,
                        formattedLastTime : item.formattedTime
                    }));
                }

            });

            $scope.displayItems = clusters;

        };

        /**
         * Toggles whether the individual member timestamps of a clustered card
         * are revealed.
         *
         * @param {!Object} item
         *     The cluster display item to toggle.
         */
        $scope.toggleCluster = function toggleCluster(item) {
            item.clusterExpanded = !item.clusterExpanded;
        };

        $scope.$watch('clipboardEvents', rebuild);

        /**
         * Returns the portion of a text clipboard event's content that should
         * be shown, honoring the card's current expanded state.
         *
         * @param {!Object} item
         *     The display item for the clipboard event.
         *
         * @returns {!String}
         *     The text to display for the item.
         */
        $scope.displayText = function displayText(item) {
            if (!item.truncatable || item.expanded)
                return item.text;
            return item.text.substring(0, TEXT_TRUNCATE_LENGTH);
        };

        /**
         * Toggles whether a truncated text clipboard event shows its full
         * content.
         *
         * @param {!Object} item
         *     The display item to toggle.
         */
        $scope.toggleExpanded = function toggleExpanded(item) {
            item.expanded = !item.expanded;
        };

        /**
         * Seeks the recording to the moment the given clipboard event occurred.
         *
         * @param {!Object} item
         *     The display item to seek to.
         */
        $scope.seekToEvent = function seekToEvent(item) {
            $scope.seek({ timestamp: item.timestamp });
        };

        /**
         * Handles keyboard activation (Enter or Space) of a focusable card,
         * seeking to the associated clipboard event.
         *
         * @param {!KeyboardEvent} event
         *     The keydown event.
         *
         * @param {!Object} item
         *     The display item associated with the card.
         */
        $scope.cardKeydown = function cardKeydown(event, item) {
            if (event.keyCode === 13 || event.keyCode === 32) {
                event.preventDefault();
                $scope.seekToEvent(item);
            }
        };

        /**
         * The clipboard image currently shown enlarged in the lightbox, or
         * null if the lightbox is closed.
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

        /**
         * Maps an image mimetype to a file extension for downloads.
         *
         * @param {String} mimetype
         *     The image mimetype.
         *
         * @returns {!String}
         *     A file extension (without the leading dot).
         */
        const imageExtension = mimetype => {
            const subtype = (mimetype || '').toLowerCase().replace(/^image\//, '');
            if (subtype === 'jpeg' || subtype === 'jpg')
                return 'jpg';
            if (subtype === 'png')
                return 'png';
            if (subtype === 'bmp')
                return 'bmp';
            if (subtype === 'tiff' || subtype === 'tif')
                return 'tiff';
            return subtype || 'bin';
        };

        /**
         * Converts a data: URL into a Blob suitable for download.
         *
         * @param {!String} dataURL
         *     The data: URL to convert.
         *
         * @returns {!Blob}
         *     A Blob containing the decoded payload.
         */
        const dataURLToBlob = dataURL => {
            const parts = dataURL.split(',');
            const meta = parts[0];
            const mimeMatch = /data:([^;]+)/.exec(meta);
            const mimetype = mimeMatch ? mimeMatch[1] : 'application/octet-stream';
            const binary = atob(parts[1] || '');
            const bytes = new Uint8Array(binary.length);
            for (let i = 0; i < binary.length; i++)
                bytes[i] = binary.charCodeAt(i);
            return new Blob([bytes], { type: mimetype });
        };

        /**
         * Downloads the payload of the given clipboard event as a file: text
         * events download a .txt file, image events download the image in its
         * original format.
         *
         * @param {!Object} item
         *     The display item whose payload should be downloaded.
         */
        $scope.download = function download(item) {
            if (item.isImage && item.raw.dataURL) {

                // A corrupt/truncated base64 payload in the recording would
                // make atob() throw; guard so the button degrades to a no-op
                // rather than throwing out of the click handler.
                let blob;
                try {
                    blob = dataURLToBlob(item.raw.dataURL);
                }
                catch (e) {
                    return;
                }

                const ext = imageExtension(item.mimetype);
                saveAs(blob, 'clipboard-' + (item.index + 1) + '.' + ext);
            }
            else {
                const blob = new Blob([item.text || ''],
                        { type: 'text/plain;charset=utf-8' });
                saveAs(blob, 'clipboard-' + (item.index + 1) + '.txt');
            }
        };

        /**
         * Escapes a single value for inclusion in a CSV field, quoting it when
         * it contains a comma, quote, or newline.
         *
         * @param {*} value
         *     The value to escape.
         *
         * @returns {!String}
         *     The CSV-safe representation of the value.
         */
        const csvEscape = value => {
            let str = (value === null || value === undefined) ? '' : String(value);

            // Neutralize spreadsheet formula injection: a leading =, +, -, @,
            // tab, or CR makes Excel/Sheets evaluate the (attacker-controlled)
            // clipboard value as a formula. Prefix with a single quote so it is
            // always treated as text. Quoting alone does not prevent this.
            if (/^[=+\-@\t\r]/.test(str))
                str = "'" + str;

            if (/[",\r\n]/.test(str))
                return '"' + str.replace(/"/g, '""') + '"';
            return str;
        };

        /**
         * Builds and downloads a CSV export of all clipboard events. Columns:
         * index, time (formatted), timestamp_ms, direction, mimetype, bytes,
         * text.
         */
        $scope.exportCsv = function exportCsv() {

            const header = ['index', 'time', 'timestamp_ms', 'direction',
                    'mimetype', 'bytes', 'text'];

            const rows = $scope.sortedEvents.map(item => [
                item.index + 1,
                item.formattedTime,
                item.timestamp,
                item.raw.direction || '',
                item.mimetype,
                item.bytes,
                item.isImage ? '' : (item.text || '')
            ].map(csvEscape).join(','));

            // Prepend a UTF-8 BOM so Excel opens the file as UTF-8 rather than
            // the OS ANSI codepage (otherwise non-ASCII clipboard text mojibakes).
            const csv = '\uFEFF' + [header.join(','), ...rows].join('\r\n');
            saveAs(new Blob([csv], { type: 'text/csv;charset=utf-8' }),
                    'clipboard-activity.csv');

        };

        // Abort any in-flight thumbnail loads, and unbind the lightbox key
        // handler, when the directive is destroyed
        $scope.$on('$destroy', function clipboardViewDestroyed() {
            pendingImages.forEach(function detach(image) {
                image.onload = null;
                image.onerror = null;
                image.src = '';
            });
            pendingImages.clear();
            angular.element(document).off('keydown', dismissOnEscape);
        });

        /**
         * @borrows playerTimeService.formatTime
         */
        $scope.formatTime = playerTimeService.formatTime;

    }];

    return config;
}]);
