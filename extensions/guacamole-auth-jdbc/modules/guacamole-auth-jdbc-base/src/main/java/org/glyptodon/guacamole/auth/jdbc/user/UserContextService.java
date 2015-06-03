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

package org.glyptodon.guacamole.auth.jdbc.user;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.form.Field;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.credentials.CredentialsInfo;
import org.glyptodon.guacamole.net.auth.credentials.GuacamoleInsufficientCredentialsException;
import org.glyptodon.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service which creates new UserContext instances for valid users based on
 * credentials.
 *
 * @author Michael Jumper
 */
public class UserContextService  {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    /**
     * Service for accessing users.
     */
    @Inject
    private UserService userService;

    /**
     * Provider for retrieving UserContext instances.
     */
    @Inject
    private Provider<UserContext> userContextProvider;

    /**
     * The name of the HTTP password parameter to expect if the user is
     * changing their expired password upon login.
     */
    private static final String NEW_PASSWORD_PARAMETER = "new-password";

    /**
     * The password field to provide the user when their password is expired
     * and must be changed.
     */
    private static final Field NEW_PASSWORD = new Field(NEW_PASSWORD_PARAMETER, "New password", Field.Type.PASSWORD);

    /**
     * The name of the HTTP password confirmation parameter to expect if the
     * user is changing their expired password upon login.
     */
    private static final String CONFIRM_NEW_PASSWORD_PARAMETER = "confirm-new-password";

    /**
     * The password confirmation field to provide the user when their password
     * is expired and must be changed.
     */
    private static final Field CONFIRM_NEW_PASSWORD = new Field(CONFIRM_NEW_PASSWORD_PARAMETER, "Confirm new password", Field.Type.PASSWORD);

    /**
     * Information describing the expected credentials if a user's password is
     * expired. If a user's password is expired, it must be changed during the
     * login process.
     */
    private static final CredentialsInfo EXPIRED_PASSWORD = new CredentialsInfo(Arrays.asList(
        CredentialsInfo.USERNAME,
        CredentialsInfo.PASSWORD,
        NEW_PASSWORD,
        CONFIRM_NEW_PASSWORD
    ));

    /**
     * Authenticates the user having the given credentials, returning a new
     * UserContext instance only if the credentials are valid. If the
     * credentials are invalid or expired, an appropriate GuacamoleException
     * will be thrown.
     *
     * @param credentials
     *     The credentials to use to produce the UserContext.
     *
     * @return
     *     A new UserContext instance for the user identified by the given
     *     credentials.
     *
     * @throws GuacamoleException
     *     If an error occurs during authentication, or if the given
     *     credentials are invalid or expired.
     */
    public org.glyptodon.guacamole.net.auth.UserContext
        getUserContext(Credentials credentials)
                throws GuacamoleException {

        // Authenticate user
        ModeledUser user = userService.retrieveUser(credentials);
        if (user != null && !user.getModel().isDisabled()) {

            // Update password if password is expired
            if (user.getModel().isExpired()) {

                // Pull new password from HTTP request
                HttpServletRequest request = credentials.getRequest();
                String newPassword = request.getParameter(NEW_PASSWORD_PARAMETER);
                String confirmNewPassword = request.getParameter(CONFIRM_NEW_PASSWORD_PARAMETER);

                // Require new password if account is expired
                if (newPassword == null || confirmNewPassword == null) {
                    logger.info("The password of user \"{}\" has expired and must be reset.", user.getIdentifier());
                    throw new GuacamoleInsufficientCredentialsException("Password expired", EXPIRED_PASSWORD);
                }

                // New password must be different from old password
                if (newPassword.equals(credentials.getPassword()))
                    throw new GuacamoleClientException("LOGIN.ERROR_PASSWORD_SAME");

                // New password must not be blank
                if (newPassword.isEmpty())
                    throw new GuacamoleClientException("LOGIN.ERROR_PASSWORD_BLANK");

                // Confirm that the password was entered correctly twice
                if (!newPassword.equals(confirmNewPassword))
                    throw new GuacamoleClientException("LOGIN.ERROR_PASSWORD_MISMATCH");

                // STUB: Change password if new password given
                logger.info("Resetting expired password of user \"{}\".", user.getIdentifier());

            }

            // Upon successful authentication, return new user context
            UserContext context = userContextProvider.get();
            context.init(user.getCurrentUser());
            return context;

        }

        // Otherwise, unauthorized
        throw new GuacamoleInvalidCredentialsException("Invalid login", CredentialsInfo.USERNAME_PASSWORD);

    }

}
