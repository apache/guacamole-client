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

package org.apache.guacamole.vault.hv.vault;

import org.springframework.util.Assert;

/**
 * This is a minimal backport of this class to version 2.3.4 of spring-vault-core
 */
public final class UsernamePasswordAuthenticationOptions {
    /**
     * Path of the userpass authentication method mount.
     */
    private final String mountPath;

    /**
     * Username of the userpass authetication method mount.
     */
    private final String username;

    /**
     * Password of the userpass authetication method mount.
     */
    private final String password;


    private UsernamePasswordAuthenticationOptions(final Builder builder) {
        this.username = builder.usern;
        this.password = builder.passw;
        this.mountPath = builder.mount;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getMountPath() {
        return mountPath;
    }

    /**
     * Returns a builder for this class
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A Builder class for UsernamePasswordOptions
     */
    public static final class Builder {

        /** The username */
        private String usern;
        /** The password */
        private String passw;
        /** The mount path for Vault authentication */
        private String mount = "userpass";

        /**
         * Set the username for this instance of UsernamePasswordAuthenticationOptions.
         *
         * @param username
         *     A username to be used. It must be set for the built class to be valid
         *
         * @return
         *     The builder instance
         */
        public Builder username(final String usern) {
            this.usern = usern;
            return this;
        }

        /**
         * Set the password for this instance of UsernamePasswordAuthenticationOptions.
         *
         * @param password
         *     A password to be used. It must be set for the built class to be valid
         *
         * @return
         *     The builder instance
         */
        public Builder password(final String passw) {
            this.passw = passw;
            return this;
        }
         
        /**
         * Set the mountPath for this instance of UsernamePasswordAuthenticationOptions.
         *
         * @param mount
         *     A Vault mount path to use for authentication. Defaults to "userpass" if not
         *     set.
         */
        public Builder mountPath(final String mount) {
            this.mount = mount;
            return this;
        }

        /**
         * Once arguments are finalized, return the corresponding
         * UsernamePasswordAuthenticationOptions.
         *
         * @return
         *      A  UsernamePasswordAuthenticationOptions for use with a
         *      spring-vault ClientAuthentication class.
         */
        public UsernamePasswordAuthenticationOptions build() {

            Assert.hasText(usern, "Username must not be empty");
            Assert.hasText(passw, "Password must not be empty");
            Assert.hasText(mount, "Mount path must not be empty");

            return new UsernamePasswordAuthenticationOptions(this);
        }
    }
}
