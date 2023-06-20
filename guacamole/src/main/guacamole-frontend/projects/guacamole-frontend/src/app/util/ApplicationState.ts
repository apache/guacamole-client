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
 * Possible overall states of the client side of the web application.
 */
export enum ApplicationState {

    /**
     * A non-interactive authentication attempt failed.
     */
    AUTOMATIC_LOGIN_REJECTED = 'automaticLoginRejected',

    /**
     * The application has fully loaded but is awaiting credentials from
     * the user before proceeding.
     */
    AWAITING_CREDENTIALS = 'awaitingCredentials',

    /**
     * A fatal error has occurred that will prevent the client side of the
     * application from functioning properly.
     */
    FATAL_ERROR = 'fatalError',

    /**
     * The application has just started within the user's browser and has
     * not yet settled into any specific state.
     */
    LOADING = 'loading',

    /**
     * The user has manually logged out.
     */
    LOGGED_OUT = 'loggedOut',

    /**
     * The application has fully loaded and the user has logged in
     */
    READY = 'ready'


}
