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
 * A service for checking browser video support.
 */
angular.module('client').factory('guacVideo', [function guacVideo() {
           
    /**
     * Object describing the UI's level of video support.
     */
    return new (function() {

        var codecs = [
            'video/ogg; codecs="theora, vorbis"',
            'video/mp4; codecs="avc1.4D401E, mp4a.40.5"',
            'video/webm; codecs="vp8.0, vorbis"'
        ];

        var probably_supported = [];
        var maybe_supported = [];

        /**
         * Array of all supported video mimetypes, ordered by liklihood of
         * working.
         */
        this.supported = [];

        // Build array of supported audio formats
        codecs.forEach(function(mimetype) {

            var video = document.createElement("video");
            var support_level = video.canPlayType(mimetype);

            // Trim semicolon and trailer
            var semicolon = mimetype.indexOf(";");
            if (semicolon != -1)
                mimetype = mimetype.substring(0, semicolon);

            // Partition by probably/maybe
            if (support_level == "probably")
                probably_supported.push(mimetype);
            else if (support_level == "maybe")
                maybe_supported.push(mimetype);

        });

        // Add probably supported types first
        Array.prototype.push.apply(
            this.supported, probably_supported);

        // Prioritize "maybe" supported types second
        Array.prototype.push.apply(
            this.supported, maybe_supported);

    })();

}]);
