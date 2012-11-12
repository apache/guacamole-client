
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
    "eventTarget" : document.getElementById("eventTarget"),

    "buttons": {
        "reconnect" : document.getElementById("reconnect")
    },

    "containers": {
        "state"     : document.getElementById("statusDialog"),
        "keyboard"  : document.getElementById("keyboardContainer"),
        "magnifier" : document.getElementById("magnifier")
    },
    
    "pan_overlay"  : document.getElementById("pan-overlay"),
    "magnifier_background"    : document.getElementById("magnifier-background"),
    "magnifier"    : document.getElementById("magnifier-display"),
    "state"        : document.getElementById("statusText"),
    "client"       : null,
    "sessionState" : new GuacamoleSessionState()

};

GuacUI.Client.magnifier_background.onclick = function() {
    GuacUI.StateManager.setState(GuacUI.Client.states.INTERACTIVE);
};

GuacUI.Client.containers.magnifier.addEventListener("click", function(e) {
    GuacUI.StateManager.setState(GuacUI.Client.states.PAN_TYPING);
    e.stopPropagation();
}, true);

/**
 * Component which displays a magnified (100% zoomed) client display.
 * 
 * @constructor
 * @augments GuacUI.DraggableComponent
 */
GuacUI.Client.Magnifier = function(magnifier) {

    /**
     * Reference to this magnifier.
     * @private
     */
    var guac_magnifier = this;

    /*
     * Call parent constructor. 
     */
    GuacUI.DraggableComponent.apply(this, [magnifier]);

    // Ensure transformations on display originate at 0,0
    GuacamoleUI.containers.magnifier.style.transformOrigin =
    GuacamoleUI.containers.magnifier.style.webkitTransformOrigin =
    GuacamoleUI.containers.magnifier.style.MozTransformOrigin =
    GuacamoleUI.containers.magnifier.style.OTransformOrigin =
    GuacamoleUI.containers.magnifier.style.msTransformOrigin =
        "0 0";

    var magnifier_display = GuacUI.Client.magnifier;
    var magnifier_context = magnifier_display.getContext("2d");

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

        GuacamoleUI.eventTarget.style.left = clip_x + "px";
        GuacamoleUI.eventTarget.style.top  = clip_y + "px";

    };

    this.show = function() {

        // Copy displayed image
        magnifier_display.width = GuacamoleUI.client.getWidth();
        magnifier_display.height = GuacamoleUI.client.getHeight();
        magnifier_context.drawImage(GuacamoleUI.client.flatten(), 0, 0);

        // Show magnifier container
        GuacUI.Client.magnifier_background.style.display = "block";

    };

    this.hide = function() {

        // Hide magnifier container
        GuacUI.Client.magnifier_background.style.display = "none";

    };

};

/*
 * We inherit from GuacUI.DraggableComponent.
 */
GuacUI.Client.Magnifier.prototype = new GuacUI.DraggableComponent();

GuacUI.StateManager.registerComponent(
    new GuacUI.Client.Magnifier(GuacUI.Client.containers.magnifier),
    GuacUI.Client.states.MAGNIFIER
);

/*
 * Zoomed Display
 */

GuacUI.Client.ZoomedDisplay = function(client) {

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

GuacUI.StateManager.registerComponent(
    new GuacUI.Client.ZoomedDisplay(),
    GuacUI.Client.states.PAN,
    GuacUI.Client.states.PAN_TYPING
);

/*
 * Pan UI
 */

GuacUI.Client.PanOverlay = function(client) {

    this.show = function() {
        GuacUI.Client.pan_overlay.style.display = "block";
    };

    this.hide = function() {
        GuacUI.Client.pan_overlay.style.display = "none";
    };

};

GuacUI.Client.PanOverlay.prototype = new GuacUI.Component();

GuacUI.StateManager.registerComponent(
    new GuacUI.Client.PanOverlay(),
    GuacUI.Client.states.PAN,
    GuacUI.Client.states.PAN_TYPING
);

/*
 * Native Keyboard
 */

GuacUI.Client.NativeKeyboard = function() {

    this.show = function() {
        GuacamoleUI.eventTarget.focus();
    };

    this.hide = function() {
        GuacamoleUI.eventTarget.blur();
    };

};

GuacUI.StateManager.registerComponent(
    new GuacUI.Client.NativeKeyboard(),
    GuacUI.Client.states.PAN_TYPING
);

GuacUI.Client.eventTarget.onblur = function() {
    GuacUI.StateManager.setState(GuacUI.Client.states.INTERACTIVE);
};

/*
 * Set initial state
 */

GuacUI.StateManager.setState(GuacUI.Client.states.INTERACTIVE);
