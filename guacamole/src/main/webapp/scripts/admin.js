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

/**
 * Main Guacamole admin namespace.
 * @namespace
 */
var GuacAdmin = GuacAdmin || {};

/**
 * An arbitrary input field.
 * 
 * @constructor
 * @param {String} title A human-readable title for the field.
 * @param {String[]} available The allowed value(s), if any.
 * @param {String[]} selected The selected value(s), if any.
 */
GuacAdmin.Field = function(title, available, selected) {

    /**
     * A human-readable title describing this field.
     */
    this.title = title;

    /**
     * All available values, if any.
     */
    this.available = available || [];

    /**
     * All selected values, if any.
     */
    this.selected = selected || [];

    /**
     * Returns the DOM Element representing this field.
     * 
     * @return {Element} The DOM Element representing this field.
     */
    this.getElement = function() {};

    /**
     * Returns the selected values of this field.
     * 
     * @return {String[]} All selected values.
     */
    this.getSelected = function() {};

};


/**
 * Simple HTML input field.
 * 
 * @augments GuacAdmin.Field
 */
GuacAdmin.Field._HTML_INPUT = function(type, title, available, selected) {

    // Call parent constructor
    GuacAdmin.Field.apply(this, [title, available, selected]);

    // Create backing element
    var element = GuacUI.createElement("input");
    element.setAttribute("type", type);
    if (selected && selected.length == 1)
        element.setAttribute("value", selected[0]);

    this.getSelected = function() {
        return [element.value];
    };

    this.getElement = function() {
        return element;
    };

};

GuacAdmin.Field._HTML_INPUT.prototype = new GuacAdmin.Field();


/**
 * A basic text field.
 * 
 * @augments GuacAdmin.Field._HTML_INPUT
 */
GuacAdmin.Field.TEXT = function(title, available, selected) {
    GuacAdmin.Field._HTML_INPUT.apply(this, ["text", title, available, selected]);
};

GuacAdmin.Field.TEXT.prototype = new GuacAdmin.Field._HTML_INPUT();


/**
 * A basic password field.
 * 
 * @augments GuacAdmin.Field._HTML_INPUT
 */
GuacAdmin.Field.PASSWORD = function(title, available, selected) {
    GuacAdmin.Field._HTML_INPUT.apply(this, ["password", title, available, selected]);
};

GuacAdmin.Field.PASSWORD.prototype = new GuacAdmin.Field._HTML_INPUT();


/**
 * Multi-select list where each element has a corresponding checkbox.
 * 
 * @augments GuacAdmin.Field
 */
GuacAdmin.Field.LIST = function(title, available, selected) {

    // Call parent constructor
    GuacAdmin.Field.apply(this, [title, available, selected]);

    var i;

    // All selected connections 
    var is_selected = {};
    for (i=0; i<selected.length; i++)
        is_selected[selected[i]] = true;

    // Add elements for all list items
    var element = GuacUI.createElement("div", "list");
    for (i=0; i<available.length; i++) {

        (function() {

            // Get name 
            var name = available[i];

            // Containing div
            var list_item = GuacUI.createChildElement(element, "div", "connection");

            // Checkbox
            var checkbox = GuacUI.createChildElement(list_item, "input");
            checkbox.setAttribute("type", "checkbox");
            if (is_selected[name])
                checkbox.checked = true;

            // Update selected set when changed
            checkbox.onclick =
            checkbox.onchange = function() {

                if (checkbox.checked)
                    is_selected[name] = true;
                else if (is_selected[name])
                    delete is_selected[name];

            };

            // Connection name
            var name_element = GuacUI.createChildElement(list_item, "span", "name");
            name_element.textContent = name;

        })();

    }

    this.getElement = function() {
        return element;
    };

    this.getSelected = function() {
        return Object.keys(is_selected);
    };

};

GuacAdmin.Field.LIST.prototype = new GuacAdmin.Field();


/**
 * An arbitrary button.
 * 
 * @constructor
 * @param {String} title A human-readable title for the button.
 */
GuacAdmin.Button = function(title) {

    /**
     * A human-readable title describing this button.
     */
    this.title = title;

    // Button element
    var element = GuacUI.createElement("button");
    element.textContent = title;

    /**
     * Returns the DOM element associated with this button.
     */
    this.getElement = function() {
        return element;
    };

};

/**
 * An arbitrary table-based form. Given an array of fields and an array
 * of buttons, GuacAdmin.Form constructs a clean HTML form using DOM Elements.
 * 
 * @constructor
 * @param {GuacAdmin.Field[]} fields An array of all fields to include in the
 *                                   form.
 * @param {GuacAdmin.Button[]} buttons An array of all buttons to include in the
 *                                     form.
 */
