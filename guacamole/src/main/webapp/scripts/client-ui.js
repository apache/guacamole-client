/*
 * Copyright (C) 2013 Glyptodon LLC
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

/**
 * Client UI root object.
 */
GuacUI.Client = {

    /**
     * Collection of all Guacamole client UI states.
     */
    "states": {

        /**
         * The normal default Guacamole client UI mode
         */
        "INTERACTIVE"      : 0,

        /**
         * Same as INTERACTIVE except with visible on-screen keyboard.
         */
        "OSK"              : 1,

        /**
         * No on-screen keyboard, but a visible magnifier.
         */
        "MAGNIFIER"        : 2,

        /**
         * Arrows and a draggable view.
         */
        "PAN"              : 3,

        /**
         * Same as PAN, but with visible native OSK.
         */
        "PAN_TYPING"       : 4,

        /**
         * Precursor to PAN_TYPING, like PAN, except does not pan the
         * screen, but rather hints at how to start typing.
         */
        "WAIT_TYPING"      : 5

    },

    /**
     * Enumeration of all tunnel-specific error messages for each applicable
     * error code.
     */
    "tunnel_errors": {

        0x0201: "The Guacamole server has rejected this connection attempt  \
                 because there are too many active connections. Please wait \
                 a few minutes and try again.",

        0x0202: "The connection has been closed because the server is taking \
                 too long to respond. This is usually caused by network      \
                 problems, such as a spotty wireless signal, or slow network \
                 speeds. Please check your network connection and try again  \
                 or contact your system administrator.",

        0x0203: "The server encountered an error and has closed the \
                 connection. Please try again or contact your       \
                 system administrator.",

        0x0204: "The requested connection does not exist. Please check the \
                 connection name and try again.",

        0x0205: "This connection is currently in use, and concurrent access to \
                 this connection is not allowed. Please try again later.",

        0x0301: "You do not have permission to access this connection because \
                 you are not logged in. Please log in and try again.",

        0x0303: "You do not have permission to access this connection. If you \
                 require access, please ask your system administrator to add  \
                 you the list of allowed users, or check your system settings.",

        0x0308: "The Guacamole server has closed the connection because there \
                 has been no response from your browser for long enough that  \
                 it appeared to be disconnected. This is commonly caused by   \
                 network problems, such as spotty wireless signal, or simply  \
                 very slow network speeds. Please check your network and try  \
                 again.",

        0x031D: "The Guacamole server is denying access to this connection \
                 because you have exhausted the limit for simultaneous     \
                 connection use by an individual user. Please close one or \
                 more connections and try again.",

        "DEFAULT": "An internal error has occurred within the Guacamole \
                    server, and the connection has been terminated. If  \
                    the problem persists, please notify your system     \
                    administrator, or check your system logs."

    },

    /**
     * Enumeration of all client-specific error messages for each applicable
     * error code.
     */
    "client_errors": {

        0x0201: "This connection has been closed because the server is busy. \
                 Please wait a few minutes and try again.",

        0x0202: "The Guacamole server has closed the connection because the \
                 remote desktop is taking too long to respond. Please try   \
                 again or contact your system administrator.",

        0x0203: "The remote desktop server encountered an error and has closed \
                 the connection. Please try again or contact your system       \
                 administrator.",

        0x0205: "This connection has been closed because it conflicts with \
                 another connection. Please try again later.",

        0x0301: "Log in failed. Please reconnect and try again.",

        0x0303: "You do not have permission to access this connection. If you \
                 require access, please ask your system administrator to add  \
                 you the list of allowed users, or check your system settings.",

        0x0308: "The Guacamole server has closed the connection because there \
                 has been no response from your browser for long enough that  \
                 it appeared to be disconnected. This is commonly caused by   \
                 network problems, such as spotty wireless signal, or simply  \
                 very slow network speeds. Please check your network and try  \
                 again.",

        0x031D: "The Guacamole server is denying access to this connection \
                 because you have exhausted the limit for simultaneous     \
                 connection use by an individual user. Please close one or \
                 more connections and try again.",

        "DEFAULT": "An internal error has occurred within the Guacamole \
                    server, and the connection has been terminated. If  \
                    the problem persists, please notify your system     \
                    administrator, or check your system logs."

    },

    /**
     * Enumeration of all error messages for each applicable error code. This
     * list is specific to file uploads.
     */
    "upload_errors": {

        0x0100: "File transfer is either not supported or not enabled. Please \
                 contact your system administrator, or check your system logs.",

        0x0201: "Too many files are currently being transferred. Please wait \
                 for existing transfers to complete, and then try again.",

        0x0202: "The file cannot be transferred because the remote desktop \
                 server is taking too long to respond. Please try again or \
                 or contact your system administrator.",

        0x0203: "The remote desktop server encountered an error during \
                 transfer. Please try again or contact your system     \
                 administrator.",

        0x0204: "The destination for the file transfer does not exist. Please \
                 check that the destionation exists and try again.",

        0x0205: "The destination for the file transfer is currently locked. \
                 Please wait for any in-progress tasks to complete and try  \
                 again.",

        0x0301: "You do not have permission to upload this file because you \
                 are not logged in. Please log in and try again.",

        0x0303: "You do not have permission to upload this file. If you \
                 require access, please check your system settings, or  \
                 check with your system administrator.",

        0x0308: "The file transfer has stalled. This is commonly caused by \
                 network problems, such as spotty wireless signal, or      \
                 simply very slow network speeds. Please check your        \
                 network and try again.",

        0x031D: "Too many files are currently being transferred. Please wait \
                 for existing transfers to complete, and then try again.",

        "DEFAULT": "An internal error has occurred within the Guacamole \
                    server, and the connection has been terminated. If  \
                    the problem persists, please notify your system     \
                    administrator, or check your system logs.",

    },

    /**
     * All error codes for which automatic reconnection is appropriate when a
     * tunnel error occurs.
     */
    "tunnel_auto_reconnect": {
        0x0200: true,
        0x0202: true,
        0x0203: true,
        0x0308: true
    },

    /**
     * All error codes for which automatic reconnection is appropriate when a
     * client error occurs.
     */
    "client_auto_reconnect": {
        0x0200: true,
        0x0202: true,
        0x0203: true,
        0x0301: true,
        0x0308: true
    },

    /* Constants */
    
    "LONG_PRESS_DETECT_TIMEOUT"     : 800, /* milliseconds */
    "LONG_PRESS_MOVEMENT_THRESHOLD" : 10,  /* pixels */    
    "KEYBOARD_AUTO_RESIZE_INTERVAL" : 30,  /* milliseconds */
    "RECONNECT_PERIOD"              : 15,  /* seconds */

    /* UI Components */

    "viewport"          : document.getElementById("viewportClone"),
    "display"           : document.getElementById("display"),
    "notification_area" : document.getElementById("notificationArea"),

    /* Expected Input Rectangle */

    "expected_input_x"      : 0,
    "expected_input_y"      : 0,
    "expected_input_width"  : 1,
    "expected_input_height" : 1,

    "connectionName"  : "Guacamole",
    "overrideAutoFit" : false,
    "attachedClient"  : null

};

