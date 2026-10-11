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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the wording of password reset mails.
 */
public class PasswordResetMailTest {

    @Test
    public void defaultServiceName() {
        assertTrue(PasswordResetService.mailBody(PasswordResetService.DEFAULT_SERVICE_NAME, "alice", 30, "http://x/")
                .startsWith("A password reset was requested for the Guacamole account \"alice\".\n\n"));
        assertEquals("Guacamole password reset", PasswordResetService.mailSubject(PasswordResetService.DEFAULT_SERVICE_NAME));
    }

    @Test
    public void configuredServiceName() {
        String body = PasswordResetService.mailBody("remote-access", "ziv.lifshits", 30, "http://x/");
        assertTrue(body.contains("the remote-access account \"ziv.lifshits\""), body);
        assertTrue(body.contains("open this link within 30 minutes:\n\nhttp://x/\n\n"), body);
        assertEquals("remote-access password reset", PasswordResetService.mailSubject("remote-access"));
    }

}
