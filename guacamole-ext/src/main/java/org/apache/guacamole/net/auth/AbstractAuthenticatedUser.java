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
 * Basic implementation of an AuthenticatedUser which uses the username to
 * determine equality. Username comparison is case-sensitive.
 *
 * @author Michael Jumper
 */
public abstract class AbstractAuthenticatedUser implements AuthenticatedUser {

    /**
     * The name of this user.
     */
    private String username;

    @Override
    public String getIdentifier() {
        return username;
    }

    @Override
    public void setIdentifier(String username) {
        this.username = username;
    }

    @Override
    public int hashCode() {
        if (username == null) return 0;
        return username.hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        // Not equal if null or not a User
        if (obj == null) return false;
        if (!(obj instanceof AbstractAuthenticatedUser)) return false;

        // Get username
        String objUsername = ((AbstractAuthenticatedUser) obj).username;

        // If null, equal only if this username is null
        if (objUsername == null) return username == null;

        // Otherwise, equal only if strings are identical
        return objUsername.equals(username);

    }

}
