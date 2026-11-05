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

package org.apache.guacamole.auth.jdbc.encryption;

/**
 * The kinds of object whose parameters may be stored encrypted. The type
 * string is bound into each ciphertext, so a value cannot be moved between
 * a connection and a sharing profile.
 */
public enum ParameterOwner {

    /** Parameters of a connection (guacamole_connection_parameter). */
    CONNECTION("connection"),

    /** Parameters of a sharing profile (guacamole_sharing_profile_parameter). */
    SHARING_PROFILE("sharing-profile");

    private final String type;

    ParameterOwner(String type) {
        this.type = type;
    }

    /**
     * Returns the stable type string bound into ciphertexts.
     *
     * @return
     *     The type string.
     */
    public String getType() {
        return type;
    }

}
