
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

function Config(protocol, id) {
    this.protocol = protocol;
    this.id = id;
}

function getConfigList(parameters) {

    // Construct request URL
    var configs_url = "configs";
    if (parameters) configs_url += "?" + parameters;

    // Get config list
    var xhr = new XMLHttpRequest();
    xhr.open("GET", configs_url, false);
    xhr.send(null);

    // If fail, throw error
    if (xhr.status != 200)
        throw new Error(xhr.statusText);

    // Otherwise, get list
    var configs = new Array();

    var configElements = xhr.responseXML.getElementsByTagName("config");
    for (var i=0; i<configElements.length; i++) {
        configs.push(new Config(
            configElements[i].getAttribute("protocol"),
            configElements[i].getAttribute("id")
        ));
    }

    return configs;
    
}
