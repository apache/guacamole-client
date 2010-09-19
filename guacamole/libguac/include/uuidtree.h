
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

#ifndef _UUIDTREE_H
#define _UUIDTREE_H

#include <uuid/uuid.h>

typedef struct guac_uuid_tree_node guac_uuid_tree_node;

/**
 * Represents a single node of a tree storing objects by UUID.
 */
struct guac_uuid_tree_node {

    /**
     * The number of pointers used inside the next array.
     */
    int used;

    /**
     * The next guac_uuid_tree_node if currently looking at any byte
     * of the UUID except the last, or the stored object if looking at the
     * last byte of the UUID.
     */
    void* next[256];

};

guac_uuid_tree_node* guac_create_uuid_tree();
void guac_uuid_tree_put(guac_uuid_tree_node* tree, uuid_t uuid, void* obj);
void* guac_uuid_tree_get(guac_uuid_tree_node* tree, uuid_t uuid);
void guac_uuid_tree_remove(guac_uuid_tree_node* tree, uuid_t uuid);
void guac_cleanup_uuid_tree(guac_uuid_tree_node* tree);

#endif
