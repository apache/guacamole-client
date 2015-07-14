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
 * Service for loading translation definition files, conforming to the
 * angular-translate documentation for custom translation loaders:
 * 
 * https://github.com/angular-translate/angular-translate/wiki/Asynchronous-loading#using-custom-loader-service
 */
angular.module('locale').factory('translationLoader', ['$injector', function translationLoader($injector) {

    // Required services
    var $http        = $injector.get('$http');
    var $q           = $injector.get('$q');
    var cacheService = $injector.get('cacheService');

    /**
     * Satisfies a translation request for the given key by searching for the
     * translation files for each key in the given array, in order. The request
     * fails only if none of the files can be found.
     *
     * @param {Deferred} deferred
     *     The Deferred object to resolve or reject depending on whether at
     *     least one translation file can be successfully loaded.
     *
     * @param {String} requestedKey
     *     The originally-requested language key.
     *
     * @param {String[]} remainingKeys
     *     The keys of the languages to attempt to load, in order, where the
     *     first key in this array is the language to try within this function
     *     call. The first key in the array is not necessarily the originally-
     *     requested language key.
     */
    var satisfyTranslation = function satisfyTranslation(deferred, requestedKey, remainingKeys) {

        // Get current language key
        var currentKey = remainingKeys.shift();

        // If no languages to try, "succeed" with an empty translation (force fallback)
        if (!currentKey) {
            deferred.resolve('{}');
            return;
        }

        // Attempt to retrieve language
        $http({
            cache   : cacheService.languages,
            method  : 'GET',
            url     : 'translations/' + encodeURIComponent(currentKey) + '.json'
        })

        // Resolve promise if translation retrieved successfully
        .success(function translationFileRetrieved(translation) {
            deferred.resolve(translation);
        })

        // Retry with remaining languages if translation file could not be retrieved
        .error(function translationFileUnretrievable() {
            satisfyTranslation(deferred, requestedKey, remainingKeys);
        });

    };

    /**
     * Given a valid language key, returns all possible legal variations of
     * that key. Currently, this will be the given key and the given key
     * without the country code. If the key has no country code, only the
     * given key will be included in the returned array.
     *
     * @param {String} key
     *     The language key to generate variations of.
     *
     * @returns {String[]}
     *     All possible variations of the given language key.
     */
    var getKeyVariations = function getKeyVariations(key) {

        var underscore = key.indexOf('_');

        // If no underscore, only one possibility
        if (underscore === -1)
            return [key];

        // Otherwise, include the lack of country code as an option
        return [key, key.substr(0, underscore)];

    };

    /**
     * Custom loader function for angular-translate which loads the desired
     * language file dynamically via HTTP. If the language file cannot be
     * found, the fallback language is used instead.
     *
     * @param {Object} options
     *     Arbitrary options, containing at least a "key" property which
     *     contains the requested language key.
     *
     * @returns {Promise.<Object>}
     *     A promise which resolves to the requested translation string object.
     */
    return function loadTranslationFile(options) {

        var translation = $q.defer();

        // Satisfy the translation request using possible variations of the given key
        satisfyTranslation(translation, options.key, getKeyVariations(options.key));

        // Return promise which is resolved only after the translation file is loaded
        return translation.promise;

    };

}]);