/**
 * Component which displays a magnified (100% zoomed) client display.
 * 
 * @constructor
 * @augments GuacUI.DraggableComponent
 */
GuacUI.Client.Magnifier = function() {

    /**
     * Reference to this magnifier.
     * @private
     */
    var guac_magnifier = this;

    /**
     * Large background div which will block touch events from reaching the
     * client while also providing a click target to deactivate the
     * magnifier.
     * @private
     */
    var magnifier_background = GuacUI.createElement("div", "magnifier-background");

    /**
     * Container div for the magnifier, providing a clipping rectangle.
     * @private
     */
    var magnifier = GuacUI.createChildElement(magnifier_background,
        "div", "magnifier");

    /**
     * Canvas which will contain the static image copy of the display at time
     * of show.
     * @private
     */
    var magnifier_display = GuacUI.createChildElement(magnifier, "canvas");

    /**
     * Context of magnifier display.
     * @private
     */
    var magnifier_context = magnifier_display.getContext("2d");

    /*
     * This component is draggable.
     */
    GuacUI.DraggableComponent.apply(this, [magnifier]);

    // Ensure transformations on display originate at 0,0
    magnifier.style.transformOrigin =
    magnifier.style.webkitTransformOrigin =
    magnifier.style.MozTransformOrigin =
    magnifier.style.OTransformOrigin =
    magnifier.style.msTransformOrigin =
        "0 0";

    /*
     * Reposition magnifier display relative to own position on screen.
     */

    this.onmove = function(x, y) {

        var width = magnifier.offsetWidth;
        var height = magnifier.offsetHeight;

        // Update contents relative to new position
        var clip_x = x
            / (window.innerWidth - width) * (GuacUI.Client.attachedClient.getWidth() - width);
        var clip_y = y
            / (window.innerHeight - height) * (GuacUI.Client.attachedClient.getHeight() - height);
       
        magnifier_display.style.WebkitTransform =
        magnifier_display.style.MozTransform =
        magnifier_display.style.OTransform =
        magnifier_display.style.msTransform =
        magnifier_display.style.transform = "translate("
            + (-clip_x) + "px, " + (-clip_y) + "px)";

        /* Update expected input rectangle */
        GuacUI.Client.expected_input_x = clip_x;
        GuacUI.Client.expected_input_y = clip_y;
        GuacUI.Client.expected_input_width  = width;
        GuacUI.Client.expected_input_height = height;

    };

    /*
     * Copy display and add self to body on show.
     */

    this.show = function() {

        // Copy displayed image
        magnifier_display.width = GuacUI.Client.attachedClient.getWidth();
        magnifier_display.height = GuacUI.Client.attachedClient.getHeight();
        magnifier_context.drawImage(GuacUI.Client.attachedClient.flatten(), 0, 0);

        // Show magnifier container
        document.body.appendChild(magnifier_background);

    };

    /*
     * Remove self from body on hide.
     */

    this.hide = function() {

        // Hide magnifier container
        document.body.removeChild(magnifier_background);

    };

    /*
     * If the user clicks on the background, switch to INTERACTIVE mode.
     */

    magnifier_background.addEventListener("click", function() {
        GuacUI.StateManager.setState(GuacUI.Client.states.INTERACTIVE);
    }, true);

    /*
     * If the user clicks on the magnifier, switch to PAN_TYPING mode.
     */

    magnifier.addEventListener("click", function(e) {
        GuacUI.StateManager.setState(GuacUI.Client.states.PAN_TYPING);
        e.stopPropagation();
    }, true);

};

