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
 * Service which generates downscaled thumbnails for image clipboard events,
 * shared by the key-log text viewer and the clipboard activity viewer so that
 * both render identical thumbnails without duplicating the canvas-downscale
 * logic.
 */
angular.module('player').factory('clipboardMediaService', [
        function clipboardMediaService() {

    /**
     * The maximum width/height, in pixels, of a generated clipboard image
     * thumbnail. The full-resolution image is retained only for the lightbox;
     * inline previews use this downscaled copy to keep the DOM light even when
     * a recording contains many or large clipboard images.
     *
     * @type {!Number}
     */
    const THUMBNAIL_MAX_DIMENSION = 96;

    const service = {};

    /**
     * Generate a downscaled thumbnail, and capture the natural dimensions, for
     * the given image clipboard metadata - asynchronously populating its
     * thumbURL, width, and height fields. Falls back to the full data URL if
     * the browser cannot rasterize a thumbnail. No-op (returns null) if the
     * thumbnail already exists or is in progress, or if the metadata does not
     * describe an image.
     *
     * @param {Object} clipboard
     *     The image clipboard metadata to enrich in place.
     *
     * @param {!Function} apply
     *     A callback which schedules the provided mutation function to run
     *     within an AngularJS digest (typically a wrapper around
     *     $scope.$evalAsync).
     *
     * @param {Function} [onSettled]
     *     An optional callback invoked with the underlying Image object once
     *     its load has resolved or failed, allowing the caller to release any
     *     tracking it holds for cleanup.
     *
     * @returns {Image}
     *     The Image object used to load the source, so the caller can track it
     *     and abort it on teardown, or null if no work was scheduled.
     */
    service.generateThumbnail = function generateThumbnail(clipboard, apply, onSettled) {

        // Only process each image event once
        if (!clipboard || !clipboard.isImage || !clipboard.dataURL
                || clipboard.thumbURL || clipboard.thumbPending)
            return null;

        clipboard.thumbPending = true;

        const image = new Image();

        const settle = function settle() {
            if (onSettled)
                onSettled(image);
        };

        image.onload = function thumbnailLoaded() {

            settle();

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

            apply(function applyThumbnail() {
                clipboard.width = w;
                clipboard.height = h;
                clipboard.thumbURL = thumb;
                clipboard.thumbPending = false;
            });
        };

        image.onerror = function thumbnailFailed() {
            settle();
            apply(function applyFailure() {
                clipboard.thumbPending = false;
            });
        };

        image.src = clipboard.dataURL;

        return image;

    };

    return service;

}]);
