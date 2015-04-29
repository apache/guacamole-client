/*
 * Copyright (C) 2015 Glyptodon LLC
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

var Guacamole = Guacamole || {};

/**
 * Dynamic on-screen keyboard. Given the layout object for an on-screen
 * keyboard, this object will construct a clickable on-screen keyboard with its
 * own key events.
 *
 * @constructor
 * @param {Guacamole.OnScreenKeyboard.Layout} layout
 *     The layout of the on-screen keyboard to display.
 */
Guacamole.OnScreenKeyboard = function(layout) {

    /**
     * Reference to this Guacamole.OnScreenKeyboard.
     *
     * @type Guacamole.OnScreenKeyboard
     */
    var osk = this;

    /**
     * State of all modifiers. This is the bitwise OR of all active modifier
     * values.
     * 
     * @private
     * @type Number
     */
    var modifiers = 0;

    /**
     * Map of currently-set modifiers to the keysym associated with their
     * original press. When the modifier is cleared, this keysym must be
     * released.
     *
     * @private
     * @type Object.<String, Number>
     */
    var modifierKeysyms = {};

    /**
     * All scalable elements which are part of the on-screen keyboard. Each
     * scalable element is carefully controlled to ensure the interface layout
     * and sizing remains constant, even on browsers that would otherwise
     * experience rounding error due to unit conversions.
     *
     * @private
     * @type ScaledElement[]
     */
    var scaledElements = [];

    /**
     * Adds a CSS class to an element.
     * 
     * @private
     * @function
     * @param {Element} element
     *     The element to add a class to.
     *
     * @param {String} classname
     *     The name of the class to add.
     */
    var addClass = function addClass(element, classname) {

        // If classList supported, use that
        if (element.classList)
            element.classList.add(classname);

        // Otherwise, simply append the class
        else
            element.className += " " + classname;

    };

    /**
     * Removes a CSS class from an element.
     * 
     * @private
     * @function
     * @param {Element} element
     *     The element to remove a class from.
     *
     * @param {String} classname
     *     The name of the class to remove.
     */
    var removeClass = function removeClass(element, classname) {

        // If classList supported, use that
        if (element.classList)
            element.classList.remove(classname);

        // Otherwise, manually filter out classes with given name
        else {
            element.className = element.className.replace(/([^ ]+)[ ]*/g,
                function removeMatchingClasses(match, testClassname) {

                    // If same class, remove
                    if (testClassname === classname)
                        return "";

                    // Otherwise, allow
                    return match;
                    
                }
            );
        }

    };

    /**
     * Counter of mouse events to ignore. This decremented by mousemove, and
     * while non-zero, mouse events will have no effect.
     *
     * @private
     * @type Number
     */
    var ignoreMouse = 0;

    /**
     * Ignores all pending mouse events when touch events are the apparent
     * source. Mouse events are ignored until at least touchMouseThreshold
     * mouse events occur without corresponding touch events.
     *
     * @private
     */
    var ignorePendingMouseEvents = function ignorePendingMouseEvents() {
        ignoreMouse = osk.touchMouseThreshold;
    };

    /**
     * Map of modifier names to their corresponding, unique, power-of-two value
     * bitmasks.
     *
     * @private
     * @type Object.<String, Number>
     */
    var modifierMasks = {};

    /**
     * The bitmask to assign to the next modifier, if a new modifier is used
     * which does not yet have a corresponding bitmask.
     *
     * @private
     * @type Number
     */
    var nextMask = 1;

    /**
     * Returns a unique power-of-two value for the modifier with the given
     * name. The same value will be returned consistently for the same
     * modifier.
     *
     * @private
     * @param {String} name
     *     The name of the modifier to return a bitmask for.
     * 
     * @return {Number}
     *     The unique power-of-two value associated with the modifier having
     *     the given name, which may be newly allocated.
     */
    var getModifierMask = function getModifierMask(name) {
        
        var value = modifierMasks[name];
        if (!value) {

            // Get current modifier, advance to next
            value = nextMask;
            nextMask <<= 1;

            // Store value of this modifier
            modifierMasks[name] = value;

        }

        return value;
            
    };

    /**
     * An element whose dimensions are maintained according to an arbitrary
     * scale. The conversion factor for these arbitrary units to pixels is
     * provided later via a call to scale().
     *
     * @private
     * @constructor
     * @param {Element} element
     *     The element whose scale should be maintained.
     *
     * @param {Number} width
     *     The width of the element, in arbitrary units, relative to other
     *     ScaledElements.
     *
     * @param {Number} height
     *     The height of the element, in arbitrary units, relative to other
     *     ScaledElements.
     *     
     * @param {Boolean} [scaleFont=false]
     *     Whether the line height and font size should be scaled as well.
     */
    var ScaledElement = function ScaledElement(element, width, height, scaleFont) {

        /**
         * The width of this ScaledElement, in arbitrary units, relative to
         * other ScaledElements.
         *
         * @type Number
         */
         this.width = width;

        /**
         * The height of this ScaledElement, in arbitrary units, relative to
         * other ScaledElements.
         *
         * @type Number
         */
         this.height = height;
 
        /**
         * Resizes the associated element, updating its dimensions according to
         * the given pixels per unit.
         *
         * @param {Number} pixels
         *     The number of pixels to assign per arbitrary unit.
         */
        this.scale = function(pixels) {

            // Scale element width/height
            element.style.width  = (width  * pixels) + "px";
            element.style.height = (height * pixels) + "px";

            // Scale font, if requested
            if (scaleFont) {
                element.style.lineHeight = (height * pixels) + "px";
                element.style.fontSize   = pixels + "px";
            }

        };

    };

    // Create keyboard
    var keyboard = document.createElement("div");
    keyboard.className = "guac-keyboard";

    // Do not allow selection or mouse movement to propagate/register.
    keyboard.onselectstart =
    keyboard.onmousemove   =
    keyboard.onmouseup     =
    keyboard.onmousedown   = function handleMouseEvents(e) {

        // If ignoring events, decrement counter
        if (ignoreMouse)
            ignoreMouse--;

        e.stopPropagation();
        return false;

    };

    /**
     * The number of mousemove events to require before re-enabling mouse
     * event handling after receiving a touch event.
     *
     * @type Number
     */
    this.touchMouseThreshold = 3;

    /**
     * Fired whenever the user presses a key on this Guacamole.OnScreenKeyboard.
     * 
     * @event
     * @param {Number} keysym The keysym of the key being pressed.
     */
    this.onkeydown = null;

    /**
     * Fired whenever the user releases a key on this Guacamole.OnScreenKeyboard.
     * 
     * @event
     * @param {Number} keysym The keysym of the key being released.
     */
    this.onkeyup = null;

    /**
     * The keyboard layout provided at time of construction.
     *
     * @type Guacamole.OnScreenKeyboard.Layout
     */
    this.layout = layout;

    /**
     * Returns the element containing the entire on-screen keyboard.
     * @returns {Element} The element containing the entire on-screen keyboard.
     */
    this.getElement = function() {
        return keyboard;
    };

    /**
     * Resizes all elements within this Guacamole.OnScreenKeyboard such that
     * the width is close to but does not exceed the specified width. The
     * height of the keyboard is determined based on the width.
     * 
     * @param {Number} width The width to resize this Guacamole.OnScreenKeyboard
     *                       to, in pixels.
     */
    this.resize = function(width) {

        // Get pixel size of a unit
        var unit = Math.floor(width * 10 / osk.layout.width) / 10;

        // Resize all scaled elements
        for (var i=0; i<scaledElements.length; i++) {
            var scaledElement = scaledElements[i];
            scaledElement.scale(unit);
        }

    };

    /**
     * Given the name of a key, returns the set of key objects associated with
     * that name. A particular key may have many associated key objects due to
     * the various effects of modifiers. Regardless of how the key is declared
     * in the layout, this function will ALWAYS return an array of key objects.
     *
     * @private
     * @param {String} name
     *     The name of the key whose set of possible keys should be returned.
     *
     * @returns {Guacamole.OnScreenKeyboard.Key[]}
     *     The array of all keys associated with the given name.
     */
    var getKeys = function getKeys(name) {

        // Pull associated object, which might be a key object already
        var object = osk.layout.keys[name];
        if (!object)
            return null;

        // If already an array, just coerce into a true Key[] 
        if (object instanceof Array) {
            var keys = [];
            for (var i=0; i < object.length; i++) {
                keys.push(new Guacamole.OnScreenKeyboard.Key(object[i], name));
            }
            return keys;
        }

        // Derive key object from keysym if that's all we have
        if (typeof object === 'number') {
            return [new Guacamole.OnScreenKeyboard.Key({
                name   : name,
                keysym : object
            })];
        }

        // Derive key object from title if that's all we have
        if (typeof object === 'string') {
            return [new Guacamole.OnScreenKeyboard.Key({
                name  : name,
                title : object
            })];
        }

        // Otherwise, assume it's already a key object, just not an array
        return [new Guacamole.OnScreenKeyboard.Key(object, name)];

    };

    /**
     * Given an arbitrary string representing the name of some component of the
     * on-screen keyboard, returns a string formatted for use as a CSS class
     * name. The result will be lowercase. Word boundaries previously denoted
     * by CamelCase will be replaced by individual hyphens, as will all
     * contiguous non-alphanumeric characters.
     *
     * @private
     * @param {String} name
     *     An arbitrary string representing the name of some component of the
     *     on-screen keyboard.
     *
     * @returns {String}
     *     A string formatted for use as a CSS class name.
     */
    var getCSSName = function getCSSName(name) {

        // Convert name from possibly-CamelCase to hyphenated lowercase
        var cssName = name
               .replace(/([a-z])([A-Z])/g, '$1-$2')
               .replace(/[^A-Za-z0-9]+/g, '-')
               .toLowerCase();

        return cssName;

    };

    /**
     * Appends DOM elements to the given element as dictated by the layout
     * structure object provided. If a name is provided, an additional CSS
     * class, prepended with "guac-osk-", will be added to the top-level
     * element.
     * 
     * If the layout structure object is an array, all elements within that
     * array will be recursively appended as children of a group, and the
     * top-level element will be given the CSS class "guac-osk-group".
     *
     * If the layout structure object is an object, all properties within that
     * object will be recursively appended as children of a group, and the
     * top-level element will be given the CSS class "guac-osk-group". The
     * name of each property will be applied as the name of each child object
     * for the sake of CSS. Each property will be added in sorted order.
     *
     * If the layout structure object is a string, the key having that name
     * will be appended. The key will be given the CSS class "guac-osk-key" and
     * "guac-osk-key-NAME", where NAME is the name of the key. If the name of
     * the key is a single character, this will first be transformed into the
     * C-style hexadecimal literal for the Unicode codepoint of that character.
     * For example, the key "A" would become "guac-osk-key-0x41".
     *
     * @private
     * @param {Element} element
     *     The element to append elements to.
     *
     * @param {Array|Object|String} object
     *     The layout structure object to use when constructing the elements to
     *     append.
     *
     * @param {String} [name]
     *     The name of the top-level element being appended, if any.
     */
    var appendElements = function appendElements(element, object, name) {

        var i;

        // Create div which will become the group or key
        var div = document.createElement('div');

        // Add class based on name, if name given
        if (name)
            addClass(div, 'guac-osk-' + getCSSName(name));

        // If an array, append each element
        if (object instanceof Array) {

            // Add group class
            addClass(div, 'guac-osk-group');

            // Append all elements of array
            for (i=0; i < object.length; i++)
                appendElements(div, object[i]);

        }

        // If an object, append each property value
        else if (object instanceof Object) {

            // Add group class
            addClass(div, 'guac-osk-group');

            // Append all children, sorted by name
            var names = Object.keys(object).sort();
            for (i=0; i < names.length; i++) {
                var name = names[i];
                appendElements(div, object[name], name);
            }

        }

        // If a string, create as a key
        else if (typeof object === 'string') {

            // If key name is only one character, use codepoint for name
            var keyName = object;
            if (keyName.length === 1)
                keyName = '0x' + keyName.charCodeAt(0).toString(16);

            // Add key-specific classes
            addClass(div, 'guac-osk-key');
            addClass(div, 'guac-osk-key-' + getCSSName(keyName));

            // Retrieve all associated keys
            var keys = getKeys(object);
            console.log(object, keys);

        }

        // Add newly-created group/key
        element.appendChild(div);

    };

    // Create keyboard layout in DOM
    appendElements(keyboard, layout.layout);

};

