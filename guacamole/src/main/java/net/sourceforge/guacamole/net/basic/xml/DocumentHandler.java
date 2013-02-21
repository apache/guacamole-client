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

import java.util.LinkedList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A simple ContentHandler implementation which digests SAX document events and
 * produces simpler tag-level events, maintaining its own stack for the
 * convenience of the tag handlers.
 *
 * @author Mike Jumper
 */
public class DocumentHandler extends DefaultHandler {

    /**
     * The name of the root element of the document.
     */
    private String rootElementName;

    /**
     * The handler which will be used to handle element events for the root
     * element of the document.
     */
    private TagHandler root;

    /**
     * The stack of all states applicable to the current parser state. Each
     * element of the stack references the TagHandler for the element being
     * parsed at that level of the document, where the current element is
     * last in the stack, and the root element is first.
     */
    private LinkedList<DocumentHandlerState> stack =
            new LinkedList<DocumentHandlerState>();

    /**
     * Creates a new DocumentHandler which will use the given TagHandler
     * to handle the root element.
     *
     * @param rootElementName The name of the root element of the document
     *                        being handled.
     * @param root The TagHandler to use for the root element.
     */
    public DocumentHandler(String rootElementName, TagHandler root) {
        this.root = root;
        this.rootElementName = rootElementName;
    }

    /**
     * Returns the current element state. The current element state is the
     * state of the element the parser is currently within.
     *
     * @return The current element state.
     */
    private DocumentHandlerState getCurrentState() {

        // If no state, return null
        if (stack.isEmpty())
            return null;

        return stack.getLast();
    }

    @Override
    public void startElement(String uri, String localName, String qName,
        Attributes attributes) throws SAXException {

        // Get current state
        DocumentHandlerState current = getCurrentState();

        // Handler for tag just read
        TagHandler handler;

        // If no stack, use root handler
        if (current == null) {

            // Validate element name
            if (!localName.equals(rootElementName))
                throw new SAXException("Root element must be '" + rootElementName + "'");

            handler = root;
        }

        // Otherwise, get handler from parent
        else {
            TagHandler parent_handler = current.getTagHandler();
            handler = parent_handler.childElement(localName, attributes);
        }

        // If no handler returned, the element was not expected
        if (handler == null)
            throw new SAXException("Unexpected element: '" + localName + "'");

        // Append new element state to stack
        stack.addLast(new DocumentHandlerState(handler));

    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {

        // Pop last element from stack
        DocumentHandlerState completed = stack.removeLast();

        // Finish element by sending text content
        completed.getTagHandler().complete(
                completed.getTextContent().toString());

    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {

        // Append received chunk to text content
        getCurrentState().getTextContent().append(ch, start, length);

    }

    /**
     * The current state of the DocumentHandler.
     */
    private static class DocumentHandlerState {

        /**
         * The current text content of the current element being parsed.
         */
        private StringBuilder textContent = new StringBuilder();

        /**
         * The TagHandler which must handle document events related to the
         * element currently being parsed.
         */
        private TagHandler tagHandler;

        /**
         * Creates a new DocumentHandlerState which will maintain the state
         * of parsing of the current element, as well as contain the TagHandler
         * which will receive events related to that element.
         *
         * @param tagHandler The TagHandler which should receive any events
         *                   related to the element being parsed.
         */
        public DocumentHandlerState(TagHandler tagHandler) {
            this.tagHandler = tagHandler;
        }

        /**
         * Returns the mutable StringBuilder which contains the current text
         * content of the element being parsed.
         *
         * @return The mutable StringBuilder which contains the current text
         *         content of the element being parsed.
         */
        public StringBuilder getTextContent() {
            return textContent;
        }

        /**
         * Returns the TagHandler which must handle any events relating to the
         * element being parsed.
         *
         * @return The TagHandler which must handle any events relating to the
         *         element being parsed.
         */
        public TagHandler getTagHandler() {
            return tagHandler;
        }

    }

}
