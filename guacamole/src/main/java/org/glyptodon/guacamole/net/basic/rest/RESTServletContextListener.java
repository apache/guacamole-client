package org.glyptodon.guacamole.net.basic.rest;

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

import com.google.inject.Guice;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * A ServletContextListenr to listen for initialization of the servlet context
 * in order to set up the REST services.
 * 
 * @author James Muehlner
 */
public class RESTServletContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Guice.createInjector(
            new RESTServletModule(), 
            new RESTModule()
        );
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}
    
}
