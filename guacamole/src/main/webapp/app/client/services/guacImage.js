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
 * A service for checking browser image support.
 */
angular.module('client').factory('guacImage', ['$injector', function guacImage($injector) {

    // Required services
    var $q = $injector.get('$q');

    var service = {};

    /**
     * Map of possibly-supported image mimetypes to corresponding test images
     * encoded with base64. If the image is correctly decoded, it will be a
     * single pixel (1x1) image.
     *
     * @type Object.<String, String>
     */
    var testImages = {

        /**
         * Test JPEG image, encoded as base64.
         */
        'image/jpeg' :
            '/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoH'
          + 'BwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT/2wBDAQME'
          + 'BAUEBQkFBQkUDQsNFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQU'
          + 'FBQUFBQUFBQUFBQUFBT/wAARCAABAAEDAREAAhEBAxEB/8QAFAABAAAAAAAAAAA'
          + 'AAAAAAAAACf/EABQQAQAAAAAAAAAAAAAAAAAAAAD/xAAUAQEAAAAAAAAAAAAAAA'
          + 'AAAAAA/8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAwDAQACEQMRAD8AVMH/2Q==',

        /**
         * Test PNG image, encoded as base64.
         */
        'image/png' :
            'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABAQMAAAAl21bKAAAAA1BMVEX///+nxBvI'
          + 'AAAACklEQVQI12NgAAAAAgAB4iG8MwAAAABJRU5ErkJggg==',

        /**
         * Test WebP image, encoded as base64.
         */
        'image/webp' : 'UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA=='

    };

    /**
     * Return a promise which resolves with to an array of image mimetypes
     * supported by the browser, once those mimetypes are known. The returned
     * promise is guaranteed to resolve successfully.
     *
     * @returns {Promise.<String[]>}
     *     A promise which resolves with an array of image mimetypes supported
     *     by the browser.
     */
    service.getSupportedMimetypes = function getSupportedMimetypes() {

        var deferred = $q.defer();

        var supported = [];
        var pendingTests = [];

        // Test each possibly-supported image
        angular.forEach(testImages, function testImageSupport(data, mimetype) {

            // Add promise for current image test
            var imageTest = $q.defer();
            pendingTests.push(imageTest.promise);

            // Attempt to load image
            var image = new Image();
            image.src = 'data:' + mimetype + ';base64,' + data;

            // Store as supported depending on whether load was successful
            image.onload = image.onerror = function imageTestComplete() {

                // Image format is supported if successfully decoded
                if (image.width === 1 && image.height === 1)
                    supported.push(mimetype);

                // Test is complete
                imageTest.resolve();

            };

        });

        // When all image tests are complete, resolve promise with list of
        // supported formats
        $q.all(pendingTests).then(function imageTestsCompleted() {
            deferred.resolve(supported);
        });

        return deferred.promise;

    };

    return service;

}]);