/*
 * We inherit from GuacUI.DraggableComponent.
 */
GuacUI.Client.Magnifier.prototype = new GuacUI.DraggableComponent();

GuacUI.StateManager.registerComponent(
    new GuacUI.Client.Magnifier(),
    GuacUI.Client.states.MAGNIFIER
);

/**
 * Zoomed Display, a pseudo-component.
 * 
 * @constructor
 * @augments GuacUI.Component
 */
GuacUI.Client.ZoomedDisplay = function() {

    this.show = function() {
        GuacUI.Client.overrideAutoFit = true;
        GuacUI.Client.updateDisplayScale();
    };

    this.hide = function() {
        GuacUI.Client.overrideAutoFit = false;
        GuacUI.Client.updateDisplayScale();
    };

};

GuacUI.Client.ZoomedDisplay.prototype = new GuacUI.Component();

/*
 * Zoom the main display during PAN and PAN_TYPING modes.
 */

GuacUI.StateManager.registerComponent(
    new GuacUI.Client.ZoomedDisplay(),
    GuacUI.Client.states.PAN,
    GuacUI.Client.states.PAN_TYPING
);

/**
 * Type overlay UI. This component functions to provide a means of activating
 * the keyboard, when neither panning nor magnification make sense.
 * 
 * @constructor
 * @augments GuacUI.Component
 */
GuacUI.Client.TypeOverlay = function() {

    /**
     * Overlay which will provide the means of scrolling the screen.
     */
    var type_overlay = GuacUI.createElement("div", "type-overlay");

    /*
     * Add exit button
     */

    var start = GuacUI.createChildElement(type_overlay, "p", "hint");
    start.textContent = "Tap here to type, or tap the screen to cancel.";

    // Begin typing when user clicks hint
    start.addEventListener("click", function(e) {
        GuacUI.StateManager.setState(GuacUI.Client.states.PAN_TYPING);
        e.stopPropagation();
    }, false);

    this.show = function() {
        document.body.appendChild(type_overlay);
    };

    this.hide = function() {
        document.body.removeChild(type_overlay);
    };

    /*
     * Cancel when user taps screen
     */

    type_overlay.addEventListener("click", function(e) {
        GuacUI.StateManager.setState(GuacUI.Client.states.INTERACTIVE);
        e.stopPropagation();
    }, false);

};

GuacUI.Client.TypeOverlay.prototype = new GuacUI.Component();

/*
 * Show the type overlay during WAIT_TYPING mode only
 */

GuacUI.StateManager.registerComponent(
    new GuacUI.Client.TypeOverlay(),
    GuacUI.Client.states.WAIT_TYPING
);

/**
 * Pan overlay UI. This component functions to receive touch events and
 * translate them into scrolling of the main UI.
 * 
 * @constructor
 * @augments GuacUI.Component
 */
GuacUI.Client.PanOverlay = function() {

    /**
     * Overlay which will provide the means of scrolling the screen.
     */
    var pan_overlay = GuacUI.createElement("div", "pan-overlay");

    /*
     * Add arrows
     */

    GuacUI.createChildElement(pan_overlay, "div", "indicator up");
    GuacUI.createChildElement(pan_overlay, "div", "indicator down");
    GuacUI.createChildElement(pan_overlay, "div", "indicator right");
    GuacUI.createChildElement(pan_overlay, "div", "indicator left");

    /*
     * Add exit button
     */

    var back = GuacUI.createChildElement(pan_overlay, "p", "hint");
    back.textContent = "Tap here to exit panning mode";

    // Return to interactive when back is clicked
    back.addEventListener("click", function() {
        GuacUI.StateManager.setState(GuacUI.Client.states.INTERACTIVE);
    }, false);

    this.show = function() {
        document.body.appendChild(pan_overlay);
    };

    this.hide = function() {
        document.body.removeChild(pan_overlay);
    };

    /*
     * Transition to PAN_TYPING when the user taps on the overlay.
     */

    pan_overlay.addEventListener("click", function(e) {
        GuacUI.StateManager.setState(GuacUI.Client.states.PAN_TYPING);
        e.stopPropagation();
    }, true);

};

GuacUI.Client.PanOverlay.prototype = new GuacUI.Component();

/*
 * Show the pan overlay during PAN or PAN_TYPING modes.
 */

GuacUI.StateManager.registerComponent(
    new GuacUI.Client.PanOverlay(),
    GuacUI.Client.states.PAN,
    GuacUI.Client.states.PAN_TYPING
);

/**
 * Native Keyboard. This component uses a hidden textarea field to show the
 * platforms native on-screen keyboard (if any) or otherwise enable typing,
 * should the platform require a text field with focus for keyboard events to
 * register.
 * 
 * @constructor
 * @augments GuacUI.Component
 */
