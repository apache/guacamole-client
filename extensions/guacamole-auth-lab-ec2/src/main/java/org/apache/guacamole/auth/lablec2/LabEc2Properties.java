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
package org.apache.guacamole.auth.lablec2;

import org.apache.guacamole.properties.GuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;

/**
 * Properties used to configure the lab EC2 decorator extension.
 */
public final class LabEc2Properties {

    /**
     * Base URL for the Illustrator backend (resource server).
     */
    public static final GuacamoleProperty<String> LAB_EC2_ILLUSTRATOR_BASE_URL =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "lab-ec2-illustrator-base-url";
                }
            };

    /**
     * Path used to resolve/ensure a Guacamole connection.
     */
    public static final GuacamoleProperty<String> LAB_EC2_ILLUSTRATOR_CONNECTION_PATH =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "lab-ec2-illustrator-connection-path";
                }
            };

    /**
     * Path (file: or classpath:) to PKCS#8 PEM private key used to decrypt
     * password returned by Illustrator /guac/connection.
     */
    public static final GuacamoleProperty<String> LAB_EC2_ILLUSTRATOR_PRIVATE_KEY_PATH =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "lab-ec2-illustrator-private-key-path";
                }
            };

    /**
     * Optional key identifier expected from Illustrator password envelope.
     */
    public static final GuacamoleProperty<String> LAB_EC2_ILLUSTRATOR_PASSWORD_KEY_ID =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "lab-ec2-illustrator-password-key-id";
                }
            };

    /**
     * Utility class; do not instantiate.
     */
    private LabEc2Properties() {
    }

}
