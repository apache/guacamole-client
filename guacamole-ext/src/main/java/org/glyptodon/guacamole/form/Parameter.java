/*
 * Copyright (C) 2013 Glyptodon LLC
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

package org.glyptodon.guacamole.form;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents an arbitrary parameter, such as an HTTP parameter, or the
 * parameter of a remote desktop protocol.
 *
 * @author Michael Jumper
 */
public class Parameter {

    /**
     * All possible types of parameter.
     */
    public enum Type {

        /**
         * A text parameter, accepting arbitrary values.
         */
        TEXT,

        /**
         * A username parameter. This parameter type generally behaves
         * identically to arbitrary text parameters, but has semantic
         * differences. If credential pass-through is in use, the value for this
         * parameter may be automatically provided using the credentials
         * originally used by the user to authenticate.
         */
        USERNAME,

        /**
         * A password parameter, whose value is sensitive and must be hidden. If
         * credential pass-through is in use, the value for this parameter may
         * be automatically provided using the credentials originally used by
         * the user to authenticate.
         */
        PASSWORD,

        /**
         * A numeric parameter, whose value must contain only digits.
         */
        NUMERIC,

        /**
         * A boolean parameter, whose value is either blank or "true".
         */
        BOOLEAN,

        /**
         * An enumerated parameter, whose legal values are fully enumerated
         * by a provided, finite list.
         */
        ENUM,

        /**
         * A text parameter that can span more than one line.
         */
        MULTILINE

    }

    /**
     * The unique name that identifies this parameter.
     */
    private String name;

    /**
     * A human-readable name to be presented to the user.
     */
    private String title;

    /**
     * The type of this parameter.
     */
    private Type type;

    /**
     * The value of this parameter, when checked. This is only applicable to
     * BOOLEAN parameters.
     */
    private String value;

    /**
     * A collection of all associated parameter options.
     */
    private Collection<ParameterOption> options;

    /**
     * Creates a new Parameter with no associated name, title, or type.
     */
    public Parameter() {
        this.options = new ArrayList<ParameterOption>();
    }

    /**
     * Creates a new Parameter with the given name, title, and type.
     *
     * @param name
     *     The unique name to associate with this parameter.
     *
     * @param title
     *     The human-readable title to associate with this parameter.
     *
     * @param type
     *     The type of this parameter.
     */
    public Parameter(String name, String title, Type type) {
        this.name    = name;
        this.title   = title;
        this.type    = type;
        this.options = new ArrayList<ParameterOption>();
    }

    /**
     * Creates a new BOOLEAN Parameter with the given name, title, and value.
     *
     * @param name
     *     The unique name to associate with this parameter.
     *
     * @param title
     *     The human-readable title to associate with this parameter.
     *
     * @param value
     *     The value that should be assigned to this parameter if enabled.
     */
    public Parameter(String name, String title, String value) {
        this.name    = name;
        this.title   = title;
        this.type    = Type.BOOLEAN;
        this.value   = value;
        this.options = new ArrayList<ParameterOption>();
    }

    /**
     * Creates a new ENUM Parameter with the given name, title, and options.
     *
     * @param name
     *     The unique name to associate with this parameter.
     *
     * @param title
     *     The human-readable title to associate with this parameter.
     *
     * @param options
     *     A collection of all possible valid options for this parameter.
     */
    public Parameter(String name, String title, Collection<ParameterOption> options) {
        this.name    = name;
        this.title   = title;
        this.type    = Type.ENUM;
        this.options = options;
    }

    /**
     * Returns the unique name associated with this parameter.
     *
     * @return
     *     The unique name associated with this parameter.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the unique name associated with this parameter.
     *
     * @param name
     *     The unique name to assign to this parameter.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the human-readable title associated with this parameter.
     *
     * @return
     *     The human-readable title associated with this parameter.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title associated with this parameter. The title must be a
     * human-readable string which describes accurately this parameter.
     *
     * @param title
     *     A human-readable string describing this parameter.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the value that should be assigned to this parameter if enabled.
     * This is only applicable to BOOLEAN parameters.
     *
     * @return
     *     The value that should be assigned to this parameter if enabled.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value that should be assigned to this parameter if enabled.
     * This is only applicable to BOOLEAN parameters.
     *
     * @param value
     *     The value that should be assigned to this parameter if enabled.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns the type of this parameter.
     *
     * @return
     *     The type of this parameter.
     */
    public Type getType() {
        return type;
    }

    /**
     * Sets the type of this parameter.
     *
     * @param type
     *     The type of this parameter.
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Returns a mutable collection of parameter options. Changes to this
     * collection directly affect the available options. This is only
     * applicable to ENUM parameters.
     *
     * @return
     *     A mutable collection of parameter options.
     */
    public Collection<ParameterOption> getOptions() {
        return options;
    }

    /**
     * Sets the options available as possible values of this parameter. This
     * is only applicable to ENUM parameters.
     *
     * @param options
     *     The options to associate with this parameter.
     */
    public void setOptions(Collection<ParameterOption> options) {
        this.options = options;
    }

}
