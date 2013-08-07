package net.sourceforge.guacamole.net.auth;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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

/**
 * Simple arbitrary set of credentials, including a username/password pair,
 * the HttpServletRequest associated with the request for authorization
 * (if any) and the HttpSession associated with that request.
 *
 * This class is used along with AuthenticationProvider to provide arbitrary
 * HTTP-based authentication for Guacamole.
 *
 * @author Michael Jumper
 */
public class Credentials implements Serializable {

    /**
     * Unique identifier associated with this specific version of Credentials.
     */
    private static final long serialVersionUID = 2L;

    /**
     * The HttpServletRequest carrying additional credentials, if any.
     */
    private transient HttpServletRequest request;

    /**
     * The HttpSession carrying additional credentials, if any.
     */
    private transient HttpSession session;

    /**
     * Map of query parameter names to values.
     */
    private Map<String, String> queryParameters = null;
    
    /**
     * Returns the password provided by the user in the request. Note that
     * this function will potentially read from the entire request body in
     * search of the "username" parameter, and thus can interfere with tunnel
     * usage if used at a time that the user is POSTing data to the tunnel
     * (such as while updating the UserContext during a tunnel write).
     *
     * This function will prefer parameters in the query string of a request to
     * those of the entire request body, so if it is known that the password
     * will always be present in the query string, this function is safe to
     * call at all times.
     *
     * @return The password given in the associated request, or null if no
     *         password was provided.
     */
    public String getPassword() {

        // Attempt to pull from GET parameters first
        String get_password = getQueryParameter("password");
        if (get_password != null)
            return get_password;
        
        // Otherwise, resort to parameters anywhere in the request body
        return request.getParameter("password");
        
    }

    /**
     * Returns the username provided by the user in the request. Note that
     * this function will potentially read from the entire request body in
     * search of the "username" parameter, and thus can interfere with tunnel
     * usage if used at a time that the user is POSTing data to the tunnel
     * (such as while updating the UserContext during a tunnel write).
     * 
     * This function will prefer parameters in the query string of a request to
     * those of the entire request body, so if it is known that the username
     * will always be present in the query string, this function is safe to
     * call at all times.
     * 
     * @return The username given in the associated request, or null if no
     *         username was provided.
     */
    public String getUsername() {

        // Attempt to pull from GET parameters first
        String get_password = getQueryParameter("username");
        if (get_password != null)
            return get_password;
        
        // Otherwise, resort to parameters anywhere in the request body
        return request.getParameter("username");

    }

    /**
     * Returns the contents of the given parameter, if present. Unlike
     * getParameter() of HttpServletRequest, this function is safe to call
     * when POST data is still required (such as during tunnel requests or
     * when the UserContext is being updated).
     * 
     * @param parameter The name of the parameter to read.
     * @return The value of the parameter, or null if no such parameter exists.
     */
    public String getQueryParameter(String parameter) {

        // Parse parameters, if not yet parsed
        if (queryParameters == null) {
        
            // If no request, then no parameters
            if (request == null)
                return null;

            // If no query string, then no parameters
            String query_string = request.getQueryString();
            if (query_string == null)
                return null;

            // Get name/value pairs
            String[] nv_pairs = query_string.split("&");

            // Add each pair to hash
            queryParameters = new HashMap<String, String>();
            for (String nv_pair : nv_pairs) {

                String name;
                String value;
                
                int eq = nv_pair.indexOf('=');

                // If no equals sign, parameter is blank
                if (eq == -1) {
                    name  = nv_pair;
                    value = "";
                }

                // Otherwise, parse pair
                else {
                    name  = nv_pair.substring(0, eq);
                    value = nv_pair.substring(eq+1);
                }
                
                // Save pair to hash
                queryParameters.put(name, value);
                
            }
            
        } // end if parameters cached

        // Return parsed parameter, if any
        return queryParameters.get(parameter);

    }
    
    /**
     * Returns the HttpServletRequest associated with this set of credentials.
     * @return The HttpServletRequest associated with this set of credentials,
     *         or null if no such request exists.
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * Sets the HttpServletRequest associated with this set of credentials.
     * @param request  The HttpServletRequest to associated with this set of
     *                 credentials.
     */
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Returns the HttpSession associated with this set of credentials.
     * @return The HttpSession associated with this set of credentials, or null
     *         if no such request exists.
     */
    public HttpSession getSession() {
        return session;
    }

    /**
     * Sets the HttpSession associated with this set of credentials.
     * @param session The HttpSession to associated with this set of
     *                credentials.
     */
    public void setSession(HttpSession session) {
        this.session = session;
    }

}
