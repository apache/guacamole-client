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

package org.apache.guacamole.rest.usergroup;

import java.util.Map;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.apache.guacamole.net.auth.UserGroup;

/**
 * A simple UserGroup to expose through the REST endpoints.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class APIUserGroup {

    /**
     * The identifier of this user group.
     */
    private String identifier;

    /**
     * Map of all associated attributes by attribute identifier.
     */
    private Map<String, String> attributes;

    /**
     * Construct a new empty APIUserGroup.
     */
    public APIUserGroup() {}

    /**
     * Construct a new APIUserGroup from the provided UserGroup.
     *
     * @param group
     *     The UserGroup to construct the APIUserGroup from.
     */
    public APIUserGroup(UserGroup group) {
        this.identifier = group.getIdentifier();
        this.attributes = group.getAttributes();
    }

    /**
     * Returns the unique string which identifies this group relative to other
     * groups.
     *
     * @return
     *     The unique string which identifies this group.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the unique string which identifies this group relative to other
     * groups.
     *
     * @param identifier
     *     The unique string which identifies this group.
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns a map of all attributes associated with this user group. Each
     * entry key is the attribute identifier, while each value is the attribute
     * value itself.
     *
     * @return
     *     The attribute map for this user group.
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * Sets the map of all attributes associated with this user group. Each
     * entry key is the attribute identifier, while each value is the attribute
     * value itself.
     *
     * @param attributes
     *     The attribute map for this user group.
     */
    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

}
