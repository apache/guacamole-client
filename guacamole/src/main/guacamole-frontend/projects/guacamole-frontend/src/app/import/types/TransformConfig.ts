

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

    // Callbacks for required fields
    nameGetter?: (row: string[]) => any;
    protocolGetter?: (row: string[]) => any;

    // Callbacks for a parent group ID or group path
    groupGetter?: (row: string[]) => any;
    parentIdentifierGetter?: (row: string[]) => any;

    // Callbacks for user and user group identifiers
    usersGetter: (row: string[]) => string[];
    userGroupsGetter: (row: string[]) => string[];

    // Callbacks that will generate either connection attributes or
    // parameters. These callbacks will return a {type, name, value}
    // object containing the type ("parameter" or "attribute"),
    // the name of the attribute or parameter, and the corresponding
    // value.
    parameterOrAttributeGetters: ((row: string[]) => {
        type?: string;
        name?: string;
        value?: any;
        errors?: ParseError[]
    })[];

}
