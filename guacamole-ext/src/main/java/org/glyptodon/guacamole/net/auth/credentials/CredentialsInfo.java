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
import org.glyptodon.guacamole.form.Field;

/**
 * Information which describes a set of valid credentials.
 *
 * @author Michael Jumper
 */
public class CredentialsInfo {

    /**
     * All fields required for valid credentials.
     */
    private final Collection<Field> fields;

    /**
     * Creates a new CredentialsInfo object which requires the given fields for
     * any conforming credentials.
     *
     * @param fields
     *     The fields to require.
     */
    public CredentialsInfo(Collection<Field> fields) {
        this.fields = fields;
    }
    
    /**
     * Returns all fields required for valid credentials as described by this
     * object.
     *
     * @return
     *     All fields required for valid credentials.
     */
    public Collection<Field> getFields() {
        return Collections.unmodifiableCollection(fields);
    }

    /**
     * CredentialsInfo object which describes empty credentials. No fields are
     * required.
     */
    public static final CredentialsInfo EMPTY = new CredentialsInfo(Collections.<Field>emptyList());

    /**
     * CredentialsInfo object which describes standard username/password
     * credentials.
     */
    public static final CredentialsInfo USERNAME_PASSWORD = new CredentialsInfo(Arrays.asList(
        new Field("username", "username", Field.Type.USERNAME),
        new Field("password", "password", Field.Type.PASSWORD)
    ));
    
}
