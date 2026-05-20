/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.auth.restrict.form;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.guacamole.form.Field;

/**
 * A field that parses a string containing an absolute date and time value.
 */
public class DateTimeRestrictionField extends Field {

    /**
     * The field type.
     */
    public static final String FIELD_TYPE = "GUAC_DATETIME_RESTRICTION";
    
    /**
     * The format of the data for this field as it will be stored in the
     * underlying storage mechanism.
     */
    public static final String FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    
    /**
     * Create a new field that tracks time restrictions.
     * 
     * @param name
     *     The name of the parameter that will be used to pass this field
     *     between the REST API and the web front-end.
     * 
     */
    public DateTimeRestrictionField(String name) {
        super(name, FIELD_TYPE);
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
        DateFormat dateFormat = new SimpleDateFormat(DateTimeRestrictionField.FORMAT);
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
        DateFormat dateFormat = new SimpleDateFormat(DateTimeRestrictionField.FORMAT);
        return dateFormat.parse(dateString);

    }
    
}
