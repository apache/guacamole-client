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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.keepersecurity.secretsManager.core.KeeperRecord;
import com.keepersecurity.secretsManager.core.SecretsManagerOptions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.vault.ksm.conf.KsmConfigurationService;

/**
 * Client which retrieves records from Keeper Secrets Manager, allowing
 * content-based record retrieval. Note that because KSM is zero-knowledge,
 * searching or indexing based on content can only be accomplished by
 * retrieving and indexing everything. Except for record UIDs (which contain no
 * information), it's not possible for the server to perform a search of
 * content on the client's behalf. The client has to perform its own search.
 */
@Singleton
public class KsmClient {

    /**
     * Service for retrieving configuration information.
     */
    @Inject
    private KsmConfigurationService confService;

    /**
     * Factory for creating KSM cache instances for particular KSM configs.
     */
    @Inject
    private KsmCacheFactory ksmCacheFactory;

    /**
     * A map of base-64 encoded JSON KSM config blobs to associated KSM cache instances.
     * The `null` entry in this Map is associated with the KSM configuration parsed
     * from the guacamole.properties config file.
     */
    private final Map<String, KsmCache> ksmCacheMap = new HashMap<>();

    /**
     * Create and return a KSM cache for the provided KSM config if not already
     * present in the cache map, the existing cache entry.
     *
     * @param ksmConfig
     *     The base-64 encoded JSON KSM config blob associated with the cache entry.
     *     If an associated entry does not already exist, it will be created using
     *     this configuration.
     *
     * @return
     *     A KSM cache for the provided KSM config if not already present in the
     *     cache map, otherwise the existing cache entry.
     *
     * @throws GuacamoleException
     *     If an error occurs while creating the KSM cache.
     */
    private KsmCache createCacheIfNeeded(@Nullable String ksmConfig)
            throws GuacamoleException {

        // If a cache already exists for the provided config, use it
        KsmCache ksmCache = ksmCacheMap.get(ksmConfig);
        if (ksmCache != null)
            return ksmCache;

        // Create and store a new KSM cache instance for the provided KSM config blob
        SecretsManagerOptions options = confService.getSecretsManagerOptions(ksmConfig);
        return ksmCacheMap.put(ksmConfig, ksmCacheFactory.create(options));
    }

    /**
     * Returns all records accessible via Keeper Secrets Manager, associated
     * with the provided KSM config. If no KSM config is provided, records
     * associated with the default configuration as retrieved from the config
     * file will be used. The records returned are arbitrarily ordered.
     *
     * @param ksmConfig
     *     The base-64 encoded JSON KSM config blob associated associated with
     *     the KSM vault that should be used to fetch the records.
     *
     * @return
     *     An unmodifiable Collection of all records accessible via Keeper
     *     Secrets Manager, in no particular order.
     *
     * @throws GuacamoleException
     *     If an error occurs that prevents records from being retrieved.
     */
    public Collection<KeeperRecord> getRecords(
                @Nullable String ksmConfig) throws GuacamoleException {

        // Call through to the associated KSM cache instance
        return createCacheIfNeeded(ksmConfig).getRecords();
    }

    /**
     * Returns the record having the given KSM config, and the given UID.
     * If no such record exists, null is returned.
     *
     * @param ksmConfig
     *     The base-64 encoded JSON KSM config blob associated associated with
     *     the KSM vault that should be used to fetch the record.
     *
     * @param uid
     *     The UID of the record to return.
     *
     * @return
     *     The record having the given UID, or null if there is no such record.
     *
     * @throws GuacamoleException
     *     If an error occurs that prevents the record from being retrieved.
     */
    public KeeperRecord getRecord(
                @Nullable String ksmConfig, String uid) throws GuacamoleException {

        // Call through to the associated KSM cache instance
        return createCacheIfNeeded(ksmConfig).getRecord(uid);
    }

    /**
     * Returns the record associated with the given hostname or IP address
     * and the given KSM config. If no such record exists, or there are multiple
     * such records, null is returned.
     *
     * @param ksmConfig
     *     The base-64 encoded JSON KSM config blob associated associated with
     *     the KSM vault that should be used to fetch the record.
     *
     * @param hostname
     *     The hostname of the record to return.
     *
     * @return
     *     The record associated with the given hostname, or null if there is
     *     no such record or multiple such records.
     *
     * @throws GuacamoleException
     *     If an error occurs that prevents the record from being retrieved.
     */
    public KeeperRecord getRecordByHost(
                @Nullable String ksmConfig, String hostname) throws GuacamoleException {

        // Call through to the associated KSM cache instance
        return createCacheIfNeeded(ksmConfig).getRecordByHost(hostname);

    }

    /**
     * Returns the record associated with the given username. If no such record
     * exists, or there are multiple such records, null is returned.
     *
     * @param ksmConfig
     *     The base-64 encoded JSON KSM config blob associated associated with
     *     the KSM vault that should be used to fetch the record.
     *
     * @param username
     *     The username of the record to return.
     *
     * @return
     *     The record associated with the given username, or null if there is
     *     no such record or multiple such records.
     *
     * @throws GuacamoleException
     *     If an error occurs that prevents the record from being retrieved.
     */
    public KeeperRecord getRecordByLogin(
                @Nullable String ksmConfig, String username) throws GuacamoleException {

        // Call through to the associated KSM cache instance
        return createCacheIfNeeded(ksmConfig).getRecordByLogin(username);

    }

    /**
     * Returns the value of the secret stored within the Keeper Secrets Manager vault
     * associated with the provided KSM config, and represented by the given Keeper
     * notation. Keeper notation locates the value of a specific field, custom field,
     * or file associated with a specific record.
     * See: https://docs.keeper.io/secrets-manager/secrets-manager/about/keeper-notation
     *
     * @param ksmConfig
     *     The base-64 encoded JSON KSM config blob associated associated with
     *     the KSM vault that should be used to fetch the record.
     *
     * @param notation
     *     The Keeper notation of the secret to retrieve.
     *
     * @return
     *     A Future which completes with the value of the secret represented by
     *     the given Keeper notation, or null if there is no such secret.
     *
     * @throws GuacamoleException
     *     If the requested secret cannot be retrieved or the Keeper notation
     *     is invalid.
     */
    public Future<String> getSecret(
        @Nullable String ksmConfig, String notation) throws GuacamoleException {

        // Call through to the associated KSM cache instance
        return createCacheIfNeeded(ksmConfig).getSecret(notation);

    }

}
