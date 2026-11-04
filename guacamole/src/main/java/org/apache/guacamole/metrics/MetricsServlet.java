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


package org.apache.guacamole.metrics;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.inject.Provider;
import org.apache.guacamole.cluster.metrics.ClusterMetrics;
import org.apache.guacamole.rest.auth.AuthenticationService;

/**
 * Serves the cluster metrics registry in the Prometheus text exposition
 * format, guarded by a bearer token.
 *
 * The endpoint cannot require a Guacamole session, because a scraper does not
 * have one, and it cannot be left open, because the reverse proxy or ingress
 * in front of Guacamole typically routes every path to this application and
 * offers no per-path deny.
 */
public class MetricsServlet extends HttpServlet {

    /**
     * The content type of the Prometheus text exposition format.
     */
    private static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

    /**
     * The prefix of an Authorization header carrying a bearer token.
     */
    private static final String BEARER = "Bearer ";

    /**
     * Whether the endpoint is served at all. When false, every request
     * receives a 404, as if the endpoint did not exist.
     */
    private final boolean enabled;

    /**
     * The bearer token a request must carry, which may be null. If null, every
     * request is refused.
     */
    private final String token;

    /**
     * Supplies the authentication service holding the store whose gauges are
     * sampled before each render, which may be null.
     *
     * A provider rather than the service itself: this servlet is bound while
     * the injector is still being created, so the service does not exist yet.
     */
    private final Provider<AuthenticationService> authenticationService;

    /**
     * The status and body of a single metrics request, as decided by
     * render() independently of the Servlet API.
     */
    public static class Result {

        /**
         * The HTTP status to send.
         */
        private final int status;

        /**
         * The body to send, which is empty for every refusal.
         */
        private final String body;

        /**
         * Creates a result with the given status and body.
         *
         * @param status
         *     The HTTP status to send.
         *
         * @param body
         *     The body to send, which is empty for every refusal.
         */
        public Result(int status, String body) {
            this.status = status;
            this.body = body;
        }

        /**
         * Returns the HTTP status of this result.
         *
         * @return
         *     The HTTP status.
         */
        public int getStatus() {
            return status;
        }

        /**
         * Returns the body of this result.
         *
         * @return
         *     The body, which is empty for every refusal.
         */
        public String getBody() {
            return body;
        }

    }

    /**
     * Creates a new MetricsServlet which serves the cluster metrics registry
     * to requests carrying the given bearer token.
     *
     * @param enabled
     *     Whether the endpoint is served at all. If false, every request
     *     receives a 404.
     *
     * @param token
     *     The bearer token a request must carry, which may be null. If null,
     *     every request is refused.
     *
     * @param authenticationService
     *     Supplies the authentication service holding the store whose gauges
     *     are sampled before each render, which may be null.
     */
    public MetricsServlet(boolean enabled, String token,
            Provider<AuthenticationService> authenticationService) {
        this.enabled = enabled;
        this.token = token;
        this.authenticationService = authenticationService;
    }

    /**
     * Decides what a request carrying the given Authorization header receives.
     *
     * Separated from doGet so that it can be tested without mocking the
     * Servlet API, which this module has no library for.
     *
     * @param authorization
     *     The request's Authorization header, which may be null.
     *
     * @return
     *     The status and body to send.
     */
    public Result render(String authorization) {

        // A disabled endpoint is indistinguishable from one that was never
        // built
        if (!enabled)
            return new Result(HttpServletResponse.SC_NOT_FOUND, "");

        // Refuse anything not carrying the configured token, revealing
        // nothing in the body
        if (!authorized(authorization))
            return new Result(HttpServletResponse.SC_UNAUTHORIZED, "");

        // Refresh the store-derived gauges so the scrape reflects current
        // state rather than the values of the previous scrape
        if (authenticationService != null)
            authenticationService.get().getClusterStore().publishMetrics();

        return new Result(HttpServletResponse.SC_OK, ClusterMetrics.render());

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Decide the outcome from the Authorization header alone
        Result result = render(request.getHeader("Authorization"));

        response.setStatus(result.getStatus());

        // Only a successful request carries a body; refusals send the status
        // alone
        if (result.getStatus() == HttpServletResponse.SC_OK) {
            response.setContentType(CONTENT_TYPE);
            response.getWriter().write(result.getBody());
        }

    }

    /**
     * Returns whether the given Authorization header carries the configured
     * token, compared in constant time.
     *
     * @param header
     *     The Authorization header, which may be null.
     *
     * @return
     *     true if the header authorises the request, false otherwise.
     */
    private boolean authorized(String header) {

        // Refuse a missing header, an unconfigured token, or any scheme other
        // than Bearer
        if (header == null || token == null || !header.startsWith(BEARER))
            return false;

        // A length mismatch can be refused early, as the token length is not
        // the secret being protected
        String presented = header.substring(BEARER.length());
        if (presented.length() != token.length())
            return false;

        // Compare every character regardless of where the first mismatch
        // occurs, so that response timing reveals nothing about the token
        int difference = 0;
        for (int i = 0; i < token.length(); i++)
            difference |= presented.charAt(i) ^ token.charAt(i);

        return difference == 0;

    }

}
