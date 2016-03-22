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

package org.apache.guacamole.auth.jdbc.user;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;

/**
 * Associates a user with the credentials they used to authenticate.
 *
 * @author Michael Jumper 
 */
public class AuthenticatedUser implements org.apache.guacamole.net.auth.AuthenticatedUser {

    /**
     * The user that authenticated.
     */
    private final ModeledUser user;

    /**
     * The credentials given when this user authenticated.
     */
    private final Credentials credentials;

    /**
     * The AuthenticationProvider that authenticated this user.
     */
    private final AuthenticationProvider authenticationProvider;

    /**
     * The host from which this user authenticated.
     */
    private final String remoteHost;

    /**
     * Regular expression which matches any IPv4 address.
     */
    private static final String IPV4_ADDRESS_REGEX = "([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})";

    /**
     * Regular expression which matches any IPv6 address.
     */
    private static final String IPV6_ADDRESS_REGEX = "([0-9a-fA-F]*(:[0-9a-fA-F]*){0,7})";

    /**
     * Regular expression which matches any IP address, regardless of version.
     */
    private static final String IP_ADDRESS_REGEX = "(" + IPV4_ADDRESS_REGEX + "|" + IPV6_ADDRESS_REGEX + ")";

    /**
     * Pattern which matches valid values of the de-facto standard
     * "X-Forwarded-For" header.
     */
    private static final Pattern X_FORWARDED_FOR = Pattern.compile("^" + IP_ADDRESS_REGEX + "(, " + IP_ADDRESS_REGEX + ")*$");

    /**
     * Derives the remote host of the authenticating user from the given
     * credentials object. The remote host is derived from X-Forwarded-For
     * in addition to the actual source IP of the request, and thus is not
     * trusted. The derived remote host is really only useful for logging,
     * unless the server is configured such that X-Forwarded-For is guaranteed
     * to be trustworthy.
     *
     * @param credentials
     *     The credentials to derive the remote host from.
     *
     * @return
     *     The remote host from which the user with the given credentials is
     *     authenticating.
     */
    private static String getRemoteHost(Credentials credentials) {

        HttpServletRequest request = credentials.getRequest();

        // Use X-Forwarded-For, if present and valid
        String header = request.getHeader("X-Forwarded-For");
        if (header != null) {
            Matcher matcher = X_FORWARDED_FOR.matcher(header);
            if (matcher.matches())
                return matcher.group(1);
        }

        // If header absent or invalid, just use source IP
        return request.getRemoteAddr();

    }
    
    /**
     * Creates a new AuthenticatedUser associating the given user with their
     * corresponding credentials.
     *
     * @param authenticationProvider
     *     The AuthenticationProvider that has authenticated the given user.
     *
     * @param user
     *     The user this object should represent.
     *
     * @param credentials 
     *     The credentials given by the user when they authenticated.
     */
    public AuthenticatedUser(AuthenticationProvider authenticationProvider,
            ModeledUser user, Credentials credentials) {
        this.authenticationProvider = authenticationProvider;
        this.user = user;
        this.credentials = credentials;
        this.remoteHost = getRemoteHost(credentials);
    }

    /**
     * Returns the user that authenticated.
     *
     * @return 
     *     The user that authenticated.
     */
    public ModeledUser getUser() {
        return user;
    }

    /**
     * Returns the credentials given during authentication by this user.
     *
     * @return 
     *     The credentials given during authentication by this user.
     */
    @Override
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Returns the host from which this user authenticated.
     *
     * @return
     *     The host from which this user authenticated.
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    @Override
    public String getIdentifier() {
        return user.getIdentifier();
    }

    @Override
    public void setIdentifier(String identifier) {
        user.setIdentifier(identifier);
    }

}
