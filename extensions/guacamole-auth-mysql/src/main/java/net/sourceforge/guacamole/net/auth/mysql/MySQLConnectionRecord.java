
package net.sourceforge.guacamole.net.auth.mysql;

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
 * The Original Code is guacamole-auth-mysql.
 *
 * The Initial Developer of the Original Code is
 * James Muehlner.
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
import net.sourceforge.guacamole.net.auth.ConnectionRecord;

/**
 * A ConnectionRecord which is based on data stored in MySQL.
 *
 * @author James Muehlner
 */
public class MySQLConnectionRecord implements ConnectionRecord {

    /**
     * The start date of the ConnectionRecord.
     */
    private Date startDate;

    /**
     * The end date of the ConnectionRecord.
     */
    private Date endDate;

    /**
     * The name of the user that is associated with this ConnectionRecord.
     */
    private String username;

    /**
     * Initialize this MySQLConnectionRecord with the start/end dates,
     * and the name of the user it represents.
     *
     * @param startDate The start date of the connection history.
     * @param endDate The end date of the connection history.
     * @param username The name of the user that used the connection.
     */
    public MySQLConnectionRecord(Date startDate, Date endDate,
            String username) {
        if (startDate != null) this.startDate = new Date(startDate.getTime());
        if (endDate != null) this.endDate = new Date(endDate.getTime());
        this.username = username;
    }

    @Override
    public Date getStartDate() {
        if (startDate == null) return null;
        return new Date(startDate.getTime());
    }

    @Override
    public Date getEndDate() {
        if (endDate == null) return null;
        return new Date(endDate.getTime());
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isActive() {
        // If the end date hasn't been stored yet, the connection is still open.
        return endDate == null;
    }

}
