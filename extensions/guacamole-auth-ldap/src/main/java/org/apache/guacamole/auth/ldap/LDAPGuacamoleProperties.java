/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.apache.guacamole.auth.ldap;

import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;


/**
 * Provides properties required for use of the LDAP authentication provider.
 * These properties will be read from guacamole.properties when the LDAP
 * authentication provider is used.
 *
 * @author Michael Jumper
 */
public class LDAPGuacamoleProperties {

    /**
     * This class should not be instantiated.
     */
    private LDAPGuacamoleProperties() {}

    /**
     * The base DN to search for Guacamole configurations.
     */
    public static final StringGuacamoleProperty LDAP_CONFIG_BASE_DN = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "ldap-config-base-dn"; }

    };

    /**
     * The base DN of users. All users must be contained somewhere within the
     * subtree of this DN. If the LDAP authentication will not be given its own
     * credentials for querying other LDAP users, all users must be direct
     * children of this base DN, varying only by LDAP_USERNAME_ATTRIBUTE.
     */
    public static final StringGuacamoleProperty LDAP_USER_BASE_DN = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "ldap-user-base-dn"; }

    };

    /**
     * The base DN of role based access control (RBAC) groups. All groups which
     * will be used for RBAC must be contained somewhere within the subtree of
     * this DN.
     */
    public static final StringGuacamoleProperty LDAP_GROUP_BASE_DN = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "ldap-group-base-dn"; }

    };

    /**
     * The attribute or attributes which identify users. One of these
     * attributes must be present within each Guacamole user's record in the
     * LDAP directory. If the LDAP authentication will not be given its own
     * credentials for querying other LDAP users, this list may contain only
     * one attribute, and the concatenation of that attribute and the value of
     * LDAP_USER_BASE_DN must equal the user's full DN.
     */
    public static final StringListProperty LDAP_USERNAME_ATTRIBUTE = new StringListProperty() {

        @Override
        public String getName() { return "ldap-username-attribute"; }

    };

    /**
     * The port on the LDAP server to connect to when authenticating users.
     */
    public static final IntegerGuacamoleProperty LDAP_PORT = new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "ldap-port"; }

    };

    /**
     * The hostname of the LDAP server to connect to when authenticating users.
     */
    public static final StringGuacamoleProperty LDAP_HOSTNAME = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "ldap-hostname"; }

    };

    /**
     * The DN of the user that the LDAP authentication should bind as when
     * searching for the user accounts of users attempting to log in. If not
     * specified, the DNs of users attempting to log in will be derived from
     * the LDAP_BASE_DN and LDAP_USERNAME_ATTRIBUTE directly.
     */
    public static final StringGuacamoleProperty LDAP_SEARCH_BIND_DN = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "ldap-search-bind-dn"; }

    };

    /**
     * The password to provide to the LDAP server when binding as
     * LDAP_SEARCH_BIND_DN. If LDAP_SEARCH_BIND_DN is not specified, this
     * property has no effect. If this property is not specified, no password
     * will be provided when attempting to bind as LDAP_SEARCH_BIND_DN.
     */
    public static final StringGuacamoleProperty LDAP_SEARCH_BIND_PASSWORD = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "ldap-search-bind-password"; }

    };

    /**
     * The encryption method to use when connecting to the LDAP server, if any.
     * The chosen method will also dictate the default port if not already
     * explicitly specified via LDAP_PORT.
     */
    public static final EncryptionMethodProperty LDAP_ENCRYPTION_METHOD = new EncryptionMethodProperty() {

        @Override
        public String getName() { return "ldap-encryption-method"; }

    };

}
