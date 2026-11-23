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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.guacamole.resource.ByteArrayResource;
import org.apache.guacamole.resource.ClassPathResource;
import org.apache.guacamole.resource.Resource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests which verify that NativeFederationService generates the Native
 * Federation manifest and configuration read by the web application from the
 * Native Federation configurations of extensions.
 */
public class NativeFederationServiceTest {

    /**
     * The Jackson parser for parsing the generated JSON and the expected JSON
     * of each test.
     */
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Returns a resource containing the given JSON, in place of the Native
     * Federation configuration file of an extension.
     *
     * @param json
     *     The JSON that the resource should contain.
     *
     * @return
     *     A resource containing the given JSON, with the mimetype
     *     "application/json".
     */
    private static Resource jsonResource(String json) {
        return new ByteArrayResource("application/json",
                json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses the given resource as JSON.
     *
     * @param resource
     *     The resource to parse.
     *
     * @return
     *     The root of the parsed JSON tree.
     *
     * @throws IOException
     *     If the resource cannot be read or does not contain valid JSON.
     */
    private static JsonNode parse(Resource resource) throws IOException {
        try (InputStream stream = resource.asStream()) {
            return mapper.readTree(stream);
        }
    }

    /**
     * Verifies that the given resource is JSON equal to the given expected
     * JSON, with the mimetype "application/json".
     *
     * @param expected
     *     The JSON that the resource is expected to contain.
     *
     * @param resource
     *     The resource to verify.
     *
     * @throws IOException
     *     If the resource cannot be read or does not contain valid JSON.
     */
    private static void assertJson(String expected, Resource resource)
            throws IOException {
        Assert.assertEquals("application/json", resource.getMimeType());
        Assert.assertEquals(mapper.readTree(expected), parse(resource));
    }

    /**
     * Verifies that the given resource is a JSON object whose properties are
     * the given namespaces, in the given order.
     *
     * @param resource
     *     The resource to verify.
     *
     * @param namespaces
     *     The namespaces expected as properties of the JSON object, in the
     *     expected order.
     *
     * @throws IOException
     *     If the resource cannot be read or does not contain valid JSON.
     */
    private static void assertNamespaces(Resource resource, String... namespaces)
            throws IOException {
        List<String> properties = new ArrayList<>();
        parse(resource).fieldNames().forEachRemaining(properties::add);
        Assert.assertEquals(Arrays.asList(namespaces), properties);
    }

    /**
     * Verifies that both the manifest and the configuration are empty JSON
     * objects if no extension has been added.
     *
     * @throws IOException
     *     If a generated resource does not contain valid JSON.
     */
    @Test
    public void testNoExtensions() throws IOException {
        NativeFederationService service = new NativeFederationService();
        assertJson("{}", service.getNativeFederationManifest());
        assertJson("{}", service.getNativeFederationConfiguration());
    }

    /**
     * Verifies that the manifest lists the remoteEntry.json of an added
     * extension relative to the web application, and that the configuration
     * contains the Native Federation configuration of that extension, both
     * under the namespace of the extension.
     *
     * @throws IOException
     *     If a generated resource does not contain valid JSON.
     */
    @Test
    public void testOneExtension() throws IOException {

        NativeFederationService service = new NativeFederationService();
        service.addNativeFederationConfigurationResource("example", jsonResource(
                "{ \"bootstrapFunctionName\": \"bootstrapExample\","
                + " \"pageTitle\": \"Example\", \"routePath\": \"example\" }"));

        assertJson("{ \"example\": \"./app/ext/example/remoteEntry.json\" }",
                service.getNativeFederationManifest());
        assertJson("{ \"example\": { \"bootstrapFunctionName\": \"bootstrapExample\","
                + " \"pageTitle\": \"Example\", \"routePath\": \"example\" } }",
                service.getNativeFederationConfiguration());

    }

    /**
     * Verifies that the manifest and the configuration contain all added
     * extensions, in the order the extensions were added, which is the order
     * in which the web application loads them and adds their routes.
     *
     * @throws IOException
     *     If a generated resource does not contain valid JSON.
     */
    @Test
    public void testTwoExtensions() throws IOException {

        // Add in non-alphabetical order, so that the order cannot be the
        // result of sorting
        NativeFederationService service = new NativeFederationService();
        service.addNativeFederationConfigurationResource("example-b",
                jsonResource("{ \"bootstrapFunctionName\": \"bootstrapB\","
                        + " \"pageTitle\": \"B\", \"routePath\": \"b\" }"));
        service.addNativeFederationConfigurationResource("example-a",
                jsonResource("{ \"bootstrapFunctionName\": \"bootstrapA\","
                        + " \"pageTitle\": \"A\", \"routePath\": \"a\" }"));

        Resource manifest = service.getNativeFederationManifest();
        assertJson("{"
                + " \"example-b\": \"./app/ext/example-b/remoteEntry.json\","
                + " \"example-a\": \"./app/ext/example-a/remoteEntry.json\""
                + " }", manifest);
        assertNamespaces(manifest, "example-b", "example-a");

        Resource configuration = service.getNativeFederationConfiguration();
        assertJson("{"
                + " \"example-b\": { \"bootstrapFunctionName\": \"bootstrapB\","
                + " \"pageTitle\": \"B\", \"routePath\": \"b\" },"
                + " \"example-a\": { \"bootstrapFunctionName\": \"bootstrapA\","
                + " \"pageTitle\": \"A\", \"routePath\": \"a\" }"
                + " }", configuration);
        assertNamespaces(configuration, "example-b", "example-a");

    }

    /**
     * Verifies that the configuration names the default bootstrap function,
     * "bootstrapExtension", for an extension whose Native Federation
     * configuration does not name one, and no page title for an extension
     * whose configuration does not define one.
     *
     * @throws IOException
     *     If a generated resource does not contain valid JSON.
     */
    @Test
    public void testDefaults() throws IOException {

        NativeFederationService service = new NativeFederationService();
        service.addNativeFederationConfigurationResource("example",
                jsonResource("{ \"routePath\": \"example\" }"));

        assertJson("{ \"example\": { \"bootstrapFunctionName\": \"bootstrapExtension\","
                + " \"pageTitle\": null, \"routePath\": \"example\" } }",
                service.getNativeFederationConfiguration());

    }

    /**
     * Verifies that an extension without Native Federation configuration is
     * listed neither in the manifest nor in the configuration.
     *
     * @throws IOException
     *     If a generated resource does not contain valid JSON.
     */
    @Test
    public void testNoConfiguration() throws IOException {

        NativeFederationService service = new NativeFederationService();
        service.addNativeFederationConfigurationResource("example", null);

        assertJson("{}", service.getNativeFederationManifest());
        assertJson("{}", service.getNativeFederationConfiguration());

    }

    /**
     * Verifies that an extension whose Native Federation configuration is
     * not valid JSON is listed neither in the manifest nor in the
     * configuration, while other extensions are still listed.
     *
     * @throws IOException
     *     If a generated resource does not contain valid JSON.
     */
    @Test
    public void testInvalidConfiguration() throws IOException {

        NativeFederationService service = new NativeFederationService();
        service.addNativeFederationConfigurationResource("broken",
                jsonResource("{ \"routePath\": "));
        service.addNativeFederationConfigurationResource("example",
                jsonResource("{ \"routePath\": \"example\" }"));

        assertNamespaces(service.getNativeFederationManifest(), "example");
        assertNamespaces(service.getNativeFederationConfiguration(), "example");

    }

    /**
     * Verifies that an extension whose manifest names a Native Federation
     * configuration file missing from the extension .jar is listed neither in
     * the manifest nor in the configuration, while other extensions are still
     * listed.
     *
     * @throws IOException
     *     If a generated resource does not contain valid JSON.
     */
    @Test
    public void testMissingConfiguration() throws IOException {

        NativeFederationService service = new NativeFederationService();
        service.addNativeFederationConfigurationResource("missing",
                new ClassPathResource(getClass().getClassLoader(),
                        "application/json", "missing/nativeFederationConfiguration.json"));
        service.addNativeFederationConfigurationResource("example",
                jsonResource("{ \"routePath\": \"example\" }"));

        assertNamespaces(service.getNativeFederationManifest(), "example");
        assertNamespaces(service.getNativeFederationConfiguration(), "example");

    }

}
