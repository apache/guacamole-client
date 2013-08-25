package org.glyptodon.guacamole.net.basic.xml.protocol;

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

import org.glyptodon.guacamole.net.basic.ProtocolInfo;
import org.glyptodon.guacamole.net.basic.xml.TagHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * TagHandler for the "protocol" element.
 *
 * @author Mike Jumper
 */
public class ProtocolTagHandler implements TagHandler {

    /**
     * The ProtocolInfo object which will contain all data parsed by this tag
     * handler.
     */
    private ProtocolInfo info = new ProtocolInfo();

    @Override
    public void init(Attributes attributes) throws SAXException {
        info.setName(attributes.getValue("name"));
        info.setTitle(attributes.getValue("title"));
    }

    @Override
    public TagHandler childElement(String localName) throws SAXException {

        // Start parsing of param tags, add to list of all parameters
        if (localName.equals("param")) {

            // Get tag handler for param tag
            ParamTagHandler tagHandler = new ParamTagHandler();

            // Store stub in parameters collection
            info.getParameters().add(tagHandler.asProtocolParameter());
            return tagHandler;

        }

        return null;

    }

    @Override
    public void complete(String textContent) throws SAXException {
        // Do nothing
    }

    /**
     * Returns the ProtocolInfo backing this tag.
     * @return The ProtocolInfo backing this tag.
     */
    public ProtocolInfo asProtocolInfo() {
        return info;
    }

}