GuacUI.Client.NativeKeyboard = function() {

    /**
     * Event target. This is a hidden textarea element which will receive
     * key events.
     * @private
     */
    var eventTarget = GuacUI.createElement("textarea", "event-target");
    eventTarget.setAttribute("autocorrect", "off");
    eventTarget.setAttribute("autocapitalize", "off");

    this.show = function() {

        // Move to location of expected input
        eventTarget.style.left   = GuacUI.Client.expected_input_x + "px";
        eventTarget.style.top    = GuacUI.Client.expected_input_y + "px";
        eventTarget.style.width  = GuacUI.Client.expected_input_width + "px";
        eventTarget.style.height = GuacUI.Client.expected_input_height + "px";

        // Show and focus target
        document.body.appendChild(eventTarget);
        eventTarget.focus();

    };

    this.hide = function() {

        // Hide and blur target
        eventTarget.blur();
        document.body.removeChild(eventTarget);

    };

    /*
     * Automatically switch to INTERACTIVE mode after target loses focus
     */

    eventTarget.addEventListener("blur", function() {
        GuacUI.StateManager.setState(GuacUI.Client.states.INTERACTIVE);
    }, false);

};

GuacUI.Client.NativeKeyboard.prototype = new GuacUI.Component();

/*
 * Show native keyboard during PAN_TYPING mode only.
 */

GuacUI.StateManager.registerComponent(
    new GuacUI.Client.NativeKeyboard(),
    GuacUI.Client.states.PAN_TYPING
);

/**
 * On-screen Keyboard. This component provides a clickable/touchable keyboard
 * which sends key events to the Guacamole client.
 * 
 * @constructor
 * @augments GuacUI.Component
 */
GuacUI.Client.OnScreenKeyboard = function() {

    /**
     * Event target. This is a hidden textarea element which will receive
     * key events.
     * @private
     */
    var keyboard_container = GuacUI.createElement("div", "keyboard-container");

    var keyboard_resize_interval = null;

    // On-screen keyboard
    var keyboard = new Guacamole.OnScreenKeyboard("layouts/en-us-qwerty.xml");
    keyboard_container.appendChild(keyboard.getElement());

    var last_keyboard_width = 0;

    // Function for automatically updating keyboard size
    function updateKeyboardSize() {
        var currentSize = keyboard.getElement().offsetWidth;
        if (last_keyboard_width != currentSize) {
            keyboard.resize(currentSize);
            last_keyboard_width = currentSize;
        }
    }

    keyboard.onkeydown = function(keysym) {
        GuacUI.Client.attachedClient.sendKeyEvent(1, keysym);
    };

    keyboard.onkeyup = function(keysym) {
        GuacUI.Client.attachedClient.sendKeyEvent(0, keysym);
    };

    this.show = function() {

        // Show keyboard
        document.body.appendChild(keyboard_container);

        // Start periodic update of keyboard size
        keyboard_resize_interval = window.setInterval(
            updateKeyboardSize,
            GuacUI.Client.KEYBOARD_AUTO_RESIZE_INTERVAL);

        // Resize on window resize
        window.addEventListener("resize", updateKeyboardSize, true);

        // Initialize size
        updateKeyboardSize();

    };

    this.hide = function() {

        // Hide keyboard
        document.body.removeChild(keyboard_container);
        window.clearInterval(keyboard_resize_interval);
        window.removeEventListener("resize", updateKeyboardSize, true);

    };

};

GuacUI.Client.OnScreenKeyboard.prototype = new GuacUI.Component();

/*
 * Show on-screen keyboard during OSK mode only.
 */

GuacUI.StateManager.registerComponent(
    new GuacUI.Client.OnScreenKeyboard(),
    GuacUI.Client.states.OSK
);

/*
 * Set initial state
 */

GuacUI.StateManager.setState(GuacUI.Client.states.INTERACTIVE);

/**
 * Modal status display. Displays a message to the user, covering the entire
 * screen.
 * 
 * Normally, this should only be used when user interaction with other
 * components is impossible.
 * 
 * @constructor
 * @augments GuacUI.Component
 */
GuacUI.Client.ModalStatus = function(title_text, text, classname, reconnect) {

    // Create element hierarchy
    var outer  = GuacUI.createElement("div", "dialogOuter");
    var middle = GuacUI.createChildElement(outer, "div", "dialogMiddle");
    var dialog = GuacUI.createChildElement(middle, "div", "dialog");

    // Add title if given
    if (title_text) {
        var title = GuacUI.createChildElement(dialog, "p", "title");
        title.textContent = title_text;
    }

    var status = GuacUI.createChildElement(dialog, "p", "status");
    status.textContent = text;

    // Set classname if given
    if (classname)
        GuacUI.addClass(outer, classname);

    // Automatically reconnect after the given time period
    var reconnect_interval = null;
    if (reconnect) {

        var countdown = GuacUI.createChildElement(dialog, "p", "countdown");

        function update_status() {

            // Use appropriate description of time remaining 
            if (reconnect === 0)
                countdown.textContent = "Reconnecting...";
            if (reconnect === 1)
                countdown.textContent = "Reconnecting in 1 second...";
            else
                countdown.textContent = "Reconnecting in " + reconnect + " seconds...";

            // Reconnect if countdown complete
            if (reconnect === 0) {
                window.clearInterval(reconnect_interval);
                GuacUI.Client.connect();
            }

        }

        // Update counter every second
        reconnect_interval = window.setInterval(function update_countdown() {
            reconnect--;
            update_status();
        }, 1000);

        // Init status
        update_status();

    }

    // Reconnect button
    var reconnect_section = GuacUI.createChildElement(dialog, "div", "reconnect");
    var reconnect_button = GuacUI.createChildElement(reconnect_section, "button");
    reconnect_button.textContent = "Reconnect";

    // Reconnect if button clicked
    reconnect_button.onclick = function() {
        window.clearInterval(reconnect_interval);
        GuacUI.Client.connect();
    };

    this.show = function() {
        document.body.appendChild(outer);
    };

    this.hide = function() {
        window.clearInterval(reconnect_interval);
        document.body.removeChild(outer);
    };

};

