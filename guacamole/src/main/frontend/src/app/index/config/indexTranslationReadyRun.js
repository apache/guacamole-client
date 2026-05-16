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
 * Manages $rootScope.translationsReady so guacFocus can focus inputs correctly
 * after translations are loaded. Until then translate-cloak may hide the page,
 * so an early focus does nothing useful; when the flag becomes true, guacFocus
 * runs focus again.
 */
angular.module('index').run(['$rootScope', '$translate', '$log',
        function indexTranslationReadyRun($rootScope, $translate, $log) {

    /**
     * False while a language file is being loaded or translate-cloak may still
     * hide content; true when the current translation is ready so guacFocus can
     * rely on a visible, translated UI for focus.
     */
    $rootScope.translationsReady = false;

    /**
     * Triggers guacFocus to focus on a field when it is necessary.
     */
    const markReady = function markReady() {
        $rootScope.translationsReady = true;
    };

    // This sets the flag to false to show that a language is being loaded.
    // That avoids treating the UI as ready while the page is still cloaked.
    $rootScope.$on('$translateChangeStart', function translateChangeStart() {
        $rootScope.translationsReady = false;
    });

    // This sets the flag to true when the language change has been completed
    // so guacFocus can run after the cloak clears.
    $rootScope.$on('$translateChangeSuccess', function translateChangeSuccess() {
        markReady();
    });

    try {
        if ($translate.onReady) {
            // This may return a promise. Using then(markReady, markReady) calls markReady
            // whether that promise fulfills or rejects.
            const result = $translate.onReady();
            if (result && typeof result.then === 'function')
                result.then(markReady, markReady);
            else
                $translate.onReady(markReady);
        }
        else
            markReady();
    }
    catch (err) {
        $log.warn('Could not wait for $translate.onReady(); allowing focus anyway.', err);
        markReady();
    }

}]);