GuacAdmin.Form = function(fields, buttons) {

    /**
     * Reference to this form.
     */
    var guac_form = this;

    // Main div and fields
    var element     = GuacUI.createElement("div", "form");
    var field_table = GuacUI.createChildElement(element, "table", "fields");

    // Buttons
    var button_div = GuacUI.createChildElement(element, "div", "object-buttons");
   
    /**
     * Returns the DOM element representing this form.
     */
    this.getElement = function() {
        return element;
    };

    /**
     * Event called when a button is clicked.
     * 
     * @event
     * @param {String} title The title of the button clicked.
     */
    this.onaction = null;

    /*
     * Add all fields
     */

    var i;
    for (i=0; i<fields.length; i++) {

        // Get field
        var field = fields[i];

        // Add elements
        var row    = GuacUI.createChildElement(field_table, "tr");
        var header = GuacUI.createChildElement(row, "th");
        var cell   = GuacUI.createChildElement(row, "td");

        // Set title
        header.textContent = field.title;

        // Add to cell
        cell.appendChild(field.getElement());

    }
    
    /*
     * Add all buttons
     */

    for (i=0; i<buttons.length; i++) {

        (function() {

            // Get title and element
            var title = buttons[i].title;
            var button_element = buttons[i].getElement();

            // Trigger onaction event when clicked
            button_element.addEventListener("click", function(e) {

                if (guac_form.onaction) {

                    // Build array of field values
                    var field_values = [];
                    for (var j=0; j<fields.length; j++)
                        field_values.push(fields[j].getSelected());

                    guac_form.onaction(title, field_values);
                    e.stopPropagation();
                    
                }

            });

            // Add to cell
            button_div.appendChild(button_element);

        })();

    }

};

/**
 * An arbitrary list item with an icon and caption.
 */
GuacAdmin.ListItem = function(type, title) {

    // Create connection display elements
    var element = GuacUI.createElement("div",  "list-item");
    var icon    = GuacUI.createChildElement(element, "div",  "icon");
    var name    = GuacUI.createChildElement(element, "span", "name");
    GuacUI.addClass(icon, type);

    // Set name
    name.textContent = title;

    /**
     * Returns the DOM element representing this connection.
     */
    this.getElement = function() {
        return element;
    };

};


/**
 * User management component.
 * @constructor
 */
GuacAdmin.UserManager = function() {

    /**
     * Reference to this UserManager.
     */
    var user_manager = this;

    /**
     * Container element for UserManager.
     */
    var element = GuacUI.createElement("div", "user-list");

    /**
     * The selected username.
     */
    this.selected = null;

    /**
     * Set of all user GuacAdmin.ListItems.
     */
    this.items = {};

    /**
     * Returns the DOM element representing this UserManager.
     */
    this.getElement = function() {
        return element;
    };

    /**
     * Adds the given username to the users visible within this UserManager.
     */
    this.add = function(username) {

        // Create item
        var item = new GuacAdmin.ListItem("user", username);
        var item_element = item.getElement();

        // Select on click
        item_element.onclick = function() {

            // If nothing selected, select this item
            if (!user_manager.selected) {

                // Change styling to reflect selected item
                GuacUI.addClass(element, "disabled");
                GuacUI.addClass(item_element, "selected");

                // Update selected item
                user_manager.selected = username;

                // User property form.
                var user_properties = new GuacAdmin.Form(

                    /* Fields */
                    [new GuacAdmin.Field.PASSWORD("Password:", [],
                         ["f12a1930-7195-11e2-bcfd-0800200c9a66"]),

                     new GuacAdmin.Field.PASSWORD("Re-enter Password:", [],
                         ["f12a1930-7195-11e2-bcfd-0800200c9a66"]),
                    
                     new GuacAdmin.Field.LIST("Connections:",
                         ["A", "B", "C"],
                         ["A"])],

                    /* Buttons */
                    [new GuacAdmin.Button("Save"),
                     new GuacAdmin.Button("Cancel"),
                     new GuacAdmin.Button("Delete")]

                );

                // Set up events
                user_properties.onaction = function(title, fields) {

                    // Call removal handler, keep window up if not allowed.
                    if (title == "Delete") {
                        if (user_manager.onremove) {
                            if (!user_manager.onremove(user_manager.selected))
                                return;
                        }
                        else
                            return;
                    }

                    // Call save handler, keep window up if not allowed.
                    if (title == "Save") {

                        if (user_manager.onsave) {

                            // FIXME: Validate passwords match
                            var password = fields[0][0];
                            var connections = fields[2];

                            if (!user_manager.onsave(user_manager.selected, password, connections))
                                return;
                        }
                        else
                            return;

                    }

                    // Hide
                    user_manager.selected = null;
                    item_element.removeChild(user_properties.getElement());
                    GuacUI.removeClass(element, "disabled");
                    GuacUI.removeClass(item_element, "selected");
                    
                };

                // Display properties
                item_element.appendChild(user_properties.getElement());

            }

        };

        // Append item
        element.appendChild(item_element);
        user_manager.items[username] = item;

    };

    /**
     * Removes the given username from the users visible within this UserManager.
     */
    this.remove = function(username) {

        // If user exists, remove corresponding element and item entry
        var item = user_manager.items[username];
        if (item) {
            element.removeChild(item.getElement());
            delete user_manager.items[username];
        }

    };

    /**
     * Sets all visible usernames.
     */
    this.setUsers = function(users) {
        /* STUB */
    };

    /**
     * Event handler called when the user wishes to add a given user.
     * 
     * @event
     * @param {String} username The username added.
     */
    this.onadd = null;

    /**
     * Event handler called when the user wishes to remove a given user.
     * 
     * @event
     * @param {String} username The username removed.
     */
    this.onremove = null;

    /**
     * Event handler called when the user has edited a given user, and wishes
     * to save the updated user to the server.
     * 
     * @event
     * @param {String} username The username edited.
     * @param {String} password The password chosen.
     * @param {String[]} connections The IDs of the connections chosen.
     */
    this.onsave = null;

};