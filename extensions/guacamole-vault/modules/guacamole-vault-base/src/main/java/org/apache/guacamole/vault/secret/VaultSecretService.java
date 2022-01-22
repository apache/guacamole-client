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

package org.apache.guacamole.vault.secret;

import java.util.concurrent.Future;
import org.apache.guacamole.GuacamoleException;

/**
 * Generic service for retrieving the value of a secret stored in a vault.
 */
public interface VaultSecretService {

    /**
     * Translates an arbitrary string, which may contain characters not allowed
     * by the vault implementation, into a string which is valid within a
     * secret name. The type of transformation performed on the string, if any,
     * will depend on the specific requirements of the vault provider.
     *
     * NOTE: It is critical that this transformation is deterministic and
     * reasonably predictable for users. If an implementation must apply a
     * transformation to secret names, that transformation needs to be
     * documented.
     *
     * @param nameComponent
     *     An arbitrary string intended for use within a secret name, but which
     *     may contain characters not allowed by the vault implementation.
     *
     * @return
     *     A string containing essentially the same content as the provided
     *     string, but transformed deterministically such that it is acceptable
     *     as a component of a secret name by the vault provider.
     */
    String canonicalize(String nameComponent);

    /**
     * Returns a Future which eventually completes with the value of the secret
     * having the given name. If no such secret exists, the Future will be
     * completed with null.
     *
     * @param name
     *     The name of the secret to retrieve.
     *
     * @return
     *     A Future which completes with value of the secret having the given
     *     name. If no such secret exists, the Future will be completed with
     *     null. If an error occurs asynchronously which prevents retrieval of
     *     the secret, that error will be exposed through an ExecutionException
     *     when an attempt is made to retrieve the value from the Future.
     *
     * @throws GuacamoleException
     *     If the secret cannot be retrieved due to an error.
     */
    Future<String> getValue(String name) throws GuacamoleException;

}
