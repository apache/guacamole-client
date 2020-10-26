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

package org.apache.guacamole.properties;

import java.util.TimeZone;
import java.util.regex.Pattern;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;

/**
 * A GuacamoleProperty whose value is a TimeZone.
 */
public abstract class TimeZoneGuacamoleProperty
        implements GuacamoleProperty<TimeZone> {
    
    /**
     * A regex that matches valid variants of GMT timezones.
     */
    public static final Pattern GMT_REGEX =
            Pattern.compile("^GMT([+-](0|00)((:)?00)?)?$");
    
    @Override
    public TimeZone parseValue(String value) throws GuacamoleException {
        
        // Nothing in, nothing out
        if (value == null || value.isEmpty())
            return null;
        
        // Attempt to return the TimeZone of the provided string value.
        TimeZone tz = TimeZone.getTimeZone(value);
        
        // If the input is not GMT, but the output is GMT, the TimeZone is not
        // valid and we throw an exception.
        if (!GMT_REGEX.matcher(value).matches()
                && GMT_REGEX.matcher(tz.getID()).matches())
            throw new GuacamoleServerException("Property \"" + getName()
                + "\" does not specify a valid time zone.");

        return tz;
        
    }
    
}
