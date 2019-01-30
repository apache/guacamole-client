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

package org.apache.guacamole.auth.common.sharingprofile;

import java.util.Map;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.common.user.ModeledAuthenticatedUser;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating sharing profiles.
 */
public interface SharingProfileServiceInterface {

	/**
     * Retrieves all parameters visible to the given user and associated with
     * the sharing profile having the given identifier. If the given user has no
     * access to such parameters, or no such sharing profile exists, the
     * returned map will be empty.
     *
     * @param user
     *            The user retrieving sharing profile parameters.
     *
     * @param identifier
     *            The identifier of the sharing profile whose parameters are
     *            being retrieved.
     *
     * @return A new map of all parameter name/value pairs that the given user
     *         has access to.
     */
	public Map<String, String> retrieveParameters(ModeledAuthenticatedUser user,
            String identifier);

	public ModeledSharingProfile retrieveObject(ModeledAuthenticatedUser user,
            String identifier) throws GuacamoleException;
}
