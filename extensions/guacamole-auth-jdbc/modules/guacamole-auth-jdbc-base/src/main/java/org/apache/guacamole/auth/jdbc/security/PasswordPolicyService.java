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

package org.apache.guacamole.auth.jdbc.security;

import com.google.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.JDBCEnvironment;
import org.apache.guacamole.auth.jdbc.user.ModeledUser;
import org.apache.guacamole.auth.jdbc.user.PasswordRecordModel;

/**
 * Service which verifies compliance with the password policy configured via
 * guacamole.properties.
 *
 * @author Michael Jumper
 */
public class PasswordPolicyService {

    /**
     * The Guacamole server environment.
     */
    @Inject
    private JDBCEnvironment environment;

    /**
     * Regular expression which matches only if the string contains at least one
     * lowercase character.
     */
    private final Pattern CONTAINS_LOWERCASE = Pattern.compile("\\p{javaLowerCase}");

    /**
     * Regular expression which matches only if the string contains at least one
     * uppercase character.
     */
    private final Pattern CONTAINS_UPPERCASE = Pattern.compile("\\p{javaUpperCase}");

    /**
     * Regular expression which matches only if the string contains at least one
     * numeric character.
     */
    private final Pattern CONTAINS_DIGIT = Pattern.compile("\\p{Digit}");

    /**
     * Regular expression which matches only if the string contains at least one
     * non-alphanumeric character.
     */
    private final Pattern CONTAINS_NON_ALPHANUMERIC =
            Pattern.compile("[^\\p{javaLowerCase}\\p{javaUpperCase}\\p{Digit}]");

    /**
     * Returns whether the given string matches all of the provided regular
     * expressions.
     *
     * @param str
     *     The string to test against all provided regular expressions.
     *
     * @param patterns
     *     The regular expressions to match against the given string.
     *
     * @return
     *     true if the given string matches all provided regular expressions,
     *     false otherwise.
     */
    private boolean matches(String str, Pattern... patterns) {

        // Check given string against all provided patterns
        for (Pattern pattern : patterns) {

            // Fail overall test if any pattern fails to match
            Matcher matcher = pattern.matcher(str);
            if (!matcher.find())
                return false;

        }

        // All provided patterns matched
        return true;

    }

    /**
     * Verifies that the given new password complies with the password policy
     * configured within guacamole.properties, throwing a GuacamoleException if
     * the policy is violated in any way.
     *
     * @param username
     *     The username of the user whose password is being changed.
     *
     * @param password
     *     The proposed new password.
     *
     * @throws GuacamoleException
     *     If the password policy cannot be parsed, or if the proposed password
     *     violates the password policy.
     */
    public void verifyPassword(String username, String password)
            throws GuacamoleException {

        // Retrieve password policy from environment
        PasswordPolicy policy = environment.getPasswordPolicy();

        // Enforce minimum password length
        if (password.length() < policy.getMinimumLength())
            throw new PasswordMinimumLengthException(
                    "Password does not meet minimum length requirements.",
                    policy.getMinimumLength());

        // Disallow passwords containing the username
        if (policy.isUsernameProhibited() && password.toLowerCase().contains(username.toLowerCase()))
            throw new PasswordContainsUsernameException(
                    "Password must not contain username.");

        // Require both uppercase and lowercase characters
        if (policy.isMultipleCaseRequired() && !matches(password, CONTAINS_LOWERCASE, CONTAINS_UPPERCASE))
            throw new PasswordRequiresMultipleCaseException(
                    "Password must contain both uppercase and lowercase.");

        // Require digits
        if (policy.isNumericRequired() && !matches(password, CONTAINS_DIGIT))
            throw new PasswordRequiresDigitException(
                    "Passwords must contain at least one digit.");

        // Require non-alphanumeric symbols
        if (policy.isNonAlphanumericRequired() && !matches(password, CONTAINS_NON_ALPHANUMERIC))
            throw new PasswordRequiresSymbolException(
                    "Passwords must contain at least one non-alphanumeric character.");

        // Password passes all defined restrictions

    }

    /**
     * Returns the age of the given user's password, in days. The age of a
     * user's password is the amount of time elapsed since the password was last
     * changed or reset.
     *
     * @param user
     *     The user to calculate the password age of.
     *
     * @return
     *     The age of the given user's password, in days.
     */
    private long getPasswordAge(ModeledUser user) {

        // If no password was set, then no time has elapsed
        PasswordRecordModel previousPassword = user.getPreviousPassword();
        if (previousPassword == null)
            return 0;

        // Pull both current time and the time the password was last reset
        long currentTime = System.currentTimeMillis();
        long lastResetTime = previousPassword.getPasswordDate().getTime();

        // Calculate the number of days elapsed since the password was last reset
        return TimeUnit.DAYS.convert(currentTime - lastResetTime, TimeUnit.MILLISECONDS);
        
    }

    /**
     * Verifies that the given user can change their password without violating
     * password aging policy. If changing the user's password would violate the
     * aging policy, a GuacamoleException will be thrown.
     *
     * @param user
     *     The user whose password is changing.
     *
     * @throws GuacamoleException
     *     If the user's password cannot be changed due to the password aging
     *     policy, or of the password policy cannot be parsed from
     *     guacamole.properties.
     */
    public void verifyPasswordAge(ModeledUser user) throws GuacamoleException {

        // Retrieve password policy from environment
        PasswordPolicy policy = environment.getPasswordPolicy();

        long minimumAge = policy.getMinimumAge();
        long passwordAge = getPasswordAge(user);

        // Require that sufficient time has elapsed before allowing the password
        // to be changed
        if (passwordAge < minimumAge)
            throw new PasswordTooYoungException("Password was already recently changed.",
                    minimumAge - passwordAge);

    }

    /**
     * Returns whether the given user's password is expired due to the password
     * aging policy.
     *
     * @param user
     *     The user to check.
     *
     * @return
     *     true if the user needs to change their password to comply with the
     *     password aging policy, false otherwise.
     *
     * @throws GuacamoleException
     *     If the password policy cannot be parsed.
     */
    public boolean isPasswordExpired(ModeledUser user)
            throws GuacamoleException {

        // Retrieve password policy from environment
        PasswordPolicy policy = environment.getPasswordPolicy();

        // There is no maximum password age if 0
        int maxPasswordAge = policy.getMaximumAge();
        if (maxPasswordAge == 0)
            return false;

        // Determine whether password is expired based on maximum age
        return getPasswordAge(user) >= maxPasswordAge;

    }

}
