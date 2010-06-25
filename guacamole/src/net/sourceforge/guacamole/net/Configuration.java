
package net.sourceforge.guacamole.net;

/*
 *  Guacamole - Pure JavaScript/HTML VNC Client
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import javax.servlet.ServletContext;

public abstract class Configuration {

    private ServletContext context;

    protected String humanReadableList(Object... values) {

        String list = "";
        for (int i=0; i<values.length; i++) {

            if (i >= 1)
                list += ", ";

            if (i == values.length -1)
                list += " or ";

            list += "\"" + values[i] + "\"";
        }

        return list;

    }

    protected String readParameter(String name, String defaultValue, String... allowedValues) throws GuacamoleException {

        String value = context.getInitParameter(name);

        // Use default if not specified
        if (value == null) {
            if (defaultValue == null)
                throw new GuacamoleException("Parameter \"" + name + "\" is required.");

            return defaultValue;
        }

        // If not restricted to certain values, just return whatever is given.
        if (allowedValues.length == 0)
            return value;

        // If restricted, only return value within given list
        for (String allowedValue : allowedValues)
            if (value.equals(allowedValue))
                return value;

        throw new GuacamoleException("Parameter \"" + name + "\" must be " + humanReadableList((Object) allowedValues));
    }

    protected boolean readBooleanParameter(String name, Boolean defaultValue) throws GuacamoleException {

        String value = context.getInitParameter(name);

        // Use default if not specified
        if (value == null) {
            if (defaultValue == null)
                throw new GuacamoleException("Parameter \"" + name + "\" is required.");

            return defaultValue;
        }

        value = value.trim();
        if (value.equals("true"))
            return true;

        if (value.equals("false"))
            return false;

        throw new GuacamoleException("Parameter \"" + name + "\" must be \"true\" or \"false\".");

    }

    protected int readIntParameter(String name, Integer defaultValue, Integer... allowedValues) throws GuacamoleException {

        String parmString = context.getInitParameter(name);

        // Use default if not specified
        if (parmString== null) {
            if (defaultValue == null)
                throw new GuacamoleException("Parameter \"" + name + "\" is required.");

            return defaultValue;
        }

        try {
            int value = Integer.parseInt(parmString);

            // If not restricted to certain values, just return whatever is given.
            if (allowedValues.length == 0)
                return value;

            // If restricted, only return value within given list
            for (int allowedValue : allowedValues)
                if (value == allowedValue)
                    return value;

            throw new GuacamoleException("Parameter \"" + name + "\" must be " + humanReadableList((Object) allowedValues));
        }
        catch (NumberFormatException e) {
            throw new GuacamoleException("Parameter \"" + name + "\" must be an integer.", e);
        }

    }

    public Configuration(ServletContext context) {
        this.context = context;
    }

}
