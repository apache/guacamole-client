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

import com.google.inject.Inject;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.resource.Singleton;
import java.io.InputStream;
import javax.ws.rs.ext.Provider;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.properties.LongGuacamoleProperty;

/**
 * Filter which restricts REST API requests to a particular maximum size.
 */
@Singleton
@Provider
public class RequestSizeFilter implements ContainerRequestFilter {

    /**
     * The default maximum number of bytes to accept within the entity body of
     * any particular REST request.
     */
    private final long DEFAULT_MAX_REQUEST_SIZE = 2097152;

    /**
     * The maximum number of bytes to accept within the entity body of any
     * particular REST request. If not specified, requests will be limited to
     * 2 MB by default. Specifying 0 disables request size limitations.
     */
    private final LongGuacamoleProperty API_MAX_REQUEST_SIZE = new LongGuacamoleProperty() {

        @Override
        public String getName() { return "api-max-request-size"; }

    };

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    @Override
    public ContainerRequest filter(ContainerRequest request) {

        // Retrieve configured request size limits
        final long maxRequestSize;
        try {
            maxRequestSize = environment.getProperty(API_MAX_REQUEST_SIZE, DEFAULT_MAX_REQUEST_SIZE);
        }
        catch (GuacamoleException e) {
            throw new APIException(e);
        }

        // Ignore request size if limit is disabled
        if (maxRequestSize == 0)
            return request;

        // Restrict maximum size of requests which have an input stream
        // available to be limited
        InputStream stream = request.getEntityInputStream();
        if (stream != null)
            request.setEntityInputStream(new LimitedRequestInputStream(stream, maxRequestSize));

        return request;

    }

}
