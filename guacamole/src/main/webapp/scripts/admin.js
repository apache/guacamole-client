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
 * An arbitrary input field. Note that this object is not a component, and has
 * no corresponding element. Other objects which use GuacAdmin.Field may
 * interpret its values and render an element, however.
 * 
 * @constructor
 * @param {String} title A human-readable title for the field.
 * @param {String} type The type of the input field.
 * @param {String} value The default value, if any.
 */
GuacAdmin.Field = function(title, type, value) {

    /**
     * A human-readable title describing this field.
     */
    this.title = title;

    /**
     * The type of this field. Possible values are "text", "password", and
     * "checkbox".
     */
    this.type = type;

    /**
     * The default value of this field.
     */
    this.value = value;

};

/**
 * An arbitrary button. Note that this object is not a component, and has
 * no corresponding element. Other objects which use GuacAdmin.Button may
 * interpret its values and render an element, however.
 * 
 * @constructor
 * @param {String} title A human-readable title for the button.
 */
GuacAdmin.Button = function(title) {

    /**
     * A human-readable title describing this button.
     */
    this.title = title;

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

        switch (field.type) {

            // HTML types
            case "text":
            case "password":
            case "checkbox":
                var input  = GuacUI.createChildElement(cell, "input");

                // Set type and value
                input.setAttribute("type", field.type);
                if (field.value) input.setAttribute("value", field.value);
                break;

            // Connection list
            case "connections":
                var connection_selector = GuacUI.createChildElement(cell, "div", "connection-list");
                break;

        }

    }
    
    /*
     * Add all buttons
     */

    for (i=0; i<buttons.length; i++) {

        // Add new button
        var button = GuacUI.createChildElement(button_div, "button", name);
        button.textContent = buttons[i].title;

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
     * User property form.
     */
    var user_properties = new GuacAdmin.Form(

        /* Fields */
        [new GuacAdmin.Field("Password:", "password", "12341234"),
         new GuacAdmin.Field("Re-enter Password:", "password", "12341234"),
         new GuacAdmin.Field("Connections:", "connections")],

        /* Buttons */
        [new GuacAdmin.Button("Save"),
         new GuacAdmin.Button("Cancel"),
         new GuacAdmin.Button("Delete")]

    );


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

        // Create and append item
        var item = new GuacAdmin.ListItem("user", username);
        element.appendChild(item.getElement());

    };

    /**
     * Removes the given username from the users visible within this UserManager.
     */
    this.remove = function(username) {
        /* STUB */
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
     */
    this.onsave = null;

};