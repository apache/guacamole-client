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

package org.apache.guacamole.cluster.guacd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.cluster.ClusterStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chooses which guacd instance a connection should be established against.
 */
public class GuacdSelector {

    private static final Logger logger = LoggerFactory.getLogger(GuacdSelector.class);

    private final ClusterStore store;
    private final GuacdPool pool;
    private final long circuitBreakMs;
    private final Random random = new Random();

    /**
     * Endpoint key to the time at which its circuit break expires.
     */
    private final Map<String, Long> brokenUntil = new ConcurrentHashMap<String, Long>();

    public GuacdSelector(ClusterStore store, GuacdPool pool, long circuitBreakMs) {
        this.store = store;
        this.pool = pool;
        this.circuitBreakMs = circuitBreakMs;
    }

    /**
     * Records that connecting to the given endpoint failed, excluding it from
     * selection until its circuit break expires.
     *
     * @param endpoint
     *     The endpoint which could not be reached.
     */
    public void markFailed(GuacdEndpoint endpoint) {
        brokenUntil.put(endpoint.toKey(), System.currentTimeMillis() + circuitBreakMs);
        logger.info("guacd \"{}\" failed and will be skipped for {} ms.",
                endpoint, circuitBreakMs);
    }

    private boolean isBroken(GuacdEndpoint endpoint) {
        Long until = brokenUntil.get(endpoint.toKey());
        return until != null && System.currentTimeMillis() < until;
    }

    /**
     * Returns the guacd instance already hosting the given connection.
     *
     * @param guacdConnectionId
     *     The connection ID issued by guacd.
     *
     * @return
     *     The guacd instance hosting that connection.
     *
     * @throws GuacamoleException
     *     If no live route exists, or the route cannot be read. This fails
     *     closed deliberately: connecting elsewhere would silently open a NEW
     *     session rather than joining the intended one.
     */
    public GuacdEndpoint selectForJoin(String guacdConnectionId) throws GuacamoleException {

        GuacdEndpoint endpoint = store.lookupRoute(guacdConnectionId);
        if (endpoint == null)
            throw new GuacamoleResourceNotFoundException(
                    "The connection being joined is no longer available.");

        return endpoint;

    }

    /**
     * Returns the least-loaded available guacd instance.
     *
     * @return
     *     The guacd instance which should host a new connection.
     *
     * @throws GuacamoleException
     *     If the pool contains no instances at all.
     */
    public GuacdEndpoint selectForNew() throws GuacamoleException {

        List<GuacdEndpoint> candidates = pool.getCandidates();
        if (candidates.isEmpty())
            throw new GuacamoleServerException("No guacd instances are available.");

        List<GuacdEndpoint> usable = new ArrayList<GuacdEndpoint>(candidates.size());
        for (GuacdEndpoint candidate : candidates) {
            if (!isBroken(candidate))
                usable.add(candidate);
        }

        // Trying a suspect instance beats refusing every connection
        if (usable.isEmpty()) {
            logger.warn("Every guacd instance is currently circuit-broken. "
                    + "Selecting from the full pool regardless.");
            usable = new ArrayList<GuacdEndpoint>(candidates);
        }

        // Shuffle first so that equally loaded instances are chosen evenly
        // rather than always favouring the first in DNS order
        Collections.shuffle(usable, random);

        GuacdEndpoint best = null;
        long bestLoad = Long.MAX_VALUE;

        for (GuacdEndpoint candidate : usable) {
            long load = store.countTunnels(candidate);
            if (load < bestLoad) {
                bestLoad = load;
                best = candidate;
            }
        }

        return best;

    }

}
