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
 * Selector for the password input element on the login page.
 */
export const passwordElement = () => cy.get('input[name="password"]');

/**
 * Selector for the username input element on the login page.
 */
export const usernameElement = () => cy.get('input[name="username"]');

/**
 * Selector for the submit button on the login page.
 */
export const submitButton = () => cy.get('input[type="submit"]');

/**
 * Logs in with the given username and password.
 *
 * @param username
 *     The username to login with.
 *
 * @param password
 *     The password to login with.
 */
export const login = (username: string, password: string) => {
    usernameElement().type(username);
    passwordElement().type(password);
    submitButton().eq(0).click();
};
