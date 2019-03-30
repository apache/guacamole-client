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

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleUnsupportedException;

/**
 * An enum that defines the available Guacamole protocol versions that can be
 * used between guacd and clients, and provides convenience methods for parsing
 * and comparing versions.
 */
public enum GuacamoleProtocolVersion {
    
    VERSION_1_0_0("1.0.0", 1, 0, 0),

    VERSION_1_1_0("1.1.0", 1, 1, 0);

    // The string representation of the version.
    private final String version;
    
    // The major version number.
    private final int major;

    // The minor version number.
    private final int minor;

    // The patch version number.
    private final int patch;
    
    /**
     * Generate a new GuacamoleProtocolVersion object with the given
     * string version, major version, and minor version.
     * 
     * @param version
     *     The String representation of the version.
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
    GuacamoleProtocolVersion(String version, int major, int minor, int patch) {
        this.version = version;
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }
    
    /**
     * Return the string representation of the version.
     * 
     * @return 
     *     The string representation of the version.
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Return the major version number.
     * 
     * @return 
     *     The integer major version.
     */
    public int getMajor() {
        return major;
    }
    
    /**
     * Return the minor version number.
     * 
     * @return 
     *     The integer minor version.
     */
    public int getMinor() {
        return minor;
    }
    
    /**
     * Return the patch version number.
     * @return 
     *     The integer patch version.
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
        
        // Major version is greater
        if (major > otherVersion.getMajor())
            return true;
        
        // Major version is less than or equal to.
        else {
            
            // Major version is less than
            if (major < otherVersion.getMajor())
                return false;
            
            // Major version is equal, minor version is greater
            if (minor > otherVersion.getMinor())
                return true;
            
            // Minor version is less than or equal to.
            else {
                
                // Minor version is less than
                if (minor < otherVersion.getMinor())
                    return false;
                
                // Patch version is greater or equal
                if (patch >= otherVersion.getPatch())
                    return true;
            }
        }
        
        // Version is either less than or equal to.
        return false;
        
    }
    
    /**
     * Parses the given string version, returning the GuacamoleProtocolVersion
     * object that matches the string version.  If a matching version is not
     * found an exception is thrown.
     * 
     * @param version
     *     The String representation of a version to parse.
     * 
     * @return
     *     The GuacamoleProtocolVersion that matches the string version
     *     that has been provided.
     * 
     * @throws GuacamoleException 
     *     If the string version does not match any known enum values.
     */
    public static GuacamoleProtocolVersion parseVersion(String version) 
            throws GuacamoleException {
        
        for (GuacamoleProtocolVersion v : GuacamoleProtocolVersion.values()) {
            if (v.getVersion().equals(version))
                return v;
        }
        
        throw new GuacamoleUnsupportedException("Version " + version
                + " of Guacamole protocol is not valid.");
        
    }
    
    /**
     * Parse the version provided as individual version components to the
     * matching enum version.  If a match is not found an exception is
     * thrown.
     * 
     * @param major
     *     The integer major version.
     * 
     * @param minor
     *     The integer minor version.
     * 
     * @param patch
     *     The integer patch version.
     * 
     * @return
     *     The GuacamoleProtocolVersion that matches the individual components
     *     that were provided.
     * 
     * @throws GuacamoleException 
     *     If the provided components do not match any known enum value.
     */
    public static GuacamoleProtocolVersion parseVersion(int major, int minor, int patch)
            throws GuacamoleException {
        
        for (GuacamoleProtocolVersion v : GuacamoleProtocolVersion.values()) {
            if (v.getMajor() == major 
                    && v.getMinor() == minor
                    && v.getPatch() == patch)
                return v;
        }
        
        throw new GuacamoleUnsupportedException("Version " 
                + Integer.toString(major) + "."
                + Integer.toString(minor) + "."
                + Integer.toString(patch)
                + " is not valid.");
        
    }
    
}
