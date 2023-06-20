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
 * Returned by REST API calls when representing the successful
 * result of an authentication attempt.
 */
export class AuthenticationResult {

    /**
     * The unique token generated for the user that authenticated.
     */
    authToken: string;

    /**
     * The name which uniquely identifies the user that authenticated.
     */
    username: string;

    /**
     * The unique identifier of the data source which authenticated the
     * user.
     */
    dataSource: string;

    /**
     * The identifiers of all data sources available to the user that
     * authenticated.
     */
    availableDataSources?: string[];

    /**
     * Creates a new AuthenticationResult object.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     AuthenticationResult.
     */
    constructor(template: AuthenticationResult) {
        this.authToken = template.authToken;
        this.username = template.username;
        this.dataSource = template.dataSource;
        this.availableDataSources = template.availableDataSources;
    }

    /**
     * The username reserved by the Guacamole extension API for users which have
     * authenticated anonymously.
     */
    static readonly ANONYMOUS_USERNAME: string = '';
}
