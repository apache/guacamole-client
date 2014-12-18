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
 * Service which defines the PermissionPatch class.
 */
angular.module('rest').factory('PermissionPatch', [function definePermissionPatch() {
            
    /**
     * The object returned by REST API calls when representing changes to the
     * permissions granted to a specific user.
     * 
     * @constructor
     * @param {PermissionPatch|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     PermissionPatch.
     */
    var PermissionPatch = function PermissionPatch(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The operation to apply to the permissions indicated by the path.
         * Valid operation values are defined within PermissionPatch.Operation.
         *
         * @type String
         */
        this.op = template.op;

        /**
         * The path of the permissions to modify. Depending on the type of the
         * permission, this will be either "/connectionPermissions/ID",
         * "/connectionGroupPermissions/ID", "/userPermissions/ID", or
         * "/systemPermissions", where "ID" is the identifier of the object
         * to which the permissions apply, if any.
         *
         * @type String
         */
        this.path = template.path;

        /**
         * The array of permissions. If the permission applies to an object,
         * such as a connection or connection group, these will be values from
         * PermissionSet.ObjectPermissionType. If the permission applies to
         * the system as a whole (the path is "/systemPermissions"), these will
         * be values from PermissionSet.SystemPermissionType.
         *
         * @type String[]
         */
        this.value = template.value || [];

    };

    /**
     * All valid patch operations for permissions. Currently, only add and
     * remove are supported.
     */
    PermissionPatch.Operation = {

        /**
         * Adds (grants) the specified permissions.
         */
        ADD : "add",

        /**
         * Removes (revokes) the specified permissions.
         */
        REMOVE : "remove"

    };

    return PermissionPatch;

}]);