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

package org.apache.guacamole.vault.openbao.secret;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import org.apache.guacamole.vault.openbao.conf.OpenBaoConfigurationService;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenBaoSshKeys {
    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(OpenBaoSshKeys.class);

    /**
     * The PEM encoded private SSH key
     */
    public final String privateSshPem;

    /**
     * The OpenSSH encoded public SSH key
     */
    public final String publicSsh;

    /**
     * Class instantiation to return generated SSH keys. Default type
     */
    public OpenBaoSshKeys() {
        this(OpenBaoConfigurationService.DEFAULT_SSH_TYPE);
    }

    /**
     * Class instantiation to return generated SSH keys
     *
     * @param type
     *      The type of ssh key to generate. Can be "rsa" or "ed25519" only.
     *      Generated RSA keys are 4096 bit only
     */
    public OpenBaoSshKeys(String type) {
        KeyPair keyPair;

        if ("rsa".equals(type)) {
            keyPair = generateRsa();
        }
        else if ("ed25519".equals(type)) {
            keyPair = generateEd25519WithFallback();
        }
        else {
            throw new IllegalArgumentException("Unrecognized SSH encryption : "+ type);
        }

        try {
            this.publicSsh = PublicKeyEntry.toString(keyPair.getPublic());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OpenSSHKeyPairResourceWriter writer =
                    new OpenSSHKeyPairResourceWriter();
            writer.writePrivateKey(keyPair, null, null, baos);
            this.privateSshPem =
                    baos.toString(StandardCharsets.UTF_8);
        }
        catch (Exception e) {
            throw new IllegalStateException("Failed to serialize SSH keypair: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a ed25519 key-pair
     *
     * @return
     *      A java.security.KeyPair containing the ed25519 key pair or RSA if failure
     */
    private KeyPair generateEd25519WithFallback() {
        try {
            KeyPairGenerator keyPairGenerator =
                    SecurityUtils.getKeyPairGenerator("EdDSA");
            return keyPairGenerator.generateKeyPair();
        }
        catch (Exception e) {
            logger.warn("Ed25519 not available via SSHD EdDSA. Falling back to RSA : {}", e.getMessage());
            return generateRsa();
        }
    }

    /**
     * Generate a 4096-bit RSA key-pair
     *
     * @return
     *      A java.security.KeyPair containing the RSA key pair
     */
    private KeyPair generateRsa() {
        try {
            KeyPairGenerator keyPairGenerator =
                    SecurityUtils.getKeyPairGenerator("RSA");
            keyPairGenerator.initialize(4096);
            return keyPairGenerator.generateKeyPair();
        }
        catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA SSH keypair", e);
        }
    }
}
