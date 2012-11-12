
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

// UI Definition
var GuacamoleUI = {

    /* Constants */
    
    "LONG_PRESS_DETECT_TIMEOUT"     : 800, /* milliseconds */
    "LONG_PRESS_MOVEMENT_THRESHOLD" : 10,  /* pixels */    
    "KEYBOARD_AUTO_RESIZE_INTERVAL" : 30,  /* milliseconds */

    /* UI Components */

    "viewport"    : document.getElementById("viewportClone"),
    "display"     : document.getElementById("display"),
    "logo"        : document.getElementById("status-logo"),
    "eventTarget" : document.getElementById("eventTarget"),

    "buttons": {
        "reconnect" : document.getElementById("reconnect")
    },

    "containers": {
        "state"     : document.getElementById("statusDialog"),
        "keyboard"  : document.getElementById("keyboardContainer"),
        "magnifier" : document.getElementById("magnifier")
    },
    
    "magnifier"    : document.getElementById("magnifier-display"),
    "state"        : document.getElementById("statusText"),
    "client"       : null,
    "sessionState" : new GuacamoleSessionState()

};

/**
 * Array of all supported audio mimetypes, populated when this script is
 * loaded.
 */
GuacamoleUI.supportedAudio = [];

/**
 * Array of all supported video mimetypes, populated when this script is
 * loaded.
 */
GuacamoleUI.supportedVideo = [];

// On-screen keyboard
GuacamoleUI.keyboard = new Guacamole.OnScreenKeyboard("layouts/en-us-qwerty.xml");
GuacamoleUI.containers.keyboard.appendChild(GuacamoleUI.keyboard.getElement());

GuacamoleUI.lastKeyboardWidth = 0;

// Function for automatically updating keyboard size
GuacamoleUI.updateKeyboardSize = function() {
    var currentSize = GuacamoleUI.keyboard.getElement().offsetWidth;
    if (GuacamoleUI.lastKeyboardWidth != currentSize) {
        GuacamoleUI.keyboard.resize(currentSize);
        GuacamoleUI.lastKeyboardWidth = currentSize;
    }
};

GuacamoleUI.keyboardResizeInterval = null;

// Show/Hide keyboard
GuacamoleUI.toggleKeyboard = function() {

    // If Guac OSK shown, hide it.
    var displayed = GuacamoleUI.containers.keyboard.style.display;
    if (displayed == "block") {

        // Hide keyboard
        GuacamoleUI.containers.keyboard.style.display = "none";

        // Stop automatic periodic update
        window.clearInterval(GuacamoleUI.keyboardResizeInterval);

    }

    // Otherwise, show it
    else {

        // Show keyboard
        GuacamoleUI.containers.keyboard.style.display = "block";

        // Start periodic update of keyboard size
        GuacamoleUI.keyboardResizeInterval = window.setInterval(
            GuacamoleUI.updateKeyboardSize,
            GuacamoleUI.KEYBOARD_AUTO_RESIZE_INTERVAL);

        // Update keyboard size initially
        GuacamoleUI.updateKeyboardSize();

    }

};

// If Node.classList is supported, implement addClass/removeClass using that
if (Node.classList) {

    GuacamoleUI.addClass = function(element, classname) {
        element.classList.add(classname);
    };
    
    GuacamoleUI.removeClass = function(element, classname) {
        element.classList.remove(classname);
    };
    
}

