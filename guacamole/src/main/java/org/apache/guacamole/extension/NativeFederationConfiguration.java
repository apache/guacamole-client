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

/**
 * Java representation of the JSON Native Federation configuration contained
 * within an extension, describing how the web application bootstraps the
 * Angular part of that extension and where it adds the routes of the
 * extension.
 */
public class NativeFederationConfiguration {

    /**
     * The name of the bootstrap function of an extension whose Native
     * Federation configuration does not define one.
     */
    public static final String DEFAULT_BOOTSTRAP_FUNCTION_NAME = "bootstrapExtension";

    /**
     * The name under which the extension exposes its bootstrap function
     * through Native Federation. The web application calls this function on
     * startup to obtain the routes of the extension.
     */
    private String bootstrapFunctionName = DEFAULT_BOOTSTRAP_FUNCTION_NAME;

    /**
     * The title of the browser tab while a route of the extension is active,
     * or null if the title of the web application should be used. Routes of
     * the extension may define their own title instead.
     */
    private String pageTitle;

    /**
     * The path below which the web application adds the routes of the
     * extension, such as "angular-extension".
     */
    private String routePath;

    /**
     * Returns the name under which the extension exposes its bootstrap
     * function through Native Federation. It is defined within the
     * configuration by the "bootstrapFunctionName" property and is
     * "bootstrapExtension" if that property is absent.
     *
     * @return
     *     The name of the bootstrap function of the extension.
     */
    public String getBootstrapFunctionName() {
        return bootstrapFunctionName;
    }

    /**
     * Sets the name under which the extension exposes its bootstrap function
     * through Native Federation. It is defined within the configuration by
     * the "bootstrapFunctionName" property.
     *
     * @param bootstrapFunctionName
     *     The name of the bootstrap function of the extension.
     */
    public void setBootstrapFunctionName(String bootstrapFunctionName) {
        this.bootstrapFunctionName = bootstrapFunctionName;
    }

    /**
     * Returns the title of the browser tab while a route of the extension is
     * active. It is defined within the configuration by the "pageTitle"
     * property.
     *
     * @return
     *     The title of the browser tab while a route of the extension is
     *     active, or null if the title of the web application should be used.
     */
    public String getPageTitle() {
        return pageTitle;
    }

    /**
     * Sets the title of the browser tab while a route of the extension is
     * active. It is defined within the configuration by the "pageTitle"
     * property.
     *
     * @param pageTitle
     *     The title of the browser tab while a route of the extension is
     *     active, or null if the title of the web application should be used.
     */
    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    /**
     * Returns the path below which the web application adds the routes of
     * the extension. It is defined within the configuration by the
     * "routePath" property.
     *
     * @return
     *     The path below which the web application adds the routes of the
     *     extension.
     */
    public String getRoutePath() {
        return routePath;
    }

    /**
     * Sets the path below which the web application adds the routes of the
     * extension. It is defined within the configuration by the "routePath"
     * property.
     *
     * @param routePath
     *     The path below which the web application adds the routes of the
     *     extension.
     */
    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

}
