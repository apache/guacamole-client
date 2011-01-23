
/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


function GuacamoleOnScreenKeyboard(url) {

    var tabIndex = 1;
    var allKeys = new Array();
    var modifierState = new function() {};

    function getKeySize(size) {
        return (5*size) + "ex";
    }

    function getCapSize(size) {
        return (5*size - 0.5) + "ex";
    }

    function clearModifiers() {

        // Send key release events for all pressed modifiers
        for (var k=0; k<allKeys.length; k++) {

            var key = allKeys[k];
            var cap = key.getCap();
            var modifier = cap.getModifier();

            if (modifier && isModifierActive(modifier) && !cap.isSticky() && key.isPressed())
                key.release();

        }

    }

    function setModifierReleased(modifier) {
        if (isModifierActive(modifier))
            modifierState[modifier]--;
    }

    function setModifierPressed(modifier) {
        if (modifierState[modifier] == null)
            modifierState[modifier] = 1;
        else
            modifierState[modifier]++;
    }

    function isModifierActive(modifier) {
        if (modifierState[modifier] > 0)
            return true;

        return false;
    }

    function toggleModifierPressed(modifier) {
        if (isModifierActive(modifier))
            setModifierReleased(modifier);
        else
            setModifierPressed(modifier);
    }

    function refreshAllKeysState() {
        for (var k=0; k<allKeys.length; k++)
            allKeys[k].refreshState();
    }

    function Key(key) {

        function Cap(cap) {

            // Displayed text
            var displayText = cap.textContent;
            
            // Keysym
            var keysym = null;
            if (cap.attributes["keysym"])
                keysym = parseInt(cap.attributes["keysym"].value);

            // If keysym not specified, get keysym from display text.
            else if (displayText.length == 1) {

                var charCode = displayText.charCodeAt(0);

                if (charCode >= 0x0000 && charCode <= 0x00FF)
                    keysym = charCode;

                else if (charCode >= 0x0100 && charCode <= 0x10FFFF)
                    keysym = 0x01000000 | charCode;
            }

            // Required modifiers for this keycap
            var reqMod = null;
            if (cap.attributes["if"])
                reqMod = cap.attributes["if"].value.split(",");


            // Modifier represented by this keycap
            var modifier = null;
            if (cap.attributes["modifier"])
                modifier = cap.attributes["modifier"].value;
            

            // Whether this key is sticky (toggles)
            // Currently only valid for modifiers.
            var sticky = false;
            if (cap.attributes["sticky"] && cap.attributes["sticky"].value == "true")
                sticky = true;

            this.getDisplayText = function() {
                return cap.textContent;
            };

            this.getKeySym = function() {
                return keysym;
            };

            this.getRequiredModifiers = function() {
                return reqMod;
            };

            this.getModifier = function() {
                return modifier;
            };

            this.isSticky = function() {
                return sticky;
            };

        }

        var size = null;
        if (key.attributes["size"])
            size = parseFloat(key.attributes["size"].value);

        var caps = key.getElementsByTagName("cap");
        var keycaps = new Array();
        for (var i=0; i<caps.length; i++)
            keycaps.push(new Cap(caps[i]));

        var rowKey = document.createElement("div");
        rowKey.className = "key";

        var keyCap = document.createElement("div");
        keyCap.className = "cap";
        rowKey.appendChild(keyCap);


        var STATE_RELEASED = 0;
        var STATE_PRESSED = 1;
        var state = STATE_RELEASED;

        rowKey.isPressed = function() {
            return state == STATE_PRESSED;
        }

        var currentCap = null;
        function refreshState(modifier) {

            // Find current cap
            currentCap = null;
            for (var j=0; j<keycaps.length; j++) {

                var keycap = keycaps[j];
                var required = keycap.getRequiredModifiers();

                var matches = true;

                // If modifiers required, make sure all modifiers are active
                if (required) {

                    for (var k=0; k<required.length; k++) {
                        if (!isModifierActive(required[k])) {
                            matches = false;
                            break;
                        }
                    }

                }

                if (matches)
                    currentCap = keycap;

            }

            rowKey.className = "key";

            if (currentCap.getModifier())
                rowKey.className += " modifier";

            if (currentCap.isSticky())
                rowKey.className += " sticky";

            if (isModifierActive(currentCap.getModifier()))
                rowKey.className += " active";

            if (state == STATE_PRESSED)
                rowKey.className += " pressed";

            keyCap.textContent = currentCap.getDisplayText();
        }
        rowKey.refreshState = refreshState;

        rowKey.getCap = function() {
            return currentCap;
        };

        refreshState();

        // Set size
        if (size) {
            rowKey.style.width = getKeySize(size);
            keyCap.style.width = getCapSize(size);
        }



        // Set pressed, if released
        function press() {

            if (state == STATE_RELEASED) {

                state = STATE_PRESSED;

                var keysym = currentCap.getKeySym();
                var modifier = currentCap.getModifier();
                var sticky = currentCap.isSticky();

                if (keyPressedHandler && keysym)
                    keyPressedHandler(keysym);

                if (modifier) {

                    // If sticky modifier, toggle
                    if (sticky) 
                        toggleModifierPressed(modifier);

                    // Otherwise, just set on.
                    else 
                        setModifierPressed(modifier);

                    refreshAllKeysState();
                }
                else
                    refreshState();
            }

        }
        rowKey.press = press;


        // Set released, if pressed 
        function release() {

            if (state == STATE_PRESSED) {

                state = STATE_RELEASED;

                var keysym = currentCap.getKeySym();
                var modifier = currentCap.getModifier();
                var sticky = currentCap.isSticky();

                if (keyReleasedHandler && keysym)
                    keyReleasedHandler(keysym);

                if (modifier) {

                    // If not sticky modifier, release modifier
                    if (!sticky) {
                        setModifierReleased(modifier);
                        refreshAllKeysState();
                    }
                    else
                        refreshState();

                }
                else {
                    refreshState();

                    // If not a modifier, also release all pressed modifiers
                    clearModifiers();
                }

            }

        }
        rowKey.release = release;

        // Toggle press/release states
        function toggle() {
            if (state == STATE_PRESSED)
                release();
            else
                press();
        }


        // Send key press on mousedown
        rowKey.onmousedown = function(e) {

            e.stopPropagation();

            var modifier = currentCap.getModifier();
            var sticky = currentCap.isSticky();

            // Toggle non-sticky modifiers
            if (modifier && !sticky)
                toggle();

            // Press all others
            else
                press();

            return false;
        };

        // Send key release on mouseup/out
        rowKey.onmouseout =
        rowKey.onmouseout =
        rowKey.onmouseup = function(e) {

            e.stopPropagation();

            var modifier = currentCap.getModifier();
            var sticky = currentCap.isSticky();

            // Release non-modifiers and sticky modifiers
            if (!modifier || sticky)
                release();

            return false;
        };

        rowKey.onselectstart = function() { return false; };

        return rowKey;

    }

    function Gap(gap) {

        var keyboardGap = document.createElement("div");
        keyboardGap.className = "gap";
        keyboardGap.textContent = " ";

        var size = null;
        if (gap.attributes["size"])
            size = parseFloat(gap.attributes["size"].value);

        if (size) {
            keyboardGap.style.width = getKeySize(size);
            keyboardGap.style.height = getKeySize(size);
        }

        return keyboardGap;

    }

    function Row(row) {

        var keyboardRow = document.createElement("div");
        keyboardRow.className = "row";

        var children = row.childNodes;
        for (var j=0; j<children.length; j++) {
            var child = children[j];

            // <row> can contain <key> or <column>
            if (child.tagName == "key") {
                var key = new Key(child);
                keyboardRow.appendChild(key);
                allKeys.push(key);
            }
            else if (child.tagName == "gap") {
                var gap = new Gap(child);
                keyboardRow.appendChild(gap);
            }
            else if (child.tagName == "column") {
                var col = new Column(child);
                keyboardRow.appendChild(col);
            }

        }

        return keyboardRow;

    }

    function Column(col) {

        var keyboardCol = document.createElement("div");
        keyboardCol.className = "col";

        var align = null;
        if (col.attributes["align"])
            align = col.attributes["align"].value;

        var children = col.childNodes;
        for (var j=0; j<children.length; j++) {
            var child = children[j];

            // <column> can only contain <row> 
            if (child.tagName == "row") {
                var row = new Row(child);
                keyboardCol.appendChild(row);
            }

        }

        if (align)
            keyboardCol.style.textAlign = align;

        return keyboardCol;

    }



    // Create keyboard
    var keyboard = document.createElement("div");
    keyboard.className = "keyboard";


    // Retrieve keyboard XML
    var xmlhttprequest = new XMLHttpRequest();
    xmlhttprequest.open("GET", url, false);
    xmlhttprequest.send(null);

    var xml = xmlhttprequest.responseXML;

    if (xml) {

        // Parse document
        var root = xml.documentElement;
        if (root) {

            var children = root.childNodes;
            for (var i=0; i<children.length; i++) {
                var child = children[i];

                // <keyboard> can contain <row> or <column>
                if (child.tagName == "row") {
                    keyboard.appendChild(new Row(child));
                }
                else if (child.tagName == "column") {
                    keyboard.appendChild(new Column(child));
                }

            }

        }

    }

    var keyPressedHandler = null;
    var keyReleasedHandler = null;

    keyboard.setKeyPressedHandler = function(kh) { keyPressedHandler = kh; };
    keyboard.setKeyReleasedHandler = function(kh) { keyReleasedHandler = kh; };

    // Do not allow selection or mouse movement to propagate/register.
    keyboard.onselectstart =
    keyboard.onmousemove   =
    keyboard.onmouseup     =
    keyboard.onmousedown   =
    function(e) {
        e.stopPropagation();
        return false;
    };

    return keyboard;
}

