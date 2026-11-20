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

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests which verify GuacamoleVersion correctly parses and compares
 * Guacamole versions.
 */
public class GuacamoleVersionTest {

    /**
     * Verifies that trivially valid version numbers are correctly parsed.
     */
    @Test
    public void testValidVersion() {
        GuacamoleVersion version = new GuacamoleVersion("1.2.3");
        Assert.assertTrue(version.provides(version));
    }

    /**
     * Verifies that invalid version numbers correctly fail to parse.
     */
    @Test
    public void testInvalidVersion() {
        for (String invalid : Arrays.asList("1.2")) {
            try {
                GuacamoleVersion version = new GuacamoleVersion(invalid);
                Assert.fail("Invalid Guacamole version string \""
                        + invalid + "\" should have failed to parse.");
            }
            catch (IllegalArgumentException e) {
                // Expected
            }
        }
    }

    /**
     * Verifies that newer versions are recognized as providing older versions
     * when they differ by minor number (but not the other way around).
     */
    @Test
    public void testMinorNewerProvidesOlder() {

        GuacamoleVersion olderVersion = new GuacamoleVersion("1.2.3");
        GuacamoleVersion newerVersion = new GuacamoleVersion("1.22.3");

        Assert.assertTrue(newerVersion.provides(olderVersion));
        Assert.assertFalse(olderVersion.provides(newerVersion));

    }

    /**
     * Verifies that newer versions are recognized as providing older versions
     * when they differ by patch number (but not the other way around).
     */
    @Test
    public void testPatchNewerProvidesOlder() {

        GuacamoleVersion olderVersion = new GuacamoleVersion("1.2.3");
        GuacamoleVersion newerVersion = new GuacamoleVersion("1.2.7");

        Assert.assertTrue(newerVersion.provides(olderVersion));
        Assert.assertFalse(olderVersion.provides(newerVersion));

    }

    /**
     * Verifies that two versions are NOT recognized as providing each other
     * when they differ by major number (regardless of which is newer).
     */
    @Test
    public void testPatchMajorIncompatible() {

        GuacamoleVersion olderVersion = new GuacamoleVersion("1.2.3");
        GuacamoleVersion newerVersion = new GuacamoleVersion("2.2.3");

        Assert.assertFalse(newerVersion.provides(olderVersion));
        Assert.assertFalse(olderVersion.provides(newerVersion));

    }

    /**
     * Verifies that a version number with an arbitrary suffix is recognized
     * as providing that same version without the suffix (but not the other way
     * around).
     */
    @Test
    public void testCompatibleSuffix() {

        GuacamoleVersion olderVersion = new GuacamoleVersion("1.2.3");
        GuacamoleVersion newerVersion = new GuacamoleVersion("1.2.3-foo");

        Assert.assertTrue(newerVersion.provides(olderVersion));
        Assert.assertFalse(olderVersion.provides(newerVersion));

    }

    /**
     * Verifies that two version numbers that differ by arbitrary suffix are
     * NOT recognized as providing each other, even when the version numbers
     * are otherwise the same.
     */
    @Test
    public void testIncompatibleSuffix() {

        GuacamoleVersion olderVersion = new GuacamoleVersion("1.2.3-bar");
        GuacamoleVersion newerVersion = new GuacamoleVersion("1.2.3-foo");

        Assert.assertFalse(newerVersion.provides(olderVersion));
        Assert.assertFalse(olderVersion.provides(newerVersion));

    }

    /**
     * Verifies that the special wildcard version is provided by any version,
     * but not the other way around.
     */
    @Test
    public void testWildcardVersion() {

        GuacamoleVersion specificVersion = new GuacamoleVersion("1.2.3-foo");
        GuacamoleVersion wildcardVersion = new GuacamoleVersion("*");

        Assert.assertTrue(specificVersion.provides(wildcardVersion));
        Assert.assertFalse(wildcardVersion.provides(specificVersion));

    }

}
