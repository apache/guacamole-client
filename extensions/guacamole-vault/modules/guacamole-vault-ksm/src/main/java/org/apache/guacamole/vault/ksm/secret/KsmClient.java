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
import com.keepersecurity.secretsManager.core.Hosts;
import com.keepersecurity.secretsManager.core.KeeperFile;
import com.keepersecurity.secretsManager.core.KeeperRecord;
import com.keepersecurity.secretsManager.core.KeeperSecrets;
import com.keepersecurity.secretsManager.core.Login;
import com.keepersecurity.secretsManager.core.Notation;
import com.keepersecurity.secretsManager.core.SecretsManager;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.vault.ksm.conf.KsmConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(KsmClient.class);

    /**
     * Service for retrieving configuration information.
     */
    @Inject
    private KsmConfigurationService confService;

    /**
     * Service for retrieving data from records.
     */
    @Inject
    private KsmRecordService recordService;

    /**
     * The publicly-accessible URL for Keeper's documentation covering Keeper
     * notation.
     */
    private static final String KEEPER_NOTATION_DOC_URL =
            "https://docs.keeper.io/secrets-manager/secrets-manager/about/keeper-notation";

    /**
     * The regular expression that Keeper notation must match to be related to
     * file retrieval. As the Keeper SDK provides mutually-exclusive for
     * retrieving secret values and files via notation, the notation must first
     * be tested to determine whether it refers to a file.
     */
    private static final Pattern KEEPER_FILE_NOTATION = Pattern.compile("^(keeper://)?[^/]*/file/.+");

    /**
     * The maximum amount of time that an entry will be stored in the cache
     * before being refreshed, in milliseconds.
     */
    private static final long CACHE_INTERVAL = 5000;

    /**
     * Read/write lock which guards access to all cached data, including the
     * timestamp recording the last time the cache was refreshed. Readers of
     * the cache must first acquire (and eventually release) the read lock, and
     * writers of the cache must first acquire (and eventually release) the
     * write lock.
     */
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();

    /**
     * The timestamp that the cache was last refreshed, in milliseconds, as
     * returned by System.currentTimeMillis(). This value is automatically
     * updated if {@link #validateCache()} refreshes the cache. This value must
     * not be accessed without {@link #cacheLock} acquired appropriately.
     */
    private volatile long cacheTimestamp = 0;

    /**
     * The full cached set of secrets last retrieved from Keeper Secrets
     * Manager. This value is automatically updated if {@link #validateCache()}
     * refreshes the cache. This value must not be accessed without
     * {@link #cacheLock} acquired appropriately.
     */
    private KeeperSecrets cachedSecrets = null;
    
    /**
     * All records retrieved from Keeper Secrets Manager, where each key is the
     * UID of the corresponding record. The contents of this Map are
     * automatically updated if {@link #validateCache()} refreshes the cache.
     * This Map must not be accessed without {@link #cacheLock} acquired
     * appropriately.
     */
    private final Map<String, KeeperRecord> cachedRecordsByUid = new HashMap<>();

    /**
     * All records retrieved from Keeper Secrets Manager, where each key is the
     * hostname or IP address of the corresponding record. The hostname or IP
     * address of a record is determined by {@link Hosts} fields, thus a record
     * may be associated with multiple hosts. If a record is associated with
     * multiple hosts, there will be multiple references to that record within
     * this Map. The contents of this Map are automatically updated if
     * {@link #validateCache()} refreshes the cache. This Map must not be
     * accessed without {@link #cacheLock} acquired appropriately. Before using
     * a value from this Map, {@link #cachedAmbiguousHosts} must first be
     * checked to verify that there is indeed only one record associated with
     * that host.
     */
    private final Map<String, KeeperRecord> cachedRecordsByHost = new HashMap<>();

    /**
     * The set of all hostnames or IP addresses that are associated with
     * multiple records, and thus cannot uniquely identify a record. The
     * contents of this Set are automatically updated if
     * {@link #validateCache()} refreshes the cache. This Set must not be
     * accessed without {@link #cacheLock} acquired appropriately.This Set
     * must be checked before using a value retrieved from
     * {@link #cachedRecordsByHost}.
     */
    private final Set<String> cachedAmbiguousHosts = new HashSet<>();

    /**
     * All records retrieved from Keeper Secrets Manager, where each key is the
     * username of the corresponding record. The username of a record is
     * determined by {@link Login} fields, thus a record may be associated with
     * multiple users. If a record is associated with multiple users, there
     * will be multiple references to that record within this Map. The contents
     * of this Map are automatically updated if {@link #validateCache()}
     * refreshes the cache. This Map must not be accessed without
     * {@link #cacheLock} acquired appropriately. Before using a value from
     * this Map, {@link #cachedAmbiguousUsernames} must first be checked to
     * verify that there is indeed only one record associated with that user.
     */
    private final Map<String, KeeperRecord> cachedRecordsByUsername = new HashMap<>();

    /**
     * The set of all usernames that are associated with multiple records, and
     * thus cannot uniquely identify a record. The contents of this Set are
     * automatically updated if {@link #validateCache()} refreshes the cache.
     * This Set must not be accessed without {@link #cacheLock} acquired
     * appropriately.This Set must be checked before using a value retrieved
     * from {@link #cachedRecordsByUsername}.
     */
    private final Set<String> cachedAmbiguousUsernames = new HashSet<>();

    /**
     * Validates that all cached data is current with respect to
     * {@link #CACHE_INTERVAL}, refreshing data from the server as needed.
     *
     * @throws GuacamoleException
     *     If an error occurs preventing the cached data from being refreshed.
     */
    private void validateCache() throws GuacamoleException {

        long currentTime = System.currentTimeMillis();

        // Perform a read-only check that the cache has actually expired before
        // continuing
        cacheLock.readLock().lock();
        try {
            if (currentTime - cacheTimestamp < CACHE_INTERVAL)
                return;
        }
        finally {
            cacheLock.readLock().unlock();
        }

        cacheLock.writeLock().lock();
        try {

            // Cache may have been updated since the read-only check. Re-verify
            // that the cache has expired before continuing with a full refresh
            if (currentTime - cacheTimestamp < CACHE_INTERVAL)
                return;

            // Attempt to pull all records first, allowing that operation to
            // succeed/fail BEFORE we clear out the last cached success
            KeeperSecrets secrets = SecretsManager.getSecrets(confService.getSecretsManagerOptions());
            List<KeeperRecord> records = secrets.getRecords();

            // Store all secrets within cache
            cachedSecrets = secrets;
            
            // Clear unambiguous cache of all records by UID
            cachedRecordsByUid.clear();

            // Clear cache of host-based records
            cachedAmbiguousHosts.clear();
            cachedRecordsByHost.clear();

            // Clear cache of login-based records
            cachedAmbiguousUsernames.clear();
            cachedRecordsByUsername.clear();

            // Store all records, sorting each into host-based and login-based
            // buckets
            records.forEach(record -> {

                // Store based on UID ...
                cachedRecordsByUid.put(record.getRecordUid(), record);

                // ... and hostname/address
                String hostname = recordService.getHostname(record);
                addRecordForHost(record, hostname);

                // Store based on username ONLY if no hostname (will otherwise
                // result in ambiguous entries for servers tied to identical
                // accounts)
                if (hostname == null)
                    addRecordForLogin(record, recordService.getUsername(record));

            });

            // Cache has been refreshed
            this.cacheTimestamp = System.currentTimeMillis();

        }
        finally {
            cacheLock.writeLock().unlock();
        }

    }

    /**
     * Associates the given record with the given hostname. The hostname may be
     * null. Both {@link #cachedRecordsByHost} and {@link #cachedAmbiguousHosts}
     * are updated appropriately. The write lock of {@link #cacheLock} must
     * already be acquired before invoking this function.
     *
     * @param record
     *     The record to associate with the hosts in the given field.
     *
     * @param hostname
     *     The hostname/address that the given record should be associated
     *     with. This may be null.
     */
    private void addRecordForHost(KeeperRecord record, String hostname) {

        if (hostname == null)
            return;

        KeeperRecord existing = cachedRecordsByHost.putIfAbsent(hostname, record);
        if (existing != null && record != existing)
            cachedAmbiguousHosts.add(hostname);

    }

    /**
     * Associates the given record with the given username. The given username
     * may be null. Both {@link #cachedRecordsByUsername} and
     * {@link #cachedAmbiguousUsernames} are updated appropriately. The write
     * lock of {@link #cacheLock} must already be acquired before invoking this
     * function.
     *
     * @param record
     *     The record to associate with the given username.
     *
     * @param username
     *     The username that the given record should be associated with. This
     *     may be null.
     */
    private void addRecordForLogin(KeeperRecord record, String username) {

        if (username == null)
            return;

        KeeperRecord existing = cachedRecordsByUsername.putIfAbsent(username, record);
        if (existing != null && record != existing)
            cachedAmbiguousUsernames.add(username);

    }

    /**
     * Returns all records accessible via Keeper Secrets Manager. The records
     * returned are arbitrarily ordered.
     *
     * @return
     *     An unmodifiable Collection of all records accessible via Keeper
     *     Secrets Manager,  in no particular order.
     *
     * @throws GuacamoleException
     *     If an error occurs that prevents records from being retrieved.
     */
    public Collection<KeeperRecord> getRecords() throws GuacamoleException {
        validateCache();
        cacheLock.readLock().lock();
        try {
            return Collections.unmodifiableCollection(cachedRecordsByUid.values());
        }
        finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Returns the record having the given UID. If no such record exists, null
     * is returned.
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
    public KeeperRecord getRecord(String uid) throws GuacamoleException {
        validateCache();
        cacheLock.readLock().lock();
        try {
            return cachedRecordsByUid.get(uid);
        }
        finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Returns the record associated with the given hostname or IP address. If
     * no such record exists, or there are multiple such records, null is
     * returned.
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
    public KeeperRecord getRecordByHost(String hostname) throws GuacamoleException {
        validateCache();
        cacheLock.readLock().lock();
        try {

            if (cachedAmbiguousHosts.contains(hostname)) {
                logger.debug("The hostname/address \"{}\" is referenced by "
                        + "multiple Keeper records and cannot be used to "
                        + "locate individual secrets.", hostname);
                return null;
            }

            return cachedRecordsByHost.get(hostname);

        }
        finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Returns the record associated with the given username. If no such record
     * exists, or there are multiple such records, null is returned.
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
    public KeeperRecord getRecordByLogin(String username) throws GuacamoleException {
        validateCache();
        cacheLock.readLock().lock();
        try {

            if (cachedAmbiguousUsernames.contains(username)) {
                logger.debug("The username \"{}\" is referenced by multiple "
                        + "Keeper records and cannot be used to locate "
                        + "individual secrets.", username);
                return null;
            }

            return cachedRecordsByUsername.get(username);

        }
        finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Returns the value of the secret stored within Keeper Secrets Manager and
     * represented by the given Keeper notation. Keeper notation locates the
     * value of a specific field, custom field, or file associated with a
     * specific record. See: https://docs.keeper.io/secrets-manager/secrets-manager/about/keeper-notation
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
    public Future<String> getSecret(String notation) throws GuacamoleException {
        validateCache();
        cacheLock.readLock().lock();
        try {

            // Retrieve any relevant file asynchronously
            Matcher fileNotationMatcher = KEEPER_FILE_NOTATION.matcher(notation);
            if (fileNotationMatcher.matches())
                return recordService.download(Notation.getFile(cachedSecrets, notation));

            // Retrieve string values synchronously
            return CompletableFuture.completedFuture(Notation.getValue(cachedSecrets, notation));

        }

        // Unfortunately, the notation parser within the Keeper SDK throws
        // plain Errors for retrieval failures ...
        catch (Error e) {
            logger.warn("Record \"{}\" does not exist.", notation);
            logger.debug("Retrieval of record by Keeper notation failed.", e);
            return CompletableFuture.completedFuture(null);
        }

        // ... and plain Exceptions for parse failures (no subclasses)
        catch (Exception e) {
            logger.warn("\"{}\" is not valid Keeper notation. Please check "
                    + "the documentation at {} for valid formatting.",
                    notation, KEEPER_NOTATION_DOC_URL);
            logger.debug("Provided Keeper notation could not be parsed.", e);
            return CompletableFuture.completedFuture(null);
        }
        finally {
            cacheLock.readLock().unlock();
        }

    }

}