/**
 * Represents an entire on-screen keyboard layout, including all available
 * keys, their behaviors, and their relative position and sizing.
 *
 * @constructor
 * @param {Guacamole.OnScreenKeyboard.Layout|Object} template
 *     The object whose identically-named properties will be used to initialize
 *     the properties of this layout.
 */
Guacamole.OnScreenKeyboard.Layout = function(template) {

    /**
     * The language of keyboard layout, such as "en_US". This property is for
     * informational purposes only, but it is recommend to conform to the
     * [language code]_[country code] format.
     *
     * @type String
     */
    this.language = template.language;

    /**
     * The type of keyboard layout, such as "qwerty". This property is for
     * informational purposes only, and does not conform to any standard.
     *
     * @type String
     */
    this.type = template.type;

    /**
     * Map of key name to corresponding keysym, title, or key object. If only
     * the keysym or title is provided, the key object will be created
     * implicitly. In all cases, the name property of the key object will be
     * taken from the name given in the mapping.
     *
     * @type Object.<String, Number|String|Key[]>
     */
    this.keys = template.keys;

    /**
     * Arbitrarily nested, arbitrarily grouped key names. The contents of the
     * layout will be traversed to produce an identically-nested grouping of
     * keys in the DOM tree. All strings will be transformed into their
     * corresponding sets of keys, while all objects and arrays will be
     * transformed into named groups and anonymous groups respectively.
     *
     * @type Object
     */
    this.layout = template.layout;

    /**
     * The width of the entire keyboard, in arbitrary units. The width of each
     * key is relative to this width, as both width values are assumed to be in
     * the same units. The conversion factor between these units and pixels is
     * derived later via a call to resize() on the Guacamole.OnScreenKeyboard.
     *
     * @type Number
     */
    this.width = template.width;

};

