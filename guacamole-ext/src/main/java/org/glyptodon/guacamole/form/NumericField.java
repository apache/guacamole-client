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

package org.glyptodon.guacamole.form;

/**
 * Represents a field which may contain only integer values.
 *
 * @author Michael Jumper
 */
public class NumericField extends Field {

    /**
     * Creates a new NumericField with the given name.
     *
     * @param name
     *     The unique name to associate with this field.
     */
    public NumericField(String name) {
        super(name, Field.Type.NUMERIC);
    }

    /**
     * Formats the given integer in the format required by a numeric field.
     *
     * @param i
     *     The integer to format, which may be null.
     *
     * @return
     *     A string representation of the given integer, or null if the given
     *     integer was null.
     */
    public static String format(Integer i) {

        if (i == null)
            return null;

        return i.toString();

    }

    /**
     * Parses the given string as an integer, where the given string is in the
     * format required by a numeric field.
     *
     * @param str
     *     The string to parse as an integer, which may be null.
     *
     * @return
     *     The integer representation of the given string, or null if the given
     *     string was null.
     *
     * @throws NumberFormatException
     *     If the given string is not in a parseable format.
     */
    public static Integer parse(String str) throws NumberFormatException {

        if (str == null || str.isEmpty())
            return null;

        return new Integer(str);

    }

}
