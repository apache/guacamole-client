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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.RuntimeDelegate;

/**
 * Minimal JAX-RS {@link RuntimeDelegate} implementation for unit tests where
 * a servlet container / Jersey runtime is not present on the classpath.
 */
public class TestJaxRsRuntimeDelegate extends RuntimeDelegate {

    /**
     * Installs this test delegate into JAX-RS {@link RuntimeDelegate}.
     */
    public static void install() {
        RuntimeDelegate.setInstance(new TestJaxRsRuntimeDelegate());
    }

    @Override
    public UriBuilder createUriBuilder() {
        return new SimpleUriBuilder();
    }

    @Override
    public Response.ResponseBuilder createResponseBuilder() {
        return new SimpleResponseBuilder();
    }

    @Override
    public Variant.VariantListBuilder createVariantListBuilder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T createEndpoint(Application application, Class<T> endpointType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> type) {
        throw new UnsupportedOperationException();
    }

    /**
     * Minimal {@link UriBuilder} supporting the subset of operations used by
     * the SAML extension ({@code fromUri}, {@code path}, {@code queryParam},
     * {@code build}).
     */
    private static final class SimpleUriBuilder extends UriBuilder {

        private String scheme;
        private String authority;
        private StringBuilder path = new StringBuilder();
        private final List<String> queryParams = new ArrayList<>();

        @Override
        public UriBuilder clone() {
            SimpleUriBuilder copy = new SimpleUriBuilder();
            copy.scheme = this.scheme;
            copy.authority = this.authority;
            copy.path = new StringBuilder(this.path);
            copy.queryParams.addAll(this.queryParams);
            return copy;
        }

        @Override
        public UriBuilder uri(URI uri) {
            if (uri == null)
                throw new IllegalArgumentException("URI must not be null.");
            this.scheme = uri.getScheme();
            this.authority = uri.getRawAuthority();
            this.path = new StringBuilder(uri.getRawPath() != null ? uri.getRawPath() : "");
            this.queryParams.clear();
            if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty())
                this.queryParams.add(uri.getRawQuery());
            return this;
        }

        @Override
        public UriBuilder path(String segment) {
            if (segment == null)
                throw new IllegalArgumentException("Path must not be null.");
            String trimmed = segment;
            while (trimmed.startsWith("/"))
                trimmed = trimmed.substring(1);
            if (path.length() == 0 || path.charAt(path.length() - 1) != '/')
                path.append('/');
            path.append(trimmed);
            return this;
        }

