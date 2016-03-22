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

package org.apache.guacamole.form;

import java.util.Collections;

/**
 * Represents a field with strictly one possible value. It is assumed that the
 * field may be blank, but that its sole non-blank value is the value provided.
 * The provided value represents "true" while all other values, including
 * having no associated value, represent "false".
 *
 * @author Michael Jumper
 */
public class BooleanField extends Field {

    /**
     * Creates a new BooleanField with the given name and truth value. The
     * truth value is the value that, when assigned to this field, means that
     * this field is "true".
     *
     * @param name
     *     The unique name to associate with this field.
     *
     * @param truthValue
     *     The value to consider "true" for this field. All other values will
     *     be considered "false".
     */
    public BooleanField(String name, String truthValue) {
        super(name, Field.Type.BOOLEAN, Collections.singletonList(truthValue));
    }

}
