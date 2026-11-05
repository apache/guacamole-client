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

package org.apache.guacamole.auth.jdbc.encryption;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.jdbc.connection.ConnectionParameterModel;
import org.junit.jupiter.api.Test;

public class TunnelDecryptFailureTest {

    @Test
    public void tunnelGuardNeverReturnsCiphertext() throws Exception {
        ParameterEncryptionService writer = new ParameterEncryptionService(
            ParameterEncryptionServiceTest.provider("k1", "k1"), ParameterEncryptionServiceTest.policy(), false);
        ConnectionParameterModel pw = ParameterEncryptionServiceTest.param("7", "password", "s3cret");
        writer.protectConnectionParameters(Collections.singletonList(pw));

        // key removed, feature disabled
        ParameterEncryptionService disabled = new ParameterEncryptionService(
            PropertyParameterKeyProvider.DISABLED, ParameterEncryptionServiceTest.policy(), false);
        assertThrows(GuacamoleServerException.class, () -> disabled.reveal(
            ParameterOwner.CONNECTION, "7", "password", pw.getValue(), pw.getEncryption()));

        // row moved to another connection
        assertThrows(GuacamoleServerException.class, () -> writer.reveal(
            ParameterOwner.CONNECTION, "8", "password", pw.getValue(), pw.getEncryption()));
    }

}
