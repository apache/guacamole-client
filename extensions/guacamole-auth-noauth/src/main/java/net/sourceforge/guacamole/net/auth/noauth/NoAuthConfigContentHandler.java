
package net.sourceforge.guacamole.net.auth.noauth;

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-auth-noauth.
 *
 * The Initial Developer of the Original Code is
 * Laurent Meunier
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Laurent Meunier
 */
public class NoAuthConfigContentHandler extends DefaultHandler {

    private Map<String, GuacamoleConfiguration> configs = new HashMap<String, GuacamoleConfiguration>();
    private String current = null;
    private GuacamoleConfiguration currentConfig = null;

    public Map<String, GuacamoleConfiguration> getConfigs() {
        return Collections.unmodifiableMap(configs);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (localName.equals("config")) {
            configs.put(current, currentConfig);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        if (localName.equals("config")) {
            current = attributes.getValue("name");
            currentConfig = new GuacamoleConfiguration();
            currentConfig.setProtocol(attributes.getValue("protocol"));
        }
        
        else if (localName.equals("param")) {
            currentConfig.setParameter(attributes.getValue("name"), attributes.getValue("value"));
        }

    }

}
