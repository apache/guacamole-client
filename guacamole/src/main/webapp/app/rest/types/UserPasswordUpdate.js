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
 * Service which defines the UserPasswordUpdate class.
 */
angular.module('rest').factory('UserPasswordUpdate', [function defineUserPasswordUpdate() {
            
    /**
     * The object sent to the REST API when representing the data
     * associated with a user password update.
     * 
     * @constructor
     * @param {UserPasswordUpdate|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     UserPasswordUpdate.
     */
    var UserPasswordUpdate = function UserPasswordUpdate(template) {

        // Use empty object by default
        template = template || {};

        /**
         * This user's current password. Required for authenticating the user
         * as part of to the password update operation.
         * 
         * @type String
         */
        this.oldPassword = template.oldPassword;

        /**
         * The new password to set for the user.
         * 
         * @type String
         */
        this.newPassword = template.newPassword;

    };

    return UserPasswordUpdate;

}]);