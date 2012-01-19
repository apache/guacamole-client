
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
 * The Original Code is guacamole-common-js.
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

// Guacamole namespace
var Guacamole = Guacamole || {};

/**
 * Dynamic on-screen keyboard. Given the URL to an XML keyboard layout file,
 * this object will download and use the XML to construct a clickable on-screen
 * keyboard with its own key events.
 * 
 * @constructor
 * @param {String} url The URL of an XML keyboard layout file.
 */
Guacamole.OnScreenKeyboard = function(url) {

    var scaledElements = [];

    function ScaledElement(element, width, height, scaleFont) {

        this.width = width;
        this.height = height;

        this.scale = function(pixels) {
            element.style.width      = width  * pixels + "px";
            element.style.height     = height * pixels + "px";

            if (scaleFont) {
                element.style.lineHeight = height * pixels + "px";
                element.style.fontSize   = pixels + "px";
            }
        }

    }

    // For each child of element, call handler defined in next
    function parseChildren(element, next) {

        var children = element.childNodes;
        for (var i=0; i<children.length; i++) {

            // Get child node
            var child = children[i];

            // Do not parse text nodes
            if (!child.tagName)
                continue;

            // Get handler for node
            var handler = next[child.tagName];

            // Call handler if defined
            if (handler)
                handler(child);

            // Throw exception if no handler
            else
                throw new Error(
                      "Unexpected " + child.tagName
                    + " within " + element.tagName
                );

        }

    }

    // Create keyboard
    var keyboard = document.createElement("div");
    keyboard.className = "guacamole-keyboard";

    // Retrieve keyboard XML
    var xmlhttprequest = new XMLHttpRequest();
    xmlhttprequest.open("GET", url, false);
    xmlhttprequest.send(null);

    var xml = xmlhttprequest.responseXML;

    if (xml) {

        function parse_row(e) {
            
            var row = document.createElement("div");
            row.className = "guacamole-keyboard-row";

            parseChildren(e, {
                
                "column": function(e) {
                    row.appendChild(parse_column(e));
                },
                
                "gap": function parse_gap(e) {

                    // Get attributes
                    var gap_size = e.attributes["size"];

                    // Create element
                    var gap = document.createElement("div");
                    gap.className = "guacamole-keyboard-gap";

                    // Set gap size
                    var gap_units = 1;
                    if (gap_size)
                        gap_units = parseFloat(gap_size.value);

                    scaledElements.push(new ScaledElement(gap, gap_units, gap_units));
                    row.appendChild(gap);

                },
                
                "key": function parse_key(e) {
                    

                    // Get attributes
                    var key_size = e.attributes["size"];

                    // Create element
                    var key_element = document.createElement("div");
                    key_element.className = "guacamole-keyboard-key";
                    
                    // Create cap
                    var cap_element = document.createElement("div");
                    cap_element.className = "guacamole-keyboard-cap";
                    key_element.appendChild(cap_element);

                    // Set key size
                    var key_units = 1;
                    if (key_size)
                        key_units = parseFloat(key_size.value);

                    parseChildren(e, {
                        "cap": function cap(e) {

                            // Get attributes
                            var required = e.attributes["if"];
                            var modifier = e.attributes["modifier"];
                            var keysym   = e.attributes["keysym"];
                            var sticky   = e.attributes["sticky"];
                            
                            // Get content of key cap
                            var content = e.textContent;
                            
                            // If no requirements, then show cap by default
                            if (!required) {
                                cap_element.textContent = content;
                            }

                        }
                    });

                    scaledElements.push(new ScaledElement(key_element, key_units, 1, true));
                    row.appendChild(key_element);

                }
                
            });

            return row;

        }

        function parse_column(e) {
            
            var col = document.createElement("div");
            col.className = "guacamole-keyboard-column";

            var align = col.attributes["align"];

            if (align)
                col.style.textAlign = align.value;

            // Columns can only contain rows
            parseChildren(e, {
                "row": function(e) {
                    col.appendChild(parse_row(e));
                }
            });

            return col;

        }


        // Parse document
        var keyboard_element = xml.documentElement;
        if (keyboard_element.tagName != "keyboard")
            throw new Error("Root element must be keyboard");

        // Get attributes
        if (!keyboard_element.attributes["size"])
            throw new Error("size attribute is required for keyboard");
        
        var keyboard_size = parseFloat(keyboard_element.attributes["size"].value);
        
        parseChildren(keyboard_element, {
            
            "row": function(e) {
                keyboard.appendChild(parse_row(e));
            },
            
            "column": function(e) {
                keyboard.appendChild(parse_column(e));
            }
            
        });

    }

    // Do not allow selection or mouse movement to propagate/register.
    keyboard.onselectstart =
    keyboard.onmousemove   =
    keyboard.onmouseup     =
    keyboard.onmousedown   =
    function(e) {
        e.stopPropagation();
        return false;
    };


    this.onkeypressed  = null;
    this.onkeyreleased = null;

    this.getElement = function() {
        return keyboard;
    };

    this.resize = function(width) {

        // Get pixel size of a unit
        var unit = width / keyboard_size;

        // Resize all scaled elements
        for (var i=0; i<scaledElements.length; i++) {
            var scaledElement = scaledElements[i];
            scaledElement.scale(unit)
        }

    };

};

Guacamole.OnScreenKeyboard.Key = function() {

    /**
     * Width of the key, relative to the size of the keyboard.
     */
    this.size = 1;

    /**
     * Whether this key is currently pressed.
     */
    this.pressed = false;

    /**
     * An associative map of all caps by modifier.
     */
    this.caps = {};

}

Guacamole.OnScreenKeyboard.Cap = function(text, keycode, modifier) {
    
    /**
     * Modifier represented by this keycap
     */
    this.modifier = 0;
    
    /**
     * The text to be displayed within this keycap
     */
    this.text = text;

    /**
     * The keycode this cap sends when its associated key is pressed/released
     */
    this.keycode = keycode;

    // Set modifier if provided
    if (modifier) this.modifier = modifier;
    
}
