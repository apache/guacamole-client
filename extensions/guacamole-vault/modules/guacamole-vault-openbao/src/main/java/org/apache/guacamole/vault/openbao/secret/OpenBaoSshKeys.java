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




import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.config.keys.writer.openssh.OpenSSHKeyPairResourceWriter;
import org.apache.sshd.common.util.security.SecurityUtils;


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
     * Class instantiation to return generated SSH keys
     */
    public OpenBaoSshKeys(String type) {
        KeyPair keyPair;

        if ("rsa".equals(type)) {
            keyPair = generateRsa();
        }
        else {
            keyPair = generateEd25519WithFallback();
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
            throw new IllegalStateException("Failed to serialize SSH keypair", e);
        }
    }

    /**
     * Class instantiation to return generated SSH keys. Default type
     */
    public OpenBaoSshKeys() {
        this(OpenBaoConfigurationService.DEFAULT_SSH_TYPE);
    }

    /**
     * Generate a ed25519 key-pair
     *
     * @return
     *      A java.security.KeyPair containing the ed25519 key pair or RSA if failure
     */
    private KeyPair generateEd25519WithFallback() {
        try {
            // Use SSHD's EdDSA generator, not JDK "Ed25519"
            KeyPairGenerator keyPairGenerator =
                    SecurityUtils.getKeyPairGenerator("EdDSA");
            keyPairGenerator.initialize(256); // Ed25519
            return keyPairGenerator.generateKeyPair();
        }
        catch (Exception e) {
            logger.info("Ed25519 not available via SSHD EdDSA. Falling back to RSA : " + e.getMessage());
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
