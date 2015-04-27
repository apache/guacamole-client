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

package org.glyptodon.guacamole.net.auth.credentials;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.glyptodon.guacamole.form.Parameter;

/**
 * Information which describes a set of valid credentials.
 *
 * @author Michael Jumper
 */
public class CredentialsInfo {

    /**
     * All parameters required for valid credentials.
     */
    private final Collection<Parameter> parameters;

    /**
     * Creates a new CredentialsInfo object which requires the given parameters
     * for any conforming credentials.
     *
     * @param parameters
     *     The parameters to require.
     */
    public CredentialsInfo(Collection<Parameter> parameters) {
        this.parameters = parameters;
    }
    
    /**
     * Returns all parameters required for valid credentials as described by
     * this object.
     *
     * @return
     *     All parameters required for valid credentials.
     */
    public Collection<Parameter> getParameters() {
        return Collections.unmodifiableCollection(parameters);
    }

    /**
     * CredentialsInfo object which describes empty credentials. No parameters
     * are required.
     */
    public static final CredentialsInfo EMPTY = new CredentialsInfo(Collections.<Parameter>emptyList());

    /**
     * CredentialsInfo object which describes standard username/password
     * credentials.
     */
    public static final CredentialsInfo USERNAME_PASSWORD = new CredentialsInfo(Arrays.asList(
        new Parameter("username", "username", Parameter.Type.USERNAME),
        new Parameter("password", "password", Parameter.Type.PASSWORD)
    ));
    
}