GuacUI.Client.ModalStatus.prototype = new GuacUI.Component();

/**
 * Flattens the attached Guacamole.Client, storing the result within the
 * connection history.
 */
GuacUI.Client.updateThumbnail = function() {

    // Get screenshot
    var canvas = GuacUI.Client.attachedClient.flatten();

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

};

/**
 * Updates the scale of the attached Guacamole.Client based on current window
 * size and "auto-fit" setting.
 */
GuacUI.Client.updateDisplayScale = function() {

    // Currently attacched client
    var guac = GuacUI.Client.attachedClient;
    var adjusted_scale = 1 / (window.devicePixelRatio || 1);

    // If auto-fit is enabled, scale display
    if (!GuacUI.Client.overrideAutoFit
         && GuacUI.sessionState.getProperty("auto-fit")) {

        // Calculate scale to fit screen
        var fit_scale = Math.min(
            window.innerWidth  / guac.getWidth(),
            window.innerHeight / guac.getHeight()
        );
          
        // Scale client
        if (guac.getScale() !== fit_scale)
            guac.scale(fit_scale);

    }

    // Otherwise, scale to 100%
    else if (guac.getScale() !== adjusted_scale)
        guac.scale(adjusted_scale);

};

/**
 * Updates the document title based on the connection name.
 */
GuacUI.Client.updateTitle = function () {
    
    if (GuacUI.Client.titlePrefix)
        document.title = GuacUI.Client.titlePrefix + " " + GuacUI.Client.connectionName;
    else
        document.title = GuacUI.Client.connectionName;

};

/**
 * Hides the currently-visible status overlay, if any.
 */
GuacUI.Client.hideStatus = function() {
    if (GuacUI.Client.visibleStatus)
        GuacUI.Client.visibleStatus.hide();
    GuacUI.Client.visibleStatus = null;
};

/**
 * Displays a status overlay with the given text.
 */
GuacUI.Client.showStatus = function(title, status) {
    GuacUI.Client.hideStatus();

    GuacUI.Client.visibleStatus = new GuacUI.Client.ModalStatus(title, status);
    GuacUI.Client.visibleStatus.show();
};

/**
 * Displays an error status overlay with the given text.
 */
GuacUI.Client.showError = function(title, status, reconnect) {
    GuacUI.Client.hideStatus();

    GuacUI.Client.visibleStatus =
        new GuacUI.Client.ModalStatus(title, status, "guac-error", reconnect);
    GuacUI.Client.visibleStatus.show();
};

/**
 * Connects to the current Guacamole connection, attaching a new Guacamole
 * client to the user interface. If a Guacamole client is already attached,
 * it is replaced.
 */
GuacUI.Client.connect = function() {

    var tunnel;

    // If WebSocket available, try to use it.
    if (window.WebSocket)
        tunnel = new Guacamole.ChainedTunnel(
            new Guacamole.WebSocketTunnel("websocket-tunnel"),
            new Guacamole.HTTPTunnel("tunnel")
        );

    // If no WebSocket, then use HTTP.
    else
        tunnel = new Guacamole.HTTPTunnel("tunnel");

    // Instantiate client
    var guac = new Guacamole.Client(tunnel);

    // Tie UI to client
    GuacUI.Client.attach(guac);

    // Calculate optimal width/height for display
    var pixel_density = window.devicePixelRatio || 1;
    var optimal_dpi = pixel_density * 96;
    var optimal_width = window.innerWidth * pixel_density;
    var optimal_height = window.innerHeight * pixel_density;

    // Scale width/height to be at least 600x600
    if (optimal_width < 600 || optimal_height < 600) {
        var scale = Math.max(600 / optimal_width, 600 / optimal_height);
        optimal_width = optimal_width * scale;
        optimal_height = optimal_height * scale;
    }

    // Get entire query string, and pass to connect().
    // Normally, only the "id" parameter is required, but
    // all parameters should be preserved and passed on for
    // the sake of authentication.

    var connect_string =
        window.location.search.substring(1)
        + "&width="  + Math.floor(optimal_width)
        + "&height=" + Math.floor(optimal_height)
        + "&dpi="    + Math.floor(optimal_dpi);

    // Add audio mimetypes to connect_string
    GuacUI.Audio.supported.forEach(function(mimetype) {
        connect_string += "&audio=" + encodeURIComponent(mimetype);
    });

    // Add video mimetypes to connect_string
    GuacUI.Video.supported.forEach(function(mimetype) {
        connect_string += "&video=" + encodeURIComponent(mimetype);
    });

    // Show connection errors from tunnel
    tunnel.onerror = function(status) {
        var message = GuacUI.Client.tunnel_errors[status.code] || GuacUI.Client.tunnel_errors.DEFAULT;
        GuacUI.Client.showError("Connection Error", message,
            GuacUI.Client.tunnel_auto_reconnect[status.code] && GuacUI.Client.RECONNECT_PERIOD);
    };

    // Notify of disconnections (if not already notified of something else)
    tunnel.onstatechange = function(state) {
        if (state === Guacamole.Tunnel.State.CLOSED && !GuacUI.Client.visibleStatus)
            GuacUI.Client.showStatus("Disconnected", "You have been disconnected. Reload the page to reconnect.");
    };

    // Connect
    guac.connect(connect_string);


};

