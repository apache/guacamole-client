/*
 * Copyright (C) 2015 Glyptodon LLC
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

package org.glyptodon.guacamole.auth.jdbc.connection;

/**
 * A single parameter name/value pair belonging to a connection.
 *
 * @author Michael Jumper
 */
public class ParameterModel {

    /**
     * The identifier of the connection associated with this parameter.
     */
    private String connectionIdentifier;

    /**
     * The name of the parameter.
     */
    private String name;

    /**
     * The value the parameter is set to.
     */
    private String value;

    /**
     * Returns the identifier of the connection associated with this parameter.
     *
     * @return
     *     The identifier of the connection associated with this parameter.
     */
    public String getConnectionIdentifier() {
        return connectionIdentifier;
    }

    /**
     * Sets the identifier of the connection associated with this parameter.
     *
     * @param connectionIdentifier
     *     The identifier of the connection to associate with this parameter.
     */
    public void setConnectionIdentifier(String connectionIdentifier) {
        this.connectionIdentifier = connectionIdentifier;
    }

    /**
     * Returns the name of this parameter.
     *
     * @return
     *     The name of this parameter.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this parameter.
     *
     * @param name
     *     The name of this parameter.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the value of this parameter.
     *
     * @return
     *     The value of this parameter.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of this parameter.
     *
     * @param value
     *     The value of this parameter.
     */
    public void setValue(String value) {
        this.value = value;
    }

}
