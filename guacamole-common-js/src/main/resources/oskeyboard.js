
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
 * The Original Code is guac-common-js.
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

    var on_screen_keyboard = this;

    var scaledElements = [];
    
    var modifiers = {};
    var currentModifier = 1;

    // Function for adding a class to an element
    var addClass;

    // Function for removing a class from an element
    var removeClass;

    // If Node.classList is supported, implement addClass/removeClass using that
    if (Node.classList) {

        addClass = function(element, classname) {
            element.classList.add(classname);
        };
        
        removeClass = function(element, classname) {
            element.classList.remove(classname);
        };
        
    }

    // Otherwise, implement own
    else {

        addClass = function(element, classname) {

            // Simply add new class
            element.className += " " + classname;

        };
        
        removeClass = function(element, classname) {

            // Filter out classes with given name
            element.className = element.className.replace(/([^ ]+)[ ]*/g,
                function(match, testClassname, spaces, offset, string) {

                    // If same class, remove
                    if (testClassname == classname)
                        return "";

                    // Otherwise, allow
                    return match;
                    
                }
            );

        };
        
    }

    // Returns a unique power-of-two value for the modifier with the
    // given name. The same value will be returned for the same modifier.
    function getModifier(name) {
        
        var value = modifiers[name];
        if (!value) {

            // Get current modifier, advance to next
            value = currentModifier;
            currentModifier <<= 1;

            // Store value of this modifier
            modifiers[name] = value;

        }

        return value;
            
    }

    function ScaledElement(element, width, height, scaleFont) {

        this.width = width;
        this.height = height;

        this.scale = function(pixels) {
            element.style.width      = (width  * pixels) + "px";
            element.style.height     = (height * pixels) + "px";

            if (scaleFont) {
                element.style.lineHeight = (height * pixels) + "px";
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
    keyboard.className = "guac-keyboard";

    // Retrieve keyboard XML
    var xmlhttprequest = new XMLHttpRequest();
    xmlhttprequest.open("GET", url, false);
    xmlhttprequest.send(null);

    var xml = xmlhttprequest.responseXML;

    if (xml) {

        function parse_row(e) {
            
            var row = document.createElement("div");
            row.className = "guac-keyboard-row";

            parseChildren(e, {
                
                "column": function(e) {
                    row.appendChild(parse_column(e));
                },
                
                "gap": function parse_gap(e) {

                    // Create element
                    var gap = document.createElement("div");
                    gap.className = "guac-keyboard-gap";

                    // Set gap size
                    var gap_units = 1;
                    if (e.getAttribute("size"))
                        gap_units = parseFloat(e.getAttribute("size"));

                    scaledElements.push(new ScaledElement(gap, gap_units, gap_units));
                    row.appendChild(gap);

                },
                
                "key": function parse_key(e) {
                    
                    // Create element
                    var key_element = document.createElement("div");
                    key_element.className = "guac-keyboard-key";

                    // Append class if specified
                    if (e.getAttribute("class"))
                        key_element.className += " " + e.getAttribute("class");

                    // Position keys using container div
                    var key_container_element = document.createElement("div");
                    key_container_element.className = "guac-keyboard-key-container";
                    key_container_element.appendChild(key_element);

                    // Create key
                    var key = new Guacamole.OnScreenKeyboard.Key();

                    // Set key size
                    var key_units = 1;
                    if (e.getAttribute("size"))
                        key_units = parseFloat(e.getAttribute("size"));

                    key.size = key_units;

                    parseChildren(e, {
                        "cap": function parse_cap(e) {

                            // TODO: Handle "sticky" attribute
                            
                            // Get content of key cap
                            var content = e.textContent || e.text;

                            // If read as blank, assume cap is a single space.
                            if (content.length == 0)
                                content = " ";
                            
                            // Get keysym
                            var real_keysym = null;
                            if (e.getAttribute("keysym"))
                                real_keysym = parseInt(e.getAttribute("keysym"));

                            // If no keysym specified, try to get from key content
                            else if (content.length == 1) {

                                var charCode = content.charCodeAt(0);
                                if (charCode >= 0x0000 && charCode <= 0x00FF)
                                    real_keysym = charCode;
                                else if (charCode >= 0x0100 && charCode <= 0x10FFFF)
                                    real_keysym = 0x01000000 | charCode;

                            }
                            
                            // Create cap
                            var cap = new Guacamole.OnScreenKeyboard.Cap(content, real_keysym);

                            if (e.getAttribute("modifier"))
                                cap.modifier = e.getAttribute("modifier");
                            
                            // Create cap element
                            var cap_element = document.createElement("div");
                            cap_element.className = "guac-keyboard-cap";
                            cap_element.textContent = content;
                            key_element.appendChild(cap_element);

                            // Append class if specified
                            if (e.getAttribute("class"))
                                cap_element.className += " " + e.getAttribute("class");

                            // Get modifier value
                            var modifierValue = 0;
                            if (e.getAttribute("if")) {

                                // Get modifier value for specified comma-delimited
                                // list of required modifiers.
                                var requirements = e.getAttribute("if").split(",");
                                for (var i=0; i<requirements.length; i++) {
                                    modifierValue |= getModifier(requirements[i]);
                                    addClass(cap_element, "guac-keyboard-requires-" + requirements[i]);
                                    addClass(key_element, "guac-keyboard-uses-" + requirements[i]);
                                }

                            }

                            // Store cap
                            key.modifierMask |= modifierValue;
                            key.caps[modifierValue] = cap;

                        }
                    });

                    scaledElements.push(new ScaledElement(key_container_element, key_units, 1, true));
                    row.appendChild(key_container_element);

                    // Set up click handler for key
                    function press(e) {

                        // Press key if not yet pressed
                        if (!key.pressed) {

                            addClass(key_element, "guac-keyboard-pressed");

                            // Get current cap based on modifier state
                            var cap = key.getCap(on_screen_keyboard.modifiers);

                            // Update modifier state
                            if (cap.modifier) {

                                // Construct classname for modifier
                                var modifierClass = "guac-keyboard-modifier-" + cap.modifier;
                                var modifierFlag = getModifier(cap.modifier);

                                // Toggle modifier state
                                on_screen_keyboard.modifiers ^= modifierFlag;

                                // Activate modifier if pressed
                                if (on_screen_keyboard.modifiers & modifierFlag) {
                                    
                                    addClass(keyboard, modifierClass);
                                    
                                    // Send key event
                                    if (on_screen_keyboard.onkeydown && cap.keysym)
                                        on_screen_keyboard.onkeydown(cap.keysym);

                                }

                                // Deactivate if not pressed
                                else {

                                    removeClass(keyboard, modifierClass);
                                    
                                    // Send key event
                                    if (on_screen_keyboard.onkeyup && cap.keysym)
                                        on_screen_keyboard.onkeyup(cap.keysym);

                                }

                            }

                            // If not modifier, send key event now
                            else if (on_screen_keyboard.onkeydown && cap.keysym)
                                on_screen_keyboard.onkeydown(cap.keysym);

                            // Mark key as pressed
                            key.pressed = true;

                        }

                        e.preventDefault();

                    };

                    function release(e) {

                        // Release key if currently pressed
                        if (key.pressed) {

                            // Get current cap based on modifier state
                            var cap = key.getCap(on_screen_keyboard.modifiers);

                            removeClass(key_element, "guac-keyboard-pressed");

                            // Send key event if not a modifier key
                            if (!cap.modifier && on_screen_keyboard.onkeyup && cap.keysym)
                                on_screen_keyboard.onkeyup(cap.keysym);

                            // Mark key as released
                            key.pressed = false;

                        }

                        e.preventDefault();

                    };

                    key_element.addEventListener("mousedown", press, true);
                    key_element.addEventListener("touchstart", press, true);

                    key_element.addEventListener("mouseup", release, true);
                    key_element.addEventListener("mouseout", release, true);
                    key_element.addEventListener("touchend", release, true);

                }
                
            });

            return row;

        }

        function parse_column(e) {
            
            var col = document.createElement("div");
            col.className = "guac-keyboard-column";

            if (col.getAttribute("align"))
                col.style.textAlign = col.getAttribute("align");

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
        if (!keyboard_element.getAttribute("size"))
            throw new Error("size attribute is required for keyboard");
        
        var keyboard_size = parseFloat(keyboard_element.getAttribute("size"));
        
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

    /**
     * State of all modifiers.
     */
    this.modifiers = 0;

    this.onkeydown = null;
    this.onkeyup   = null;

    this.getElement = function() {
        return keyboard;
    };

    this.resize = function(width) {

        // Get pixel size of a unit
        var unit = Math.floor(width * 10 / keyboard_size) / 10;

        // Resize all scaled elements
        for (var i=0; i<scaledElements.length; i++) {
            var scaledElement = scaledElements[i];
            scaledElement.scale(unit)
        }

    };

};

Guacamole.OnScreenKeyboard.Key = function() {

    var key = this;

    /**
     * Whether this key is currently pressed.
     */
    this.pressed = false;

    /**
     * Width of the key, relative to the size of the keyboard.
     */
    this.size = 1;

    /**
     * An associative map of all caps by modifier.
     */
    this.caps = {};

    /**
     * Bit mask with all modifiers that affect this key set.
     */
    this.modifierMask = 0;

    /**
     * Given the bitwise OR of all active modifiers, returns the key cap
     * which applies.
     */
    this.getCap = function(modifier) {
        return key.caps[modifier & key.modifierMask];
    };

}

Guacamole.OnScreenKeyboard.Cap = function(text, keysym, modifier) {
    
    /**
     * Modifier represented by this keycap
     */
    this.modifier = null;
    
    /**
     * The text to be displayed within this keycap
     */
    this.text = text;

    /**
     * The keysym this cap sends when its associated key is pressed/released
     */
    this.keysym = keysym;

    // Set modifier if provided
    if (modifier) this.modifier = modifier;
    
}
