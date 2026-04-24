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
 
 // This is a minimal backport of this class to version 2.3.4 of spring-vault-core

package org.apache.guacamole.vault.openbao.secret;

import org.springframework.util.Assert;

public final class UsernamePasswordAuthenticationOptions {

    private final String username;
    private final String password;
    private final String mountPath;

    private UsernamePasswordAuthenticationOptions(Builder builder) {
        this.username = builder.username;
        this.password = builder.password;
        this.mountPath = builder.mountPath;
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String username;
        private String password;
        private String mountPath = "userpass";

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        /** Optional – defaults to "userpass" */
        public Builder mountPath(String mountPath) {
            this.mountPath = mountPath;
            return this;
        }

        public UsernamePasswordAuthenticationOptions build() {

            Assert.hasText(username, "Username must not be empty");
            Assert.hasText(password, "Password must not be empty");
            Assert.hasText(mountPath, "Mount path must not be empty");

            return new UsernamePasswordAuthenticationOptions(this);
        }
    }
}
