/*
 * Copyright (C) 2014 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.net.basic;

import java.util.List;
import javax.servlet.http.HttpSession;

/**
 * Request interface which provides only the functions absolutely required
 * to retrieve and connect to a tunnel.
 *
 * @author Michael Jumper
 */
public interface TunnelRequest {

    /**
     * All supported identifier types.
     */
    public static enum IdentifierType {

        /**
         * The unique identifier of a connection.
         */
        CONNECTION("c/"),

        /**
         * The unique identifier of a connection group.
         */
        CONNECTION_GROUP("g/");
        
        /**
         * The prefix which precedes an identifier of this type.
         */
        final String PREFIX;
        
        /**
         * Defines an IdentifierType having the given prefix.
         * @param prefix The prefix which will precede any identifier of this
         *               type, thus differentiating it from other identifier
         *               types.
         */
        IdentifierType(String prefix) {
            PREFIX = prefix;
        }

        /**
         * Given an identifier, determines the corresponding identifier type.
         * 
         * @param identifier The identifier whose type should be identified.
         * @return The identified identifier type.
         */
        static IdentifierType getType(String identifier) {

            // If null, no known identifier
            if (identifier == null)
                return null;

            // Connection identifiers
            if (identifier.startsWith(CONNECTION.PREFIX))
                return CONNECTION;
            
            // Connection group identifiers
            if (identifier.startsWith(CONNECTION_GROUP.PREFIX))
                return CONNECTION_GROUP;
            
            // Otherwise, unknown
            return null;
            
        }
        
    };

    /**
     * Returns the current HTTP session, or null if no session yet exists.
     *
     * @return The current HTTP session, or null if no session yet exists.
     */
    public HttpSession getSession();

    /**
     * Returns the value of the parameter having the given name.
     *
     * @param name The name of the parameter to return.
     * @return The value of the parameter having the given name, or null
     *         if no such parameter was specified.
     */
    public String getParameter(String name);

    /**
     * Returns a list of all values specified for the given parameter.
     *
     * @param name The name of the parameter to return.
     * @return All values of the parameter having the given name , or null
     *         if no such parameter was specified.
     */
    public List<String> getParameterValues(String name);
    
}
