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
    public FileTokenAuthentication(String tokenPath) {
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
        String token;
        try {
            if (Files.isSymbolicLink(tokenPath)) {
                throw new VaultException("Refusing to use symbolic link for token sink file: " + tokenPath);
            }

            if (!Files.isRegularFile(tokenPath)) {
                throw new VaultException("Token sink path must be a regular file: " + tokenPath);
            }

            // I allow group readable but maybe I shouldn't
            if (Files.isReadable(tokenPath) &&
                    Files.getPosixFilePermissions(tokenPath).contains(PosixFilePermission.OTHERS_READ)) {
                throw new VaultException("Refusing to use a world readable token sink file : " + tokenPath);
            }

            token = Files.readString(tokenPath).trim();
        }
        catch (IOException e) {
            // This might be recoverable. So throw a VautException
            throw new VaultException("Cannot read Vault token sink: " + tokenPath, e);
        }

        return VaultToken.of(token);
    }
}
