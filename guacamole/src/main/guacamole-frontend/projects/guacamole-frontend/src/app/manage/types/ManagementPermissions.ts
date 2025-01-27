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

import { PermissionSet } from '../../rest/types/PermissionSet';

/**
 * Higher-level representation of the management-related permissions
 * available to the current user on a particular, arbitrary object.
 */
export class ManagementPermissions {

    /**
     * The identifier of the associated object, or null if the object does
     * not yet exist.
     */
    identifier: string | null;

    /**
     * Whether the user can save the associated object. This could be
     * updating an existing object, or creating a new object.
     */
    canSaveObject: boolean;

    /**
     * Whether the user can clone the associated object.
     */
    canCloneObject: boolean;

    /**
     * Whether the user can delete the associated object.
     */
    canDeleteObject: boolean;

    /**
     * Whether the user can change attributes which are currently
     * associated with the object.
     */
    canChangeAttributes: boolean;

    /**
     * Whether the user can change absolutely all attributes associated
     * with the object, including those which are not already present.
     */
    canChangeAllAttributes: boolean;

    /**
     * Whether the user can change permissions which are assigned to the
     * associated object, if the object is capable of being assigned
     * permissions.
     */
    canChangePermissions: boolean;

    /**
     * Creates a new ManagementPermissions. This constructor initializes the properties of the
     * new ManagementPermissions with the corresponding properties of the given template.
     *
     * @param template
     *     An object whose properties should be copied into the new
     *     ManagementPermissions object.
     */
    constructor(template: ManagementPermissions) {
        this.identifier = template.identifier || null;
        this.canSaveObject = template.canSaveObject;
        this.canCloneObject = template.canCloneObject;
        this.canDeleteObject = template.canDeleteObject;
        this.canChangeAttributes = template.canChangeAttributes;
        this.canChangeAllAttributes = template.canChangeAllAttributes;
        this.canChangePermissions = template.canChangePermissions;
    }

    /**
     * Creates a new {@link ManagementPermissions} which defines the high-level
     * actions the current user may take for the given object.
     *
     * @param permissions
     *     The effective permissions granted to the current user within the
     *     data source associated with the object being managed.
     *
     * @param createPermission
     *     The system permission required to create objects of the same type as
     *     the object being managed, as defined by
     *     {@link PermissionSet.SystemPermissionType}.
     *
     * @param hasObjectPermission
     *     The function to invoke to test whether a {@link PermissionSet}
     *     contains a particular object permission. The parameters accepted
     *     by this function must be identical to those accepted by
     *     {@link PermissionSet.hasUserPermission()},
     *     {@link PermissionSet.hasConnectionPermission()}, etc.
     *
     * @param identifier
     *     The identifier of the object being managed. If the object does not
     *     yet exist, this parameter should be omitted or set to null.
     *
     * @returns
     *     A new {@link ManagementPermissions} which defines the high-level
     *     actions the current user may take for the given object.
     */
    static fromPermissionSet(permissions: PermissionSet, createPermission: PermissionSet.SystemPermissionType,
                             hasObjectPermission: (permissions: PermissionSet, permission: PermissionSet.ObjectPermissionType, identifier?: string) => boolean,
                             identifier?: string): ManagementPermissions {

        const nullableIdentifier = identifier === undefined ? null : identifier;

        const isAdmin = PermissionSet.hasSystemPermission(permissions,
            PermissionSet.SystemPermissionType.ADMINISTER);

        const canCreate = PermissionSet.hasSystemPermission(permissions, createPermission);
        const canAdminister = hasObjectPermission(permissions, PermissionSet.ObjectPermissionType.ADMINISTER, identifier);
        const canUpdate = hasObjectPermission(permissions, PermissionSet.ObjectPermissionType.UPDATE, identifier);
        const canDelete = hasObjectPermission(permissions, PermissionSet.ObjectPermissionType.DELETE, identifier);

        const exists = !!nullableIdentifier;

        return new ManagementPermissions({

            identifier: nullableIdentifier,

            // A user can save (create or update) an object if they are a
            // system-level administrator, OR the object does not yet exist and
            // the user has explicit permission to create such objects, OR the
            // object does already exist and the user has explicit UPDATE
            // permission on the object
            canSaveObject: isAdmin || (!exists && canCreate) || canUpdate,

            // A user can clone an object only if the object exists, and
            // only if they are a system-level administrator OR they have
            // explicit permission to create such objects
            canCloneObject: exists && (isAdmin || canCreate),

            // A user can delete an object only if the object exists, and
            // only if they are a system-level administrator OR they have
            // explicit DELETE permission on the object
            canDeleteObject: exists && (isAdmin || canDelete),

            // Attributes in general (with or without existing values) can only
            // be changed if the object is being created, OR the user is a
            // system-level administrator, OR the user has explicit UPDATE
            // permission on the object
            canChangeAttributes: !exists || isAdmin || canUpdate,

            // A user can change the attributes of an object which are not
            // explicitly defined on that object when the object is being
            // created
            canChangeAllAttributes: !exists,

            // A user can change the system permissions related to an object
            // if they are a system-level admin, OR they are creating the
            // object, OR they have explicit ADMINISTER permission on the
            // existing object
            canChangePermissions: isAdmin || !exists || canAdminister

        });

    }
}
