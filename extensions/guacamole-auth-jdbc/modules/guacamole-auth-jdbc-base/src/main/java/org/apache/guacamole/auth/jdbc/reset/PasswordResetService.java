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


package org.apache.guacamole.auth.jdbc.reset;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Date;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.auth.jdbc.JDBCEnvironment;
import org.apache.guacamole.auth.jdbc.security.PasswordPolicyService;
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;
import org.apache.guacamole.auth.jdbc.user.UserMapper;
import org.apache.guacamole.auth.jdbc.user.UserModel;
import org.apache.guacamole.auth.jdbc.user.UserService;
import org.apache.guacamole.form.PasswordField;
import org.apache.guacamole.language.TranslatableGuacamoleClientException;
import org.apache.guacamole.language.TranslatableGuacamoleInsufficientCredentialsException;
import org.apache.guacamole.language.TranslatableGuacamoleInvalidCredentialsException;
import org.apache.guacamole.language.TranslatableGuacamoleResourceNotFoundException;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.Permissions;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.SystemPermission;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends password reset links and redeems them. A link carries a single-use
 * token as the "password-reset-token" query parameter of the Guacamole page;
 * the web application forwards page query parameters with its authentication
 * request, so the token arrives at authenticateUser() like any credential and
 * is redeemed through the same new-password prompt used for expired
 * passwords.
 */
