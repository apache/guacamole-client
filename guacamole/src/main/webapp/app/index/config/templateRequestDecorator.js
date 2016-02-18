/*
 * Copyright (C) 2016 Glyptodon LLC
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
 * Overrides $templateRequest such that HTML patches defined within Guacamole
 * extensions are automatically applied to template contents.
 */
angular.module('index').config(['$provide', function($provide) {
    $provide.decorator('$templateRequest', ['$delegate', '$injector',
            function decorateTemplateRequest($delegate, $injector) {

        // Required services
        var $q = $injector.get('$q');

        /**
         * Invokes $templateRequest() with all arguments exactly as provided,
         * applying all HTML patches from any installed Guacamole extensions
         * to the HTML of the requested template.
         *
         * @returns {Promise.<String>}
         *     A Promise which resolves with the patched HTML contents of the
         *     requested template if retrieval of the template is successful.
         */
        var decoratedTemplateRequest = function decoratedTemplateRequest() {
            
            var deferred = $q.defer();

            // Resolve promise with patched template HTML
            $delegate.apply(this, arguments).then(function patchTemplate(data) {

                // Parse HTML into DOM tree
                var root = $('<div></div>').html(data);

                // STUB: Apply HTML patches
                root.find('a').after('<p>HELLO</p>');

                // Transform back into HTML
                deferred.resolve.call(this, root.html());

            }, deferred.reject);

            return deferred.promise;

        };

        return decoratedTemplateRequest;

    }]);
}]);