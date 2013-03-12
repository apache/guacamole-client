package net.sourceforge.guacamole.net.basic.xml.user_mapping;

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

import net.sourceforge.guacamole.net.basic.auth.Authorization;
import net.sourceforge.guacamole.net.basic.xml.TagHandler;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * TagHandler for the "authorize" element.
 *
 * @author Mike Jumper
 */
public class AuthorizeTagHandler implements TagHandler {

    /**
     * The Authorization corresponding to the "authorize" tag being handled
     * by this tag handler. The data of this Authorization will be populated
     * as the tag is parsed.
     */
    private Authorization authorization = new Authorization();

    /**
     * The default GuacamoleConfiguration to use if "param" or "protocol"
     * tags occur outside a "connection" tag.
     */
    private GuacamoleConfiguration default_config = null;

    @Override
    public void init(Attributes attributes) throws SAXException {

        // Init username and password
        authorization.setUsername(attributes.getValue("username"));
        authorization.setPassword(attributes.getValue("password"));

        // Get encoding
        String encoding = attributes.getValue("encoding");
        if (encoding != null) {

            // If "md5", use MD5 encoding
            if (encoding.equals("md5"))
                authorization.setEncoding(Authorization.Encoding.MD5);

            // If "plain", use plain text
            else if (encoding.equals("plain"))
                authorization.setEncoding(Authorization.Encoding.PLAIN_TEXT);

            // Otherwise, bad encoding
            else
                throw new SAXException(
                        "Invalid encoding: '" + encoding + "'");

        }

    }

    @Override
    public TagHandler childElement(String localName) throws SAXException {

        // "connection" tag
        if (localName.equals("connection")) {

            // Get tag handler for connection tag
            ConnectionTagHandler tagHandler = new ConnectionTagHandler();

            // Store configuration stub
            GuacamoleConfiguration config_stub = tagHandler.asGuacamoleConfiguration();
            authorization.addConfiguration(tagHandler.getName(), config_stub);

            return tagHandler;
        }

        // "param" tag
        if (localName.equals("param")) {

            // Create default config if it doesn't exist
            if (default_config == null) {
                default_config = new GuacamoleConfiguration();
                authorization.addConfiguration("DEFAULT", default_config);
            }

            return new ParamTagHandler(default_config);
        }

        // "protocol" tag
        if (localName.equals("protocol")) {

            // Create default config if it doesn't exist
            if (default_config == null) {
                default_config = new GuacamoleConfiguration();
                authorization.addConfiguration("DEFAULT", default_config);
            }

            return new ProtocolTagHandler(default_config);
        }

        return null;

    }

    @Override
    public void complete(String textContent) throws SAXException {
        // Do nothing
    }

    /**
     * Returns an Authorization backed by the data of this authorize tag
     * handler. This Authorization is guaranteed to at least have the username,
     * password, and encoding available. Any associated configurations will be
     * added dynamically as the authorize tag is parsed.
     *
     * @return An Authorization backed by the data of this authorize tag
     *         handler.
     */
    public Authorization asAuthorization() {
        return authorization;
    }

}
