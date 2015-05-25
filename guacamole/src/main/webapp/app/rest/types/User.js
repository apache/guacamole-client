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
 * Service which defines the User class.
 */
angular.module('rest').factory('User', [function defineUser() {
            
    /**
     * The object returned by REST API calls when representing the data
     * associated with a user.
     * 
     * @constructor
     * @param {User|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     User.
     */
    var User = function User(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The name which uniquely identifies this user.
         *
         * @type String
         */
        this.username = template.username;

        /**
         * This user's password. Note that the REST API may not populate this
         * property for the sake of security. In most cases, it's not even
         * possible for the authentication layer to retrieve the user's true
         * password.
         * 
         * @type String
         */
        this.password = template.password;

        /**
         * Arbitrary name/value pairs which further describe this user. The
         * semantics and validity of these attributes are dictated by the
         * extension which defines them.
         *
         * @type Object.<String, String>
         */
        this.attributes = {};

    };

    return User;

}]);