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

package org.apache.guacamole.auth.saml;

import com.google.inject.Singleton;
import com.onelogin.saml2.authn.SamlResponse;
import com.onelogin.saml2.exception.ValidationError;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A class that handles mapping of hashes to SAMLResponse objects.
 */
@Singleton
public class SAMLResponseMap {
    
    /**
     * The internal data structure that holds a map of SHA-256 hashes to
     * SAML responses.
     */
    private final ConcurrentMap<String, SamlResponse> samlResponseMap =
        new ConcurrentHashMap<>();
    
    /**
     * Executor service which runs the periodic cleanup task
     */
    private final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(1);

    /**
     * Create a new instance of this response map and kick off the executor
     * that schedules the response cleanup task to run every five minutes.
     */
    public SAMLResponseMap() {
        // Cleanup unclaimed responses every five minutes
        executor.scheduleAtFixedRate(new SAMLResponseCleanupTask(), 5, 5, TimeUnit.MINUTES);
    }
    
    /**
     * Retrieve the SamlResponse from the map that is represented by the
     * provided hash, or null if no such object exists.
     * 
     * @param hash
     *     The SHA-256 hash of the SamlResponse.
     * 
     * @return 
     *     The SamlResponse object matching the hash provided.
     */
    protected SamlResponse getSamlResponse(String hash) {
        return samlResponseMap.remove(hash);
    }
    
    /**
     * Place the provided mapping of hash to SamlResponse into the map.
     * 
     * @param hash
     *     The hash that will be the lookup key for this SamlResponse.
     * 
     * @param samlResponse 
     *     The SamlResponse object.
     */
    protected void putSamlResponse(String hash, SamlResponse samlResponse) {
        samlResponseMap.put(hash, samlResponse);
    }
    
    /**
     * Return true if the provided hash key exists in the map, otherwise false.
     * 
     * @param hash
     *     The hash key to look for in the map.
     * 
     * @return 
     *     true if the provided hash is present, otherwise false.
     */
    protected boolean hasSamlResponse(String hash) {
        return samlResponseMap.containsKey(hash);
    }
    
    /**
     * Task which runs every five minutes and cleans up any expired SAML
     * responses that haven't been claimed and removed from the map.
     */
    private class SAMLResponseCleanupTask implements Runnable {
        
        @Override
        public void run() {

            // Loop through responses in map and remove ones that are no longer valid.
            Iterator<SamlResponse> responseIterator = samlResponseMap.values().iterator();
            while (responseIterator.hasNext()) {
                try {
                    responseIterator.next().validateTimestamps();
                }
                catch (ValidationError e) {
                    responseIterator.remove();
                }
            }

        }
    
    }
    
    /**
     * Shut down the executor service that periodically cleans out the
     * SamlResponse Map.  This must be invoked during webapp shutdown in order
     * to avoid resource leaks.
     */
    public void shutdown() {
        executor.shutdownNow();
    }
    
}
