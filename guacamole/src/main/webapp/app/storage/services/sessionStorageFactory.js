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
 * Factory for session-local storage. Creating session-local storage creates a
 * getter/setter with semantics tied to the user's session. If a user is logged
 * in, the storage is consistent. If the user logs out, the storage will not
 * persist new values, and attempts to retrieve the existing value will result
 * only in the default value.
 */
angular.module('storage').factory('sessionStorageFactory', ['$injector', function sessionStorageFactory($injector) {

    // Required services
    var $rootScope            = $injector.get('$rootScope');
    var authenticationService = $injector.get('authenticationService');

    var service = {};

    /**
     * Creates session-local storage that uses the provided default value or
     * getter to obtain new values as necessary. Beware that if the default is
     * an object, the resulting getter provide deep copies for new values.
     *
     * @param {Function|*} [template]
     *     The default value for new users, or a getter which returns a newly-
     *     created default value.
     *
     * @param {Function} [destructor]
     *     Function which will be called just before the stored value is
     *     destroyed on logout, if a value is stored.
     *
     * @returns {Function}
     *     A getter/setter which returns or sets the current value of the new
     *     session-local storage. Newly-set values will only persist of the
     *     user is actually logged in.
     */
    service.create = function create(template, destructor) {

        /**
         * Whether new values may be stored and retrieved.
         *
         * @type Boolean
         */
        var enabled = !!authenticationService.getCurrentToken();

        /**
         * Getter which returns the default value for this storage.
         *
         * @type Function
         */
        var getter;

        // If getter provided, use that
        if (typeof template === 'function')
            getter = template;

        // Otherwise, always create a deep copy
        else
            getter = function getCopy() {
                return angular.copy(template);
            };

        /**
         * The current value of this storage, or undefined if not yet set.
         */
        var value = undefined;

        // Reset value and allow storage when the user is logged in
        $rootScope.$on('guacLogin', function userLoggedIn() {
            enabled = true;
            value = undefined;
        });

        // Reset value and disallow storage when the user is logged out
        $rootScope.$on('guacLogout', function userLoggedOut() {

            // Call destructor before storage is teared down
            if (angular.isDefined(value) && destructor)
                destructor(value);

            // Destroy storage
            enabled = false;
            value = undefined;

        });

        // Return getter/setter for value
        return function sessionLocalGetterSetter(newValue) {

            // Only actually store/retrieve values if enabled
            if (enabled) {

                // Set value if provided
                if (angular.isDefined(newValue))
                    value = newValue;

                // Obtain new value if unset
                if (!angular.isDefined(value))
                    value = getter();

                // Return current value
                return value;

            }

            // Otherwise, just pretend to store/retrieve
            return angular.isDefined(newValue) ? newValue : getter();

        };

    };

    return service;

}]);
