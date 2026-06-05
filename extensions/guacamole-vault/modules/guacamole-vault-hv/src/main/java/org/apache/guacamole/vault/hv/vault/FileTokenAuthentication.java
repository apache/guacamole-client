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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import org.springframework.vault.VaultException;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.support.VaultToken;

/**
 * A spring-vault ClientAuthentication class for VaultAgent sink 
 * files
 */
public final class FileTokenAuthentication implements ClientAuthentication {

    /**
     * The path to the file containing the token
     */
    private final Path tokenPath;

    /**
     * An instantiator for a Token Authentication class where the token
     * is reread from a file on renewal requests. This allows integration
     * with a VaultAgent for complex authentication methods
     *
     * @param String tokenPath
     *     A path to a readable file containing the token
     */
    public FileTokenAuthentication(final String tokenPath) {
        this.tokenPath = Path.of(tokenPath);
    }

    /*
     * Returns the current token
     *
     * @return VaultToken
     *      The current vault token
     */
    @Override
    public VaultToken login() {
        if (Files.isSymbolicLink(tokenPath)) {
            throw new VaultException("Refusing to use symbolic link for token sink file: " + tokenPath);
        }

        if (!Files.isRegularFile(tokenPath)) {
            throw new VaultException("Token sink path must be a regular file: " + tokenPath);
        }

        try {
            if (! Files.getPosixFilePermissions(tokenPath).contains(PosixFilePermission.OTHERS_READ)) {
                return VaultToken.of(Files.readString(tokenPath).trim());
            }
        } catch (IOException e) {
            throw new VaultException("Cannot read or inspect Vault token sink: " + tokenPath, e);
        }
        throw new VaultException("Refusing to use a world readable token sink file: " + tokenPath);
    }
}
