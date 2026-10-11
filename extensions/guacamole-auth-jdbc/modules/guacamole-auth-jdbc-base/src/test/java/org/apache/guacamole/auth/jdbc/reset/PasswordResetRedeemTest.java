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

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;
import org.apache.guacamole.auth.jdbc.user.UserModel;
import org.apache.guacamole.auth.jdbc.user.UserService;
import org.apache.guacamole.form.Field.Type;
import org.apache.guacamole.language.TranslatableGuacamoleInsufficientCredentialsException;
import org.apache.guacamole.language.TranslatableGuacamoleInvalidCredentialsException;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests how a password reset link is answered: a valid token asks for the
 * new password, and an invalid one is rejected as invalid credentials which
 * the login page can tell apart from an ordinary failed login.
 */
public class PasswordResetRedeemTest {

    private final InMemoryPasswordResetTokenMapper mapper = new InMemoryPasswordResetTokenMapper();

    private final PasswordResetTokenStore store = new PasswordResetTokenStore(mapper);

    private final PasswordResetService service = new PasswordResetService();

    public PasswordResetRedeemTest() throws Exception {
        inject("tokenStore", store);
        inject("userService", new UserService() {
            @Override
            public ModeledAuthenticatedUser retrieveUserForPasswordReset(
                    AuthenticationProvider authenticationProvider, String username,
                    Credentials credentials) {
                ModeledUser user = new ModeledUser() {
                    @Override
                    public Set<String> getEffectiveUserGroups() {
                        return Collections.<String>emptySet();
                    }
                };
                user.setModel(new UserModel(username));
                return new ModeledAuthenticatedUser(authenticationProvider, user, credentials);
            }
        });
    }

    private void inject(String name, Object value) throws Exception {
        Field field = PasswordResetService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }

    private static Credentials credentials(String username, final Map<String, String> parameters) {
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> {
                    if (method.getName().equals("getParameterNames"))
                        return Collections.enumeration(parameters.keySet());
                    if (method.getName().equals("getParameterValues"))
                        return parameters.containsKey(args[0]) ? new String[] { parameters.get(args[0]) } : null;
                    if (method.getName().equals("getParameter"))
                        return parameters.get(args[0]);
                    if (method.getReturnType() == Enumeration.class)
                        return Collections.emptyEnumeration();
                    if (method.getReturnType() == boolean.class)
                        return false;
                    if (method.getReturnType() == int.class)
                        return 0;
                    return null;
                });
        return new Credentials(username, null, request);
    }

    private static Map<String, String> token(String token) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PasswordResetService.TOKEN_PARAMETER, token);
        return parameters;
    }

    private void assertLinkInvalid(Credentials credentials) {
        TranslatableGuacamoleInvalidCredentialsException e = assertThrows(
                TranslatableGuacamoleInvalidCredentialsException.class,
                () -> service.redeem(null, credentials));
        assertEquals("PASSWORD_RESET.ERROR_LINK_INVALID", e.getTranslatableMessage().getKey());
        assertNull(e.getTranslatableMessage().getVariables(), "no account details for a bad link");
    }

    @Test
    public void unknownTokenIsRejectedDistinctly() {
        assertLinkInvalid(credentials(null, token(PasswordResetTokenStore.generateToken())));
    }

    @Test
    public void usedTokenIsRejectedDistinctly() throws GuacamoleException {
        String token = store.issue(1, 30, 3, new Date());
        Map<String, String> parameters = token(token);
        parameters.put("new-password", "S3cret!pass");
        parameters.put("confirm-new-password", "S3cret!pass");
        assertEquals(1, mapper.consume(PasswordResetTokenStore.hash(token), new Date()));
        assertLinkInvalid(credentials(null, parameters));
    }

    @Test
    public void validTokenAsksForNewPasswordOnly() throws GuacamoleException {
        String token = store.issue(1, 30, 3, new Date());
        TranslatableGuacamoleInsufficientCredentialsException e = assertThrows(
                TranslatableGuacamoleInsufficientCredentialsException.class,
                () -> service.redeem(null, credentials(null, token(token))));
        assertEquals("PASSWORD_RESET.INFO_CHOOSE_PASSWORD", e.getTranslatableMessage().getKey());
        assertEquals(Collections.singletonMap("USERNAME", "user1"), e.getTranslatableMessage().getVariables());
        assertEquals(2, e.getCredentialsInfo().getFields().size());
        for (org.apache.guacamole.form.Field field : e.getCredentialsInfo().getFields())
            assertEquals(Type.PASSWORD, field.getType());
        assertEquals("new-password", e.getCredentialsInfo().getFields().iterator().next().getName());
    }

    @Test
    public void ordinaryLoginIgnoresToken() throws GuacamoleException {
        assertNull(service.redeem(null, credentials("alice", token("whatever"))));
    }

}
