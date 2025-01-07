

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

import { ManagedFileTransferState } from './ManagedFileTransferState';
import { Optional } from '../../util/utility-types';

/**
 * Object which serves as a surrogate interface, encapsulating a Guacamole
 * file upload while it is active, allowing it to be detached and
 * reattached from different client views.
 */
export class ManagedFileUpload {

    /**
     * The current state of the file transfer stream.
     */
    transferState: ManagedFileTransferState;

    /**
     * The mimetype of the file being transferred.
     */
    mimetype?: string;

    /**
     * The filename of the file being transferred.
     */
    filename?: string;

    /**
     * The number of bytes transferred so far.
     */
    progress?: number;

    /**
     * The total number of bytes in the file.
     */
    length?: number;

    /**
     * Creates a new ManagedFileUpload. This constructor initializes the properties of the
     * new ManagedFileUpload with the corresponding properties of the given template.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     ManagedFileUpload.
     */
    constructor(template: Optional<ManagedFileUpload, 'transferState'> = {}) {
        this.transferState = template.transferState || new ManagedFileTransferState();
        this.mimetype = template.mimetype;
        this.filename = template.filename;
        this.progress = template.progress;
        this.length = template.length;
    }

}
