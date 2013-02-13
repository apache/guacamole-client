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
package net.sourceforge.guacamole.net.auth.mysql;

/**
 * Constants relevant to the guacamole-auth-mysql project.
 * @author James Muehlner
 */
public interface MySQLConstants {
    
    //*********** Permission Strings ***********
    // operations
    public static final String CREATE = "CREATE";
    public static final String READ = "READ";
    public static final String WRITE = "WRITE";
    public static final String DELETE = "DELETE";
    public static final String ADMINISTER = "ADMINISTER";
    
    // used to separate operations from objects
    public static final String SEPARATOR = "_";
    
    //object types
    public static final String USER = "USER";
    public static final String CONNECTION = "CONNECTION";
    
    //combinations
    public static final String CREATE_USER = CREATE + SEPARATOR + USER;
    public static final String READ_USER = READ + SEPARATOR + USER;
    public static final String WRITE_USER = WRITE + SEPARATOR + USER;
    public static final String DELETE_USER = DELETE + SEPARATOR + USER;
    public static final String ADMINISTER_USER = ADMINISTER + SEPARATOR + USER;
    
    public static final String CREATE_CONNECTION = CREATE + SEPARATOR + CONNECTION;
    public static final String READ_CONNECTION = READ + SEPARATOR + CONNECTION;
    public static final String WRITE_CONNECTION = WRITE + SEPARATOR + CONNECTION;
    public static final String DELETE_CONNECTION = DELETE + SEPARATOR + CONNECTION;
    public static final String ADMINISTER_CONNECTION = ADMINISTER + SEPARATOR + CONNECTION;
}
