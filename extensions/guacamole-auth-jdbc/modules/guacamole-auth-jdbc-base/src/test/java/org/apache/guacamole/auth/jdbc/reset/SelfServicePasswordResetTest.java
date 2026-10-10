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


package org.apache.guacamole.auth.jdbc.reset;

import java.lang.reflect.Proxy;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;
import org.apache.guacamole.auth.jdbc.user.UserModel;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests that self-service requests never reveal whether an account exists,
 * and that mail goes only to eligible accounts.
 */
public class SelfServicePasswordResetTest {

    /**
     * Identifiers of the users a link was sent to.
     */
    private final List<String> sent = new ArrayList<>();

    /**
     * The database users, by username.
     */
    private final Map<String, ModeledUser> users = new HashMap<>();

    /**
     * Service running work synchronously against the fakes above.
     */
    private final SelfServicePasswordResetService service = new SelfServicePasswordResetService(
            new PasswordResetService() {
                @Override
                public void sendResetLink(ModeledUser user) {
                    sent.add(user.getIdentifier());
                }
            }, null, Runnable::run) {
        @Override
        protected ModeledUser findUser(AuthenticationProvider authenticationProvider,
                String username, Credentials credentials) {
            return users.get(username);
        }
    };

    private void addUser(String username, String email, boolean disabled, Date validUntil) {
        UserModel model = new UserModel(username);
        model.setEmailAddress(email);
        model.setDisabled(disabled);
        model.setValidUntil(validUntil);
        ModeledUser user = new ModeledUser();
        user.setModel(model);
        users.put(username, user);
    }

    private static HttpServletRequest request(final String address) {
        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> {
                    if (method.getName().equals("getRemoteAddr") || method.getName().equals("getRemoteHost"))
                        return address;
                    if (method.getReturnType() == Enumeration.class)
                        return Collections.emptyEnumeration();
                    if (method.getReturnType() == boolean.class)
                        return false;
                    if (method.getReturnType() == int.class)
                        return 0;
                    return null;
                });
    }

    @Test
    public void responseIsIdenticalAndOnlyEligibleUsersGetMail() {

        addUser("alice", "alice@example.com", false, null);
        addUser("noemail", null, false, null);
        addUser("blank", "  ", false, null);
        addUser("disabled", "disabled@example.com", true, null);
        addUser("expired", "expired@example.com", false, Date.valueOf("2000-01-01"));

        SelfServicePasswordResetResource resource = new SelfServicePasswordResetResource(service, null, 100);
        for (String username : new String[] { "alice", "unknown", "noemail", "blank", "disabled", "expired", "" })
            assertSame(SelfServicePasswordResetResource.RESPONSE,
                    resource.requestReset(username, request("10.0.0.1")), username);

        assertEquals(Collections.singletonList("alice"), sent);

    }

    @Test
    public void requestsAreLimitedPerSourceAddress() {

        addUser("alice", "alice@example.com", false, null);
        SelfServicePasswordResetResource resource = new SelfServicePasswordResetResource(service, null, 2);

        for (int i = 0; i < 5; i++)
            assertSame(SelfServicePasswordResetResource.RESPONSE,
                    resource.requestReset("alice", request("10.0.0.1")));
        assertEquals(2, sent.size(), "only 2 requests from 10.0.0.1 processed");

        resource.requestReset("alice", request("10.0.0.2"));
        assertEquals(3, sent.size(), "another address is unaffected");

    }

}
