
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
 * The Original Code is guacamole-ext.
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
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;


/**
 * Provides access to a collection of all configurations, and allows
 * configuration manipulation and removal.
 * 
 * @author Michael Jumper
 */
public interface GuacamoleConfigurationDirectory {

    /**
     * Returns a Map containing all GuacamoleConfigurations. The keys of this
     * Map are Strings which uniquely identify each configuration.
     *
     * @return A Map of all configurations visible.
     * @throws GuacamoleException If an error occurs while retrieving
     *                            configurations.
     */
    Map<String, GuacamoleConfiguration> getConfigurations()
            throws GuacamoleException;

    /**
     * Returns a Map containing GuacamoleConfigurationTemplates which describe
     * legal parameters and value. These templates are expected to be used as
     * the blueprints for new connections.
     * 
     * @return A Map of configuration templates.
     * @throws GuacamoleException If an error occurs while retrieving the
     *                            templates.
     */
    Map<String, GuacamoleConfigurationTemplate> getTemplates()
            throws GuacamoleException;

    /**
     * Adds the given GuacamoleConfiguration to the overall set of available
     * GuacamoleConfigurations, using the given unique identifier.
     * 
     * @param identifier The identifier to assign to the configuration.
     * @param config The configuration to add.
     * @throws GuacamoleException If an error occurs while adding the
     *                            configuration, or if adding the configuration
     *                            is not allowed.
     */
    void addConfiguration(String identifier, GuacamoleConfiguration config)
            throws GuacamoleException;
    
    /**
     * Updates the GuacamoleConfiguration having the given unique identifier
     * with the data contained in the given GuacamoleConfiguration.
     * 
     * @param identifier The identifier to use when locating the configuration
     *                   to update.
     * @param config The configuration to use when updating the stored
     *               configuration.
     * @throws GuacamoleException If an error occurs while updating the
     *                            configuration, or if updating the
     *                            configuration is not allowed.
     */
    void updateConfiguration(String identifier, GuacamoleConfiguration config)
            throws GuacamoleException;
    
    /**
     * Removes the GuacamoleConfiguration having the given unique identifier.
     * 
     * @param identifier The identifier of the configuration to remove.
     * @throws GuacamoleException If an error occurs while removing the
     *                            configuration, or if removing the
     *                            configuration is not allowed.
     */
    void removeConfiguration(String identifier) throws GuacamoleException;

}
