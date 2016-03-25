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

import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;
import org.apache.guacamole.net.auth.permission.SystemPermissionSet;


/**
 * A user of the Guacamole web application.
 *
 * @author Michael Jumper
 */
public interface User extends Identifiable {

    /**
     * Returns this user's password. Note that the password returned may be
     * hashed or completely arbitrary.
     *
     * @return A String which may (or may not) be the user's password.
     */
    public String getPassword();

    /**
     * Sets this user's password. Note that while this function is guaranteed
     * to change the password of this User object, there is no guarantee that
     * getPassword() will return the value given to setPassword().
     *
     * @param password The password to set.
     */
    public void setPassword(String password);

    /**
     * Returns all attributes associated with this user. The returned map may
     * not be modifiable.
     *
     * @return
     *     A map of all attribute identifiers to their corresponding values,
     *     for all attributes associated with this user, which may not be
     *     modifiable.
     */
    Map<String, String> getAttributes();

    /**
     * Sets the given attributes. If an attribute within the map is not
     * supported, it will simply be dropped. Any attributes not within the
     * given map will be left untouched.
     *
     * @param attributes
     *     A map of all attribute identifiers to their corresponding values.
     */
    void setAttributes(Map<String, String> attributes);

    /**
     * Returns all system-level permissions given to this user.
     *
     * @return
     *     A SystemPermissionSet of all system-level permissions granted to
     *     this user.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving permissions, or if reading all
     *     permissions is not allowed.
     */
    SystemPermissionSet getSystemPermissions() throws GuacamoleException;

    /**
     * Returns all connection permissions given to this user.
     *
     * @return
     *     An ObjectPermissionSet of all connection permissions granted to this
     *     user.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving permissions, or if reading all
     *     permissions is not allowed.
     */
    ObjectPermissionSet getConnectionPermissions()
            throws GuacamoleException;

    /**
     * Returns all connection group permissions given to this user.
     *
     * @return
     *     An ObjectPermissionSet of all connection group permissions granted
     *     to this user.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving permissions, or if reading all
     *     permissions is not allowed.
     */
    ObjectPermissionSet getConnectionGroupPermissions()
            throws GuacamoleException;

    /**
     * Returns all permissions given to this user regarding currently-active
     * connections.
     *
     * @return
     *     An ObjectPermissionSet of all active connection permissions granted
     *     to this user.
     *
     * @throws GuacamoleException 
     *     If an error occurs while retrieving permissions, or if reading all
     *     permissions is not allowed.
     */
    ObjectPermissionSet getActiveConnectionPermissions()
            throws GuacamoleException;

    /**
     * Returns all user permissions given to this user.
     *
     * @return
     *     An ObjectPermissionSet of all user permissions granted to this user.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving permissions, or if reading all
     *     permissions is not allowed.
     */
    ObjectPermissionSet getUserPermissions() throws GuacamoleException;

}
