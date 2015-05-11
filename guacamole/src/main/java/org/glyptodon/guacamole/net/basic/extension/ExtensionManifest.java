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

package org.glyptodon.guacamole.net.basic.extension;

import java.util.Collection;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Java representation of the JSON manifest contained within every Guacamole
 * extension, identifying an extension and describing its contents.
 *
 * @author Michael Jumper
 */
public class ExtensionManifest {

    /**
     * The name of the extension associated with this manifest. The extension
     * name is human-readable, and used for display purposes only.
     */
    private String name;

    /**
     * The namespace of the extension associated with this manifest. The
     * extension namespace is required for internal use, and is used wherever
     * extension-specific files or resources need to be isolated from those of
     * other extensions.
     */
    private String namespace;

    /**
     * The paths of all JavaScript resources within the .jar of the extension
     * associated with this manifest.
     */
    private Collection<String> javaScriptPaths;

    /**
     * The paths of all CSS resources within the .jar of the extension
     * associated with this manifest.
     */
    private Collection<String> cssPaths;

    /**
     * The names of all authentication provider classes within this extension,
     * if any.
     */
    private Collection<String> authProviders;

    /**
     * Returns the name of the extension associated with this manifest. The
     * name is human-readable, for display purposes only, and is defined within
     * the manifest by the "name" property.
     *
     * @return
     *     The name of the extension associated with this manifest.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the extension associated with this manifest. The name
     * is human-readable, for display purposes only, and is defined within the
     * manifest by the "name" property.
     *
     * @param name
     *     The name of the extension associated with this manifest.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the namespace of the extension associated with this manifest.
     * The namespace is required for internal use, and is used wherever
     * extension-specific files or resources need to be isolated from those of
     * other extensions. It is defined within the manifest by the "namespace"
     * property.
     *
     * @return
     *     The namespace of the extension associated with this manifest.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets the namespace of the extension associated with this manifest. The
     * namespace is required for internal use, and is used wherever extension-
     * specific files or resources need to be isolated from those of other
     * extensions. It is defined within the manifest by the "namespace"
     * property.
     *
     * @param namespace
     *     The namespace of the extension associated with this manifest.
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Returns the paths to all JavaScript resources within the extension.
     * These paths are defined within the manifest by the "js" property as an
     * array of strings, where each string is a path relative to the root of
     * the extension .jar.
     *
     * @return
     *     A collection of paths to all JavaScript resources within the
     *     extension.
     */
    @JsonProperty("js")
    public Collection<String> getJavaScriptPaths() {
        return javaScriptPaths;
    }

    /**
     * Sets the paths to all JavaScript resources within the extension. These
     * paths are defined within the manifest by the "js" property as an array
     * of strings, where each string is a path relative to the root of the
     * extension .jar.
     *
     * @param javaScriptPaths
     *     A collection of paths to all JavaScript resources within the
     *     extension.
     */
    @JsonProperty("js")
    public void setJavaScriptPaths(Collection<String> javaScriptPaths) {
        this.javaScriptPaths = javaScriptPaths;
    }

    /**
     * Returns the paths to all CSS resources within the extension. These paths
     * are defined within the manifest by the "js" property as an array of
     * strings, where each string is a path relative to the root of the
     * extension .jar.
     *
     * @return
     *     A collection of paths to all CSS resources within the extension.
     */
    @JsonProperty("css")
    public Collection<String> getCSSPaths() {
        return cssPaths;
    }

    /**
     * Sets the paths to all CSS resources within the extension. These paths
     * are defined within the manifest by the "js" property as an array of
     * strings, where each string is a path relative to the root of the
     * extension .jar.
     *
     * @param cssPaths
     *     A collection of paths to all CSS resources within the extension.
     */
    @JsonProperty("css")
    public void setCSSPaths(Collection<String> cssPaths) {
        this.cssPaths = cssPaths;
    }

    /**
     * Returns the classnames of all authentication provider classes within the
     * extension. These classnames are defined within the manifest by the
     * "authProviders" property as an array of strings, where each string is an
     * authentication provider classname.
     *
     * @return
     *     A collection of classnames of all authentication providers within
     *     the extension.
     */
    public Collection<String> getAuthProviders() {
        return authProviders;
    }

    /**
     * Sets the classnames of all authentication provider classes within the
     * extension. These classnames are defined within the manifest by the
     * "authProviders" property as an array of strings, where each string is an
     * authentication provider classname.
     *
     * @param authProviders
     *     A collection of classnames of all authentication providers within
     *     the extension.
     */
    public void setAuthProviders(Collection<String> authProviders) {
        this.authProviders = authProviders;
    }

}
