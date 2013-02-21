
package net.sourceforge.guacamole.net.auth;

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-auth-mock.
 *
 * The Initial Developer of the Original Code is
 * Michael Jumper.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

import java.util.Date;

/**
 * A logging record describing when a user started and ended usage of a
 * particular connection.
 *
 * @author Michael Jumper
 */
public interface ConnectionRecord {

    /**
     * Returns the date and time the connection began.
     *
     * @return The date and time the connection began.
     */
    public Date getStartDate();

    /**
     * Returns the date and time the connection ended, if applicable.
     *
     * @return The date and time the connection ended, or null if the
     *         connection is still running or if the end time is unknown.
     */
    public Date getEndDate();

    /**
     * Returns the user who used or is using the connection at the times
     * given by this connection record.
     *
     * @return The user who used or is using the associated connection.
     */
    public User getUser();

    /**
     * Returns the connection associated with this record.
     *
     * @return The connection associated with this record.
     */
    public Connection getConnection();

    /**
     * Returns whether the connection associated with this record is still
     * active.
     *
     * @return true if the connection associated with this record is still
     *         active, false otherwise.
     */
    public boolean isActive();

}
