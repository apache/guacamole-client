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
package org.apache.guacamole.rest.user;

/**
 * All the information necessary for the password update operation on a user.
 * 
 * @author James Muehlner
 */
public class APIUserPasswordUpdate {
    
    /**
     * The old (current) password of this user.
     */
    private String oldPassword;
    
    /**
     * The new password of this user.
     */
    private String newPassword;

    /**
     * Returns the old password for this user. This password must match the
     * user's current password for the password update operation to succeed.
     *
     * @return
     *     The old password for this user.
     */
    public String getOldPassword() {
        return oldPassword;
    }

    /**
     * Set the old password for this user. This password must match the
     * user's current password for the password update operation to succeed.
     *
     * @param oldPassword
     *     The old password for this user.
     */
    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    /**
     * Returns the new password that will be assigned to this user.
     *
     * @return
     *     The new password for this user.
     */
    public String getNewPassword() {
        return newPassword;
    }

    /**
     * Set the new password that will be assigned to this user.
     *
     * @param newPassword
     *     The new password for this user.
     */
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
