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

package org.apache.guacamole.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.SecurityContext;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.GuacamoleUnauthorizedException;
import org.apache.guacamole.rest.auth.AuthenticationService;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test which verifies that RESTExceptionMapper invalidates the session of
 * the request that actually failed, even though the mapper itself is a
 * singleton shared by all requests.
 */
public class RESTExceptionMapperTest {

    /**
     * AuthenticationService which records the sessions it is asked to
     * destroy instead of destroying anything.
     */
    private static class RecordingAuthenticationService extends AuthenticationService {

        /**
         * The tokens of all sessions destroyed so far, in order.
         */
        private final List<String> destroyed = Collections.synchronizedList(new ArrayList<>());

        @Override
        public boolean destroyGuacamoleSession(String authToken) {
            destroyed.add(authToken);
            return true;
        }

    }

    /**
     * REST resource whose methods always fail with a specific exception.
     */
    @Path("/")
    public static class FailingResource {

        /**
         * Always fails as if the requested object did not exist.
         *
         * @throws GuacamoleException
         *     Always.
         */
        @GET
        @Path("missing")
        public String missing() throws GuacamoleException {
            throw new GuacamoleResourceNotFoundException("Not found.");
        }

        /**
         * Always fails as if the provided token were invalid or expired.
         *
         * @throws GuacamoleException
         *     Always.
         */
        @GET
        @Path("unauthorized")
        public String unauthorized() throws GuacamoleException {
            throw new GuacamoleUnauthorizedException("Permission denied.");
        }

    }

    /**
     * The AuthenticationService used by the RESTExceptionMapper under test.
     */
    private RecordingAuthenticationService authenticationService;

    /**
     * Jersey, processing requests in memory with only FailingResource and
     * RESTExceptionMapper registered.
     */
    private ApplicationHandler application;

    @Before
    public void setUp() {
        authenticationService = new RecordingAuthenticationService();
        application = new ApplicationHandler(new ResourceConfig()
            .register(FailingResource.class)
            .register(RESTExceptionMapper.class)
            .register(new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(authenticationService).to(AuthenticationService.class);
                }
            }));
    }

    /**
     * Sends a GET request for the given path with the given token, returning
     * the HTTP status of the response.
     *
     * @param path
     *     The path of the request, relative to the root of the application.
     *
     * @param token
     *     The authentication token to send as the "token" query parameter.
     *
     * @return
     *     The HTTP status code of the response.
     *
     * @throws Exception
     *     If the request cannot be processed.
     */
    private int get(String path, String token) throws Exception {
        URI base = URI.create("http://localhost/");
        ContainerRequest request = new ContainerRequest(base,
                base.resolve(path + "?token=" + token), "GET", (SecurityContext) null,
                new MapPropertiesDelegate(), null);
        ContainerResponse response = application.apply(request).get();
        return response.getStatus();
    }

    /**
     * Verifies that an unauthorized request invalidates its own session and
     * no other, including the session of a different request that failed
     * earlier (and which therefore was the first to reach the mapper).
     *
     * @throws Exception
     *     If a request cannot be processed.
     */
    @Test
    public void testInvalidatesOnlyFailingSession() throws Exception {

        // An unrelated failure by one user must not invalidate anything
        Assert.assertEquals(404, get("missing", "FIRST"));
        Assert.assertEquals(Collections.emptyList(), authenticationService.destroyed);

        // Each unauthorized request invalidates its own session only
        Assert.assertEquals(403, get("unauthorized", "SECOND"));
        Assert.assertEquals(403, get("unauthorized", "THIRD"));
        Assert.assertEquals(Arrays.asList("SECOND", "THIRD"), authenticationService.destroyed);

    }

}
