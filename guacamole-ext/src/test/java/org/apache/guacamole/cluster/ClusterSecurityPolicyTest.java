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

package org.apache.guacamole.cluster;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.cluster.metrics.ClusterMetrics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ClusterSecurityPolicy, which decides at startup whether the
 * Redis named by the configured URI may hold cluster state, including session
 * identity. These tests verify that only an authenticated, encrypted URI is
 * accepted by default, that anything weaker is refused unless explicitly opted
 * into, that the logged posture line is accurate and never carries a
 * credential, and that the insecure gauge is published in every case.
 */
public class ClusterSecurityPolicyTest {

    /**
     * Verifies that a "rediss://" URI carrying credentials is accepted, and
     * that the returned posture describes it as both authenticated and
     * encrypted.
     *
     * @throws Exception
     *     If an unexpected error occurs while running the test.
     */
    @Test
    public void anAuthenticatedEncryptedUriIsAccepted() throws Exception {
        String posture = ClusterSecurityPolicy.check(
                "rediss://guacamole:secret@redis:6379", false);
        assertTrue(posture.contains("authenticated"));
        assertTrue(posture.contains("encrypted"));
    }

    /**
     * Verifies that a URI with neither credentials nor TLS is refused when
     * insecure operation was not opted into, and that the refusal names the
     * property which would permit it, so an operator can act on the error.
     */
    @Test
    public void anUnauthenticatedUriIsRefused() {
        GuacamoleException e = assertThrows(GuacamoleException.class,
                () -> ClusterSecurityPolicy.check("redis://redis:6379", false));
        assertTrue(e.getMessage().contains("cluster-allow-insecure-redis"),
                "the refusal must name the property that permits it");
    }

    /**
     * Verifies that authentication alone is not sufficient: a "redis://" URI
     * with credentials but no TLS is still refused by default.
     */
    @Test
    public void anUnencryptedButAuthenticatedUriIsRefused() {

        // Authentication without TLS sends the password in clear text, and the
        // session identity behind it in clear text too
        assertThrows(GuacamoleException.class,
                () -> ClusterSecurityPolicy.check(
                        "redis://guacamole:secret@redis:6379", false));

    }

    /**
     * Verifies that an unauthenticated, unencrypted URI is accepted when
     * insecure operation is explicitly allowed, and that the posture then
     * states plainly that Redis is neither authenticated nor encrypted.
     *
     * @throws Exception
     *     If an unexpected error occurs while running the test.
     */
    @Test
    public void insecureIsAllowedWhenExplicitlyOptedInto() throws Exception {
        String posture = ClusterSecurityPolicy.check("redis://redis:6379", true);
        assertTrue(posture.contains("INSECURE"));
        assertTrue(posture.contains("neither authenticated nor encrypted"));
        assertFalse(posture.contains("secret"));
    }

    /**
     * Verifies that the posture of an authenticated but unencrypted URI reports
     * only the missing encryption, rather than wrongly claiming the connection
     * is unauthenticated.
     *
     * @throws Exception
     *     If an unexpected error occurs while running the test.
     */
    @Test
    public void thePostureNamesTheActualShortcoming() throws Exception {

        // A common arrangement: authenticated, but no TLS. Reporting this as
        // "UNAUTHENTICATED" would tell an operator something false
        String posture = ClusterSecurityPolicy.check(
                "redis://guacamole:secret@redis:6379", true);

        assertTrue(posture.contains("not encrypted"), posture);
        assertFalse(posture.contains("UNAUTHENTICATED"), posture);

    }

    /**
     * Verifies that the password within the URI never appears in the posture,
     * since the posture is written to the log.
     *
     * @throws Exception
     *     If an unexpected error occurs while running the test.
     */
    @Test
    public void thePostureNeverContainsTheCredential() throws Exception {

        String posture = ClusterSecurityPolicy.check(
                "rediss://guacamole:hunter2@redis:6379", false);

        assertFalse(posture.contains("hunter2"),
                "the posture line is logged, so it must be redacted");

    }

    /**
     * Verifies that a URI which cannot be parsed is refused, failing closed
     * rather than assuming an unknown URI is secure.
     */
    @Test
    public void anUnparseableUriIsRefusedRatherThanAssumedSafe() {
        assertThrows(GuacamoleException.class,
                () -> ClusterSecurityPolicy.check("not-a-uri", false));
    }

    /**
     * Verifies that the insecure gauge is published as 0 for an authenticated,
     * encrypted URI and as 1 for an insecure URI that was explicitly allowed.
     *
     * @throws Exception
     *     If an unexpected error occurs while running the test.
     */
    @Test
    public void publishesTheInsecureGauge() throws Exception {

        ClusterMetrics.reset();
        ClusterSecurityPolicy.check("rediss://guacamole:secret@redis:6379", false);
        assertTrue(ClusterMetrics.render().contains(
                "guacamole_cluster_redis_insecure 0"), ClusterMetrics.render());

        ClusterMetrics.reset();
        ClusterSecurityPolicy.check("redis://redis:6379", true);
        assertTrue(ClusterMetrics.render().contains(
                "guacamole_cluster_redis_insecure 1"), ClusterMetrics.render());

    }

    /**
     * Verifies that the insecure gauge is published as 1 even when the URI is
     * refused, since the gauge is set before the decision is made.
     */
    @Test
    public void publishesTheInsecureGaugeEvenWhenRefusing() {

        ClusterMetrics.reset();

        assertThrows(GuacamoleException.class,
                () -> ClusterSecurityPolicy.check("redis://redis:6379", false));

        // A refused start lives only seconds, but reporting 0 for it would be
        // a lie in whichever scrape caught it
        assertTrue(ClusterMetrics.render().contains(
                "guacamole_cluster_redis_insecure 1"), ClusterMetrics.render());

    }

}
