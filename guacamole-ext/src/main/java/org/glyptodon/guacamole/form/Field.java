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
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Represents an arbitrary field, such as an HTTP parameter, the parameter of a
 * remote desktop protocol, or an input field within a form.
 *
 * @author Michael Jumper
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class Field {

    /**
     * All possible types of field.
     */
    public enum Type {

        /**
         * A text field, accepting arbitrary values.
         */
        TEXT,

        /**
         * A username field. This field type generally behaves identically to
         * arbitrary text fields, but has semantic differences.
         */
        USERNAME,

        /**
         * A password field, whose value is sensitive and must be hidden.
         */
        PASSWORD,

        /**
         * A numeric field, whose value must contain only digits.
         */
        NUMERIC,

        /**
         * A boolean field, whose value is either blank or "true".
         */
        BOOLEAN,

        /**
         * An enumerated field, whose legal values are fully enumerated by a
         * provided, finite list.
         */
        ENUM,

        /**
         * A text field that can span more than one line.
         */
        MULTILINE

    }

    /**
     * The unique name that identifies this field.
     */
    private String name;

    /**
     * A human-readable name to be presented to the user.
     */
    private String title;

    /**
     * The type of this field.
     */
    private Type type;

    /**
     * The value of this field, when checked. This is only applicable to
     * BOOLEAN fields.
     */
    private String value;

    /**
     * A collection of all associated field options.
     */
    private Collection<FieldOption> options;

    /**
     * Creates a new Parameter with no associated name, title, or type.
     */
    public Field() {
    }

    /**
     * Creates a new Parameter with the given name, title, and type.
     *
     * @param name
     *     The unique name to associate with this field.
     *
     * @param title
     *     The human-readable title to associate with this field.
     *
     * @param type
     *     The type of this field.
     */
    public Field(String name, String title, Type type) {
        this.name    = name;
        this.title   = title;
        this.type    = type;
    }

    /**
     * Creates a new BOOLEAN Parameter with the given name, title, and value.
     *
     * @param name
     *     The unique name to associate with this field.
     *
     * @param title
     *     The human-readable title to associate with this field.
     *
     * @param value
     *     The value that should be assigned to this field if enabled.
     */
    public Field(String name, String title, String value) {
        this.name    = name;
        this.title   = title;
        this.type    = Type.BOOLEAN;
        this.value   = value;
    }

    /**
     * Creates a new ENUM Parameter with the given name, title, and options.
     *
     * @param name
     *     The unique name to associate with this field.
     *
     * @param title
     *     The human-readable title to associate with this field.
     *
     * @param options
     *     A collection of all possible valid options for this field.
     */
    public Field(String name, String title, Collection<FieldOption> options) {
        this.name    = name;
        this.title   = title;
        this.type    = Type.ENUM;
        this.options = options;
    }

    /**
     * Returns the unique name associated with this field.
     *
     * @return
     *     The unique name associated with this field.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the unique name associated with this field.
     *
     * @param name
     *     The unique name to assign to this field.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the human-readable title associated with this field.
     *
     * @return
     *     The human-readable title associated with this field.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title associated with this field. The title must be a human-
     * readable string which describes accurately this field.
     *
     * @param title
     *     A human-readable string describing this field.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the value that should be assigned to this field if enabled. This
     * is only applicable to BOOLEAN fields.
     *
     * @return
     *     The value that should be assigned to this field if enabled.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value that should be assigned to this field if enabled. This is
     * only applicable to BOOLEAN fields.
     *
     * @param value
     *     The value that should be assigned to this field if enabled.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns the type of this field.
     *
     * @return
     *     The type of this field.
     */
    public Type getType() {
        return type;
    }

    /**
     * Sets the type of this field.
     *
     * @param type
     *     The type of this field.
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Returns a mutable collection of field options. Changes to this
     * collection directly affect the available options. This is only
     * applicable to ENUM fields.
     *
     * @return
     *     A mutable collection of field options, or null if the field has no
     *     options.
     */
    public Collection<FieldOption> getOptions() {
        return options;
    }

    /**
     * Sets the options available as possible values of this field. This is
     * only applicable to ENUM fields.
     *
     * @param options
     *     The options to associate with this field.
     */
    public void setOptions(Collection<FieldOption> options) {
        this.options = options;
    }

}
