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

package org.apache.guacamole.protocol;

/**
 * An enum that defines the available Guacamole protocol versions that can be
 * used between guacd and clients, and provides convenience methods for parsing
 * and comparing versions.
 */
public enum GuacamoleProtocolVersion {
    
    /**
     * Protocol version 1.0.0 and older.  Any client that doesn't explicitly
     * set the protocol version will negotiate down to this protocol version.
     * This requires that handshake instructions be ordered correctly, and
     * lacks support for certain protocol-related features introduced in later
     * versions.
     */
    VERSION_1_0_0(1, 0, 0),

    /**
     * Protocol version 1.1.0, which introduces Client-Server version
     * detection, arbitrary handshake instruction order, and support
     * for passing the client timezone to the server during the handshake.
     */
    VERSION_1_1_0(1, 1, 0);
    
    /**
     * The major version component of the protocol version.
     */
    private final int major;

    /**
     * The minor version component of the protocol version.
     */
    private final int minor;

    /**
     * The patch version component of the protocol version.
     */
    private final int patch;
    
    /**
     * Generate a new GuacamoleProtocolVersion object with the given
     * major version, minor version, and patch version.
     * 
     * @param major
     *     The integer representation of the major version component.
     * 
     * @param minor
     *     The integer representation of the minor version component.
     * 
     * @param patch 
     *     The integer representation of the patch version component.
     */
    GuacamoleProtocolVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }
    
    /**
     * Return the major version component of the protocol version.
     * 
     * @return 
     *     The integer major version component.
     */
    public int getMajor() {
        return major;
    }
    
    /**
     * Return the minor version component of the protocol version.
     * 
     * @return 
     *     The integer minor version component.
     */
    public int getMinor() {
        return minor;
    }
    
    /**
     * Return the patch version component of the protocol version.
     * 
     * @return 
     *     The integer patch version component.
     */
    public int getPatch() {
        return patch;
    }
    
    /**
     * Determines whether or not this object is greater than or equal to the
     * the version passed in to the method.  Returns a boolean true if the
     * version is the same as or greater than the other version, otherwise
     * false.
     * 
     * @param otherVersion
     *     The version to which this object should be compared.
     * 
     * @return 
     *     True if this object is greater than or equal to the other version.
     */
    public boolean atLeast(GuacamoleProtocolVersion otherVersion) {
        
        // If major is not the same, compare first
        if (major != otherVersion.getMajor())
            return major > otherVersion.getMajor();
        
        // Major is the same, but minor is not, so compare minor versions
        if (minor != otherVersion.getMinor())
            return minor > otherVersion.getMinor();
        
        // Major and minor are identical, so compare and return patch
        return patch >= otherVersion.getPatch();
        
    }
    
    /**
     * Parse the String format of the version provided and return the
     * the enum value matching that version.  If no value is provided, return
     * null.
     * 
     * @param version
     *     The String format of the version to parse.
     * 
     * @return
     *     The enum value that matches the specified version, VERSION_1_0_0
     *     if no match is found, or null if no comparison version is provided.
     */
    public static GuacamoleProtocolVersion getVersion(String version) {
        
        if (version == null || version.isEmpty())
            return null;
        
        try {
            return valueOf(version);
        }
        // If nothing matches, then return the most compatible version.
        catch (IllegalArgumentException e) {
            return GuacamoleProtocolVersion.VERSION_1_0_0;
        }
        
    }
    
    /**
     * Returns true if the specified capability is supported in the current
     * protocol version, otherwise false.
     * 
     * @param capability
     *     The protocol capability that is being checked for support.
     * 
     * @return
     *     True if the capability is supported in the current version,
     *     otherwise false.
     */
    public boolean isSupported(GuacamoleProtocolCapability capability) {
        
        return atLeast(capability.getVersion());
        
    }
    
}
