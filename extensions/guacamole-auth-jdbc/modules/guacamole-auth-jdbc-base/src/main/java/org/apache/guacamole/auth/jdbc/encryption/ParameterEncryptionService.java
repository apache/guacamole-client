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

import java.security.GeneralSecurityException;
import java.util.Collection;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.jdbc.connection.ConnectionParameterModel;
import org.apache.guacamole.auth.jdbc.sharingprofile.SharingProfileParameterModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encrypts secret parameter values before they are stored and decrypts them
 * after they are read. Never logs or reports values or key material.
 */
public class ParameterEncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(ParameterEncryptionService.class);

    private final ParameterKeyProvider keys;
    private final SecretParameterPolicy policy;
    private final boolean decryptMode;

    public ParameterEncryptionService(ParameterKeyProvider keys,
            SecretParameterPolicy policy, boolean decryptMode) {
        this.keys = keys;
        this.policy = policy;
        this.decryptMode = decryptMode;
    }

    public boolean isEnabled() { return keys.isEnabled(); }
    public boolean isDecryptMode() { return decryptMode; }
    public String currentKeyId() { return keys.currentKeyId(); }
    public SecretParameterPolicy getPolicy() { return policy; }

    private boolean shouldEncrypt(String name, String value) {
        return keys.isEnabled() && !decryptMode && value != null
                && !value.isEmpty() && policy.isSecret(name);
    }

    /** Encrypts with the current key. Valid only when enabled. */
    public String encryptValue(ParameterOwner owner, String ownerId,
            String name, String plaintext) throws GuacamoleClientException {
        try {
            return ParameterCipher.encrypt(keys.keyFor(keys.currentKeyId()),
                    owner, ownerId, name, plaintext);
        }
        catch (IllegalArgumentException e) {
            throw new GuacamoleClientException("Parameter \"" + name + "\" is "
                    + "too long to be stored encrypted (limit "
                    + ParameterCipher.MAX_PLAINTEXT_BYTES + " bytes).");
        }
    }

    public void protectConnectionParameters(
            Collection<ConnectionParameterModel> models) throws GuacamoleException {
        for (ConnectionParameterModel model : models) {
            model.setEncryption(null);
            if (shouldEncrypt(model.getName(), model.getValue())) {
                model.setValue(encryptValue(ParameterOwner.CONNECTION,
                        model.getConnectionIdentifier(), model.getName(), model.getValue()));
                model.setEncryption(keys.currentKeyId());
            }
        }
    }

    public void protectSharingProfileParameters(
            Collection<SharingProfileParameterModel> models) throws GuacamoleException {
        for (SharingProfileParameterModel model : models) {
            model.setEncryption(null);
            if (shouldEncrypt(model.getName(), model.getValue())) {
                model.setValue(encryptValue(ParameterOwner.SHARING_PROFILE,
                        model.getSharingProfileIdentifier(), model.getName(), model.getValue()));
                model.setEncryption(keys.currentKeyId());
            }
        }
    }

    /**
     * Returns the plain value of a stored parameter.
     *
     * @throws GuacamoleServerException
     *     If the value is encrypted and cannot be decrypted.
     */
    public String reveal(ParameterOwner owner, String ownerId, String name,
            String value, String keyId) throws GuacamoleServerException {

        if (keyId == null)
            return value;

        String failure = "Cannot decrypt parameter \"" + name + "\" of "
                + owner.getType() + " " + ownerId + " (key \"" + keyId + "\").";

        byte[] key = keys.keyFor(keyId);
        if (key == null)
            throw new GuacamoleServerException(failure + " The key is not configured.");

        try {
            return ParameterCipher.decrypt(key, owner, ownerId, name, value);
        }
        catch (GeneralSecurityException e) {
            throw new GuacamoleServerException(failure + " The value does not "
                    + "match the key, owner or parameter name.");
        }

    }

    /**
     * Like reveal(), but returns null and logs an ERROR on failure. For read
     * paths that cannot throw.
     */
    public String revealOrOmit(ParameterOwner owner, String ownerId,
            String name, String value, String keyId) {
        try {
            return reveal(owner, ownerId, name, value, keyId);
        }
        catch (GuacamoleServerException e) {
            logger.error("{} The parameter is omitted; saving this {} now "
                    + "would store it without that value.", e.getMessage(), owner.getType());
            return null;
        }
    }

}
