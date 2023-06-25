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
 * Consumed by REST API calls when representing changes to an
 * arbitrary set of directory-based objects.
 *
 * @template T
 *     The type of object being modified.
 */
export class DirectoryPatch<T> {

    /**
     * The operation to apply to the objects indicated by the path. Valid
     * operation values are defined within DirectoryPatch.Operation.
     */
    op?: DirectoryPatch.Operation;

    /**
     * The path of the objects to modify. For creation of new objects, this
     * should be "/". Otherwise, it should be "/{identifier}", specifying
     * the identifier of the existing object being modified.
     *
     * @default '/'
     */
    path: string;

    /**
     * The object being added/replaced, or undefined if deleting.
     */
    value?: T;

    /**
     * Creates a new DirectoryPatch.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     DirectoryPatch.
     */
    constructor(template: Partial<DirectoryPatch<T>> = {}) {
        this.op = template.op;
        this.path = template.path || '/';
        this.value = template.value;
    }

}

export namespace DirectoryPatch {

    /**
     * All valid patch operations for directory-based objects.
     */
    export enum Operation {

        /**
         * Adds the specified object to the relation.
         */
        ADD = 'add',

        /**
         * Replaces (updates) the specified object from the relation.
         */
        REPLACE = 'replace',

        /**
         * Removes the specified object from the relation.
         */
        REMOVE = 'remove'

    }

}
