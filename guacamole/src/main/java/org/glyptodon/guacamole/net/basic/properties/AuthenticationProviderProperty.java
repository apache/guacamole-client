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

package org.glyptodon.guacamole.net.basic.properties;

import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.properties.GuacamoleProperty;

/**
 * A GuacamoleProperty whose value is the name of a class to use to
 * authenticate users. This class must implement AuthenticationProvider. Use
 * of this property type is deprecated in favor of the
 * GUACAMOLE_HOME/extensions directory.
 *
 * @author Michael Jumper
 */
@Deprecated
public abstract class AuthenticationProviderProperty implements GuacamoleProperty<Class<AuthenticationProvider>> {

    @Override
    @SuppressWarnings("unchecked") // Explicitly checked within by isAssignableFrom()
    public Class<AuthenticationProvider> parseValue(String authProviderClassName) throws GuacamoleException {

        // If no property provided, return null.
        if (authProviderClassName == null)
            return null;

        // Get auth provider instance
        try {

            // Get authentication provider class
            Class<?> authProviderClass = org.glyptodon.guacamole.net.basic.GuacamoleClassLoader.getInstance().loadClass(authProviderClassName);

            // Verify the located class is actually a subclass of AuthenticationProvider
            if (!AuthenticationProvider.class.isAssignableFrom(authProviderClass))
                throw new GuacamoleException("Specified authentication provider class is not a AuthenticationProvider.");

            // Return located class
            return (Class<AuthenticationProvider>) authProviderClass;

        }
        catch (ClassNotFoundException e) {
            throw new GuacamoleException("Authentication provider class not found", e);
        }

    }

}
