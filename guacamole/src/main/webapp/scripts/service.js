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
 * Main Guacamole web service namespace.
 * @namespace
 */
var GuacamoleService = GuacamoleService || {};

/**
 * An arbitrary Guacamole connection, consisting of an ID/protocol pair.
 * 
 * @constructor
 * @param {String} protocol The protocol used by this connection.
 * @param {String} id The ID associated with this connection.
 */
GuacamoleService.Connection = function(protocol, id) {

    /**
     * The protocol associated with this connection.
     */
    this.protocol = protocol;

    /**
     * The ID associated with this connection.
     */
    this.id = id;

};

GuacamoleService.getConnections = function(parameters) {

    // Construct request URL
    var connections_url = "connections";
    if (parameters) connections_url += "?" + parameters;

    // Get connection list
    var xhr = new XMLHttpRequest();
    xhr.open("GET", connections_url, false);
    xhr.send(null);

    // If fail, throw error
    if (xhr.status != 200)
        throw new Error(xhr.statusText);

    // Otherwise, get list
    var connections = new Array();

    var connectionElements = xhr.responseXML.getElementsByTagName("connection");
    for (var i=0; i<connectionElements.length; i++) {
        connections.push(new GuacamoleService.Connection(
            connectionElements[i].getAttribute("protocol"),
            connectionElements[i].getAttribute("id")
        ));
    }

    return connections;
 
};

