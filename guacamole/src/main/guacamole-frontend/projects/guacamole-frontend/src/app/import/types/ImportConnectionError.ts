

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

import { Optional } from '../../util/utility-types';
import { DisplayErrorList } from './DisplayErrorList';

/**
 * A representation of the errors associated with a connection to be
 * imported, along with some basic information connection information to
 * identify the connection having the error, as returned from a parsed
 * user-supplied import file.
 */
export class ImportConnectionError {

    /**
     * The row number within the original connection import file for this
     * connection. This should be 1-indexed.
     */
    rowNumber: number;

    /**
     * The human-readable name of this connection, which is not necessarily
     * unique.
     */
    name: String;

    /**
     * The human-readable connection group path for this connection, of the
     * form "ROOT/Parent/Child".
     */
    group: String;

    /**
     * The name of the protocol associated with this connection, such as
     * "vnc" or "rdp".
     */
    protocol: String;

    /**
     * The error messages associated with this particular connection, if any.
     */
    errors: DisplayErrorList;

    /**
     * Creates a new ImportConnectionError.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     ImportConnectionError.
     */
    constructor(template: Optional<ImportConnectionError, 'errors'>) {

        this.rowNumber = template.rowNumber;
        this.name = template.name;
        this.group = template.group;
        this.protocol = template.protocol;
        this.errors = template.errors || new DisplayErrorList();

    }

}
