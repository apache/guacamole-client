package net.sourceforge.guacamole.net.basic.properties;

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

import java.util.ArrayList;
import java.util.Collection;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.basic.GuacamoleClassLoader;
import net.sourceforge.guacamole.properties.GuacamoleProperty;

/**
 * A GuacamoleProperty whose value is a comma-separated list of class names,
 * where each class will be used as a listener for events.
 * 
 * @author Michael Jumper
 */
public abstract class EventListenersProperty implements GuacamoleProperty<Collection<Class>> {

    @Override
    public Collection<Class> parseValue(String classNameList) throws GuacamoleException {

        // If no property provided, return null.
        if (classNameList == null)
            return null;

        // Parse list
        String[] classNames = classNameList.split(",[\\s]*");
        
        // Fill list of classes
        Collection<Class> listeners = new ArrayList<Class>();
        try {

            // Load all classes in list
            for (String className : classNames) {
                Class clazz = GuacamoleClassLoader.getInstance().loadClass(className);
                listeners.add(clazz);
            }

        }
        catch (ClassNotFoundException e) {
            throw new GuacamoleException("Listener class not found.", e);
        }
        catch (SecurityException e) {
            throw new GuacamoleException("Security settings prevent loading of listener class.", e);
        }

        return listeners;

    }

}

