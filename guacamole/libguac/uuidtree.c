
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

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <uuid/uuid.h>

#include "uuidtree.h"

guac_uuid_tree_node* guac_create_uuid_tree() {

    guac_uuid_tree_node* tree = malloc(sizeof(guac_uuid_tree_node));

    tree->used = 0;
    memset(tree->next, 0, sizeof(tree->next));

    return tree;

}

void guac_uuid_tree_put(guac_uuid_tree_node* tree, uuid_t uuid, void* obj) {

    guac_uuid_tree_node* current = tree;
    int i;
    unsigned char index;

    for (i=0; i<sizeof(uuid_t)-1; i++) {

        guac_uuid_tree_node* next;

        /* Get next tree node */
        index = ((unsigned char*) uuid)[i];
        next = ((guac_uuid_tree_node**) current->next)[index];

        /* If no node, allocate one */
        if (next == NULL) {
            current->used++;
            next = guac_create_uuid_tree();
            ((guac_uuid_tree_node**) current->next)[index] = next;
        }

        current = next;
    }

    /* Store object */
    index = ((unsigned char*) uuid)[i];
    current->next[index] = obj;

}

void* guac_uuid_tree_get(guac_uuid_tree_node* tree, uuid_t uuid) {

    guac_uuid_tree_node* current = tree;
    int i;
    unsigned char index;

    for (i=0; i<sizeof(uuid_t)-1; i++) {

        /* Get next tree node */
        index = ((unsigned char*) uuid)[i];
        current = ((guac_uuid_tree_node**) current->next)[index];

        /* If no node, not present */
        if (current == NULL)
            return NULL;

    }

    /* Return if found */
    index = ((unsigned char*) uuid)[i];
    return current->next[index];

}

void guac_uuid_tree_remove(guac_uuid_tree_node* tree, uuid_t uuid) {

    guac_uuid_tree_node* current = tree;
    int i;
    unsigned char index;

    for (i=0; i<sizeof(uuid_t)-1; i++) {

        /* Get next tree node */
        index = ((unsigned char*) uuid)[i];
        current = ((guac_uuid_tree_node**) current->next)[index];

        /* If no node, nothing to remove */
        if (current == NULL)
            return;

    }

    /* Remove, if present*/
    if (current->next[index]) {
        current->next[index] = NULL;
        current->used--;

        /* FIXME: If no more objects at this node, clean up */
        if (current->used == 0) {
            /* STUB */
        }

    }

}

void guac_cleanup_uuid_tree(guac_uuid_tree_node* tree) {

    int i;
    for (i=0; i<sizeof(tree->next); i++) {

        if (tree->next[i] != NULL) {
            guac_cleanup_uuid_tree(tree->next[i]);
            tree->next[i] = NULL;
        }

    }

    free(tree);

}

