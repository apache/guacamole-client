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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.properties.GuacamoleProperty;

/**
 * A GuacamoleProperty whose value is a List of Strings. The string value
 * parsed to produce this list is a comma-delimited list. Duplicate values are
 * ignored, as is any whitespace following delimiters. To maintain
 * compatibility with the behavior of Java properties in general, only
 * whitespace at the beginning of each value is ignored; trailing whitespace
 * becomes part of the value.
 *
 * @author Michael Jumper
 */
public abstract class StringListProperty implements GuacamoleProperty<List<String>> {

    /**
     * A pattern which matches against the delimiters between values. This is
     * currently simply a comma and any following whitespace. Parts of the
     * input string which match this pattern will not be included in the parsed
     * result.
     */
    private static final Pattern DELIMITER_PATTERN = Pattern.compile(",\\s*");

    @Override
    public List<String> parseValue(String values) throws GuacamoleException {

        // If no property provided, return null.
        if (values == null)
            return null;

        // Split string into a list of individual values
        return Arrays.asList(DELIMITER_PATTERN.split(values));

    }

}
