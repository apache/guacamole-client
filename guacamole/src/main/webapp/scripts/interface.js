
// UI Definition
var GuacamoleUI = {

    "display"     : document.getElementById("display"),
    "menu"        : document.getElementById("menu"),
    "menuControl" : document.getElementById("menuControl"),
    "logo"        : document.getElementById("status-logo"),
    "state"       : document.getElementById("state"),

    "buttons": {

        "showClipboard": document.getElementById("showClipboard"),
        "showKeyboard" : document.getElementById("showKeyboard"),
        "ctrlAltDelete": document.getElementById("ctrlAltDelete"),
        "reconnect"    : document.getElementById("reconnect"),
        "logout"       : document.getElementById("logout")

    },

    "containers": {
        "error"    : document.getElementById("errorDialog"),
        "clipboard": document.getElementById("clipboardDiv"),
        "keyboard" : document.getElementById("keyboardContainer")
    },
    
    "error"     : document.getElementById("errorText"),
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

    GuacamoleUI.showError = function(error) {

        GuacamoleUI.menu.className = "error";
        GuacamoleUI.display.className += " guac-error";

        GuacamoleUI.logo.src = guacErrorImage.src;
        GuacamoleUI.error.textContent = error;
        GuacamoleUI.containers.error.style.visibility = "visible";

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

    // Show/Hide keyboard
    var keyboardResizeInterval = null;
    GuacamoleUI.buttons.showKeyboard.onclick = function() {

        var displayed = GuacamoleUI.containers.keyboard.style.display;
        if (displayed != "block") {
            GuacamoleUI.containers.keyboard.style.display = "block";
            GuacamoleUI.buttons.showKeyboard.textContent = "Hide Keyboard";

            // Automatically update size
            window.onresize = updateKeyboardSize;
            keyboardResizeInterval = window.setInterval(updateKeyboardSize, 30);

            updateKeyboardSize();
        }
        else {
            GuacamoleUI.containers.keyboard.style.display = "none";
            GuacamoleUI.buttons.showKeyboard.textContent = "Show Keyboard";

            window.onresize = null;
            window.clearInterval(keyboardResizeInterval);
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
    function resetMenuDetect() {

        if (detectMenuOpenTimeout != null) {
            window.clearTimeout(detectMenuOpenTimeout);
            detectMenuOpenTimeout = null;
        }

        if (detectMenuCloseTimeout != null) {
            window.clearTimeout(detectMenuCloseTimeout);
            detectMenuCloseTimeout = null;
        }

    }

    // Initiate detection of menu open action. If not canceled through some
    // user event, menu will open.
    function startMenuOpenDetect() {

        // Clear detection state
        resetMenuDetect();

        // Wait and then show menu
        detectMenuOpenTimeout = window.setTimeout(function() {
            GuacamoleUI.showMenu();
            detectMenuOpenTimeout = null;
        }, 325);

    }

    // Initiate detection of menu close action. If not canceled through some
    // user event, menu will close.
    function startMenuCloseDetect() {

        // Clear detection state
        resetMenuDetect();

        // Wait and then shade menu
        detectMenuCloseTimeout = window.setTimeout(function() {
            GuacamoleUI.shadeMenu();
            detectMenuCloseTimeout = null;
        }, 500);

    }

    // Show menu if mouseover any part of menu
    GuacamoleUI.menu.addEventListener('mouseover', GuacamoleUI.showMenu, true);

    // Stop detecting menu state change intents if mouse is over menu
    GuacamoleUI.menu.addEventListener('mouseover', resetMenuDetect, true);

    // When mouse hovers over top of screen, start detection of intent to open menu
    GuacamoleUI.menuControl.addEventListener('mousemove', startMenuOpenDetect, true);

    // When mouse enters display, start detection of intent to close menu
    GuacamoleUI.display.addEventListener('mouseover', startMenuCloseDetect, true);

    // Show menu if mouse leaves document
    document.addEventListener('mouseout', function(e) {
        
        // Get parent of the element the mouse pointer is leaving
       	if (!e) e = window.event;
        var target = e.relatedTarget || e.toElement;
        
        // Ensure target is not document nor child of document
        var targetParent = target;
        while (targetParent != null) {
            if (targetParent == document) return;
            targetParent = targetParent.parentNode;
        }

        // Start detection of intent to open menu
        startMenuOpenDetect();
 
    }, true);

    // Reconnect button
    GuacamoleUI.buttons.reconnect.onclick = function() {
        window.location.reload();
    };

    GuacamoleUI.display.onclick = function(e) {
        e.preventDefault();
        return false;
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

    // Mouse
    var mouse = new Guacamole.Mouse(GuacamoleUI.display);
    mouse.onmousedown = mouse.onmouseup = mouse.onmousemove =
        function(mouseState) {
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
                GuacamoleUI.state.textContent = "Idle."
                break;

            // Connecting
            case 1:
                GuacamoleUI.state.textContent = "Connecting...";
                break;

            // Connected + waiting
            case 2:
                GuacamoleUI.state.textContent = "Connected, waiting for first update...";
                break;

            // Connected
            case 3:
                
                GuacamoleUI.display.className =
                    GuacamoleUI.display.className.replace(/guac-loading/, '');

                GuacamoleUI.menu.className = "connected";
                GuacamoleUI.state.textContent = "Connected.";
                GuacamoleUI.shadeMenu();
                break;

            // Disconnecting
            case 4:
                GuacamoleUI.state.textContent = "Disconnecting...";
                break;

            // Disconnected
            case 5:
                GuacamoleUI.state.textContent = "Disconnected.";
                break;

            // Unknown status code
            default:
                GuacamoleUI.state.textContent = "Unknown";

        }
    };

    // Name instruction handler
    guac.onname = function(name) {
        document.title = name;
    };

    // Error handler
    guac.onerror = function(error) {

        // Disconnect, if connected
        guac.disconnect();

        // Display error message
        GuacamoleUI.showError(error);

        // Show error by desaturating display
        var layers = guac.getLayers();
        for (var i=0; i<layers.length; i++) {
            layers[i].filter(desaturateFilter);
        }

        // Filter for desaturation
        function desaturateFilter(data, width, height) {

            for (var i=0; i<data.length; i+=4) {

                // Get RGB values
                var r = data[i];
                var g = data[i+1];
                var b = data[i+2];

                // Desaturate
                var v = Math.max(r, g, b) / 2;
                data[i]   = v;
                data[i+1] = v;
                data[i+2] = v;

            }

        }
        
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

    GuacamoleUI.keyboard.onkeypress = function(keysym) {
        guac.sendKeyEvent(1, keysym);
    };

    GuacamoleUI.keyboard.onkeyrelease = function(keysym) {
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