/**
 * Attaches a Guacamole.Client to the client UI, such that Guacamole events
 * affect the UI, and local events affect the Guacamole.Client. If a client
 * is already attached, it is replaced.
 * 
 * @param {Guacamole.Client} guac The Guacamole.Client to attach to the UI.
 */
GuacUI.Client.attach = function(guac) {

    // If a client is already attached, ensure it is disconnected
    if (GuacUI.Client.attachedClient)
        GuacUI.Client.attachedClient.disconnect();

    // Store attached client
    GuacUI.Client.attachedClient = guac;

    // Get display element
    var guac_display = guac.getDisplay();

    /*
     * Update the scale of the display when the client display size changes.
     */

    guac.onresize = function(width, height) {
        GuacUI.Client.updateDisplayScale();
    };

    /*
     * Update UI when the state of the Guacamole.Client changes.
     */

    guac.onstatechange = function(clientState) {

        switch (clientState) {

            // Idle
            case 0:
                GuacUI.Client.showStatus(null, "Idle.");
                GuacUI.Client.titlePrefix = "[Idle]";
                break;

            // Connecting
            case 1:
                GuacUI.Client.showStatus("Connecting", "Connecting to Guacamole...");
                GuacUI.Client.titlePrefix = "[Connecting...]";
                break;

            // Connected + waiting
            case 2:
                GuacUI.Client.showStatus("Connecting", "Connected to Guacamole. Waiting for response...");
                GuacUI.Client.titlePrefix = "[Waiting...]";
                break;

            // Connected
            case 3:

                GuacUI.Client.hideStatus();
                GuacUI.Client.titlePrefix = null;

                // Update clipboard with current data
                if (GuacUI.sessionState.getProperty("clipboard"))
                    guac.setClipboard(GuacUI.sessionState.getProperty("clipboard"));

                break;

            // Disconnecting / disconnected are handled by tunnel instead
            case 4:
            case 5:
                break;

            // Unknown status code
            default:
                GuacUI.Client.showStatus("Unknown Status", "An unknown status code was received. This is most likely a bug.");

        }

        GuacUI.Client.updateTitle();

    };

    /*
     * Change UI to reflect the connection name
     */

    guac.onname = function(name) {
        GuacUI.Client.connectionName = name;
        GuacUI.Client.updateTitle();
    };

    /*
     * Disconnect and display an error message when the Guacamole.Client
     * receives an error.
     */

    guac.onerror = function(status) {

        // Disconnect, if connected
        guac.disconnect();

        // Display error message
        var message = GuacUI.Client.client_errors[status.code] || GuacUI.Client.client_errors.DEFAULT;
        GuacUI.Client.showError("Connection Error", message,
            GuacUI.Client.client_auto_reconnect[status.code] && GuacUI.Client.RECONNECT_PERIOD);
        
    };

    // Server copy handler
    guac.onclipboard = function(data) {
        GuacUI.sessionState.setProperty("clipboard", data);
    };

    /*
     * Prompt to download file when file received.
     */

    function getSizeString(bytes) {

        if (bytes > 1000000000)
            return (bytes / 1000000000).toFixed(1) + " GB";

        else if (bytes > 1000000)
            return (bytes / 1000000).toFixed(1) + " MB";

        else if (bytes > 1000)
            return (bytes / 1000).toFixed(1) + " KB";

        else
            return bytes + " B";

    }

    guac.onfile = function(stream, mimetype, filename) {

        var download = new GuacUI.Download(filename);
        download.updateProgress(getSizeString(0));

        var blob_reader = new Guacamole.BlobReader(stream, mimetype);

        GuacUI.Client.notification_area.appendChild(download.getElement());

        // Update progress as data is received
        blob_reader.onprogress = function() {
            download.updateProgress(getSizeString(blob_reader.getLength()));
            stream.sendAck("Received", 0x0000);
        };

        // When complete, prompt for download
        blob_reader.onend = function() {

            download.ondownload = function() {
                saveAs(blob_reader.getBlob(), filename);
            };

            download.complete();

        };

        // When close clicked, remove from notification area
        download.onclose = function() {
            GuacUI.Client.notification_area.removeChild(download.getElement());
        };

        stream.sendAck("Ready", 0x0000);

    };

    /*
     * Do nothing when the display element is clicked on.
     */

    guac_display.onclick = function(e) {
        e.preventDefault();
        return false;
    };

    /*
     * Handle mouse and touch events relative to the display element.
     */

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
            var view_width  = GuacUI.Client.viewport.offsetWidth;
            var view_height = GuacUI.Client.viewport.offsetHeight;

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

    // Hide any existing status notifications
    GuacUI.Client.hideStatus();

    // Remove old client from UI, if any
    GuacUI.Client.display.innerHTML = "";

    // Add client to UI
    guac.getDisplay().className = "software-cursor";
    GuacUI.Client.display.appendChild(guac.getDisplay());

};

