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

package org.glyptodon.guacamole.auth.ldap;

import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleServerException;
import org.glyptodon.guacamole.properties.GuacamoleProperty;

/**
 * A GuacamoleProperty whose value is an EncryptionMethod. The string values
 * "none", "ssl", and "starttls" are each parsed to their corresponding values
 * within the EncryptionMethod enum. All other string values result in parse
 * errors.
 *
 * @author Michael Jumper
 */
public abstract class EncryptionMethodProperty implements GuacamoleProperty<EncryptionMethod> {

    @Override
    public EncryptionMethod parseValue(String value) throws GuacamoleException {

        // If no value provided, return null.
        if (value == null)
            return null;

        // Plaintext (no encryption)
        if (value.equals("none"))
            return EncryptionMethod.NONE;

        // SSL
        if (value.equals("ssl"))
            return EncryptionMethod.SSL;

        // STARTTLS
        if (value.equals("starttls"))
            return EncryptionMethod.STARTTLS;

        // The provided value is not legal
        throw new GuacamoleServerException("Encryption method must be one of \"none\", \"ssl\", or \"starttls\".");

    }

}
