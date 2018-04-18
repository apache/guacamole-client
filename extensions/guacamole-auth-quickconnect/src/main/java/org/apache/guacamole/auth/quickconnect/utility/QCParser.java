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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

/**
 * A utility class to parse out a URI into the settings necessary
 * to create and establish a Guacamole connection.
 */
public class QCParser {

    /**
     * The default protocol to parse to if one is provided in
     * the incoming URI..
     */
    public static final String DEFAULT_URI_PROTOCOL = "ssh";

    /**
     * The default host to use if one is not defined.
     */
    public static final String DEFAULT_URI_HOST = "localhost";

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
     * Parse out a URI string and get a connection from that string,
     * or an exception if the parsing fails.
     *
     * @param uri
     *     The string form of the URI to be parsed.
     *
     * @return
     *     A GuacamoleConfiguration using a combination of the parsed
     *     URI values and default values when not specified in the
     *     URI.
     *
     * @throws GuacamoleException
     *     When an error occurs parsing the URI.
     */
    public static GuacamoleConfiguration getConfiguration(String uri)
            throws GuacamoleException {

        URI qcUri;
        try {
            qcUri = new URI(uri);
        }
        catch (URISyntaxException e) {
            throw new GuacamoleClientException("Invalid URI Syntax", e);
        }
        String protocol = qcUri.getScheme();
        String host = qcUri.getHost();
        int port = qcUri.getPort();
        String userInfo = qcUri.getUserInfo();
        String query = qcUri.getQuery();
        String username = null;
        String password = null;
        List<String> paramList = null;

        if (protocol == null || protocol.isEmpty())
            protocol = DEFAULT_URI_PROTOCOL;

        if (host == null || host.isEmpty())
            host = DEFAULT_URI_HOST;

        if (query != null && !query.isEmpty())
            paramList = Arrays.asList(query.split("&"));

        if (userInfo != null && !userInfo.isEmpty()) {

            Matcher userinfoMatcher = userinfoPattern.matcher(userInfo);
            if (userinfoMatcher.matches()) {
                username = userinfoMatcher.group(USERNAME_GROUP);
                password = userinfoMatcher.group(PASSWORD_GROUP);
            }

        }

        GuacamoleConfiguration qcConfig = new GuacamoleConfiguration();
        qcConfig.setProtocol(protocol);
        qcConfig.setParameter("hostname",host);

        if (port > 0)
            qcConfig.setParameter("port", Integer.toString(port));

        if (username != null && !username.isEmpty())
            qcConfig.setParameter("username", username);

        if (password != null && !password.isEmpty())
            qcConfig.setParameter("password", password);

        if (paramList != null) {
            for (String parameter : paramList) {
                String[] paramArray = parameter.split("=", 2);
                qcConfig.setParameter(paramArray[0],paramArray[1]);
            }
        }

        return qcConfig;
        
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

        String name = "";

        if (protocol != null && !protocol.isEmpty())
            name += protocol + "://";

        if (user != null && !user.isEmpty())
            name += user + "@";

        if (host != null && !host.isEmpty())
            name += host;

        if (port != null && !port.isEmpty())
            name += ":" + port;

        name += "/";

        return name;
    }

}
