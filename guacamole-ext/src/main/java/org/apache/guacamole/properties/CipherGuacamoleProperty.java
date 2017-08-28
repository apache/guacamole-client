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

package org.apache.guacamole.properties;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;

/**
 * A GuacamoleProperty whose value is derived from a private key file.
 */
public abstract class CipherGuacamoleProperty implements GuacamoleProperty<Cipher>  {

    @Override
    public Cipher parseValue(String value) throws GuacamoleException {

        try {

            final Environment environment = new LocalEnvironment();

            // Open and read the file specified in the configuration.
            File keyFile = new File(environment.getGuacamoleHome(), value);
            InputStream keyInput = new BufferedInputStream(new FileInputStream(keyFile));
            final byte[] keyBytes = new byte[(int) keyFile.length()];
            keyInput.read(keyBytes);
            keyInput.close();

            // Set up decryption infrastructure
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            KeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            final PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            final Cipher cipher = Cipher.getInstance(privateKey.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            return cipher;

        }
        catch (FileNotFoundException e) {
            throw new GuacamoleException("Could not find the specified key file.", e);
        }
        catch (IOException e) {
            throw new GuacamoleException("Could not read in the specified key file.", e);
        }
        catch (NoSuchAlgorithmException e) {
            throw new GuacamoleException("Specified algorithm does not exist.", e);
        }
        catch (InvalidKeyException e) {
            throw new GuacamoleException("Specified key is invalid.", e);
        }
        catch (InvalidKeySpecException e) {
            throw new GuacamoleException("Invalid KeySpec initialization.", e);
        }
        catch (NoSuchPaddingException e) {
            throw new GuacamoleException("No such padding exception.", e);
        }

    }

}
