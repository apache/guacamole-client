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

package org.apache.guacamole.extension;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Representation of the Guacamole webapp version. This is used both to
 * represent the expected minimum webapp version required by an extension and
 * to represent the running version of Guacamole.
 */
public class GuacamoleVersion {

    /**
     * Regular expression that splits a version number into its major, minor,
     * and patch components, as well as an optional version suffix.
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(.+)?");

    /**
     * The version of this copy of the Guacamole web application. This value is
     * substituted by Maven during the build.
     */
    public static final GuacamoleVersion RUNNING_VERSION = new GuacamoleVersion("${project.version}");

    /**
     * The first (leftmost) version number of a Guacamole version. This is the
     * number of greatest significance and denotes compatibility. If two
     * Guacamole versions have the same major number, extensions for the older
     * version should still work with the newer version.
     */
    private final int major;

    /**
     * The second (middle) version number of a Guacamole version. If two
     * Guacamole versions have the same major number but different minor
     * numbers, they would be expected to differ in the features available.
     */
    private final int minor;

    /**
     * The third (rightmost) version number of a Guacamole version. This is the
     * number of least significance and denotes minor changes or fixes. If
     * two Guacamole versions have the same major and minor numbers but
     * different patch numbers, the newer version would be expected to have
     * fixes or improvements that correct issues affecting the older version,
     * but would not be expected to have major new features.
     */
    private final int patch;

    /**
     * An arbitrary suffix that might be appended by downstream package
     * maintainers or vendors. A Guacamole version with a particular suffix
     * would be expected to have patches applied relative to the version without
     * the suffix but is otherwise compatible (as if the patch or minor numbers
     * differed instead of the suffix).
     */
    private final String suffix;

    /**
     * Creates a new GuacamoleVersion representing the version of Guacamole
     * that has the given version string. The version string must be in the
     * format "MAJOR.MINOR.PATCH" with an optional and arbitrary version
     * suffix.
     *
     * @param versionStr
     *     The Guacamole version string to parse.
     *
     * @throws IllegalArgumentException
     *     If the version string is not in the correct format.
     */
    public GuacamoleVersion(String versionStr) throws IllegalArgumentException {

        // Use all-zeroes without suffix to represent the wildcard version
        if ("*".equals(versionStr)) {
            major = minor = patch = 0;
            suffix = null;
        }

        // Extract major/minor/patch and optional suffix from version string
        else {

            Matcher versionMatcher = VERSION_PATTERN.matcher(versionStr);
            if (!versionMatcher.matches())
                throw new IllegalArgumentException("Format of version "
                        + "string \"" + versionStr + "\" is not valid");

            major = Integer.parseInt(versionMatcher.group(1));
            minor = Integer.parseInt(versionMatcher.group(2));
            patch = Integer.parseInt(versionMatcher.group(3));
            suffix = versionMatcher.group(4);

        }

    }

    /**
     * Returns whether this Guacamole version is expected to be compatible with
     * an extension that requires at least the given Guacamole version.
     *
     * @param required
     *     The minimum Guacamole version required.
     *
     * @return
     *     true if this Guacamole version is expected to be compatible with an
     *     extension requiring at least the given version, false otherwise.
     */
    public boolean provides(GuacamoleVersion required) {

        // Wildcard version always matches
        if (required.major == 0 && required.minor == 0 && required.patch == 0
                && required.suffix == null)
            return true;

        // Major version component MUST be identical
        if (this.major != required.major)
            return false;

        // Minor version component must be the same or greater than the
        // requirement
        if (this.minor < required.minor)
            return false;

        // If both major and minor are identical, the patch version component
        // must be the same or greater than the requirement
        if (this.minor == required.minor && this.patch < required.patch)
            return false;

        // Any suffix denotes vendor-specific patching that must be assumed to
        // be an arbitrary requirement only satisfied by that suffix. If no
        // suffix is required, any suffix of the current Guacamole version can
        // just be ignored.

        if (required.suffix == null)
            return true;

        return required.suffix.equals(this.suffix);

    }

}
