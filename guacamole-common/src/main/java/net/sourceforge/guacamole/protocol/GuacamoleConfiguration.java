
package net.sourceforge.guacamole.protocol;

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
 * The Original Code is guacamole-common.
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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * All information necessary to complete the initial protocol handshake of a
 * Guacamole session.
 *
 * @author Michael Jumper
 */
public class GuacamoleConfiguration implements Serializable {

    /**
     * Identifier unique to this version of GuacamoleConfiguration.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The name of the protocol associated with this configuration.
     */
    private String protocol;

    /**
     * Map of all associated parameter values, indexed by parameter name.
     */
    private Map<String, String> parameters = new HashMap<String, String>();

    /**
     * Returns the name of the protocol to be used.
     * @return The name of the protocol to be used.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Sets the name of the protocol to be used.
     * @param protocol The name of the protocol to be used.
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Returns the value set for the parameter with the given name, if any.
     * @param name The name of the parameter to return the value for.
     * @return The value of the parameter with the given name, or null if
     *         that parameter has not been set.
     */
    public String getParameter(String name) {
        return parameters.get(name);
    }

    /**
     * Sets the value for the parameter with the given name.
     *
     * @param name The name of the parameter to set the value for.
     * @param value The value to set for the parameter with the given name.
     */
    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }

    /**
     * Removes the value set for the parameter with the given name.
     *
     * @param name The name of the parameter to remove the value of.
     */
    public void unsetParameter(String name) {
        parameters.remove(name);
    }

    /**
     * Returns a set of all currently defined parameter names. Each name
     * corresponds to a parameter that has a value set on this
     * GuacamoleConfiguration via setParameter().
     *
     * @return A set of all currently defined parameter names.
     */
    public Set<String> getParameterNames() {
        return Collections.unmodifiableSet(parameters.keySet());
    }

}