        @Override
        public UriBuilder queryParam(String name, Object... values) {
            for (Object value : values) {
                if (value != null) {
                    try {
                        queryParams.add(
                                URLEncoder.encode(name, StandardCharsets.UTF_8.name())
                                + "="
                                + URLEncoder.encode(value.toString(), StandardCharsets.UTF_8.name()));
                    }
                    catch (UnsupportedEncodingException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
            return this;
        }

        @Override
        public URI build(Object... values) throws UriBuilderException {
            StringBuilder sb = new StringBuilder();
            if (scheme != null)
                sb.append(scheme).append(':');
            if (authority != null)
                sb.append("//").append(authority);
            sb.append(path);
            if (!queryParams.isEmpty()) {
                sb.append('?');
                for (int i = 0; i < queryParams.size(); i++) {
                    if (i > 0)
                        sb.append('&');
                    sb.append(queryParams.get(i));
                }
            }
            try {
                return new URI(sb.toString());
            }
            catch (URISyntaxException e) {
                throw new UriBuilderException(e);
            }
        }

        @Override public UriBuilder scheme(String scheme) { this.scheme = scheme; return this; }
        @Override public UriBuilder schemeSpecificPart(String ssp) { throw new UnsupportedOperationException(); }
        @Override public UriBuilder userInfo(String ui) { throw new UnsupportedOperationException(); }
        @Override public UriBuilder host(String host) { throw new UnsupportedOperationException(); }
        @Override public UriBuilder port(int port) { throw new UnsupportedOperationException(); }
        @Override public UriBuilder replacePath(String path) { this.path = new StringBuilder(path != null ? path : ""); return this; }
        @Override public UriBuilder path(Class resource) { throw new UnsupportedOperationException(); }
        @Override public UriBuilder path(Class resource, String method) { throw new UnsupportedOperationException(); }
        @Override public UriBuilder path(Method method) { throw new UnsupportedOperationException(); }
        @Override public UriBuilder segment(String... segments) { for (String s : segments) path(s); return this; }
        @Override public UriBuilder replaceMatrix(String matrix) { throw new UnsupportedOperationException(); }
        @Override public UriBuilder matrixParam(String name, Object... values) { throw new UnsupportedOperationException(); }
        @Override public UriBuilder replaceMatrixParam(String name, Object... values) { throw new UnsupportedOperationException(); }
        @Override public UriBuilder replaceQuery(String query) { queryParams.clear(); if (query != null) queryParams.add(query); return this; }
        @Override public UriBuilder replaceQueryParam(String name, Object... values) { throw new UnsupportedOperationException(); }
        @Override public UriBuilder fragment(String fragment) { throw new UnsupportedOperationException(); }
        @Override public URI buildFromMap(Map<String, ? extends Object> values) { return build(); }
        @Override public URI buildFromEncodedMap(Map<String, ? extends Object> values) { return build(); }
        @Override public URI buildFromEncoded(Object... values) { return build(); }

    }

    /**
     * Minimal {@link Response.ResponseBuilder} capturing status, entity, and
     * Location header for unit test assertions.
     */
    private static final class SimpleResponseBuilder extends Response.ResponseBuilder {

        private int status = 200;
        private Object entity;
        private final SimpleMultivaluedMap headers = new SimpleMultivaluedMap();

        @Override
        public Response build() {
            final int responseStatus = status;
            final Object responseEntity = entity;
            final MultivaluedMap<String, Object> responseHeaders = headers;
            return new Response() {
                @Override
                public Object getEntity() {
                    return responseEntity;
                }

                @Override
                public int getStatus() {
                    return responseStatus;
                }

                @Override
                public MultivaluedMap<String, Object> getMetadata() {
                    return responseHeaders;
                }
            };
        }

        @Override
        public Response.ResponseBuilder clone() {
            SimpleResponseBuilder copy = new SimpleResponseBuilder();
            copy.status = this.status;
            copy.entity = this.entity;
            copy.headers.putAll(this.headers);
            return copy;
        }

        @Override public Response.ResponseBuilder status(int status) { this.status = status; return this; }
        @Override public Response.ResponseBuilder entity(Object entity) { this.entity = entity; return this; }
        @Override public Response.ResponseBuilder type(MediaType type) { return header("Content-Type", type); }
        @Override public Response.ResponseBuilder type(String type) { return header("Content-Type", type); }
        @Override public Response.ResponseBuilder variant(Variant variant) { return this; }
        @Override public Response.ResponseBuilder variants(List<Variant> variants) { return this; }
        @Override public Response.ResponseBuilder language(String language) { return this; }
        @Override public Response.ResponseBuilder language(Locale language) { return this; }
        @Override public Response.ResponseBuilder location(URI location) { return header("Location", location); }
        @Override public Response.ResponseBuilder contentLocation(URI location) { return header("Content-Location", location); }
        @Override public Response.ResponseBuilder tag(EntityTag tag) { return this; }
        @Override public Response.ResponseBuilder tag(String tag) { return this; }
        @Override public Response.ResponseBuilder lastModified(Date lastModified) { return this; }
        @Override public Response.ResponseBuilder cacheControl(CacheControl cacheControl) { return this; }
        @Override public Response.ResponseBuilder expires(Date expires) { return this; }
        @Override public Response.ResponseBuilder header(String name, Object value) {
            if (value == null)
                headers.remove(name);
            else
                headers.putSingle(name, value);
            return this;
        }
        @Override public Response.ResponseBuilder cookie(NewCookie... cookies) { return this; }

    }

    /**
     * Minimal {@link MultivaluedMap} implementation for test responses.
     */
    private static final class SimpleMultivaluedMap
            extends java.util.LinkedHashMap<String, List<Object>>
            implements MultivaluedMap<String, Object> {

        private static final long serialVersionUID = 1L;

        @Override
        public void putSingle(String key, Object value) {
            List<Object> list = new ArrayList<>();
            list.add(value);
            put(key, list);
        }

        @Override
        public void add(String key, Object value) {
            List<Object> list = get(key);
            if (list == null) {
                list = new ArrayList<>();
                put(key, list);
            }
            list.add(value);
        }

        @Override
        public Object getFirst(String key) {
            List<Object> list = get(key);
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        }

    }

}
