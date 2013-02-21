
package net.sourceforge.guacamole.protocol;

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
 * The Original Code is guacamole-common.
 *
 * The Initial Developer of the Original Code is
 * Michael Jumper.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * An abstract representation of Guacamole client information, including all
 * information required by the Guacamole protocol during the preamble.
 *
 * @author Michael Jumper
 */
public class GuacamoleClientInformation {

    /**
     * The optimal screen width requested by the client, in pixels.
     */
    private int optimalScreenWidth  = 1024;

    /**
     * The optimal screen height requested by the client, in pixels.
     */
    private int optimalScreenHeight = 768;

    /**
     * The list of audio mimetypes reported by the client to be supported.
     */
    private List<String> audioMimetypes = new ArrayList<String>();

    /**
     * The list of audio mimetypes reported by the client to be supported.
     */
    private List<String> videoMimetypes = new ArrayList<String>();

    /**
     * Returns the optimal screen width requested by the client, in pixels.
     * @return The optimal screen width requested by the client, in pixels.
     */
    public int getOptimalScreenWidth() {
        return optimalScreenWidth;
    }

    /**
     * Sets the client's optimal screen width.
     * @param optimalScreenWidth The optimal screen width of the client.
     */
    public void setOptimalScreenWidth(int optimalScreenWidth) {
        this.optimalScreenWidth = optimalScreenWidth;
    }

    /**
     * Returns the optimal screen height requested by the client, in pixels.
     * @return The optimal screen height requested by the client, in pixels.
     */
    public int getOptimalScreenHeight() {
        return optimalScreenHeight;
    }

    /**
     * Sets the client's optimal screen height.
     * @param optimalScreenHeight The optimal screen height of the client.
     */
    public void setOptimalScreenHeight(int optimalScreenHeight) {
        this.optimalScreenHeight = optimalScreenHeight;
    }

    /**
     * Returns the list of audio mimetypes supported by the client. To add or
     * removed supported mimetypes, the list returned by this function can be
     * modified.
     *
     * @return The set of audio mimetypes supported by the client.
     */
    public List<String> getAudioMimetypes() {
        return audioMimetypes;
    }

    /**
     * Returns the list of video mimetypes supported by the client. To add or
     * removed supported mimetypes, the list returned by this function can be
     * modified.
     *
     * @return The set of video mimetypes supported by the client.
     */
    public List<String> getVideoMimetypes() {
        return videoMimetypes;
    }

}
