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

    /* Constants */
    
    "LONG_PRESS_DETECT_TIMEOUT"     : 800, /* milliseconds */
    "LONG_PRESS_MOVEMENT_THRESHOLD" : 10,  /* pixels */    
    "KEYBOARD_AUTO_RESIZE_INTERVAL" : 30,  /* milliseconds */

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
GuacUI.Client.ModalStatus = function(title_text, text, classname) {

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

    this.show = function() {
        document.body.appendChild(outer);
    };

    this.hide = function() {
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
GuacUI.Client.showError = function(title, status) {
    GuacUI.Client.hideStatus();

    GuacUI.Client.visibleStatus =
        new GuacUI.Client.ModalStatus(title, status, "guac-error");
    GuacUI.Client.visibleStatus.show();
}

/**
 * Attaches a Guacamole.Client to the client UI, such that Guacamole events
 * affect the UI, and local events affect the Guacamole.Client.
 * 
 * @param {Guacamole.Client} guac The Guacamole.Client to attach to the UI.
 */
GuacUI.Client.attach = function(guac) {

    // Store attached client
    GuacUI.Client.attachedClient = guac;

    // Get display element
    var guac_display = guac.getDisplay();

    /*
     * Update the scale of the display when the client display size changes.
     */

    guac.onresize = function(width, height) {
        GuacUI.Client.updateDisplayScale();
    }

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

            // Disconnecting
            case 4:
                GuacUI.Client.showStatus(null, "Disconnecting...");
                GuacUI.Client.titlePrefix = "[Disconnecting...]";
                break;

            // Disconnected
            case 5:
                GuacUI.Client.showStatus("Disconnected", "Guacamole has been manually disconnected. Reload the page to reconnect.");
                GuacUI.Client.titlePrefix = "[Disconnected]";
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

    guac.onerror = function(error) {

        // Disconnect, if connected
        guac.disconnect();

        // Display error message
        GuacUI.Client.showError("Connection Error", error);
        
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

    /*
     * Route document-level keyboard events to the client.
     */

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
            if (keyboard.pressed[0xFFE3] && keyboard.pressed[0xFFE9]) {

                // If in INTERACTIVE mode, switch to OSK
                if (GuacUI.StateManager.getState() == GuacUI.Client.states.INTERACTIVE)
                    GuacUI.StateManager.setState(GuacUI.Client.states.OSK);

                // If in OSK mode, switch to INTERACTIVE 
                else if (GuacUI.StateManager.getState() == GuacUI.Client.states.OSK)
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

    /*
     * Disconnect and update thumbnail on close
     */
    window.onunload = function() {

        GuacUI.Client.updateThumbnail();
        guac.disconnect();

    };

    /*
     * Send size events on resize
     */
    window.onresize = function() {

        var pixel_density = window.devicePixelRatio || 1;
        var width = window.innerWidth * pixel_density;
        var height = window.innerHeight * pixel_density;

        guac.sendSize(width, height);
        GuacUI.Client.updateDisplayScale();

    };

    GuacUI.sessionState.onchange = function(old_state, new_state, name) {
        if (name == "clipboard")
            guac.setClipboard(new_state[name]);
        else if (name == "auto-fit")
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
        if (e.touches.length == 1) {
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
            stream.onack = function(text, code) {

                // Handle codes
                if (code >= 0x0100) {
                    valid = false;
                    upload.showError(text);
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

        // Upload each file 
        var files = e.dataTransfer.files;
        for (var i=0; i<files.length; i++)
            _upload_file(files[i]);

    }, false);

};

