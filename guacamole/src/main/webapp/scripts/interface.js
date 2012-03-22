
// UI Definition
var GuacamoleUI = {

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
        "logout"       : document.getElementById("logout")

    },

    "containers": {
        "state"    : document.getElementById("statusDialog"),
        "clipboard": document.getElementById("clipboardDiv"),
        "keyboard" : document.getElementById("keyboardContainer")
    },
    
    "state"     : document.getElementById("statusText"),
    "clipboard" : document.getElementById("clipboard")

};

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

    GuacamoleUI.shadeMenu = function() {

        if (!menu_shaded) {

            var step = Math.floor(GuacamoleUI.menu.offsetHeight / 10) + 1;
            var offset = 0;
            menu_shaded = true;

            window.clearInterval(show_interval);
            shade_interval = window.setInterval(function() {

                offset -= step;
                GuacamoleUI.menu.style.top = offset + "px";

                if (offset <= -GuacamoleUI.menu.offsetHeight) {
                    window.clearInterval(shade_interval);
                    GuacamoleUI.menu.style.visiblity = "hidden";
                }

            }, 30);
        }

    };

    GuacamoleUI.showMenu = function() {

        if (menu_shaded) {

            var step = Math.floor(GuacamoleUI.menu.offsetHeight / 5) + 1;
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

                GuacamoleUI.menu.style.top = offset + "px";

            }, 30);
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

    /**
     * When GuacamoleUI.oskMode == OSK_MODE_NATIVE, "Show Keyboard" tries
     * to use the native OSK instead of the Guacamole OSK.
     */
    GuacamoleUI.OSK_MODE_NATIVE = 1;

    /**
     * When GuacamoleUI.oskMode == OSK_MODE_GUAC, "Show Keyboard" uses the 
     * Guacamole OSK, regardless of whether a native OSK is available.
     */
    GuacamoleUI.OSK_MODE_GUAC   = 2;

    // Assume no native OSK by default
    GuacamoleUI.oskMode = GuacamoleUI.OSK_MODE_GUAC;

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
        
        // If not shown ... action depends on OSK mode.
        else {

            // If we think the platform has a native OSK, use the event target to
            // cause it to display.
            if (GuacamoleUI.oskMode == GuacamoleUI.OSK_MODE_NATIVE) {

                // ...but use the Guac OSK if clicked again
                GuacamoleUI.oskMode = GuacamoleUI.OSK_MODE_GUAC;

                // Try to show native OSK by focusing eventTarget.
                GuacamoleUI.eventTarget.focus();
                return;

            }

            // Ensure event target is NOT focused if we are using the Guac OSK.
            GuacamoleUI.eventTarget.blur();

            GuacamoleUI.containers.keyboard.style.display = "block";
            GuacamoleUI.buttons.showKeyboard.textContent = "Hide Keyboard";

            // Automatically update size
            window.onresize = updateKeyboardSize;
            keyboardResizeInterval = window.setInterval(updateKeyboardSize, 30);

            updateKeyboardSize();
        }
        

    };

    // Logout
    GuacamoleUI.buttons.logout.onclick = function() {
        window.location.href = "logout";
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
            }, 325);

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
            }, 500);

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
                GuacamoleUI.showMenu();

            }, 800);

        }
    };

    GuacamoleUI.stopLongPressDetect = function() {
        window.clearTimeout(menuShowLongPressTimeout);
        menuShowLongPressTimeout = null;
    };

    // Reset event target (add content, reposition cursor in middle.
    GuacamoleUI.resetEventTarget = function() {
        GuacamoleUI.eventTarget.value = "GUAC";
        GuacamoleUI.eventTarget.selectionStart =
        GuacamoleUI.eventTarget.selectionEnd   = 2;
    };

    // Detect long-press at bottom of screen
    GuacamoleUI.display.addEventListener('touchstart', function(e) {
        
        // Close menu if shown
        GuacamoleUI.shadeMenu();
        
        // Record touch location
        if (e.touches.length == 1) {
            var touch = e.touches[0];
            long_press_start_x = touch.pageX;
            long_press_start_y = touch.pageY;
        }
        
        // Start detection
        GuacamoleUI.startLongPressDetect();
        
    }, true);

    // Stop detection if touch moves significantly
    GuacamoleUI.display.addEventListener('touchmove', function(e) {
        
        if (e.touches.length == 1) {

            // If touch distance from start exceeds threshold, cancel long press
            var touch = e.touches[0];
            if (Math.abs(touch.pageX - long_press_start_x) >= 10
                || Math.abs(touch.pageY - long_press_start_y) >= 10)
                GuacamoleUI.stopLongPressDetect();

        }
        
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
    GuacamoleUI.keyboard = new Guacamole.OnScreenKeyboard("layouts/en-us-qwerty-mobile.xml");
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

})();

// Tie UI events / behavior to a specific Guacamole client
GuacamoleUI.attach = function(guac) {

    var title_prefix = null;
    var connection_name = null 
    
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
       
            // Send mouse event
            guac.sendMouseState(mouseState);
            
        };

    // Keyboard
    var keyboard = new Guacamole.Keyboard(document);

    function disableKeyboard() {
        keyboard.onkeydown = null;
        keyboard.onkeyup = null;
    }

    function enableKeyboard() {
        keyboard.onkeydown = 
            function (keysym) {
          
                // If we're using native OSK, ensure event target is reset
                // on each key event.
                if (GuacamoleUI.oskMode == GuacamoleUI.OSK_MODE_NATIVE)
                    GuacamoleUI.resetEventTarget();
                
                guac.sendKeyEvent(1, keysym);
            };

        keyboard.onkeyup = 
            function (keysym) {
                guac.sendKeyEvent(0, keysym);
            };
    }

    // Enable keyboard by default
    enableKeyboard();

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
                GuacamoleUI.display.className =
                    GuacamoleUI.display.className.replace(/guac-loading/, '');

                GuacamoleUI.menu.className = "connected";

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

    // Handle clipboard events
    GuacamoleUI.clipboard.onchange = function() {

        var text = GuacamoleUI.clipboard.value;
        guac.setClipboard(text);

    };

    // Ignore keypresses when clipboard is focused
    GuacamoleUI.clipboard.onfocus = function() {
        disableKeyboard();
    };

    // Capture keypresses when clipboard is not focused
    GuacamoleUI.clipboard.onblur = function() {
        enableKeyboard();
    };

    // Server copy handler
    guac.onclipboard = function(data) {
        GuacamoleUI.clipboard.value = data;
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