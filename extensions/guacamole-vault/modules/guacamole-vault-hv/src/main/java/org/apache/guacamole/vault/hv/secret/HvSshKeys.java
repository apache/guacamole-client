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

package org.apache.guacamole.vault.hv.secret;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.GeneralSecurityException;
import org.apache.guacamole.vault.hv.conf.HvConfigurationService;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Class using Apache MINA SSHD to create temporary SSH keys and
 * corresponding public certificates. It supports the creation of
 * RSA-4096 and ed25519 keys only.
 */
public class HvSshKeys {
    /**
     * The value of vault-ssh-type to use for RSA certificate
     */
    public static final String RSA = "rsa";


    /**
     * The value of vault-ssh-type to use for ED25519 certificate
     */
    public static final String ED25519 = "ed25519";

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(HvSshKeys.class);

    /**
     * The OpenSSH encoded private SSH key
     */
    private final String privateSsh;

    /**
     * The OpenSSH encoded public SSH key
     */
    public final String publicSsh;

    /**
     * Class instantiation to return generated SSH keys. Default type
     */
    public HvSshKeys() {
        this(HvConfigurationService.DEFAULT_SSH_TYPE);
    }

    /**
     * Class instantiation to return generated SSH keys
     *
     * @param type
     *      The type of ssh key to generate. Can be "rsa" or "ed25519" only.
     *      Generated RSA keys are 4096 bit only
     */
    public HvSshKeys(final String type) {
        final KeyPair keyPair;

        if (RSA.equals(type)) {
            keyPair = generateRsa();
        }
        else if (ED25519.equals(type)) {
            keyPair = generateEd25519WithFallback();
        }
        else {
            throw new IllegalArgumentException("Unrecognized SSH encryption : "+ type);
        }

        try {
            this.publicSsh = PublicKeyEntry.toString(keyPair.getPublic());
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final OpenSSHKeyPairResourceWriter writer =
                    new OpenSSHKeyPairResourceWriter();
            writer.writePrivateKey(keyPair, null, null, baos);
            this.privateSsh =
                    baos.toString(StandardCharsets.UTF_8);
        }
        catch (IOException | GeneralSecurityException | IllegalStateException e) {
            throw new IllegalStateException("Failed to serialize SSH keypair", e);
        }
    }

    /**
     * Generate a ed25519 key-pair
     *
     * @return
     *      A java.security.KeyPair containing the ed25519 key pair or RSA if failure
     */
    private KeyPair generateEd25519WithFallback() {
        KeyPair keyPair;
        try {
            final KeyPairGenerator keyPairGenerator = SecurityUtils.getKeyPairGenerator("EdDSA");
            keyPair = keyPairGenerator.generateKeyPair();
        }
        catch (GeneralSecurityException e) {
            logger.warn("Ed25519 not available via SSHD EdDSA. Falling back to RSA : {}", e.getMessage());
            keyPair = generateRsa();
        }

        return keyPair;
    }

    /**
     * Generate a 4096-bit RSA key-pair
     *
     * @return
     *      A java.security.KeyPair containing the RSA key pair
     *
     * @throws IllegalStateException
     *      If the RSA key could not be returned
     */
    private KeyPair generateRsa() {
        try {
            final KeyPairGenerator keyPairGenerator =
                    SecurityUtils.getKeyPairGenerator("RSA");
            keyPairGenerator.initialize(4096);
            return keyPairGenerator.generateKeyPair();
        }
        catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to generate RSA SSH keypair", e);
        }
    }

    /**
     * Returns the generated SSH public certificate
     *
     * @return
     *    Return the public certificate in PEM format
     */
     public String getPublic() {
         return publicSsh;
     }

    /**
     * Returns the generated SSH private key
     *
     * @return
     *    Return the private key in OpenSSH's format
     */
     public String getPrivate() {
         return privateSsh;
     }
}
