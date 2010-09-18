
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

#include "guacio.h"
#include "protocol.h"
#include "client.h"



guac_client_registry_node* guac_create_client_registry() {

    guac_client_registry_node* registry = malloc(sizeof(guac_client_registry_node));

    registry->used = 0;
    memset(registry->next, 0, sizeof(registry->next));

    return registry;

}

void guac_register_client(guac_client_registry_node* registry, guac_client* client) {

    guac_client_registry_node* current = registry;
    int i;
    unsigned char index;

    for (i=0; i<sizeof(uuid_t)-1; i++) {

        guac_client_registry_node* next;

        /* Get next registry node */
        index = ((unsigned char*) client->uuid)[i];
        next = ((guac_client_registry_node**) current->next)[index];

        /* If no node, allocate one */
        if (next == NULL) {
            current->used++;
            next = guac_create_client_registry();
            ((guac_client_registry_node**) current->next)[index] = next;
        }

        current = next;
    }

    /* Register client */
    index = ((unsigned char*) client->uuid)[i];
    ((guac_client**) current->next)[index] = client;

}

guac_client* guac_find_client(guac_client_registry_node* registry, uuid_t uuid) {

    guac_client_registry_node* current = registry;
    int i;
    unsigned char index;

    for (i=0; i<sizeof(uuid_t)-1; i++) {

        /* Get next registry node */
        index = ((unsigned char*) uuid)[i];
        current = ((guac_client_registry_node**) current->next)[index];

        /* If no node, client not registered */
        if (current == NULL)
            return NULL;

    }

    /* Return client found (if any) */
    index = ((unsigned char*) uuid)[i];
    return ((guac_client**) current->next)[index];

}

void guac_remove_client(guac_client_registry_node* registry, guac_client* client) {

    guac_client_registry_node* current = registry;
    int i;
    unsigned char index;

    for (i=0; i<sizeof(uuid_t)-1; i++) {

        /* Get next registry node */
        index = ((unsigned char*) client->uuid)[i];
        current = ((guac_client_registry_node**) current->next)[index];

        /* If no node, client not registered */
        if (current == NULL)
            return;

    }

    /* Remove client, if registered */
    if (((guac_client**) current->next)[index]) {
        ((guac_client**) current->next)[index] = NULL;
        current->used--;

        /* FIXME: If no more clients at this node, clean up */
        if (current->used == 0) {
            /* STUB */
        }

    }

}

void guac_cleanup_client_registry(guac_client_registry_node* registry) {

    int i;
    for (i=0; i<sizeof(registry->next); i++) {

        if (registry->next[i] != NULL) {
            guac_cleanup_client_registry(registry->next[i]);
            registry->next[i] = NULL;
        }

    }

    free(registry);

}



png_byte** guac_alloc_png_buffer(int w, int h, int bpp) {

    png_byte** png_buffer;
    png_byte* row;
    int y;

    /* Allocate rows for PNG */
    png_buffer = (png_byte**) malloc(h * sizeof(png_byte*));
    for (y=0; y<h; y++) {
        row = (png_byte*) malloc(sizeof(png_byte) * bpp * w);
        png_buffer[y] = row;
    }

    return png_buffer;
}

void guac_free_png_buffer(png_byte** png_buffer, int h) {

    int y;

    /* Free PNG data */
    for (y = 0; y<h; y++)
        free(png_buffer[y]);
    free(png_buffer);

}

guac_client* __guac_alloc_client(GUACIO* io) {

    /* Allocate new client (not handoff) */
    guac_client* client = malloc(sizeof(guac_client));

    /* Init new client */
    client->io = io;
    uuid_generate(client->uuid); 

    return client;
}


guac_client* guac_get_client(int client_fd, guac_client_registry_node* registry, guac_client_init_handler* client_init, const char* hostname, int port) {

    guac_client* client;
    GUACIO* io = guac_open(client_fd);
    guac_instruction instruction;

    /* Wait for handshaking messages */
    for (;;) {

        int retval;
        retval = guac_read_instruction(io, &instruction); /* 0 if no instructions finished yet, <0 if error or EOF */

        if (retval > 0) {
           
            /* connect -> create new client connection */
            if (strcmp(instruction.opcode, "connect") == 0) {
                
                /* Create new client */
                client = __guac_alloc_client(io);

                /* Register client */
                if (registry)
                    guac_register_client(registry, client);

                /* Send UUID to web-client */
                guac_send_uuid(io, client->uuid);
                break;
            }

        }

        if (retval < 0)
            return NULL; /* EOF or error */

        /* Otherwise, retval == 0 implies unfinished instruction */

    }

    /* FIXME: hostname and port should not be required. Should be made available in some sort of client-contained argc/argv, specified after the protocol on the commandline */
    client_init(client, hostname, port);
    guac_flush(client->io);
    return client;

}


void guac_free_client(guac_client* client, guac_client_registry_node* registry) {

    if (client->free_handler)
        client->free_handler(client);

    guac_close(client->io);

    guac_remove_client(registry, client);

    free(client);
}


void guac_start_client(guac_client* client) {

    guac_instruction instruction;
    GUACIO* io = client->io;
    int wait_result;

    /* VNC Client Loop */
    for (;;) {

        /* Handle server messages */
        if (client->handle_messages) {
            client->handle_messages(client);
            guac_flush(client->io);
        }

        wait_result = guac_instructions_waiting(io);
        if (wait_result > 0) {

            int retval;
            retval = guac_read_instruction(io, &instruction); /* 0 if no instructions finished yet, <0 if error or EOF */

            if (retval > 0) {
           
                do {

                    if (strcmp(instruction.opcode, "mouse") == 0) {
                        if (client->mouse_handler)
                            client->mouse_handler(
                                client,
                                atoi(instruction.argv[0]), /* x */
                                atoi(instruction.argv[1]), /* y */
                                atoi(instruction.argv[2])  /* mask */
                            );
                    }

                    else if (strcmp(instruction.opcode, "key") == 0) {
                        if (client->key_handler)
                            client->key_handler(
                                client,
                                atoi(instruction.argv[0]), /* keysym */
                                atoi(instruction.argv[1])  /* pressed */
                            );
                    }

                    else if (strcmp(instruction.opcode, "clipboard") == 0) {
                        if (client->clipboard_handler)
                            client->clipboard_handler(
                                client,
                                guac_unescape_string_inplace(instruction.argv[0]) /* data */
                            );
                    }

                    else if (strcmp(instruction.opcode, "disconnect") == 0) {
                        return;
                    }

                } while ((retval = guac_read_instruction(io, &instruction)) > 0);

                if (retval < 0)
                    return;
            }

            if (retval < 0)
                return; /* EOF or error */

            /* Otherwise, retval == 0 implies unfinished instruction */

        }
        else if (wait_result < 0)
            return;

    }

}

