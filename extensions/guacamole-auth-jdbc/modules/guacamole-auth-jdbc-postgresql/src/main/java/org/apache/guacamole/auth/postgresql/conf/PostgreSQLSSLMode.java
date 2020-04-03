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

package org.apache.guacamole.auth.postgresql.conf;

/**
 * Possible values for PostgreSQL SSL connectivity.
 */
public enum PostgreSQLSSLMode {
    
    // Do not use SSL to connect to server.
    DISABLE("disable"),
    
    // Allow SSL connections, but try non-SSL, first.
    ALLOW("allow"),
    
    // Prefer SSL connections, falling back to non-SSL if that fails.
    PREFER("prefer"),
    
    // Require SSL connections, do not connect if SSL fails.
    REQUIRE("require"),
    
    // Require SSL connections and validate the CA certificate.
    VERIFY_CA("verify-ca"),
    
    // Require SSL connections and validate both the CA and server certificates.
    VERIFY_FULL("verify-full");
    
    // The value actually passed on to the JDBC driver.
    private String configValue;
    
    /**
     * Create a new instance of this enum with the given configValue as the
     * value that will be used when configuring the JDBC driver.
     * 
     * @param configValue
     *     The value to use when configuring the JDBC driver.
     */
    PostgreSQLSSLMode(String configValue) {
        this.configValue = configValue;
    }
    
    @Override
    public String toString() {
        return configValue;
    }
    
    /**
     * Given the String value, determine the correct enum value that matches
     * the string, or null if there is no match.
     * 
     * @param value
     *     The String value to test to find a match.
     * 
     * @return 
     *     The enum value matching the given String.
     */
    public static PostgreSQLSSLMode getValue(String value) {
        for (PostgreSQLSSLMode mode : PostgreSQLSSLMode.values()) {
            if (mode.toString().equals(value))
                return mode;
        }
        return null;
    }
    
}