/**
 * Represents a single key, or a single possible behavior of a key. Each key
 * on the on-screen keyboard must have at least one associated
 * Guacamole.OnScreenKeyboard.Key, whether that key is explicitly defined or
 * implied, and may have multiple Guacamole.OnScreenKeyboard.Key if behavior
 * depends on modifier states.
 *
 * @constructor
 * @param {Guacamole.OnScreenKeyboard.Key|Object} template
 *     The object whose identically-named properties will be used to initialize
 *     the properties of this key.
 *     
 * @param {String} [name]
 *     The name to use instead of any name provided within the template, if
 *     any. If omitted, the name within the template will be used, assuming the
 *     template contains a name.
 */
Guacamole.OnScreenKeyboard.Key = function(template, name) {

    /**
     * The unique name identifying this key within the keyboard layout.
     *
     * @type String
     */
    this.name = name || template.name;

    /**
     * The human-readable title that will be displayed to the user within the
     * key. If not provided, this will be derived from the key name.
     *
     * @type String
     */
    this.title = template.title || this.name;

    /**
     * The keysym to be pressed/released when this key is pressed/released. If
     * not provided, this will be derived from the title if the title is a
     * single character.
     *
     * @type Number
     */
    this.keysym = template.keysym || (function deriveKeysym(title) {

        // Do not derive keysym if title is not exactly one character
        if (!title || title.length !== 1)
            return null;

        // For characters between U+0000 and U+00FF, the keysym is the codepoint
        var charCode = title.charCodeAt(0);
        if (charCode >= 0x0000 && charCode <= 0x00FF)
            return charCode;

        // For characters between U+0100 and U+10FFFF, the keysym is the codepoint or'd with 0x01000000
        if (charCode >= 0x0100 && charCode <= 0x10FFFF)
            return 0x01000000 | charCode;

        // Unable to derive keysym
        return null;

    })(this.title);

    /**
     * The name of the modifier set when the key is pressed and cleared when
     * this key is released, if any. The names of modifiers are distinct from
     * the names of keys; both the "RightShift" and "LeftShift" keys may set
     * the "shift" modifier, for example. By default, the key will affect no
     * modifiers.
     * 
     * @type String
     */
    this.modifier = template.modifier;

    /**
     * An array containing the names of each modifier required for this key to
     * have an effect. For example, a lowercase letter may require nothing,
     * while an uppercase letter would require "shift", assuming the Shift key
     * is named "shift" within the layout. By default, the key will require
     * no modifiers.
     *
     * @type String[]
     */
    this.requires = template.requires || [];

    /**
     * The width of this key, in arbitrary units, relative to other keys in the
     * same layout. The true pixel size of this key will be determined by the
     * overall size of the keyboard. By default, this will be 1.
     *
     * @type Number
     */
    this.width = template.width || 1;

};
