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

package org.apache.guacamole.auth.procyon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.Credentials;
// import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
// import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.apache.guacamole.auth.procyon.user.AuthenticatedUser;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;

import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;


/**
 * Service providing convenience functions for the Procyon
 * AuthenticationProvider implementation.
 */
public class AuthenticationProviderService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationProviderService.class);

    /**
     * Provider for AuthenticatedUser objects.
     */
    @Inject
    private Provider<AuthenticatedUser> authenticatedUserProvider;

    private static final ObjectMapper mapper = new ObjectMapper();

    public static class JWTToken {
        private String aud;
        private String deviceid;
        private String exp;
        private String iat;
        private String iss;
        private String org;
        private String tenant;
        private String user;

        public String getAud() {
            return aud;
        }
        public void setAud(String aud) {
            this.aud = aud;
        }
        public String getDeviceid() {
            return deviceid;
        }
        public void setDeviceid(String deviceid) {
            this.deviceid = deviceid;
        }
        public String getExp() {
            return exp;
        }
        public void setExp(String exp) {
            this.exp = exp;
        }
        public String getIat() {
            return iat;
        }
        public void setIat(String iat) {
            this.iat = iat;
        }
        public String getIss() {
            return iss;
        }
        public void setIss(String iss) {
            this.iss = iss;
        }
        public String getOrg() {
            return org;
        }
        public void setOrg(String org) {
            this.org = org;
        }
        public String getTenant() {
            return tenant;
        }
        public void setTenant(String tenant) {
            this.tenant = tenant;
        }
        public String getUser() {
            return user;
        }
        public void setUser(String user) {
            this.user = user;
        }
    }

    /**
     * Returns an AuthenticatedUser representing the user authenticated by the
     * given credentials.
     *
     * @param credentials
     *     The credentials to use for authentication.
     *
     * @return
     *     An AuthenticatedUser representing the user authenticated by the
     *     given credentials.
     *
     * @throws GuacamoleException
     *     If an error occurs while authenticating the user, or if access is
     *     denied.
     */
    public AuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        // Pull HTTP request if present
        HttpServletRequest request = credentials.getRequest();
        if (request != null) {

            // Get the JWT token
            String authHdr = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHdr == null) {
//                throw new GuacamoleException("No Authorization header");
                throw new GuacamoleInvalidCredentialsException("Invalid headers", CredentialsInfo.USERNAME_PASSWORD);
            }

            if (authHdr.length() < 7) {
                throw new GuacamoleInvalidCredentialsException("Invalid header: " + authHdr, CredentialsInfo.USERNAME_PASSWORD);

            }
            String prefix = authHdr.substring(0, 7);
            if (prefix.toLowerCase().startsWith("bearer ")) {
                authHdr = authHdr.substring(7);
                authHdr = authHdr.substring(0, authHdr.indexOf(' '));
            } else {
                throw new GuacamoleInvalidCredentialsException("Invalid auth header: " + authHdr, CredentialsInfo.USERNAME_PASSWORD);
            }

            // Parse the JWT token
            String[] jwtParts = authHdr.split("\\.");
            if (jwtParts.length != 3) {
                throw new GuacamoleInvalidCredentialsException("Invalid JWT token: " + authHdr, CredentialsInfo.USERNAME_PASSWORD);
            }
            Base64.Decoder decoder = Base64.getUrlDecoder();

            String header = new String(decoder.decode(jwtParts[0]));
            String payload = new String(decoder.decode(jwtParts[1]));

            try {
                JWTToken jwtToken = mapper.readValue(payload, JWTToken.class);
                AuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
                String username = jwtToken.tenant+":"+jwtToken.user;
                authenticatedUser.init(username, credentials);
                return authenticatedUser;
            } catch (Exception e) {
                throw new GuacamoleInvalidCredentialsException("Invalid JWT token: " + authHdr + "\nException: "+e.getMessage(), CredentialsInfo.USERNAME_PASSWORD);
            }
        }

        throw new GuacamoleInvalidCredentialsException("Invalid login.", CredentialsInfo.USERNAME_PASSWORD);

    }

}
