/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.auth.quickconnect.utility;

import java.io.UnsupportedEncodingException;
import java.lang.StringBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.language.TranslatableGuacamoleClientException;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

/**
 * A utility class to parse out a URI into the settings necessary
 * to generate a GuacamoleConfiguration object.
 */
public class QCParser {

    /**
     * The regex to use to split username and password.
     */
    private static final Pattern userinfoPattern = Pattern.compile("(^[^:]+):?(.*)");

    /**
     * The regex group of the username.
     */
    private static final int USERNAME_GROUP = 1;

    /**
     * THe regex group of the password.
     */
    private static final int PASSWORD_GROUP = 2;

    /**
     * Parse out a URI string and get a GuacamoleConfiguration
     * from that string, or an exception if the parsing fails.
     *
     * @param uri
     *     The string form of the URI to be parsed.
     *
     * @return
     *     A GuacamoleConfiguration generated using the information
     *     provided by the user in the URI.
     *
     * @throws GuacamoleException
     *     If an error occurs parsing the URI.
     */
    public static GuacamoleConfiguration getConfiguration(String uri)
            throws GuacamoleException {

        // Parse the provided String into a URI object.
        URI qcUri;
        try {
            qcUri = new URI(uri);
            if (!qcUri.isAbsolute())
                throw new TranslatableGuacamoleClientException("URI must be absolute.",
                        "QUICKCONNECT.ERROR_NOT_ABSOLUTE_URI");
        }
        catch (URISyntaxException e) {
            throw new TranslatableGuacamoleClientException("Invalid URI Syntax",
                    "QUICKCONNECT.ERROR_INVALID_URI");
        }

        // Break out individual components of the URI.
        String protocol = qcUri.getScheme();
        String host = qcUri.getHost();
        int port = qcUri.getPort();
        String userInfo = qcUri.getUserInfo();
        String query = qcUri.getQuery();

        // Generate a new GuacamoleConfiguration
        GuacamoleConfiguration qcConfig = new GuacamoleConfiguration();

        // Check for protocol and set it, or throw an error if not present
        if (protocol != null && !protocol.isEmpty())
            qcConfig.setProtocol(protocol);
        else
            throw new TranslatableGuacamoleClientException("No protocol specified.",
                    "QUICKCONNECT.ERROR_NO_PROTOCOL");

        // Check for provided port number
        if (port > 0)
            qcConfig.setParameter("port", Integer.toString(port));

        // Check for provided host, or throw an error if not present
        if (host != null && !host.isEmpty())
            qcConfig.setParameter("hostname", host);
        else
            throw new TranslatableGuacamoleClientException("No host specified.",
                    "QUICKCONNECT.ERROR_NO_HOST");

        // Look for extra query parameters and parse them out.
        if (query != null && !query.isEmpty()) {
            try {
                Map<String, String> queryParams = parseQueryString(query);
                if (queryParams != null)
                    for (Map.Entry<String, String> entry: queryParams.entrySet())
                        qcConfig.setParameter(entry.getKey(), entry.getValue());
            }
            catch (UnsupportedEncodingException e) {
                throw new GuacamoleServerException("Unexpected lack of UTF-8 encoding support.", e);
            }
        }

        // Look for the username and password and parse them out.
        if (userInfo != null && !userInfo.isEmpty()) {

            try {
                parseUserInfo(userInfo, qcConfig);
            }
            catch (UnsupportedEncodingException e) {
                throw new GuacamoleServerException("Unexpected lack of UTF-8 encoding support.", e);
            }
        }

        return qcConfig;
        
    }

    /**
     * Parse the given string for parameter key/value pairs and return
     * a map with the parameters.
     *
     * @param queryStr
     *     The query string to parse for key/value pairs.
     *
     * @return
     *     A map with the key/value pairs.
     *
     * @throws UnsupportedEncodingException
     *     If Java lacks UTF-8 support.
     */
    public static Map<String, String> parseQueryString(String queryStr)
            throws UnsupportedEncodingException {

        // Split the query string into the pairs
        List<String> paramList = Arrays.asList(queryStr.split("&"));
        Map<String, String> parameters = new HashMap<String,String>();

        // Loop through key/value pairs and put them in the Map.
        for (String param : paramList) {
            String[] paramArray = param.split("=", 2);
            parameters.put(URLDecoder.decode(paramArray[0], "UTF-8"),
                           URLDecoder.decode(paramArray[1], "UTF-8"));
        }

        return parameters;
    }

    /**
     * Parse the given string for username and password values,
     * and, if values are present, decode them and set them in
     * the provided GuacamoleConfiguration object.
     *
     * @param userInfo
     *     The string to parse for username/password values.
     *
     * @param config
     *     The GuacamoleConfiguration object to store the username
     *     and password in.
     *
     * @throws UnsupportedEncodingException
     *     If Java lacks UTF-8 support.
     */
    public static void parseUserInfo(String userInfo, 
            GuacamoleConfiguration config)
            throws UnsupportedEncodingException {

        Matcher userinfoMatcher = userinfoPattern.matcher(userInfo);

        if (userinfoMatcher.matches()) {
            String username = userinfoMatcher.group(USERNAME_GROUP);
            String password = userinfoMatcher.group(PASSWORD_GROUP);

            if (username != null && !username.isEmpty())
                config.setParameter("username",
                        URLDecoder.decode(username, "UTF-8"));

            if (password != null && !password.isEmpty())
                config.setParameter("password",
                        URLDecoder.decode(password, "UTF-8"));
        }

    }

    /**
     * Given a GuacamoleConfiguration object, generate a name
     * for the configuration based on the protocol, host, user
     * and port in the configuration, and return the string value.
     *
     * @param config
     *     The GuacamoleConfiguration object to use to generate
     *     the name.
     *
     * @return
     *     The String value of the name that is generated.
     *
     * @throws GuacamoleException
     *     If an error occurs getting items in the configuration.
     */
    public static String getName(GuacamoleConfiguration config)
            throws GuacamoleException {

        if (config == null)
            return null;

        String protocol = config.getProtocol();
        String host = config.getParameter("hostname");
        String port = config.getParameter("port");
        String user = config.getParameter("username");

        StringBuilder name = new StringBuilder();

        if (protocol != null && !protocol.isEmpty())
            name.append(protocol).append("://");

        if (user != null && !user.isEmpty())
            name.append(user).append("@");

        if (host != null && !host.isEmpty())
            name.append(host);

        if (port != null && !port.isEmpty())
            name.append(":").append(port);

        name.append("/");

        return name.toString();
    }

}
