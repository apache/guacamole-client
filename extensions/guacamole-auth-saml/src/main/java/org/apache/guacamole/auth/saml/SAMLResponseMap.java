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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    
}
