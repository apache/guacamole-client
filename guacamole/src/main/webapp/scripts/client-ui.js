
/**
 * Client UI namespace.
 * @namespace
 */
GuacUI.Client = GuacUI.Client || {};

GuacUI.Client = {

    /**
     * Collection of all Guacamole client UI states.
     */
    "states": {

        /**
         * The normal default Guacamole client UI mode
         */
        "FULL_INTERACTIVE" : 0,

        /**
         * Same as FULL_INTERACTIVE except with visible on-screen keyboard.
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
        "PAN_TYPING"       : 4
    },

    /* Constants */
    
    "LONG_PRESS_DETECT_TIMEOUT"     : 800, /* milliseconds */
    "LONG_PRESS_MOVEMENT_THRESHOLD" : 10,  /* pixels */    
    "KEYBOARD_AUTO_RESIZE_INTERVAL" : 30,  /* milliseconds */

    /* UI Components */

    "viewport"    : document.getElementById("viewportClone"),
    "display"     : document.getElementById("display"),
    "logo"        : document.getElementById("status-logo"),

    "buttons": {
        "reconnect" : document.getElementById("reconnect")
    },

    "containers": {
        "state"     : document.getElementById("statusDialog"),
        "keyboard"  : document.getElementById("keyboardContainer")
    },
    
    "state"        : document.getElementById("statusText"),
    "client"       : null,
    "sessionState" : new GuacamoleSessionState(),

    /* Expected Input Rectangle */

    "expected_input_x" : 0,
    "expected_input_y" : 0,
    "expected_input_width" : 0,
    "expected_input_height" : 0

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
            / (window.innerWidth - width) * (GuacamoleUI.client.getWidth() - width);
        var clip_y = y
            / (window.innerHeight - height) * (GuacamoleUI.client.getHeight() - height);
       
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
        magnifier_display.width = GuacamoleUI.client.getWidth();
        magnifier_display.height = GuacamoleUI.client.getHeight();
        magnifier_context.drawImage(GuacamoleUI.client.flatten(), 0, 0);

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

    var old_scale = null;

    this.show = function() {
        old_scale = GuacamoleUI.client.getScale();
        GuacamoleUI.client.scale(1.0);
    };

    this.hide = function() {
        GuacamoleUI.client.scale(old_scale);
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

    this.show = function() {
        document.body.appendChild(pan_overlay);
    };

    this.hide = function() {
        document.body.removeChild(pan_overlay);
    };

    /*
     * Transition to PAN_TYPING when the user taps on the overlay.
     */

    pan_overlay.addEventListener("click", function() {
        GuacUI.StateManager.setState(GuacUI.Client.PAN_TYPING);
    }, false);

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

/*
 * Set initial state
 */

GuacUI.StateManager.setState(GuacUI.Client.states.INTERACTIVE);
