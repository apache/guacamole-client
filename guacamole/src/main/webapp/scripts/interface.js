
// UI Definition
var GuacamoleUI = {

    "LOGOUT_PROMPT" :   "Logging out will disconnect all of your active "
                      + "Guacamole sessions. Are you sure you wish to log out?",

    /* Detection Constants */
    
    "LONG_PRESS_DETECT_TIMEOUT"     : 800, /* milliseconds */
    "LONG_PRESS_MOVEMENT_THRESHOLD" : 10,  /* pixels */
    "MENU_CLOSE_DETECT_TIMEOUT"     : 500, /* milliseconds */
    "MENU_OPEN_DETECT_TIMEOUT"      : 325, /* milliseconds */
    "KEYBOARD_AUTO_RESIZE_INTERVAL" : 30,  /* milliseconds */

    /* Animation Constants */

    "MENU_SHADE_STEPS"    : 10, /* frames */
    "MENU_SHADE_INTERVAL" : 30, /* milliseconds */
    "MENU_SHOW_STEPS"     : 5,  /* frames */
    "MENU_SHOW_INTERVAL"  : 30, /* milliseconds */

    /* OSK Mode Constants */
    "OSK_MODE_NATIVE" : 1, /* "Show Keyboard" will show the platform's native OSK */
    "OSK_MODE_GUAC"   : 2, /* "Show Keyboard" will show Guac's built-in OSK */

    /* UI Elements */

    "viewport"    : document.getElementById("viewportClone"),
    "display"     : document.getElementById("display"),
    "menu"        : document.getElementById("menu"),
    "menuControl" : document.getElementById("menuControl"),
    "touchMenu"   : document.getElementById("touchMenu"),
    "logo"        : document.getElementById("status-logo"),
    "eventTarget" : document.getElementById("eventTarget"),

    "buttons": {

        "showClipboard": document.getElementById("showClipboard"),
        "showKeyboard" : document.getElementById("showKeyboard"),
        "ctrlAltDelete": document.getElementById("ctrlAltDelete"),
        "reconnect"    : document.getElementById("reconnect"),
        "logout"       : document.getElementById("logout"),

        "touchShowClipboard" : document.getElementById("touchShowClipboard"),
        "touchShowKeyboard"  : document.getElementById("touchShowKeyboard"),
        "touchLogout"        : document.getElementById("touchLogout")

    },

    "containers": {
        "state"         : document.getElementById("statusDialog"),
        "clipboard"     : document.getElementById("clipboardDiv"),
        "touchClipboard": document.getElementById("touchClipboardDiv"),
        "keyboard"      : document.getElementById("keyboardContainer")
    },
    
    "state"          : document.getElementById("statusText"),
    "clipboard"      : document.getElementById("clipboard"),
    "touchClipboard" : document.getElementById("touchClipboard")

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

// Constant UI initialization and behavior
(function() {

    var menu_shaded = false;

    var shade_interval = null;
    var show_interval = null;

    // Cache error image (might not be available when error occurs)
    var guacErrorImage = new Image();
    guacErrorImage.src = "images/noguacamole-logo-24.png";

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


    GuacamoleUI.hideStatus = function() {
        removeClass(document.body, "guac-error");
        GuacamoleUI.containers.state.style.visibility = "hidden";
        GuacamoleUI.display.style.opacity = "1";
    };
    
    GuacamoleUI.showStatus = function(text) {
        removeClass(document.body, "guac-error");
        GuacamoleUI.containers.state.style.visibility = "visible";
        GuacamoleUI.state.textContent = text;
        GuacamoleUI.display.style.opacity = "1";
    };
    
    GuacamoleUI.showError = function(error) {
        addClass(document.body, "guac-error");
        GuacamoleUI.state.textContent = error;
        GuacamoleUI.display.style.opacity = "0.1";
    };

    GuacamoleUI.hideTouchMenu = function() {
        GuacamoleUI.touchMenu.style.display = "none";
    };

    function positionCentered(element) {
        element.style.left =
            ((GuacamoleUI.viewport.offsetWidth - element.offsetWidth) / 2
            + window.pageXOffset)
            + "px";

        element.style.top =
            ((GuacamoleUI.viewport.offsetHeight - element.offsetHeight) / 2
            + window.pageYOffset)
            + "px";
    }

    GuacamoleUI.showTouchMenu = function() {
        GuacamoleUI.touchMenu.style.display= "";
        positionCentered(GuacamoleUI.touchMenu);
    };

    GuacamoleUI.hideTouchClipboard = function() {
        GuacamoleUI.touchClipboard.blur();
        GuacamoleUI.containers.touchClipboard.style.visibility = "hidden";
    };

    GuacamoleUI.showTouchClipboard = function() {
        positionCentered(GuacamoleUI.containers.touchClipboard);
        GuacamoleUI.containers.touchClipboard.style.visibility = "visible";
    };

    GuacamoleUI.shadeMenu = function() {

        if (!menu_shaded) {

            var step = Math.floor(GuacamoleUI.menu.offsetHeight / GuacamoleUI.MENU_SHADE_STEPS) + 1;
            var offset = 0;
            menu_shaded = true;

            window.clearInterval(show_interval);
            shade_interval = window.setInterval(function() {

                offset -= step;

                GuacamoleUI.menu.style.transform =
                GuacamoleUI.menu.style.WebkitTransform =
                GuacamoleUI.menu.style.MozTransform =
                GuacamoleUI.menu.style.OTransform =
                GuacamoleUI.menu.style.msTransform =

                    "translateY(" + offset + "px)";

                if (offset <= -GuacamoleUI.menu.offsetHeight) {
                    window.clearInterval(shade_interval);
                    GuacamoleUI.menu.style.visiblity = "hidden";
                }

            }, GuacamoleUI.MENU_SHADE_INTERVAL);
        }

    };

    GuacamoleUI.showMenu = function() {

        if (menu_shaded) {

            var step = Math.floor(GuacamoleUI.menu.offsetHeight / GuacamoleUI.MENU_SHOW_STEPS) + 1;
            var offset = -GuacamoleUI.menu.offsetHeight;
            menu_shaded = false;
            GuacamoleUI.menu.style.visiblity = "";

            window.clearInterval(shade_interval);
            show_interval = window.setInterval(function() {

                offset += step;

                if (offset >= 0) {
                    offset = 0;
                    window.clearInterval(show_interval);
                }

                GuacamoleUI.menu.style.transform =
                GuacamoleUI.menu.style.WebkitTransform =
                GuacamoleUI.menu.style.MozTransform =
                GuacamoleUI.menu.style.OTransform =
                GuacamoleUI.menu.style.msTransform =

                    "translateY(" + offset + "px)";

            }, GuacamoleUI.MENU_SHOW_INTERVAL);
        }

    };

    // Show/Hide clipboard
    GuacamoleUI.buttons.showClipboard.onclick = function() {

        var displayed = GuacamoleUI.containers.clipboard.style.display;
        if (displayed != "block") {
            GuacamoleUI.containers.clipboard.style.display = "block";
            GuacamoleUI.buttons.showClipboard.innerHTML = "Hide Clipboard";
        }
        else {
            GuacamoleUI.containers.clipboard.style.display = "none";
            GuacamoleUI.buttons.showClipboard.innerHTML = "Show Clipboard";
            GuacamoleUI.clipboard.onchange();
        }

    };

    GuacamoleUI.buttons.touchShowClipboard.onclick = function() {
        GuacamoleUI.hideTouchMenu();
        GuacamoleUI.showTouchClipboard();
    };

    // Show/Hide keyboard
    var keyboardResizeInterval = null;
    GuacamoleUI.buttons.showKeyboard.onclick = function() {

        // If Guac OSK shown, hide it.
        var displayed = GuacamoleUI.containers.keyboard.style.display;
        if (displayed == "block") {
            GuacamoleUI.containers.keyboard.style.display = "none";
            GuacamoleUI.buttons.showKeyboard.textContent = "Show Keyboard";

            window.onresize = null;
            window.clearInterval(keyboardResizeInterval);
        }

        // Otherwise, show it
        else {

            // Ensure event target is NOT focused if we are using the Guac OSK.
            GuacamoleUI.eventTarget.blur();

            GuacamoleUI.containers.keyboard.style.display = "block";
            GuacamoleUI.buttons.showKeyboard.textContent = "Hide Keyboard";

            // Automatically update size
            window.onresize = updateKeyboardSize;
            keyboardResizeInterval = window.setInterval(updateKeyboardSize,
                GuacamoleUI.KEYBOARD_AUTO_RESIZE_INTERVAL);

            updateKeyboardSize();

        }

    };

    // Touch-specific keyboard show
    GuacamoleUI.buttons.touchShowKeyboard.onclick = 
        function(e) {

            // Center event target in case browser automatically centers
            // input fields on focus.
            GuacamoleUI.eventTarget.style.left =
                (window.pageXOffset + GuacamoleUI.viewport.offsetWidth / 2) + "px";

            GuacamoleUI.eventTarget.style.top =
                (window.pageYOffset + GuacamoleUI.viewport.offsetHeight / 2) + "px";

            GuacamoleUI.eventTarget.focus();
            GuacamoleUI.hideTouchMenu();

        };

    // Logout
    GuacamoleUI.buttons.logout.onclick =
    GuacamoleUI.buttons.touchLogout.onclick =
        function() {

            // Logout after warning user about session disconnect
            if (confirm(GuacamoleUI.LOGOUT_PROMPT)) {
                window.location.href = "logout";
                GuacamoleUI.hideTouchMenu();
            }
            
        };

    // Timeouts for detecting if users wants menu to open or close
    var detectMenuOpenTimeout = null;
    var detectMenuCloseTimeout = null;

    // Clear detection timeouts
    GuacamoleUI.resetMenuDetect = function() {

        if (detectMenuOpenTimeout != null) {
            window.clearTimeout(detectMenuOpenTimeout);
            detectMenuOpenTimeout = null;
        }

        if (detectMenuCloseTimeout != null) {
            window.clearTimeout(detectMenuCloseTimeout);
            detectMenuCloseTimeout = null;
        }

    };

    // Initiate detection of menu open action. If not canceled through some
    // user event, menu will open.
    GuacamoleUI.startMenuOpenDetect = function() {

        if (!detectMenuOpenTimeout) {

            // Clear detection state
            GuacamoleUI.resetMenuDetect();

            // Wait and then show menu
            detectMenuOpenTimeout = window.setTimeout(function() {

                // If menu opened via mouse, do not show native OSK
                GuacamoleUI.oskMode = GuacamoleUI.OSK_MODE_GUAC;

                GuacamoleUI.showMenu();
                detectMenuOpenTimeout = null;
            }, GuacamoleUI.MENU_OPEN_DETECT_TIMEOUT);

        }

    };

    // Initiate detection of menu close action. If not canceled through some
    // user mouse event, menu will close.
    GuacamoleUI.startMenuCloseDetect = function() {

        if (!detectMenuCloseTimeout) {

            // Clear detection state
            GuacamoleUI.resetMenuDetect();

            // Wait and then shade menu
            detectMenuCloseTimeout = window.setTimeout(function() {
                GuacamoleUI.shadeMenu();
                detectMenuCloseTimeout = null;
            }, GuacamoleUI.MENU_CLOSE_DETECT_TIMEOUT);

        }

    };

    // Show menu if mouseover any part of menu
    GuacamoleUI.menu.addEventListener('mouseover', GuacamoleUI.showMenu, true);

    // Stop detecting menu state change intents if mouse is over menu
    GuacamoleUI.menu.addEventListener('mouseover', GuacamoleUI.resetMenuDetect, true);

    // When mouse hovers over top of screen, start detection of intent to open menu
    GuacamoleUI.menuControl.addEventListener('mousemove', GuacamoleUI.startMenuOpenDetect, true);

    var long_press_start_x = 0;
    var long_press_start_y = 0;
    var menuShowLongPressTimeout = null;

    GuacamoleUI.startLongPressDetect = function() {

        if (!menuShowLongPressTimeout) {

            menuShowLongPressTimeout = window.setTimeout(function() {
                
                menuShowLongPressTimeout = null;

                // Assume native OSK if menu shown via long-press
                GuacamoleUI.oskMode = GuacamoleUI.OSK_MODE_NATIVE;
                GuacamoleUI.showTouchMenu();

            }, GuacamoleUI.LONG_PRESS_DETECT_TIMEOUT);

        }
    };

    GuacamoleUI.stopLongPressDetect = function() {
        window.clearTimeout(menuShowLongPressTimeout);
        menuShowLongPressTimeout = null;
    };

    // Detect long-press at bottom of screen
    GuacamoleUI.display.addEventListener('touchstart', function(e) {
        
        // Close menu if shown
        GuacamoleUI.shadeMenu();
        GuacamoleUI.hideTouchMenu();
        GuacamoleUI.hideTouchClipboard();
        
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

    // Close menu on mouse movement
    GuacamoleUI.display.addEventListener('mousemove', GuacamoleUI.startMenuCloseDetect, true);
    GuacamoleUI.display.addEventListener('mousedown', GuacamoleUI.startMenuCloseDetect, true);

    // Reconnect button
    GuacamoleUI.buttons.reconnect.onclick = function() {
        window.location.reload();
    };

    // On-screen keyboard
    GuacamoleUI.keyboard = new Guacamole.OnScreenKeyboard("layouts/en-us-qwerty.xml");
    GuacamoleUI.containers.keyboard.appendChild(GuacamoleUI.keyboard.getElement());

    // Function for automatically updating keyboard size
    var lastKeyboardWidth;
    function updateKeyboardSize() {
        var currentSize = GuacamoleUI.keyboard.getElement().offsetWidth;
        if (lastKeyboardWidth != currentSize) {
            GuacamoleUI.keyboard.resize(currentSize);
            lastKeyboardWidth = currentSize;
        }
    };

    // Turn off autocorrect and autocapitalization on eventTarget
    GuacamoleUI.eventTarget.setAttribute("autocorrect", "off");
    GuacamoleUI.eventTarget.setAttribute("autocapitalize", "off");

    // Automatically reposition event target on scroll
    window.addEventListener("scroll", function() {
        GuacamoleUI.eventTarget.style.left = window.pageXOffset + "px";
        GuacamoleUI.eventTarget.style.top = window.pageYOffset + "px";
    });

    // Query audio support
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

})();

// Tie UI events / behavior to a specific Guacamole client
GuacamoleUI.attach = function(guac) {

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

    // When mouse enters display, start detection of intent to close menu
    guac_display.addEventListener('mouseover', GuacamoleUI.startMenuCloseDetect, true);

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

    // Monitor whether the event target is focused
    var eventTargetFocused = false;

    // Save length for calculation of changed value
    var currentLength = GuacamoleUI.eventTarget.value.length;

    GuacamoleUI.eventTarget.onfocus = function() {
        eventTargetFocused = true;
        GuacamoleUI.eventTarget.value = "";
        currentLength = 0;
    };

    GuacamoleUI.eventTarget.onblur = function() {
        eventTargetFocused = false;
    };

    // If text is input directly into event target without typing (as with
    // voice input, for example), type automatically.
    GuacamoleUI.eventTarget.oninput = function(e) {

        // Calculate current length and change in length
        var oldLength = currentLength;
        currentLength = GuacamoleUI.eventTarget.value.length;
        
        // If deleted or replaced text, ignore
        if (currentLength <= oldLength)
            return;

        // Get changed text
        var text = GuacamoleUI.eventTarget.value.substring(oldLength);

        // Send each character
        for (var i=0; i<text.length; i++) {

            // Get char code
            var charCode = text.charCodeAt(i);

            // Convert to keysym
            var keysym = 0x003F; // Default to a question mark
            if (charCode >= 0x0000 && charCode <= 0x00FF)
                keysym = charCode;
            else if (charCode >= 0x0100 && charCode <= 0x10FFFF)
                keysym = 0x01000000 | charCode;

            // Send keysym only if not already pressed
            if (!keyboard.pressed[keysym]) {

                // Press and release key
                guac.sendKeyEvent(1, keysym);
                guac.sendKeyEvent(0, keysym);

            }

        }

    }

    function isTypableCharacter(keysym) {
        return (keysym & 0xFFFF00) != 0xFF00;
    }

    function disableKeyboard() {
        keyboard.onkeydown = null;
        keyboard.onkeyup = null;
    }

    function enableKeyboard() {

        keyboard.onkeydown = function (keysym) {
            guac.sendKeyEvent(1, keysym);
            return eventTargetFocused && isTypableCharacter(keysym);
        };

        keyboard.onkeyup = function (keysym) {
            guac.sendKeyEvent(0, keysym);
            return eventTargetFocused && isTypableCharacter(keysym);
        };

    }

    // Enable keyboard by default
    enableKeyboard();

    // Handle resize
    guac.onresize = function(width, height) {

        // Calculate scale to fit screen
        var fit_scale = Math.min(
            window.innerWidth / width,
            window.innerHeight / height
        );
          
        // Scale client
        guac.scale(fit_scale);

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
                GuacamoleUI.shadeMenu();
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

    // Disconnect on close
    window.onunload = function() {
        guac.disconnect();
    };

    // Send size events on resize
    window.onresize = function() {

        guac.sendSize(window.innerWidth, window.innerHeight);

        // Calculate scale to fit screen
        var fit_scale = Math.min(
            window.innerWidth / guac.getWidth(),
            window.innerHeight / guac.getHeight()
        );
          
        // Scale client
        guac.scale(fit_scale);

    };

    // Handle clipboard events
    GuacamoleUI.clipboard.onchange = function() {

        var text = GuacamoleUI.clipboard.value;
        GuacamoleUI.touchClipboard.value = text;
        guac.setClipboard(text);

    };

    GuacamoleUI.touchClipboard.onchange = function() {

        var text = GuacamoleUI.touchClipboard.value;
        GuacamoleUI.clipboard.value = text;
        guac.setClipboard(text);

    };

    // Ignore keypresses when clipboard is focused
    GuacamoleUI.clipboard.onfocus =
    GuacamoleUI.touchClipboard.onfocus = function() {
        disableKeyboard();
    };

    // Capture keypresses when clipboard is not focused
    GuacamoleUI.clipboard.onblur =
    GuacamoleUI.touchClipboard.onblur = function() {
        enableKeyboard();
    };

    // Server copy handler
    guac.onclipboard = function(data) {
        GuacamoleUI.clipboard.value = data;
        GuacamoleUI.touchClipboard.value = data;
    };

    GuacamoleUI.keyboard.onkeydown = function(keysym) {
        guac.sendKeyEvent(1, keysym);
    };

    GuacamoleUI.keyboard.onkeyup = function(keysym) {
        guac.sendKeyEvent(0, keysym);
    };

    // Send Ctrl-Alt-Delete
    GuacamoleUI.buttons.ctrlAltDelete.onclick = function() {

        var KEYSYM_CTRL   = 0xFFE3;
        var KEYSYM_ALT    = 0xFFE9;
        var KEYSYM_DELETE = 0xFFFF;

        guac.sendKeyEvent(1, KEYSYM_CTRL);
        guac.sendKeyEvent(1, KEYSYM_ALT);
        guac.sendKeyEvent(1, KEYSYM_DELETE);
        guac.sendKeyEvent(0, KEYSYM_DELETE);
        guac.sendKeyEvent(0, KEYSYM_ALT);
        guac.sendKeyEvent(0, KEYSYM_CTRL);
    };

};
