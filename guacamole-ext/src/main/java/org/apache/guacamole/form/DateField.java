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
 * Represents a date field. The field may contain only date values which
 * conform to a standard pattern, defined by DateField.FORMAT.
 *
 * @author Michael Jumper
 */
public class DateField extends Field {

    /**
     * The date format used by date fields, compatible with SimpleDateFormat.
     */
    public static final String FORMAT = "yyyy-MM-dd";

    /**
     * Creates a new DateField with the given name.
     *
     * @param name
     *     The unique name to associate with this field.
     */
    public DateField(String name) {
        super(name, Field.Type.DATE);
    }

    /**
     * Converts the given date into a string which follows the format used by
     * date fields.
     *
     * @param date
     *     The date value to format, which may be null.
     *
     * @return
     *     The formatted date, or null if the provided time was null.
     */
    public static String format(Date date) {
        DateFormat dateFormat = new SimpleDateFormat(DateField.FORMAT);
        return date == null ? null : dateFormat.format(date);
    }

    /**
     * Parses the given string into a corresponding date. The string must
     * follow the standard format used by date fields, as defined by FORMAT
     * and as would be produced by format().
     *
     * @param dateString
     *     The date string to parse, which may be null.
     *
     * @return
     *     The date corresponding to the given date string, or null if the
     *     provided date string was null or blank.
     *
     * @throws ParseException
     *     If the given date string does not conform to the standard format
     *     used by date fields.
     */
    public static Date parse(String dateString)
            throws ParseException {

        // Return null if no date provided
        if (dateString == null || dateString.isEmpty())
            return null;

        // Parse date according to format
        DateFormat dateFormat = new SimpleDateFormat(DateField.FORMAT);
        return dateFormat.parse(dateString);

    }

}
