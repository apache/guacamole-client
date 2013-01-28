package net.sourceforge.guacamole.net.basic;

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

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.GuacamoleConfigurationDirectory;
import net.sourceforge.guacamole.net.auth.UserContext;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;

/**
 * Simple HttpServlet which outputs XML containing a list of all authorized
 * configurations for the current user.
 *
 * @author Michael Jumper
 */
public class ConfigurationList extends AuthenticatingHttpServlet {

    @Override
    protected void authenticatedService(
            UserContext context,
            HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {

        // Do not cache
        response.setHeader("Cache-Control", "no-cache");

        // Write XML content type
        response.setHeader("Content-Type", "text/xml");

        // Attempt to get configurations
        Map<String, GuacamoleConfiguration> configs;
        try {

            // Get configuration directory
            GuacamoleConfigurationDirectory directory =
                context.getGuacamoleConfigurationDirectory();
            
            // Get configurations
            configs = directory.getConfigurations();

        }
        catch (GuacamoleException e) {
            throw new ServletException("Unable to retrieve configurations.", e);
        }
        
        // Write actual XML
        try {

            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xml = outputFactory.createXMLStreamWriter(response.getWriter());

            // Begin document
            xml.writeStartDocument();
            xml.writeStartElement("configs");
            
            // For each entry, write corresponding config element
            for (Entry<String, GuacamoleConfiguration> entry : configs.entrySet()) {

                // Get config
                GuacamoleConfiguration config = entry.getValue();

                // Write config
                xml.writeEmptyElement("config");
                xml.writeAttribute("id", entry.getKey());
                xml.writeAttribute("protocol", config.getProtocol());

            }

            // End document
            xml.writeEndElement();
            xml.writeEndDocument();

        }
        catch (XMLStreamException e) {
            throw new IOException("Unable to write configuration list XML.", e);
        }

    }

}

