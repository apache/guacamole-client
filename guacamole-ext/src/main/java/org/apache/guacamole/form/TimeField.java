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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Represents a time field. The field may contain only time values which
 * conform to a standard pattern, defined by TimeField.FORMAT.
 *
 * @author Michael Jumper
 */
public class TimeField extends Field {

    /**
     * The time format used by time fields, compatible with SimpleDateFormat.
     */
    public static final String FORMAT = "HH:mm:ss";

    /**
     * Creates a new TimeField with the given name.
     *
     * @param name
     *     The unique name to associate with this field.
     */
    public TimeField(String name) {
        super(name, Field.Type.TIME);
    }

    /**
     * Parses the given string into a corresponding time. The string must
     * follow the standard format used by time fields, as defined by
     * FORMAT and as would be produced by format().
     *
     * @param timeString
     *     The time string to parse, which may be null.
     *
     * @return
     *     The time corresponding to the given time string, or null if the
     *     provided time string was null or blank.
     *
     * @throws ParseException
     *     If the given time string does not conform to the standard format
     *     used by time fields.
     */
    public static Date parse(String timeString)
            throws ParseException {

        // Return null if no time provided
        if (timeString == null || timeString.isEmpty())
            return null;

        // Parse time according to format
        DateFormat timeFormat = new SimpleDateFormat(TimeField.FORMAT);
        return timeFormat.parse(timeString);

    }

    /**
     * Converts the given time into a string which follows the format used by
     * time fields.
     *
     * @param time
     *     The time value to format, which may be null.
     *
     * @return
     *     The formatted time, or null if the provided time was null.
     */
    public static String format(Date time) {
        DateFormat timeFormat = new SimpleDateFormat(TimeField.FORMAT);
        return time == null ? null : timeFormat.format(time);
    }

}
