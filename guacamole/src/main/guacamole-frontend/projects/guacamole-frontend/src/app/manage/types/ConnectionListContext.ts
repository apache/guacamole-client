

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

import { PermissionFlagSet } from '../../rest/types/PermissionFlagSet';

/**
 * Expose permission query and modification functions to group list template.
 */
export interface ConnectionListContext {

    /**
     * Returns the PermissionFlagSet that contains the current state of
     * granted permissions.
     *
     * @returns
     *     The PermissionFlagSet describing the current state of granted
     *     permissions for the permission set being edited.
     */
    getPermissionFlags(): PermissionFlagSet;

    /**
     * Notifies the controller that a change has been made to the given
     * connection permission for the permission set being edited. This
     * only applies to READ permissions.
     *
     * @param identifier
     *     The identifier of the connection affected by the changed
     *     permission.
     */
    connectionPermissionChanged(identifier: string): void;

    /**
     * Notifies the controller that a change has been made to the given
     * connection group permission for the permission set being edited.
     * This only applies to READ permissions.
     *
     * @param identifier
     *     The identifier of the connection group affected by the
     *     changed permission.
     */
    connectionGroupPermissionChanged(identifier: string): void;

    /**
     * Notifies the controller that a change has been made to the given
     * sharing profile permission for the permission set being edited.
     * This only applies to READ permissions.
     *
     * @param identifier
     *     The identifier of the sharing profile affected by the changed
     *     permission.
     */
    sharingProfilePermissionChanged(identifier: string): void;

}
