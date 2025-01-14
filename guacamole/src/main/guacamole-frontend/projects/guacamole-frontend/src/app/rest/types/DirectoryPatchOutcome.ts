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

import { DirectoryPatch } from './DirectoryPatch';
import { TranslatableMessage } from './TranslatableMessage';

/**
 * Returned by a PATCH request to a directory REST API,
 * representing the outcome associated with a particular patch in the
 * request. This object can indicate either a successful or unsuccessful
 * response. The error field is only meaningful for unsuccessful patches.
 */
export class DirectoryPatchOutcome {

    /**
     * The operation to apply to the objects indicated by the path. Valid
     * operation values are defined within DirectoryPatch.Operation.
     */
    op?: DirectoryPatch.Operation;

    /**
     * The path of the object operated on by the corresponding patch in the
     * request.
     */
    path?: string;

    /**
     * The identifier of the object operated on by the corresponding patch
     * in the request. If the object was newly created and the PATCH request
     * did not fail, this will be the identifier of the newly created object.
     */
    identifier?: string;

    /**
     * The error message associated with the failure, if the patch failed to
     * apply.
     */
    error?: TranslatableMessage;

    /**
     * Creates a new DirectoryPatchOutcome.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     DirectoryPatchOutcome.
     */
    constructor(template: Partial<DirectoryPatchOutcome> = {}) {
        this.op = template.op;
        this.path = template.path;
        this.identifier = template.identifier;
        this.error = template.error;
    }

}
