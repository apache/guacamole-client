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

package org.apache.guacamole.vault.ksm.secret;

import com.google.inject.Singleton;
import com.keepersecurity.secretsManager.core.KeeperRecord;
import com.keepersecurity.secretsManager.core.KeeperRecordData;
import com.keepersecurity.secretsManager.core.KeyPair;
import com.keepersecurity.secretsManager.core.KeyPairs;
import com.keepersecurity.secretsManager.core.Login;
import com.keepersecurity.secretsManager.core.Password;
import java.util.List;

/**
 * Service for automatically parsing out secrets and data from Keeper records.
 */
@Singleton
public class KsmRecordService {

    /**
     * Returns the single username associated with the given record. If the
     * record has no associated username, or multiple usernames, null is
     * returned. Usernames are retrieved from "Login" fields.
     *
     * @param record
     *     The record to retrieve the username from.
     *
     * @return
     *     The username associated with the given record, or null if the record
     *     has no associated username or multiple usernames.
     */
    public String getUsername(KeeperRecord record) {

        KeeperRecordData data = record.getData();

        Login loginField = (Login) data.getField(Login.class);
        if (loginField == null)
            return null;

        List<String> usernames = loginField.getValue();
        if (usernames.size() != 1)
            return null;

        return usernames.get(0);

    }

    /**
     * Returns the password associated with the given record, as dictated by
     * the {@link KeeperRecord#getPassword()}.
     *
     * @param record
     *     The record to retrieve the password from.
     *
     * @return
     *     The password associated with the given record, or null if the record
     *     has no associated password.
     */
    public String getPassword(KeeperRecord record) {

        KeeperRecordData data = record.getData();

        Password passwordField = (Password) data.getField(Password.class);
        if (passwordField == null)
            return null;

        List<String> values = passwordField.getValue();
        if (values.size() != 1)
            return null;

        return values.get(0);

    }

    /**
     * Returns the private key associated with the given record. If the record
     * has no associated private key, or multiple private keys, null is
     * returned. Private keys are retrieved from "KeyPairs" fields.
     *
     * @param record
     *     The record to retrieve the private key from.
     *
     * @return
     *     The private key associated with the given record, or null if the
     *     record has no associated private key or multiple private keys.
     */
    public String getPrivateKey(KeeperRecord record) {

        KeeperRecordData data = record.getData();

        KeyPairs keyPairsField = (KeyPairs) data.getField(KeyPairs.class);
        if (keyPairsField == null)
            return null;

        List<KeyPair> keyPairs = keyPairsField.getValue();
        if (keyPairs.size() != 1)
            return null;

        return keyPairs.get(0).getPrivateKey();

    }

    /**
     * Returns the passphrase for the private key associated with the given
     * record. Currently, this is simply dictated by {@link KeeperRecord#getPassword()},
     * as there is no specific association between private keys and passphrases
     * in the "KeyPairs" field type.
     *
     * @param record
     *     The record to retrieve the passphrase from.
     *
     * @return
     *     The passphrase for the private key associated with the given record,
     *     or null if there is no such passphrase associated with the record.
     */
    public String getPassphrase(KeeperRecord record) {
        return getPassword(record);
    }

}
