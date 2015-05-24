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

package org.glyptodon.guacamole.net.auth.attribute;

import org.glyptodon.guacamole.net.auth.Identifiable;

/**
 * An arbitrary attribute. Each attribute associates an identifier with a
 * value, essentially a key/value pair, where the type dictates the semantics
 * and legal values of the attribute.
 *
 * @author Michael Jumper
 */
public interface Attribute extends Identifiable {

    /**
     * Specific types of attributes. Each attribute type describes the kind of
     * values the attribute can accept, and defines any semantics associated
     * with those values.
     */
    public enum Type {

        /**
         * A text attribute, accepting arbitrary values.
         */
        TEXT,

        /**
         * A username attribute. This attribute type generally behaves
         * identically to arbitrary text attributes, but has semantic
         * differences.
         */
        USERNAME,

        /**
         * A password attribute, whose value is sensitive and must be hidden.
         */
        PASSWORD,

        /**
         * A numeric attribute, whose value must contain only digits.
         */
        NUMERIC,

        /**
         * A boolean attribute, whose value is either blank or "true".
         */
        BOOLEAN,

        /**
         * An enumerated attribute, whose legal values are fully enumerated
         * by a provided, finite list.
         */
        ENUM,

        /**
         * A text attribute that can span more than one line.
         */
        MULTILINE

    }

    /**
     * Returns the type of this attribute. The attribute type dictates the kind
     * of values the attribute can contain, and the semantics of the attribute
     * as a whole.
     *
     * @return
     *     The type of this attribute.
     */
    Type getType();

    /**
     * Sets the type of this attribute. Attribute type dictates the kind of
     * values the attribute can contain, and the semantics of the attribute as
     * a whole.
     *
     * @param type
     *     The type to associate with this attribute.
     */
    void setType(Type type);

    /**
     * Returns the value currently associated with this attribute, if any. The
     * values acceptable by this attribute are dictated by the type.
     *
     * @return
     *     The value currently associated with this attribute, or null if no
     *     value is present.
     */
    String getValue();

    /**
     * Sets the value currently associated with this attribute, if any. The
     * values acceptable by this attribute are dictated by the type.
     *
     * @param value
     *     The value to associate with this attribute.
     */
    void setValue(String value);

}
