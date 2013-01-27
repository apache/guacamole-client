
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
 * The Original Code is guacamole-auth.
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

import java.util.Map;
import java.util.Set;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;

/**
 * Provides means of accessing and managing the available
 * GuacamoleConfiguration objects and User objects. Access to each configuration
 * and each user is limited by a given Credentials object.
 *
 * @author Michael Jumper
 */
public interface AuthenticationProvider {

    /**
     * Converts the given Credentials to a corresponding User, which need not
     * actually exist.
     * 
     * @param ownCredentials The credentials to use to authorize the conversion.
     * @param credentials The credentials to convert.
     * @return A User which corresponds to the given Credentials.
     * @throws GuacamoleException If an error occurs converting the Credentials
     *                            into a User, or if such conversion is not
     *                            allowed.
     */
    User toUser(Credentials ownCredentials, Credentials credentials)
            throws GuacamoleException;

    /**
     * Given an arbitrary Credentials object, returns a Map containing all
     * GuacamoleConfigurations visible with those credentials. The keys of
     * this Map are Strings which uniquely identify each configuration.
     *
     * @param credentials The credentials to use to retrieve authorized
     *                    configurations.
     * @return A Map of all configurations authorized by the given credentials,
     *         or null if the credentials given are not authorized.
     * @throws GuacamoleException If an error occurs while retrieving
     *                            configurations.
     */
    Map<String, GuacamoleConfiguration>
            getConfigurations(Credentials credentials)
            throws GuacamoleException;

    /**
     * Adds the given GuacamoleConfiguration to the overall set of available
     * GuacamoleConfigurations, using the given unique identifier and
     * credentials.
     * 
     * @param credentials The credentials to use when adding the given
     *                    configuration.
     * @param identifier The identifier to assign to the configuration.
     * @param config The configuration to add.
     * @throws GuacamoleException If an error occurs while adding the
     *                            configuration, or if adding the configuration
     *                            is not allowed.
     */
    void addConfiguration(Credentials credentials, String identifier,
            GuacamoleConfiguration config) throws GuacamoleException;
    
    /**
     * Updates the GuacamoleConfiguration having the given unique identifier
     * with the data contained in the given GuacamoleConfiguration, using the
     * given credentials.
     * 
     * @param credentials The credentials to use when updating the configuration
     *                    having the given identifier.
     * @param identifier The identifier to use when locating the configuration
     *                   to update.
     * @param config The configuration to use when updating the stored
     *               configuration.
     * @throws GuacamoleException If an error occurs while updating the
     *                            configuration, or if updating the
     *                            configuration is not allowed.
     */
    void updateConfiguration(Credentials credentials, String identifier,
            GuacamoleConfiguration config) throws GuacamoleException;
    
    /**
     * Removes the GuacamoleConfiguration having the given unique identifier,
     * using the given credentials.
     * 
     * @param credentials The credentials to use when removing the configuration
     *                    having the given identifier.
     * @param identifier The identifier of the configuration to remove.
     * @throws GuacamoleException If an error occurs while removing the
     *                            configuration, or if removing the
     *                            configuration is not allowed.
     */
    void removeConfiguration(Credentials credentials, String identifier)
            throws GuacamoleException;
 
    /**
     * Given an arbitrary Credentials object, returns a Set containing all
     * Users visible with those credentials.
     *
     * @param credentials The credentials to use to retrieve users. 
     * @return A Set of all users visible with the given credentials,
     *         or null if the credentials given are not authorized.
     * @throws GuacamoleException If an error occurs while retrieving
     *                            users.
     */
    Set<User> getUsers(Credentials credentials) throws GuacamoleException;

    /**
     * Adds the given User to the overall set of available Users, using the
     * given credentials.
     * 
     * @param credentials The credentials to use when adding the given
     *                    user.
     * @param user The user to add.
     * @throws GuacamoleException If an error occurs while adding the user, or
     *                            if adding the user is not allowed.
     */
    void addUser(Credentials credentials, User user)
            throws GuacamoleException;
    
    /**
     * Updates the User with the data contained in the given User, using the
     * given credentials. The user to update is identified using the username
     * of the User given.
     * 
     * @param credentials The credentials to use when updating the user. 
     * @param user The user to use when updating the stored user.
     * @throws GuacamoleException If an error occurs while updating the user,
     *                            or if updating the user is not allowed.
     */
    void updateUser(Credentials credentials, User user)
            throws GuacamoleException;
    
    /**
     * Removes the User using the given credentials.
     * 
     * @param credentials The credentials to use when removing the user. 
     * @throws GuacamoleException If an error occurs while removing the user,
     *                            or if removing user is not allowed.
     */
    void removeUser(Credentials credentials, User user)
            throws GuacamoleException;
    
}
