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

import { Injectable } from '@angular/core';

/**
 * A service for checking browser image support.
 */
@Injectable({
    providedIn: 'root'
})
export class GuacImageService {

    /**
     * Map of possibly-supported image mimetypes to corresponding test images
     * encoded with base64. If the image is correctly decoded, it will be a
     * single pixel (1x1) image.
     */
    private testImages: Record<string, string> = {

        /**
         * Test JPEG image, encoded as base64.
         */
        'image/jpeg':
            '/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoH'
            + 'BwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT/2wBDAQME'
            + 'BAUEBQkFBQkUDQsNFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQU'
            + 'FBQUFBQUFBQUFBQUFBT/wAARCAABAAEDAREAAhEBAxEB/8QAFAABAAAAAAAAAAA'
            + 'AAAAAAAAACf/EABQQAQAAAAAAAAAAAAAAAAAAAAD/xAAUAQEAAAAAAAAAAAAAAA'
            + 'AAAAAA/8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAwDAQACEQMRAD8AVMH/2Q==',

        /**
         * Test PNG image, encoded as base64.
         */
        'image/png':
            'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABAQMAAAAl21bKAAAAA1BMVEX///+nxBvI'
            + 'AAAACklEQVQI12NgAAAAAgAB4iG8MwAAAABJRU5ErkJggg==',

        /**
         * Test WebP image, encoded as base64.
         */
        'image/webp': 'UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA=='

    };

    /**
     * The resolve function associated with {@link deferredSupportedMimetypes}.
     */
    private deferredSupportedMimetypesResolve?: (value: string[] | PromiseLike<string[]>) => void;

    /**
     * Deferred which tracks the progress and ultimate result of all pending
     * image format tests.
     *
     */
    private deferredSupportedMimetypes: Promise<string[]> =
        new Promise((resolve) => {
            return this.deferredSupportedMimetypesResolve = resolve;
        });

    /**
     * Array of all promises associated with pending image tests. Each image
     * test promise MUST be guaranteed to resolve and MUST NOT be rejected.
     */
    private pendingTests: Promise<any>[] = [];

    /**
     * The array of supported image formats. This will be gradually populated
     * by the various image tests that occur in the background, and will not be
     * fully populated until all promises within pendingTests are resolved.
     */
    private supported: string[] = [];

    /**
     * Return a promise which resolves with to an array of image mimetypes
     * supported by the browser, once those mimetypes are known. The returned
     * promise is guaranteed to resolve successfully.
     *
     * @returns
     *     A promise which resolves with an array of image mimetypes supported
     *     by the browser.
     */
    getSupportedMimetypes(): Promise<string[]> {
        return this.deferredSupportedMimetypes;
    }

    constructor() {
        // Test each possibly-supported image
        for (const mimetype in this.testImages) {
            const data = this.testImages[mimetype];

            // Add promise for current image test
            const imageTest = new Promise<void>((resolve) => {
                // Attempt to load image
                const image = new Image();
                image.src = 'data:' + mimetype + ';base64,' + data;

                // Store as supported depending on whether load was successful
                image.onload = image.onerror = () => {
                    // imageTestComplete

                    // Image format is supported if successfully decoded
                    if (image.width === 1 && image.height === 1)
                        this.supported.push(mimetype);

                    // Test is complete
                    resolve();

                };
            });

            this.pendingTests.push(imageTest);

        }

        // When all image tests are complete, resolve promise with list of
        // supported formats
        Promise.all(this.pendingTests).then(() => {
            this.deferredSupportedMimetypesResolve?.(this.supported);
        });

    }

}