// One-time UI initialization
(function() {

    /*
     * Route document-level keyboard events to the client.
     */

    var keyboard = new Guacamole.Keyboard(document);
    var show_keyboard_gesture_possible = true;

    keyboard.onkeydown = function (keysym) {

        // Only handle key events if client is attached
        var guac = GuacUI.Client.attachedClient;
        if (!guac) return;

        // Handle Ctrl-shortcuts specifically
        if (keyboard.modifiers.ctrl && !keyboard.modifiers.alt && !keyboard.modifiers.shift) {

            // Allow event through if Ctrl+C or Ctrl+X
            if (keyboard.pressed[0x63] || keyboard.pressed[0x78]) {
                guac.sendKeyEvent(1, keysym);
                return true;
            }

            // If Ctrl+V, wait until after paste event (next event loop)
            if (keyboard.pressed[0x76]) {
                window.setTimeout(function after_paste() {
                    guac.sendKeyEvent(1, keysym);
                }, 10);
                return true;
            }

        }

        // Just send key for all other cases
        guac.sendKeyEvent(1, keysym);

        // If key is NOT one of the expected keys, gesture not possible
        if (keysym !== 0xFFE3 && keysym !== 0xFFE9 && keysym !== 0xFFE1)
            show_keyboard_gesture_possible = false;

    };

    keyboard.onkeyup = function (keysym) {

        // Only handle key events if client is attached
        var guac = GuacUI.Client.attachedClient;
        if (!guac) return;

        guac.sendKeyEvent(0, keysym);

        // If lifting up on shift, toggle keyboard if rest of gesture
        // conditions satisfied
        if (show_keyboard_gesture_possible && keysym === 0xFFE1) {
            if (keyboard.pressed[0xFFE3] && keyboard.pressed[0xFFE9]) {

                // If in INTERACTIVE mode, switch to OSK
                if (GuacUI.StateManager.getState() === GuacUI.Client.states.INTERACTIVE)
                    GuacUI.StateManager.setState(GuacUI.Client.states.OSK);

                // If in OSK mode, switch to INTERACTIVE 
                else if (GuacUI.StateManager.getState() === GuacUI.Client.states.OSK)
                    GuacUI.StateManager.setState(GuacUI.Client.states.INTERACTIVE);

            }
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

    // Set local clipboard contents on cut 
    document.body.addEventListener("cut", function handle_cut(e) {
        e.preventDefault();
        var data = GuacamoleService.Clipboard.get();
        e.clipboardData.setData("text/plain", data);
    }, false);

    // Set local clipboard contents on copy 
    document.body.addEventListener("copy", function handle_copy(e) {
        e.preventDefault();
        var data = GuacamoleService.Clipboard.get();
        e.clipboardData.setData("text/plain", data);
    }, false);

    // Set remote clipboard contents on paste
    document.body.addEventListener("paste", function handle_paste(e) {
        e.preventDefault();
        if (GuacUI.Client.attachedClient)
            GuacUI.Client.attachedClient.setClipboard(e.clipboardData.getData("text/plain"));
    }, false);

    /*
     * Disconnect and update thumbnail on close
     */
    window.onunload = function() {

        GuacUI.Client.updateThumbnail();

        if (GuacUI.Client.attachedClient)
            GuacUI.Client.attachedClient.disconnect();

    };

    /*
     * Send size events on resize
     */
    window.onresize = function() {

        var pixel_density = window.devicePixelRatio || 1;
        var width = window.innerWidth * pixel_density;
        var height = window.innerHeight * pixel_density;

        if (GuacUI.Client.attachedClient)
            GuacUI.Client.attachedClient.sendSize(width, height);

        GuacUI.Client.updateDisplayScale();

    };

    GuacUI.sessionState.onchange = function(old_state, new_state, name) {
        if (name === "clipboard" && GuacUI.Client.attachedClient)
            GuacUI.Client.attachedClient.setClipboard(new_state[name]);
        else if (name === "auto-fit")
            GuacUI.Client.updateDisplayScale();
    };

    var long_press_start_x = 0;
    var long_press_start_y = 0;
    var longPressTimeout = null;

    GuacUI.Client.startLongPressDetect = function() {

        if (!longPressTimeout) {

            longPressTimeout = window.setTimeout(function() {
                longPressTimeout = null;

                // If screen shrunken, show magnifier
                if (GuacUI.Client.attachedClient.getScale() < 1.0)
                    GuacUI.StateManager.setState(GuacUI.Client.states.MAGNIFIER);

                // Otherwise, if screen too big to fit, use panning mode
                else if (
                       GuacUI.Client.attachedClient.getWidth() > window.innerWidth
                    || GuacUI.Client.attachedClient.getHeight() > window.innerHeight
                )
                    GuacUI.StateManager.setState(GuacUI.Client.states.PAN);

                // Otherwise, just show a hint
                else
                    GuacUI.StateManager.setState(GuacUI.Client.states.WAIT_TYPING);
            }, GuacUI.Client.LONG_PRESS_DETECT_TIMEOUT);

        }
    };

    GuacUI.Client.stopLongPressDetect = function() {
        window.clearTimeout(longPressTimeout);
        longPressTimeout = null;
    };

    // Detect long-press at bottom of screen
    GuacUI.Client.display.addEventListener('touchstart', function(e) {
        
        // Record touch location
        if (e.touches.length === 1) {
            var touch = e.touches[0];
            long_press_start_x = touch.screenX;
            long_press_start_y = touch.screenY;
        }
        
        // Start detection
        GuacUI.Client.startLongPressDetect();
        
    }, true);

    // Stop detection if touch moves significantly
    GuacUI.Client.display.addEventListener('touchmove', function(e) {
        
        // If touch distance from start exceeds threshold, cancel long press
        var touch = e.touches[0];
        if (Math.abs(touch.screenX - long_press_start_x) >= GuacUI.Client.LONG_PRESS_MOVEMENT_THRESHOLD
            || Math.abs(touch.screenY - long_press_start_y) >= GuacUI.Client.LONG_PRESS_MOVEMENT_THRESHOLD)
            GuacUI.Client.stopLongPressDetect();
        
    }, true);

    // Stop detection if press stops
    GuacUI.Client.display.addEventListener('touchend', GuacUI.Client.stopLongPressDetect, true);

    /**
     * Ignores the given event.
     * 
     * @private
     * @param {Event} e The event to ignore.
     */
    function _ignore(e) {
        e.preventDefault();
        e.stopPropagation();
    }

    /**
     * Converts the given bytes to a base64-encoded string.
     * 
     * @private
     * @param {Uint8Array} bytes A Uint8Array which contains the data to be
     *                           encoded as base64.
     * @return {String} The base64-encoded string.
     */
    function _get_base64(bytes) {

        var data = "";

        // Produce binary string from bytes in buffer
        for (var i=0; i<bytes.byteLength; i++)
            data += String.fromCharCode(bytes[i]);

        // Convert to base64
        return window.btoa(data);

    }

    /**
     * Uploads the given file to the server.
     * 
     * @private
     * @param {File} file The file to upload.
     */
    function _upload_file(file) {

        // Construct reader for file
        var reader = new FileReader();
        reader.onloadend = function() {

            // Add upload notification
            var upload = new GuacUI.Upload(file.name);
            upload.updateProgress(getSizeString(0), 0);

            GuacUI.Client.notification_area.appendChild(upload.getElement());

            // Open file for writing
            var stream = GuacUI.Client.attachedClient.createFileStream(file.type, file.name);

            var valid = true;
            var bytes = new Uint8Array(reader.result);
            var offset = 0;

            // Invalidate stream on all errors
            // Continue upload when acknowledged
            stream.onack = function(status) {

                // Handle errors 
                if (status.isError()) {
                    valid = false;
                    var message =  GuacUI.Client.upload_errors[status.code]
                                || GuacUI.Client.upload_errors.DEFAULT;
                    upload.showError(message);
                }

                // Abort upload if stream is invalid
                if (!valid) return false;

                // Encode packet as base64
                var slice = bytes.subarray(offset, offset+4096);
                var base64 = _get_base64(slice);

                // Write packet
                stream.sendBlob(base64);

                // Advance to next packet
                offset += 4096;

                // If at end, stop upload
                if (offset >= bytes.length) {
                    stream.sendEnd();
                    GuacUI.Client.notification_area.removeChild(upload.getElement());
                }

                // Otherwise, update progress
                else
                    upload.updateProgress(getSizeString(offset), offset / bytes.length * 100);

            };

            // Close dialog and abort when close is clicked
            upload.onclose = function() {
                GuacUI.Client.notification_area.removeChild(upload.getElement());
                // TODO: Abort transfer
            };

        };
        reader.readAsArrayBuffer(file);

    }

    // Handle and ignore dragenter/dragover
    GuacUI.Client.display.addEventListener("dragenter", _ignore, false);
    GuacUI.Client.display.addEventListener("dragover", _ignore, false);

    // File drop event handler
    GuacUI.Client.display.addEventListener("drop", function(e) {
      
        e.preventDefault();
        e.stopPropagation();

        // Ignore file drops if no attached client
        if (!GuacUI.Client.attachedClient) return;

        // Upload each file 
        var files = e.dataTransfer.files;
        for (var i=0; i<files.length; i++)
            _upload_file(files[i]);

    }, false);

})();
