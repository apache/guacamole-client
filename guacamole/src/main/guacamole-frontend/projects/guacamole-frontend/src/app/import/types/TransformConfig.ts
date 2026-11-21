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

import { ParseError } from './ParseError';

/**
 * All configuration required to generate a function that can
 * transform a row of CSV into a connection object.
 */
export interface TransformConfig {

    /**
     * Returns the connection name from a CSV row. Set if the CSV has a
     * "name" column; the file cannot be imported without it.
     */
    nameGetter?: (row: string[]) => string;

    /**
     * Returns the connection protocol from a CSV row. Set if the CSV has a
     * "protocol" column; the file cannot be imported without it.
     */
    protocolGetter?: (row: string[]) => string;

    /**
     * Returns the path of the parent connection group from a CSV row. Set if
     * the CSV has a "group" column.
     */
    groupGetter?: (row: string[]) => string;

    /**
     * Returns the identifier of the parent connection group from a CSV row.
     * Set if the CSV has a "parentIdentifier" column.
     */
    parentIdentifierGetter?: (row: string[]) => string;

    /**
     * Returns the identifiers of the users who should be granted access to
     * the connection, taken from the semicolon-separated "users" column of a
     * CSV row. Returns an empty array if the CSV has no such column.
     */
    usersGetter: (row: string[]) => string[];

    /**
     * Returns the identifiers of the user groups which should be granted
     * access to the connection, taken from the semicolon-separated "groups"
     * column of a CSV row. Returns an empty array if the CSV has no such
     * column.
     */
    userGroupsGetter: (row: string[]) => string[];

    /**
     * One callback for each remaining column, which generates either a
     * connection attribute or a connection parameter from a CSV row. Each
     * callback returns an object containing the type ("attributes" or
     * "parameters"), the name of the attribute or parameter, and its value.
     * For a column whose header does not say which of the two it is, the
     * callback determines the type from the protocol of the row and adds any
     * errors; it returns an empty object if the column has no value in the
     * row or the protocol is unknown.
     */
    parameterOrAttributeGetters: ((row: string[]) => {
        type?: string;
        name?: string;
        value?: string;
        errors?: ParseError[]
    })[];

}
