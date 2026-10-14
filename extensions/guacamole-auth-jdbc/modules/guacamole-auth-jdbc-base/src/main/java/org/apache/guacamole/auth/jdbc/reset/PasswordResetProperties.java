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

import org.apache.guacamole.properties.BooleanGuacamoleProperty;
import org.apache.guacamole.properties.EnumGuacamoleProperty;
import org.apache.guacamole.properties.EnumGuacamoleProperty.PropertyValue;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;

/**
 * The guacamole.properties keys read by the password reset feature. These are
 * shared by all database backends, and so carry no database prefix.
 */
public class PasswordResetProperties {

    private PasswordResetProperties() {}

    /**
     * The transport security to use when connecting to the SMTP server.
     */
    public enum SmtpTls {

        /**
         * Plaintext SMTP. Only appropriate for a local relay.
         */
        @PropertyValue("none")
        NONE,

        /**
         * Upgrade a plaintext connection with STARTTLS, refusing to send if
         * the server does not offer it.
         */
        @PropertyValue("starttls")
        STARTTLS,

        /**
         * Implicit TLS from the first byte (typically port 465).
         */
        @PropertyValue("tls")
        TLS

    }

    /**
     * The hostname of the SMTP server used to send password reset mail.
     */
    public static final StringGuacamoleProperty SMTP_HOSTNAME =
            new StringGuacamoleProperty() {
        @Override
        public String getName() { return "smtp-hostname"; }
    };

    /**
     * The port of the SMTP server. Defaults to 587.
     */
    public static final IntegerGuacamoleProperty SMTP_PORT =
            new IntegerGuacamoleProperty() {
        @Override
        public String getName() { return "smtp-port"; }
    };

    /**
     * The username to authenticate to the SMTP server with, if any.
     */
    public static final StringGuacamoleProperty SMTP_USERNAME =
            new StringGuacamoleProperty() {
        @Override
        public String getName() { return "smtp-username"; }
    };

    /**
     * The password to authenticate to the SMTP server with, if any.
     */
    public static final StringGuacamoleProperty SMTP_PASSWORD =
            new StringGuacamoleProperty() {
        @Override
        public String getName() { return "smtp-password"; }
    };

    /**
     * The address password reset mail is sent from.
     */
    public static final StringGuacamoleProperty SMTP_FROM =
            new StringGuacamoleProperty() {
        @Override
        public String getName() { return "smtp-from"; }
    };

    /**
     * The transport security of the SMTP connection: "none", "starttls"
     * (the default) or "tls".
     */
    public static final EnumGuacamoleProperty<SmtpTls> SMTP_TLS =
            new EnumGuacamoleProperty<SmtpTls>(SmtpTls.class) {
        @Override
        public String getName() { return "smtp-tls"; }
    };

    /**
     * The public URL of this Guacamole deployment, used to build reset links,
     * for example "https://guacamole.example.com/guacamole". Links are never
     * built from the request's Host header, which an attacker controls.
     */
    public static final StringGuacamoleProperty PASSWORD_RESET_BASE_URL =
            new StringGuacamoleProperty() {
        @Override
        public String getName() { return "password-reset-base-url"; }
    };

    /**
     * The product name used in reset mails, as in "the Guacamole account".
     * Defaults to "Guacamole".
     */
    public static final StringGuacamoleProperty PASSWORD_RESET_SERVICE_NAME =
            new StringGuacamoleProperty() {
        @Override
        public String getName() { return "password-reset-service-name"; }
    };

    /**
     * How long a reset link stays valid, in minutes. Defaults to 30.
     */
    public static final IntegerGuacamoleProperty PASSWORD_RESET_TOKEN_TTL =
            new IntegerGuacamoleProperty() {
        @Override
        public String getName() { return "password-reset-token-ttl"; }
    };

    /**
     * The maximum number of reset mails sent to a single user per hour.
     * Defaults to 3.
     */
    public static final IntegerGuacamoleProperty PASSWORD_RESET_MAX_PER_HOUR =
            new IntegerGuacamoleProperty() {
        @Override
        public String getName() { return "password-reset-max-per-hour"; }
    };

    /**
     * Whether users may request a reset link for themselves from the login
     * page. Defaults to false, which also hides the "Forgot password?" link.
     */
    public static final BooleanGuacamoleProperty PASSWORD_RESET_SELF_SERVICE_ENABLED =
            new BooleanGuacamoleProperty() {
        @Override
        public String getName() { return "password-reset-self-service-enabled"; }
    };

    /**
     * The maximum number of self-service reset requests accepted from a
     * single source address per hour. Defaults to 10.
     */
    public static final IntegerGuacamoleProperty PASSWORD_RESET_MAX_PER_IP_PER_HOUR =
            new IntegerGuacamoleProperty() {
        @Override
        public String getName() { return "password-reset-max-per-ip-per-hour"; }
    };

}
