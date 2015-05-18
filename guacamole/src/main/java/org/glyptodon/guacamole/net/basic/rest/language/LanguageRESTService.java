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

package org.glyptodon.guacamole.net.basic.rest.language;

import com.google.inject.Inject;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.glyptodon.guacamole.net.basic.extension.LanguageResourceService;


/**
 * A REST Service for handling the listing of languages.
 * 
 * @author James Muehlner
 */
@Path("/languages")
@Produces(MediaType.APPLICATION_JSON)
public class LanguageRESTService {

    /**
     * Service for retrieving information regarding available language
     * resources.
     */
    @Inject
    private LanguageResourceService languageResourceService;

    /**
     * Returns a map of all available language keys to their corresponding
     * human-readable names.
     * 
     * @return
     *     A map of languages defined in the system, of language key to 
     *     display name.
     */
    @GET
    public Map<String, String> getLanguages() {
        return languageResourceService.getLanguageNames();
    }

}
