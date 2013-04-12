package net.sourceforge.guacamole.net.basic.xml.protocol;

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

import net.sourceforge.guacamole.net.basic.ProtocolParameter;
import net.sourceforge.guacamole.net.basic.xml.TagHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * TagHandler for the "param" element.
 * 
 * @author Mike Jumper 
 */
public class ParamTagHandler implements TagHandler {

    /**
     * The ProtocolParameter backing this tag handler.
     */
    private ProtocolParameter protocolParameter = new ProtocolParameter();
    
    @Override
    public void init(Attributes attributes) throws SAXException {

        protocolParameter.setName(attributes.getValue("name"));
        protocolParameter.setTitle(attributes.getValue("title"));
        protocolParameter.setValue(attributes.getValue("value"));

        // Parse type
        String type = attributes.getValue("type");

        // Text field
        if ("text".equals(type))
            protocolParameter.setType(ProtocolParameter.Type.TEXT);

        // Numeric field
        else if ("numeric".equals(type))
            protocolParameter.setType(ProtocolParameter.Type.NUMERIC);

        // Password field
        else if ("password".equals(type))
            protocolParameter.setType(ProtocolParameter.Type.PASSWORD);

        // Enumerated field
        else if ("enum".equals(type))
            protocolParameter.setType(ProtocolParameter.Type.ENUM);

        // Boolean field
        else if ("boolean".equals(type))
            protocolParameter.setType(ProtocolParameter.Type.BOOLEAN);

        // Otherwise, fail with unrecognized type
        else
            throw new SAXException("Invalid parameter type: " + type);
        
    }
    
    @Override
    public TagHandler childElement(String localName) throws SAXException {

        // Start parsing of option tags 
        if (localName.equals("option")) {
           
            // Get tag handler for option tag
            OptionTagHandler tagHandler = new OptionTagHandler();

            // Store stub in options collection
            protocolParameter.getOptions().add(
                tagHandler.asProtocolParameterOption());
            return tagHandler;
            
        }

        return null;

    }

    @Override
    public void complete(String textContent) throws SAXException {
        // Do nothing
    }

    /**
     * Returns the ProtocolParameter backing this tag.
     * @return The ProtocolParameter backing this tag.
     */
    public ProtocolParameter asProtocolParameter() {
        return protocolParameter;
    }

}
