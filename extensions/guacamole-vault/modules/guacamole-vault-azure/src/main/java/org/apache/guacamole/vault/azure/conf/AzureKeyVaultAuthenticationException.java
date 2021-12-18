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

package org.apache.guacamole.vault.azure.conf;

/**
 * Unchecked exception thrown by AzureKeyVaultCredentials if an error occurs
 * during the authentication process. Note that the base KeyVaultCredentials
 * base class does not provide for checked exceptions within the authentication
 * process.
 *
 * @see AzureKeyVaultCredentials#doAuthenticate(java.lang.String, java.lang.String, java.lang.String)
 */
public class AzureKeyVaultAuthenticationException extends RuntimeException {

    /**
     * Creates a new AzureKeyVaultAuthenticationException having the given
     * human-readable message.
     *
     * @param message
     *     A human-readable message describing the error that occurred.
     */
    public AzureKeyVaultAuthenticationException(String message) {
        super(message);
    }

    /**
     * Creates a new AzureKeyVaultAuthenticationException having the given
     * human-readable message and cause.
     *
     * @param message
     *     A human-readable message describing the error that occurred.
     *
     * @param cause
     *     The error that caused this exception.
     */
   public AzureKeyVaultAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

}
