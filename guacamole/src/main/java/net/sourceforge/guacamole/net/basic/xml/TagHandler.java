package net.sourceforge.guacamole.net.basic.xml;

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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A simple element-level event handler for events triggered by the
 * SAX-driven DocumentHandler parser.
 *
 * @author Mike Jumper
 */
public interface TagHandler {

    /**
     * Called when a child element of the current element is parsed.
     *
     * @param localName The local name of the child element seen.
     * @return The TagHandler which should handle all element-level events
     *         related to the child element.
     * @throws SAXException If the child element being parsed was not expected,
     *                      or some other error prevents a proper TagHandler
     *                      from being constructed for the child element.
     */
    public TagHandler childElement(String localName)
            throws SAXException;

    /**
     * Called when the element corresponding to this TagHandler is first seen,
     * just after an instance is created.
     *
     * @param attributes The attributes of the element seen.
     * @throws SAXException If an error prevents a the TagHandler from being
     *                      from being initialized.
     */
    public void init(Attributes attributes) throws SAXException;

    /**
     * Called when this element, and all child elements, have been fully parsed,
     * and the entire text content of this element (if any) is available.
     *
     * @param textContent The full text content of this element, if any.
     * @throws SAXException If the text content received is not valid for any
     *                      reason, or the child elements parsed are not
     *                      correct.
     */
    public void complete(String textContent) throws SAXException;

}