public class PasswordResetService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    /**
     * The request parameter carrying the reset token.
     */
    public static final String TOKEN_PARAMETER = "password-reset-token";

    /**
     * The request parameter carrying the new password. The same name as the
     * expired-password flow, so the existing field translations apply.
     */
    private static final String NEW_PASSWORD_PARAMETER = "new-password";

    /**
     * The request parameter carrying the new password confirmation.
     */
    private static final String CONFIRM_NEW_PASSWORD_PARAMETER = "confirm-new-password";

    /**
     * The fields requested from a user holding a valid reset token.
     */
    private static final CredentialsInfo RESET_PASSWORD = new CredentialsInfo(Arrays.asList(
        new PasswordField(NEW_PASSWORD_PARAMETER),
        new PasswordField(CONFIRM_NEW_PASSWORD_PARAMETER)
    ));

    /**
     * Default token lifetime, in minutes.
     */
    private static final int DEFAULT_TTL = 30;

    /**
     * Default maximum number of reset mails per user per hour.
     */
    private static final int DEFAULT_MAX_PER_HOUR = 3;

    /**
     * Default product name used in reset mails.
     */
    static final String DEFAULT_SERVICE_NAME = "Guacamole";

    @Inject
    private JDBCEnvironment environment;

    @Inject
    private PasswordResetTokenStore tokenStore;

    @Inject
    private PasswordResetMailer mailer;

    @Inject
    private UserService userService;

    @Inject
    private UserMapper userMapper;

    @Inject
    private PasswordPolicyService passwordPolicyService;

    /**
     * Returns the error for a reset link that is invalid, expired or used.
     * It is an ordinary invalid login to the server, so failure accounting
     * is unchanged; only the message key differs, so the login page can say
     * why. The token is opaque, so this reveals nothing about any account.
     *
     * @return
     *     The error to throw.
     */
    private static TranslatableGuacamoleInvalidCredentialsException invalidLink() {
        return new TranslatableGuacamoleInvalidCredentialsException("Invalid login",
                "PASSWORD_RESET.ERROR_LINK_INVALID", CredentialsInfo.USERNAME_PASSWORD);
    }

    /**
     * Verifies that the holder of the given effective permissions may send a
     * password reset link to the given user. This is the same check that
     * governs editing that user: system ADMINISTER, or UPDATE on the user.
     *
     * @param effective
     *     The effective permissions of the caller.
     *
     * @param username
     *     The user to be reset.
     *
     * @throws GuacamoleException
     *     GuacamoleSecurityException (HTTP 403) if permission is denied, or
     *     any error reading the permissions.
     */
    public static void requireResetPermission(Permissions effective, String username)
            throws GuacamoleException {
        if (!effective.getSystemPermissions().hasPermission(SystemPermission.Type.ADMINISTER)
                && !effective.getUserPermissions().hasPermission(ObjectPermission.Type.UPDATE, username))
            throw new GuacamoleSecurityException("Permission denied.");
    }

    /**
     * Sends a password reset link to the given user on behalf of an
     * administrator.
     *
     * @param caller
     *     The user requesting the reset.
     *
     * @param username
     *     The user whose password should be reset.
     *
     * @throws GuacamoleException
     *     If the caller may not edit the user, the user does not exist or has
     *     no e-mail address, the rate limit is exceeded, or the mail cannot be
     *     sent.
     */
    public void sendResetLink(ModeledAuthenticatedUser caller, String username)
            throws GuacamoleException {

        if (username == null || username.isEmpty())
            throw new TranslatableGuacamoleClientException("A username is required.",
                    "PASSWORD_RESET.ERROR_NO_SUCH_USER");

        // Checked before the lookup, so unknown users and forbidden users are
        // indistinguishable to a delegated administrator
        requireResetPermission(caller.getUser().getEffectivePermissions(), username);

        ModeledUser target = userService.retrieveObject(caller, username);
        if (target == null)
            throw new TranslatableGuacamoleResourceNotFoundException("No such user.",
                    "PASSWORD_RESET.ERROR_NO_SUCH_USER");

        sendResetLink(target);
        logger.info("User \"{}\" sent a password reset link to user \"{}\".",
                caller.getIdentifier(), target.getIdentifier());

    }

    /**
     * Issues a token for the given user and mails the link to the user's
     * stored e-mail address. The token row and the mail succeed or fail
     * together.
     *
     * @param user
     *     The user whose password should be reset.
     *
     * @throws GuacamoleException
     *     If the user has no e-mail address, the rate limit is exceeded, the
     *     feature is not configured, or the mail cannot be sent.
     */
    @Transactional
    public void sendResetLink(ModeledUser user) throws GuacamoleException {

        UserModel model = user.getModel();
        String email = model.getEmailAddress();
        if (email == null || email.trim().isEmpty())
            throw new TranslatableGuacamoleClientException("User has no e-mail "
                    + "address.", "PASSWORD_RESET.ERROR_NO_EMAIL");

        String baseUrl = environment.getRequiredProperty(PasswordResetProperties.PASSWORD_RESET_BASE_URL);
        int ttl = environment.getProperty(PasswordResetProperties.PASSWORD_RESET_TOKEN_TTL, DEFAULT_TTL);
        int maxPerHour = environment.getProperty(PasswordResetProperties.PASSWORD_RESET_MAX_PER_HOUR, DEFAULT_MAX_PER_HOUR);

        String token = tokenStore.issue(model.getObjectID(), ttl, maxPerHour, new Date());
        String link = baseUrl.replaceAll("/+$", "") + "/#/?" + TOKEN_PARAMETER + "=" + token;

        String serviceName = environment.getProperty(
                PasswordResetProperties.PASSWORD_RESET_SERVICE_NAME, DEFAULT_SERVICE_NAME);
        mailer.send(email.trim(), mailSubject(serviceName),
                mailBody(serviceName, user.getIdentifier(), ttl, link));

    }

    /**
     * Returns the subject of a reset mail.
     *
     * @param serviceName
     *     The product name, as configured.
     *
     * @return
     *     The subject line.
     */
    static String mailSubject(String serviceName) {
        return serviceName + " password reset";
    }

    /**
     * Returns the body of a reset mail.
     *
     * @param serviceName
     *     The product name, as configured.
     *
     * @param username
     *     The user whose password may be reset.
     *
     * @param ttl
     *     How long the link stays valid, in minutes.
     *
     * @param link
     *     The reset link.
     *
     * @return
     *     The plain text body.
     */
    static String mailBody(String serviceName, String username, int ttl, String link) {
        return "A password reset was requested for the " + serviceName + " account \""
                + username + "\".\n\n"
                + "To choose a new password, open this link within " + ttl
                + " minutes:\n\n" + link + "\n\n"
                + "The link can be used once. If you did not expect this "
                + "message, you can ignore it; your password is unchanged.\n";
    }

    /**
     * Redeems a reset token presented as a request parameter. Returns null if
     * the request carries no token, or also carries a username or password,
     * so that an ordinary login from a page whose URL still holds a used
     * token is unaffected.
     *
     * @param authenticationProvider
     *     The AuthenticationProvider on behalf of which the user is
     *     authenticated.
     *
     * @param credentials
     *     The credentials of the request.
     *
     * @return
     *     The authenticated user whose password was reset, or null if the
     *     request is not a reset.
     *
     * @throws GuacamoleException
     *     If the token is invalid, if the new password is still needed, or if
     *     it is rejected by the password policy.
     */
    public ModeledAuthenticatedUser redeem(AuthenticationProvider authenticationProvider,
            Credentials credentials) throws GuacamoleException {

        String token = credentials.getParameter(TOKEN_PARAMETER);
        if (token == null || credentials.getUsername() != null || credentials.getPassword() != null)
            return null;

        Date now = new Date();
        String username = tokenStore.findUsername(token, now);
        ModeledAuthenticatedUser authenticatedUser = (username == null) ? null
                : userService.retrieveUserForPasswordReset(authenticationProvider, username, credentials);

        ModeledUser user = (authenticatedUser == null) ? null : authenticatedUser.getUser();
        if (user == null || user.isDisabled() || !user.isAccountValid()) {
            logger.info("Rejected an invalid, expired or used password reset "
                    + "link from {}.", credentials.getRemoteAddress());
            throw invalidLink();
        }

        String newPassword = credentials.getParameter(NEW_PASSWORD_PARAMETER);
        String confirmNewPassword = credentials.getParameter(CONFIRM_NEW_PASSWORD_PARAMETER);
        if (newPassword == null || confirmNewPassword == null)
            throw new TranslatableGuacamoleInsufficientCredentialsException(
                    "Password reset", "PASSWORD_RESET.INFO_CHOOSE_PASSWORD", RESET_PASSWORD);

        if (newPassword.isEmpty())
            throw new TranslatableGuacamoleClientException("Passwords may not "
                    + "be blank.", "LOGIN.ERROR_PASSWORD_BLANK");

        if (!newPassword.equals(confirmNewPassword))
            throw new TranslatableGuacamoleClientException("New password does "
                    + "not match.", "LOGIN.ERROR_PASSWORD_MISMATCH");

        // Policy is checked before the token is consumed, so a rejected
        // password can be retried with the same link
        passwordPolicyService.verifyPassword(username, newPassword);
        applyReset(user, token, newPassword, now);

        logger.info("User \"{}\" reset their password using a reset link "
                + "from {}.", username, credentials.getRemoteAddress());
        return authenticatedUser;

    }

    /**
     * Consumes the token and stores the new password, atomically. Of two
     * concurrent redemptions of one token, only one changes the password.
     *
     * @param user
     *     The user whose password is reset.
     *
     * @param token
     *     The reset token.
     *
     * @param newPassword
     *     The new password, already checked against the password policy.
     *
     * @param now
     *     The current time.
     *
     * @throws GuacamoleException
     *     If the token was consumed concurrently or the update fails.
     */
    @Transactional
    protected void applyReset(ModeledUser user, String token, String newPassword,
            Date now) throws GuacamoleException {

        if (!tokenStore.consume(token, now))
            throw invalidLink();

        passwordPolicyService.recordPassword(user);
        user.setPassword(newPassword);
        user.getModel().setExpired(false);
        userMapper.update(user.getModel());

    }

}
