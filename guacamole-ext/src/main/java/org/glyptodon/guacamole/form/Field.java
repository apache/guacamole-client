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

package org.glyptodon.guacamole.form;

import java.util.Collection;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Represents an arbitrary field, such as an HTTP parameter, the parameter of a
 * remote desktop protocol, or an input field within a form. Fields are generic
 * and typed dynamically through a type string, with the semantics of the field
 * defined by the type string. The behavior of each field type is defined
 * either through the web application itself (see FormService.js) or through
 * extensions.
 *
 * @author Michael Jumper
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class Field {

    /**
     * All types of fields which are available by default. Additional field
     * types may be defined by extensions by using a unique field type name and
     * registering that name with the form service within JavaScript.
     *
     * See FormService.js.
     */
    public static class Type {

        /**
         * A text field, accepting arbitrary values.
         */
        public static String TEXT = "TEXT";

        /**
         * A username field. This field type generally behaves identically to
         * arbitrary text fields, but has semantic differences.
         */
        public static String USERNAME = "USERNAME";

        /**
         * A password field, whose value is sensitive and must be hidden.
         */
        public static String PASSWORD = "PASSWORD";

        /**
         * A numeric field, whose value must contain only digits.
         */
        public static String NUMERIC = "NUMERIC";

        /**
         * A boolean field, whose value is either blank or "true".
         */
        public static String BOOLEAN = "BOOLEAN";

        /**
         * An enumerated field, whose legal values are fully enumerated by a
         * provided, finite list.
         */
        public static String ENUM = "ENUM";

        /**
         * A text field that can span more than one line.
         */
        public static String MULTILINE = "MULTILINE";

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
    private String type;

    /**
     * A collection of all legal values of this field.
     */
    private Collection<FieldOption> options;

    /**
     * Creates a new Parameter with no associated name, title, or type.
     */
    public Field() {
    }

    /**
     * Creates a new Field with the given name, title, and type.
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
    public Field(String name, String title, String type) {
        this.name  = name;
        this.title = title;
        this.type  = type;
    }

    /**
     * Creates a new Field with the given name, title, type, and possible
     * values.
     *
     * @param name
     *     The unique name to associate with this field.
     *
     * @param title
     *     The human-readable title to associate with this field.
     *
     * @param type
     *     The type of this field.
     *
     * @param options
     *     A collection of all possible valid options for this field.
     */
    public Field(String name, String title, String type,
            Collection<FieldOption> options) {
        this.name    = name;
        this.title   = title;
        this.type    = type;
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
     * Returns the type of this field.
     *
     * @return
     *     The type of this field.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of this field.
     *
     * @param type
     *     The type of this field.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns a mutable collection of field options. Changes to this
     * collection directly affect the available options.
     *
     * @return
     *     A mutable collection of field options, or null if the field has no
     *     options.
     */
    public Collection<FieldOption> getOptions() {
        return options;
    }

    /**
     * Sets the options available as possible values of this field.
     *
     * @param options
     *     The options to associate with this field.
     */
    public void setOptions(Collection<FieldOption> options) {
        this.options = options;
    }

}
