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
     * Region to use for EC2 operations.
     */
    public static final GuacamoleProperty<String> LAB_EC2_REGION =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "lab-ec2-region";
                }
            };

    /**
     * Optional AMI ID to use when launching instances.
     */
    public static final GuacamoleProperty<String> LAB_EC2_AMI_ID =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "lab-ec2-ami-id";
                }
            };

    /**
     * Instance type to use when launching instances.
     */
    public static final GuacamoleProperty<String> LAB_EC2_INSTANCE_TYPE =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "lab-ec2-instance-type";
                }
            };

    /**
     * Subnet ID to use when creating instances.
     */
    public static final GuacamoleProperty<String> LAB_EC2_SUBNET_ID =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "lab-ec2-subnet-id";
                }
            };

    /**
     * Optional comma-delimited list of security group IDs.
     */
    public static final GuacamoleProperty<String> LAB_EC2_SECURITY_GROUP_IDS =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "lab-ec2-security-group-ids";
                }
            };

    /**
     * Optional launch template ID.
     */
    public static final GuacamoleProperty<String> LAB_EC2_LAUNCH_TEMPLATE_ID =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "lab-ec2-launch-template-id";
                }
            };

    /**
     * Protocol to use for generated Guacamole connections.
     */
    public static final GuacamoleProperty<String> LAB_EC2_PROTOCOL =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "lab-ec2-protocol";
                }
            };

    /**
     * Port used by the generated Guacamole connections.
     */
    public static final GuacamoleProperty<String> LAB_EC2_PORT =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "lab-ec2-port";
                }
            };

    /**
     * Username to inject into generated Guacamole configurations.
     */
    public static final GuacamoleProperty<String> LAB_EC2_USERNAME =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "lab-ec2-username";
                }
            };

    /**
     * Optional password for generated Guacamole configurations.
     */
    public static final GuacamoleProperty<String> LAB_EC2_PASSWORD =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "lab-ec2-password";
                }
            };

    /**
     * Name of the group required to receive the lab connection.
     */
    public static final GuacamoleProperty<String> LAB_EC2_LAB_GROUP =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "lab-ec2-lab-group";
                }
            };

    /**
     * Utility class; do not instantiate.
     */
    private LabEc2Properties() {
    }

}
