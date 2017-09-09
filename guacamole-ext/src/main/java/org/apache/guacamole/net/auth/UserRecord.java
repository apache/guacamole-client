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

package org.apache.guacamole.net.auth;

/**
 * A logging record describing when a user started and ended their Guacamole
 * session.
 */
public interface UserRecord extends ActivityRecord {

    /**
     * Returns the authentication token associated with the user's session, if
     * known. If permission is not granted to view the authentication tokens of
     * other users, this may be null.
     *
     * @return
     *     The authentication token associated with the user's session, or null
     *     if this information is unavailable.
     */
    public String getToken();

}
