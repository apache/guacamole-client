
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

#ifndef _CLIENTREG_H
#define _CLIENTREG_H

#include <stdlib.h>
#include <uuid/uuid.h>

#include "uuidtree.h"
#include "client.h"

guac_client_registry* guac_create_client_registry() {

    guac_client_registry* registry = malloc(sizeof(guac_client_registry));

    registry->root = guac_create_uuid_tree();

    return registry;

}

void guac_register_client(guac_client_registry* registry, guac_client* client) {
    guac_uuid_tree_put(registry->root, client->uuid, client);
}

guac_client* guac_find_client(guac_client_registry* registry, uuid_t uuid) {
    return (guac_client*) guac_uuid_tree_get(registry->root, uuid);
}

void guac_remove_client(guac_client_registry* registry, uuid_t uuid) {
    guac_uuid_tree_remove(registry->root, uuid);
}

void guac_cleanup_registry(guac_client_registry* registry) {
    guac_cleanup_uuid_tree(registry->root);
}

#endif