// Otherwise, implement own
else {

    GuacamoleUI.addClass = function(element, classname) {

        // Simply add new class
        element.className += " " + classname;

    };
    
    GuacamoleUI.removeClass = function(element, classname) {

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


GuacamoleUI.hideStatus = function() {
    GuacamoleUI.removeClass(document.body, "guac-error");
    GuacamoleUI.containers.state.style.visibility = "hidden";
    GuacamoleUI.display.style.opacity = "1";
};

GuacamoleUI.showStatus = function(text) {
    GuacamoleUI.removeClass(document.body, "guac-error");
    GuacamoleUI.containers.state.style.visibility = "visible";
    GuacamoleUI.state.textContent = text;
    GuacamoleUI.display.style.opacity = "1";
};

GuacamoleUI.showError = function(error) {
    GuacamoleUI.addClass(document.body, "guac-error");
    GuacamoleUI.state.textContent = error;
    GuacamoleUI.display.style.opacity = "0.1";
};

// Reconnect button
GuacamoleUI.buttons.reconnect.onclick = function() {
    window.location.reload();
};

// Query audio support
if (!GuacamoleUI.sessionState.getProperty("disable-sound"))
    (function () {
        var probably_supported = [];
        var maybe_supported = [];

        // Build array of supported audio formats
        [
            'audio/ogg; codecs="vorbis"',
            'audio/mp4; codecs="mp4a.40.5"',
            'audio/mpeg; codecs="mp3"',
            'audio/webm; codecs="vorbis"',
            'audio/wav; codecs=1'
        ].forEach(function(mimetype) {

            var audio = new Audio();
            var support_level = audio.canPlayType(mimetype);

            // Trim semicolon and trailer
            var semicolon = mimetype.indexOf(";");
            if (semicolon != -1)
                mimetype = mimetype.substring(0, semicolon);

            // Partition by probably/maybe
            if (support_level == "probably")
                probably_supported.push(mimetype);
            else if (support_level == "maybe")
                maybe_supported.push(mimetype);

        });

        Array.prototype.push.apply(GuacamoleUI.supportedAudio, probably_supported);
        Array.prototype.push.apply(GuacamoleUI.supportedAudio, maybe_supported);
    })();

GuacamoleUI.eventTarget.setAttribute("autocorrect", "off");
GuacamoleUI.eventTarget.setAttribute("autocapitalize", "off");

// Query video support
(function () {
    var probably_supported = [];
    var maybe_supported = [];

    // Build array of supported video formats
    [
        'video/ogg; codecs="theora, vorbis"',
        'video/mp4; codecs="avc1.4D401E, mp4a.40.5"',
        'video/webm; codecs="vp8.0, vorbis"'
    ].forEach(function(mimetype) {

        var video = document.createElement("video");
        var support_level = video.canPlayType(mimetype);

        // Trim semicolon and trailer
        var semicolon = mimetype.indexOf(";");
        if (semicolon != -1)
            mimetype = mimetype.substring(0, semicolon);

        // Partition by probably/maybe
        if (support_level == "probably")
            probably_supported.push(mimetype);
        else if (support_level == "maybe")
            maybe_supported.push(mimetype);

    });

    Array.prototype.push.apply(GuacamoleUI.supportedVideo, probably_supported);
    Array.prototype.push.apply(GuacamoleUI.supportedVideo, maybe_supported);
})();

// Tie UI events / behavior to a specific Guacamole client
GuacamoleUI.attach = function(guac) {

    GuacamoleUI.client = guac;

    var title_prefix = null;
    var connection_name = "Guacamole"; 
    
    var guac_display = guac.getDisplay();

    // Set document title appropriately, based on prefix and connection name
    function updateTitle() {

        // Use title prefix if present
        if (title_prefix) {
            
            document.title = title_prefix;

            // Include connection name, if present
            if (connection_name)
                document.title += " " + connection_name;

        }

        // Otherwise, just set to connection name
        else if (connection_name)
            document.title = connection_name;

    }

    guac_display.onclick = function(e) {
        e.preventDefault();
        return false;
    };

    // Mouse
    var mouse = new Guacamole.Mouse(guac_display);
    var touch = new Guacamole.Mouse.Touchpad(guac_display);
    touch.onmousedown = touch.onmouseup = touch.onmousemove =
    mouse.onmousedown = mouse.onmouseup = mouse.onmousemove =
        function(mouseState) {
       
            // Determine mouse position within view
            var mouse_view_x = mouseState.x + guac_display.offsetLeft - window.pageXOffset;
            var mouse_view_y = mouseState.y + guac_display.offsetTop  - window.pageYOffset;

            // Determine viewport dimensioins
            var view_width  = GuacamoleUI.viewport.offsetWidth;
            var view_height = GuacamoleUI.viewport.offsetHeight;

            // Determine scroll amounts based on mouse position relative to document

            var scroll_amount_x;
            if (mouse_view_x > view_width)
                scroll_amount_x = mouse_view_x - view_width;
            else if (mouse_view_x < 0)
                scroll_amount_x = mouse_view_x;
            else
                scroll_amount_x = 0;

            var scroll_amount_y;
            if (mouse_view_y > view_height)
                scroll_amount_y = mouse_view_y - view_height;
            else if (mouse_view_y < 0)
                scroll_amount_y = mouse_view_y;
            else
                scroll_amount_y = 0;

            // Scroll (if necessary) to keep mouse on screen.
            window.scrollBy(scroll_amount_x, scroll_amount_y);

            // Scale event by current scale
            var scaledState = new Guacamole.Mouse.State(
                    mouseState.x / guac.getScale(),
                    mouseState.y / guac.getScale(),
                    mouseState.left,
                    mouseState.middle,
                    mouseState.right,
                    mouseState.up,
                    mouseState.down);

            // Send mouse event
            guac.sendMouseState(scaledState);
            
        };

    // Keyboard
    var keyboard = new Guacamole.Keyboard(document);
    var show_keyboard_gesture_possible = true;

    keyboard.onkeydown = function (keysym) {
        guac.sendKeyEvent(1, keysym);

        // If key is NOT one of the expected keys, gesture not possible
        if (keysym != 0xFFE3 && keysym != 0xFFE9 && keysym != 0xFFE1)
            show_keyboard_gesture_possible = false;

    };

    keyboard.onkeyup = function (keysym) {
        guac.sendKeyEvent(0, keysym);

        // If lifting up on shift, toggle keyboard if rest of gesture
        // conditions satisfied
        if (show_keyboard_gesture_possible && keysym == 0xFFE1) {
            if (keyboard.pressed[0xFFE3] && keyboard.pressed[0xFFE9])
                GuacamoleUI.toggleKeyboard();
        }

        // Detect if no keys are pressed
        var reset_gesture = true;
        for (var pressed in keyboard.pressed) {
            reset_gesture = false;
            break;
        }

        // Reset gesture state if possible
        if (reset_gesture)
            show_keyboard_gesture_possible = true;

    };

    GuacamoleUI.keyboard.onkeydown = function(keysym) {
        guac.sendKeyEvent(1, keysym);
    };

    GuacamoleUI.keyboard.onkeyup = function(keysym) {
        guac.sendKeyEvent(0, keysym);
    };

    function isTypableCharacter(keysym) {
        return (keysym & 0xFFFF00) != 0xFF00;
    }

    function updateThumbnail() {

        // Get screenshot
        var canvas = guac.flatten();

        // Calculate scale of thumbnail (max 320x240, max zoom 100%)
        var scale = Math.min(
            320 / canvas.width,
            240 / canvas.height,
            1
        );

        // Create thumbnail canvas
        var thumbnail = document.createElement("canvas");
        thumbnail.width  = canvas.width*scale;
        thumbnail.height = canvas.height*scale;

        // Scale screenshot to thumbnail
        var context = thumbnail.getContext("2d");
        context.drawImage(canvas,
            0, 0, canvas.width, canvas.height,
            0, 0, thumbnail.width, thumbnail.height
        );

        // Save thumbnail to history
        var id = decodeURIComponent(window.location.search.substring(4));
        GuacamoleHistory.update(id, thumbnail.toDataURL());

    }

    function updateDisplayScale() {

        // If auto-fit is enabled, scale display
        if (GuacamoleUI.sessionState.getProperty("auto-fit")) {

            // Calculate scale to fit screen
            var fit_scale = Math.min(
                window.innerWidth / guac.getWidth(),
                window.innerHeight / guac.getHeight()
            );
              
            // Scale client
            if (fit_scale != guac.getScale())
                guac.scale(fit_scale);

        }

        // Otherwise, scale to 100%
        else if (guac.getScale() != 1.0)
            guac.scale(1.0);

    }

    // Handle resize
    guac.onresize = function(width, height) {
        updateDisplayScale();
    }

    // Handle client state change
    guac.onstatechange = function(clientState) {

        switch (clientState) {

            // Idle
            case 0:
                GuacamoleUI.showStatus("Idle.");
                title_prefix = "[Idle]";
                break;

            // Connecting
            case 1:
                GuacamoleUI.showStatus("Connecting...");
                title_prefix = "[Connecting...]";
                break;

            // Connected + waiting
            case 2:
                GuacamoleUI.showStatus("Connected, waiting for first update...");
                title_prefix = "[Waiting...]";
                break;

            // Connected
            case 3:

                GuacamoleUI.hideStatus();
                title_prefix = null;

                // Update clipboard with current data
                if (GuacamoleUI.sessionState.getProperty("clipboard"))
                    guac.setClipboard(GuacamoleUI.sessionState.getProperty("clipboard"));

                // Regularly update screenshot
                window.setInterval(updateThumbnail, 1000);

                break;

            // Disconnecting
            case 4:
                GuacamoleUI.showStatus("Disconnecting...");
                title_prefix = "[Disconnecting...]";
                break;

            // Disconnected
            case 5:
                GuacamoleUI.showStatus("Disconnected.");
                title_prefix = "[Disconnected]";
                break;

            // Unknown status code
            default:
                GuacamoleUI.showStatus("[UNKNOWN STATUS]");

        }

        updateTitle();
    };

    // Name instruction handler
    guac.onname = function(name) {
        connection_name = name;
        updateTitle();
    };

    // Error handler
    guac.onerror = function(error) {

        // Disconnect, if connected
        guac.disconnect();

        // Display error message
        GuacamoleUI.showError(error);
        
    };

    // Disconnect and update thumbnail on close
    window.onunload = function() {

        updateThumbnail();
        guac.disconnect();

    };

    // Send size events on resize
    window.onresize = function() {

        guac.sendSize(window.innerWidth, window.innerHeight);
        updateDisplayScale();
        GuacamoleUI.updateKeyboardSize();

    };

    // Server copy handler
    guac.onclipboard = function(data) {
        GuacamoleUI.sessionState.setProperty("clipboard", data);
    };

    GuacamoleUI.sessionState.onchange = function(old_state, new_state, name) {
        if (name == "clipboard")
            guac.setClipboard(new_state[name]);
        else if (name == "auto-fit")
            updateDisplayScale();

    };

    var long_press_start_x = 0;
    var long_press_start_y = 0;
    var longPressTimeout = null;

    GuacamoleUI.startLongPressDetect = function() {

        if (!longPressTimeout) {

            longPressTimeout = window.setTimeout(function() {
                longPressTimeout = null;
                if (GuacamoleUI.client.getScale() != 1.0)
                    GuacUI.StateManager.setState(GuacUI.Client.states.MAGNIFIER);
                else
                    GuacUI.StateManager.setState(GuacUI.Client.states.PAN);
            }, GuacamoleUI.LONG_PRESS_DETECT_TIMEOUT);

        }
    };

    GuacamoleUI.stopLongPressDetect = function() {
        window.clearTimeout(longPressTimeout);
        longPressTimeout = null;
    };

    // Detect long-press at bottom of screen
    GuacamoleUI.display.addEventListener('touchstart', function(e) {
        
        // Record touch location
        if (e.touches.length == 1) {
            var touch = e.touches[0];
            long_press_start_x = touch.screenX;
            long_press_start_y = touch.screenY;
        }
        
        // Start detection
        GuacamoleUI.startLongPressDetect();
        
    }, true);

    // Stop detection if touch moves significantly
    GuacamoleUI.display.addEventListener('touchmove', function(e) {
        
        // If touch distance from start exceeds threshold, cancel long press
        var touch = e.touches[0];
        if (Math.abs(touch.screenX - long_press_start_x) >= GuacamoleUI.LONG_PRESS_MOVEMENT_THRESHOLD
            || Math.abs(touch.screenY - long_press_start_y) >= GuacamoleUI.LONG_PRESS_MOVEMENT_THRESHOLD)
            GuacamoleUI.stopLongPressDetect();
        
    }, true);

    // Stop detection if press stops
    GuacamoleUI.display.addEventListener('touchend', GuacamoleUI.stopLongPressDetect, true);

};
