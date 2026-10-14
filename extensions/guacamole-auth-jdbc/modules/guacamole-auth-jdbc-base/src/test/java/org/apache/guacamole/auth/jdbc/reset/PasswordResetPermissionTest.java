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
import java.util.Collections;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.net.auth.Permissions;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.SystemPermission;
import org.apache.guacamole.net.auth.simple.SimpleObjectPermissionSet;
import org.apache.guacamole.net.auth.simple.SimpleSystemPermissionSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the permission check guarding the admin reset endpoint.
 */
public class PasswordResetPermissionTest {

    /**
     * Returns effective permissions holding the given system permission (or
     * none) and the given permission on the given user (or none).
     */
    private static Permissions permissions(SystemPermission.Type system,
            ObjectPermission.Type userPermission, String user) {

        final SimpleSystemPermissionSet systemSet = (system == null)
                ? new SimpleSystemPermissionSet()
                : new SimpleSystemPermissionSet(Collections.singleton(new SystemPermission(system)));

        final SimpleObjectPermissionSet userSet = (userPermission == null)
                ? new SimpleObjectPermissionSet()
                : new SimpleObjectPermissionSet(Collections.singleton(new ObjectPermission(userPermission, user)));

        return (Permissions) Proxy.newProxyInstance(Permissions.class.getClassLoader(),
                new Class<?>[] { Permissions.class }, (proxy, method, args) -> {
                    if (method.getName().equals("getSystemPermissions"))
                        return systemSet;
                    if (method.getName().equals("getUserPermissions"))
                        return userSet;
                    return new SimpleObjectPermissionSet();
                });
    }

    @Test
    public void administratorMayResetAnyone() {
        assertDoesNotThrow(() -> PasswordResetService.requireResetPermission(
                permissions(SystemPermission.Type.ADMINISTER, null, null), "alice"));
    }

    @Test
    public void delegatedAdminWithUpdateMayResetThatUser() {
        assertDoesNotThrow(() -> PasswordResetService.requireResetPermission(
                permissions(null, ObjectPermission.Type.UPDATE, "alice"), "alice"));
    }

    @Test
    public void readOnlyIsForbidden() {
        GuacamoleException e = assertThrows(GuacamoleSecurityException.class,
                () -> PasswordResetService.requireResetPermission(
                        permissions(null, ObjectPermission.Type.READ, "alice"), "alice"));
        assertEquals(403, e.getStatus().getHttpStatusCode());
    }

    @Test
    public void updateOnAnotherUserIsForbidden() {
        assertThrows(GuacamoleSecurityException.class,
                () -> PasswordResetService.requireResetPermission(
                        permissions(null, ObjectPermission.Type.UPDATE, "bob"), "alice"));
    }

    @Test
    public void otherSystemPermissionsAreNotEnough() {
        assertThrows(GuacamoleSecurityException.class,
                () -> PasswordResetService.requireResetPermission(
                        permissions(SystemPermission.Type.CREATE_USER, null, null), "alice"));
    }

}
