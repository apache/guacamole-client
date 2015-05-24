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

/**
 * An attribute which applies to a specific object.
 *
 * @author Michael Jumper
 */
public class ObjectAttribute implements Attribute {

    /**
     * The identifier of the object associated with the attribute.
     */
    private String objectIdentifier;

    /**
     * The type of value stored within this attribute.
     */
    private Type type;

    /**
     * A string which uniquely identifies this attribute.
     */
    private String identifier;

    /**
     * The value currently assigned to this attribute.
     */
    private String value;

    /**
     * Creates a new ObjectAttribute having the given identifier and type.
     * The identifier must be unique with respect to other attributes that
     * apply to the same kind of object.
     *
     * @param identifier
     *     The string which uniquely identifies this attribute.
     *
     * @param type
     *     The type of value stored within this attribute.
     */
    public ObjectAttribute(String identifier, Type type) {
        this.identifier = identifier;
        this.type = type;
    }

   /**
     * Returns the identifier of the specific object associated with this
     * attribute.
     *
     * @return
     *     The identifier of the specific object associated with this
     *     attribute.
     */
    public String getObjectIdentifier() {
        return objectIdentifier;
    }

   /**
     * Sets the identifier of the specific object associated with this
     * attribute.
     *
     * @param objectIdentifier
     *     The identifier of the specific object associated with this
     *     attribute.
     */
    public void setObjectIdentifier(String objectIdentifier) {
        this.objectIdentifier = objectIdentifier;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {

        // The identifier always uniquely identifies an attribute
        if (identifier != null)
            return identifier.hashCode();

        return 0;

    }

    @Override
    public boolean equals(Object obj) {

        // Not equal if null or wrong type
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final ObjectAttribute other = (ObjectAttribute) obj;

        // If null identifier, equality depends on whether other identifier
        // is null
        if (identifier == null)
            return other.identifier == null;

        // Otherwise, equality depends entirely on identifier
        return identifier.equals(other.identifier);

    }

}
