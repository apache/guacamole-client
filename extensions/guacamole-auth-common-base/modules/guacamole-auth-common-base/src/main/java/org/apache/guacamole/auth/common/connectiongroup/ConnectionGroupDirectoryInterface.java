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

package org.apache.guacamole.auth.common.connectiongroup;

import org.apache.guacamole.auth.common.user.ModeledAuthenticatedUser;

/**
 * Interface of the ConnectionGroup Directory which is driven by an
 * underlying, arbitrary database.
 */
public interface ConnectionGroupDirectoryInterface {

	/**
     * Initializes this object, associating it with the current authenticated
     * user and populating it with data from the given model object
     *
     * @param currentUser
     *     The user that created or retrieved this object.
     */
	public void init(ModeledAuthenticatedUser currentUser);

}
