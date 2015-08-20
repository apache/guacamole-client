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
 * A service for checking browser non-standard image format support for
 * formats such as Google's WebP.
 */

angular.module('client').factory('guacImage', [function guacImage() {

    /**
     * Object describing the UI's level of non-standard image format support.
     */
    return new (function () {

        /**
         * All non-standard image formats to test along with a 1x1 sized
         * base64 encoded test image.
         */
        var formats = {
            'image/webp': 'data:image/webp;base64,UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA==',
        };

        /**
         * Array of supported (non-standard) image mimetypes.
         */
        this.supported = [];

        // Build array of supported image formats
        angular.forEach(formats, function(testImgUri, format) {
            var image = document.createElement("img");
            image.src = testImgUri;
            image.onload = image.onerror = (function (_this) {
                return function() {
                    if (image.width === 1) {
                        _this.supported.push(format);
                    }
                };
            })(this);
        }, this);

    })();

}]);
