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

package org.glyptodon.guacamole.net.basic.rest.permission;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.glyptodon.guacamole.net.auth.permission.ConnectionGroupPermission;
import org.glyptodon.guacamole.net.auth.permission.ConnectionPermission;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.auth.permission.Permission;
import org.glyptodon.guacamole.net.auth.permission.SystemPermission;
import org.glyptodon.guacamole.net.auth.permission.UserPermission;

/**
 * A simple user permission to expose through the REST endpoints.
 * 
 * @author James Muehlner
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class APIPermission {
    
    /**
     * Create an empty APIPermission.
     */
    public APIPermission() {}
    
    /**
     * The type of object that this permission refers to.
     */
    private ObjectType objectType;
    
    /**
     * The type of object that a permission can refer to.
     */
    public enum ObjectType {
        CONNECTION,
        CONNECTION_GROUP,
        USER,
        SYSTEM
    }
    
    /**
     * The identifier of the object that this permission refers to.
     */
    private String objectIdentifier;
    
    /**
     * The object permission type for this APIPermission, if relevant. This is
     * only used if this.objectType is CONNECTION, CONNECTION_GROUP, or USER.
     */
    private ObjectPermission.Type objectPermissionType;
    
    /**
     * The system permission type for this APIPermission, if relevant. This is
     * only used if this.objectType is SYSTEM.
     */
    private SystemPermission.Type systemPermissionType;
    
    /**
     * Create an APIConnection from a Connection record.
     * 
     * @param permission The permission to create this APIPermission from.
     */
    public APIPermission(Permission permission) {
        if(permission instanceof ConnectionPermission) {
            this.objectType = ObjectType.CONNECTION;
            
            this.objectPermissionType = ((ConnectionPermission) permission).getType();
            this.objectIdentifier = ((ConnectionPermission) permission).getObjectIdentifier();
        } else if(permission instanceof ConnectionGroupPermission) {
            this.objectType = ObjectType.CONNECTION_GROUP;
            
            this.objectPermissionType = ((ConnectionGroupPermission) permission).getType();
            this.objectIdentifier = ((ConnectionGroupPermission) permission).getObjectIdentifier();
        } else if(permission instanceof UserPermission) {
            this.objectType = ObjectType.USER;
            
            this.objectPermissionType = ((UserPermission) permission).getType();
            this.objectIdentifier = ((UserPermission) permission).getObjectIdentifier();
        } else if(permission instanceof SystemPermission) {
            this.objectType = ObjectType.SYSTEM;
            
            this.systemPermissionType = ((SystemPermission) permission).getType();
        }
    }

    /**
     * Returns the type of object that this permission refers to.
     * 
     * @return The type of object that this permission refers to. 
     */
    public ObjectType getObjectType() {
        return objectType;
    }

    /**
     * Set the type of object that this permission refers to.
     * @param objectType The type of object that this permission refers to. 
     */
    public void setObjectType(ObjectType objectType) {
        this.objectType = objectType;
    }

    /**
     * Returns a string representation of the permission type.
     * If this.objectType is CONNECTION, CONNECTION_GROUP, or USER, this will be
     * the string representation of the objectPermissionType.
     * If this.objectType is SYSTEM, this will be the string representation of
     * the systemPermissionType.
     * 
     * @return A string representation of the permission type. 
     */
    public String getPermissionType() {
        switch(this.objectType) {
            case CONNECTION:
            case CONNECTION_GROUP:
            case USER:
                return this.objectPermissionType.toString();
            case SYSTEM:
                return this.systemPermissionType.toString();
            default:
                return null;
        } 
    }

    /**
     * Set the permission type from a string representation of that type.
     * Since it's not clear at this point whether this is an object permission or
     * system permission, try to set both of them.
     * 
     * @param permissionType The string representation of the permission type.
     */
    public void setPermissionType(String permissionType) {
        try {
            this.objectPermissionType = ObjectPermission.Type.valueOf(permissionType);
        } catch(IllegalArgumentException e) {}
        
        try {
            this.systemPermissionType = SystemPermission.Type.valueOf(permissionType);
        } catch(IllegalArgumentException e) {}
    }

    /**
     * Returns the identifier of the object that this permission refers to.
     * 
     * @return The identifier of the object that this permission refers to. 
     */
    public String getObjectIdentifier() {
        return objectIdentifier;
    }

    /**
     * Set the identifier of the object that this permission refers to.
     * 
     * @param objectIdentifier The identifier of the object that this permission refers to. 
     */
    public void setObjectIdentifier(String objectIdentifier) {
        this.objectIdentifier = objectIdentifier;
    }
    
    /**
     * Returns an org.glyptodon.guacamole.net.auth.permission.Permission 
     * representation of this APIPermission.
     * 
     * @return An org.glyptodon.guacamole.net.auth.permission.Permission
     * representation of this APIPermission.
     */
    public Permission toPermission() {
        switch(this.objectType) {
            case CONNECTION:
                return new ConnectionPermission
                        (this.objectPermissionType, this.objectIdentifier);
            case CONNECTION_GROUP:
                return new ConnectionGroupPermission
                        (this.objectPermissionType, this.objectIdentifier);
            case USER:
                return new UserPermission
                        (this.objectPermissionType, this.objectIdentifier);
            case SYSTEM:
                return new SystemPermission(this.systemPermissionType);
            default:
                return null;
        }
    }
}
