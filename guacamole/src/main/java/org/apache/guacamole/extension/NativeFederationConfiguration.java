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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TODO
 */
public class NativeFederationConfiguration {

    public static final String DEFAULT_BOOTSTRAP_FUNCTION_NAME = "bootsrapExtension";

    /**
     * The name of the function that will be called by the shell to bootstrap the extension.
     * Defaults to {@link NativeFederationConfiguration#DEFAULT_BOOTSTRAP_FUNCTION_NAME}.
     */
    private String bootstrapFunctionName = DEFAULT_BOOTSTRAP_FUNCTION_NAME;

    /**
     * The title of the page to be displayed in the browser tab when the route is active.
     * If not set, the title of the main application will be used. Specific routes can
     * override this value by setting the title property on the route.
     */
    private String pageTitle;

    /**
     * The path where the routes of the extension should be mounted.
     */
    private String routePath;


    public String getBootstrapFunctionName() {
        return bootstrapFunctionName;
    }

    @JsonProperty(value = "bootstrapFunctionName", defaultValue = DEFAULT_BOOTSTRAP_FUNCTION_NAME)
    public void setBootstrapFunctionName(String bootstrapFunctionName) {
        this.bootstrapFunctionName = bootstrapFunctionName;
    }

    @JsonProperty("pageTitle")
    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    @JsonProperty("routePath")
    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public String getPageTitle() {
        return pageTitle;
    }


    public String getRoutePath() {
        return routePath;
    }


}
