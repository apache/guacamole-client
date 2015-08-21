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
 * Represents a time zone field. The field may contain only valid time zone IDs,
 * as dictated by TimeZone.getAvailableIDs().
 *
 * @author Michael Jumper
 */
public class TimeZoneField extends Field {

    /**
     * Creates a new TimeZoneField with the given name.
     *
     * @param name
     *     The unique name to associate with this field.
     */
    public TimeZoneField(String name) {
        super(name, Field.Type.TIMEZONE);
    }

    /**
     * Parses the given string into a time zone ID string. As these strings are
     * equivalent, the only transformation currently performed by this function
     * is to ensure that a blank time zone string is parsed into null.
     *
     * @param timeZone
     *     The time zone string to parse, which may be null.
     *
     * @return
     *     The ID of the time zone corresponding to the given string, or null
     *     if the given time zone string was null or blank.
     */
    public static String parse(String timeZone) {

        // Return null if no time zone provided
        if (timeZone == null || timeZone.isEmpty())
            return null;

        // Otherwise, assume time zone is valid
        return timeZone;

    }

}
