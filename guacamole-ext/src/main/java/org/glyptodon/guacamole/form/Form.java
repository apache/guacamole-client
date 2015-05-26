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

import java.util.ArrayList;
import java.util.Collection;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Information which describes logical set of fields.
 *
 * @author Michael Jumper
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class Form {

    /**
     * The name of this form. The form name must identify the form uniquely
     * from other forms.
     */
    private String name;

    /**
     * The a human-readable title describing this form.
     */
    private String title;

    /**
     * All fields associated with this form.
     */
    private Collection<Field> fields;

    /**
     * Creates a new Form object with no associated fields. The name and title
     * of the form are left unset as null. If no form name is provided, this
     * form must not be used in the same context as another unnamed form.
     */
    public Form() {
        fields = new ArrayList<Field>();
    }

    /**
     * Creates a new Form object having the given name and title, and
     * containing the given fields.
     *
     * @param name
     *     A name which uniquely identifies this form.
     *
     * @param title
     *     A human-readable title describing this form.
     *
     * @param fields
     *     The fields to provided within the new Form.
     */
    public Form(String name, String title, Collection<Field> fields) {
        this.fields = fields;
    }

    /**
     * Returns a mutable collection of the fields associated with this form.
     * Changes to this collection affect the fields exposed to the user.
     *
     * @return
     *     A mutable collection of fields.
     */
    public Collection<Field> getFields() {
        return fields;
    }

    /**
     * Sets the collection of fields associated with this form.
     *
     * @param fields
     *     The collection of fields to associate with this form.
     */
    public void setFields(Collection<Field> fields) {
        this.fields = fields;
    }

    /**
     * Returns the name of this form. Form names must uniquely identify each
     * form.
     *
     * @return
     *     The name of this form, or null if the form has no name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this form. Form names must uniquely identify each form.
     *
     * @param name
     *     The name to assign to this form.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the human-readable title associated with this form. A form's
     * title describes the form, but need not be unique.
     *
     * @return
     *     A human-readable title describing this form.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the human-readable title associated with this form. A form's title
     * describes the form, but need not be unique.
     *
     * @param title
     *     A human-readable title describing this form.
     */
    public void setTitle(String title) {
        this.title = title;
    }

}
