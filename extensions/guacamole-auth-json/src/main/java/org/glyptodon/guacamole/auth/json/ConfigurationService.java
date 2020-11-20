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

package org.glyptodon.guacamole.auth.json;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;

/**
 * Service for retrieving configuration information regarding the JSON
 * authentication provider.
 *
 * @author Michael Jumper
 */
public class ConfigurationService {

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * The encryption key to use for all decryption and signature verification.
     */
    private static final ByteArrayProperty JSON_SECRET_KEY = new ByteArrayProperty() {

        @Override
        public String getName() {
            return "json-secret-key";
        }

    };

    /**
     * A comma-separated list of all IP addresses or CIDR subnets which should
     * be allowed to perform authentication. If not specified, ALL address will
     * be allowed.
     */
    private static final StringListProperty JSON_TRUSTED_NETWORKS = new StringListProperty() {

        @Override
        public String getName() {
            return "json-trusted-networks";
        }

    };

    /**
     * Returns the symmetric key which will be used to encrypt and sign all
     * JSON data and should be used to decrypt and verify any received JSON
     * data. This is dictated by the "json-secret-key" property specified
     * within guacamole.properties.
     *
     * @return
     *     The key which should be used to decrypt received JSON data.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the
     *     "json-secret-key" property is missing.
     */
    public byte[] getSecretKey() throws GuacamoleException {
        return environment.getRequiredProperty(JSON_SECRET_KEY);
    }

    /**
     * Returns a collection of all IP address or CIDR subnets which should be
     * allowed to submit authentication requests. If empty, authentication
     * attempts will be allowed through without restriction.
     *
     * @return
     *     A collection of all IP address or CIDR subnets which should be
     *     allowed to submit authentication requests.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public Collection<String> getTrustedNetworks() throws GuacamoleException {
        return environment.getProperty(JSON_TRUSTED_NETWORKS, Collections.<String>emptyList());
    }

}
