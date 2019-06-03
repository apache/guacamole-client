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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * A regular expression that matches the VERSION_X_Y_Z pattern, where
     * X is the major version component, Y is the minor version component,
     * and Z is the patch version component.  This expression puts each of
     * the version components in their own group so that they can be easily
     * used later.
     */
    private static final Pattern VERSION_PATTERN =
            Pattern.compile("^VERSION_([0-9]+)_([0-9]+)_([0-9]+)$");
    
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
    private boolean atLeast(GuacamoleProtocolVersion otherVersion) {
        
        // If major is not the same, return inequality
        if (major != otherVersion.getMajor())
            return this.major > major;
        
        // Major is the same, but minor is not, return minor inequality
        if (minor != otherVersion.getMinor())
            return this.minor > minor;
        
        // Major and minor are equal, so return patch inequality
        return patch >= otherVersion.getPatch();
        
    }
    
    /**
     * Compare this version with the major, minor, and patch components
     * provided to the method, and determine if this version is compatible
     * with the provided version, returning a boolean true if it is compatible,
     * otherwise false.  This version is compatible with the version specified
     * by the provided components if the major, minor, and patch components
     * are equivalent or less than those provided.
     * 
     * @param major
     *     The major version component to compare for compatibility.
     * 
     * @param minor
     *     The minor version component to compare for compatibility.
     * 
     * @param patch
     *     The patch version component to compare for compatibility.
     * 
     * @return 
     *     True if this version is compatibility with the version components
     *     provided, otherwise false.
     */
    private boolean isCompatible(int major, int minor, int patch) {
        
        if (this.major != major)
            return this.major < major;
        
        if (this.minor != minor)
            return this.minor < minor;
        
        return this.patch <= patch;
        
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
        
        // If nothing is passed in, return null
        if (version == null || version.isEmpty())
            return null;
        
        // Check the string against the pattern matcher
        Matcher versionMatcher = VERSION_PATTERN.matcher(version);
        
        // If there is no RegEx match, return null
        if (!versionMatcher.matches())
            return null;
        
        try {
            // Try the valueOf function
            return valueOf(version);
            
        }
        
        // If nothing matches, find the closest compatible version.
        catch (IllegalArgumentException e) {
            int myMajor = Integer.parseInt(versionMatcher.group(1));
            int myMinor = Integer.parseInt(versionMatcher.group(2));
            int myPatch = Integer.parseInt(versionMatcher.group(3));
            
            GuacamoleProtocolVersion myVersion = VERSION_1_0_0;
            
            // Loop through possible versions, grabbing the latest compatible
            for (GuacamoleProtocolVersion v : values()) {
                if (v.isCompatible(myMajor, myMinor, myPatch))
                    myVersion = v;
            }
            
            return myVersion;

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
