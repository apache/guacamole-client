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
 * One stored connection or sharing profile parameter, as read and written by
 * the parameter encryption sweep.
 */
public class ParameterRow {

    /** The identifier of the connection or sharing profile owning the row. */
    private String ownerIdentifier;

    /** The parameter name. */
    private String name;

    /** The stored value: plain text, or ciphertext if encryption is set. */
    private String value;

    /** The ID of the key that encrypted the value, or null if plain text. */
    private String encryption;

    /**
     * Returns the identifier of the connection or sharing profile owning the row.
     *
     * @return
     *     The identifier of the connection or sharing profile owning the row.
     */
    public String getOwnerIdentifier() {
        return ownerIdentifier;
    }

    /**
     * Sets the identifier of the connection or sharing profile owning the row.
     *
     * @param ownerIdentifier
     *     The identifier of the connection or sharing profile owning the row.
     */
    public void setOwnerIdentifier(String ownerIdentifier) {
        this.ownerIdentifier = ownerIdentifier;
    }

    /**
     * Returns the parameter name.
     *
     * @return
     *     The parameter name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the parameter name.
     *
     * @param name
     *     The parameter name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the stored value: plain text, or ciphertext if encryption is set.
     *
     * @return
     *     The stored value: plain text, or ciphertext if encryption is set.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the stored value: plain text, or ciphertext if encryption is set.
     *
     * @param value
     *     The stored value: plain text, or ciphertext if encryption is set.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns the ID of the key that encrypted the value, or null if plain text.
     *
     * @return
     *     The ID of the key that encrypted the value, or null if plain text.
     */
    public String getEncryption() {
        return encryption;
    }

    /**
     * Sets the ID of the key that encrypted the value, or null if plain text.
     *
     * @param encryption
     *     The ID of the key that encrypted the value, or null if plain text.
     */
    public void setEncryption(String encryption) {
        this.encryption = encryption;
    }

}
