
package net.sourceforge.guacamole.properties;

/*
 *  Guacamole - Clientless Remote Desktop
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

import net.sourceforge.guacamole.GuacamoleException;

public abstract class IntegerGuacamoleProperty implements GuacamoleProperty<Integer> {

    @Override
    public Integer parseValue(String value) throws GuacamoleException {

        try {
            Integer integer = new Integer(value);
            return integer;
        }
        catch (NumberFormatException e) {
            throw new GuacamoleException("Property \"" + getName() + "\" must be an integer.", e);
        }

    }

}
