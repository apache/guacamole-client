
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

#include <syslog.h>

#include "guacio.h"
#include "protocol.h"
#include "client.h"


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

void __guac_set_client_io(guac_client* client, GUACIO* io) {
    sem_wait(&(client->io_lock)); /* Acquire I/O */
    client->io = io;
}

void __guac_release_client_io(guac_client* client) {
    sem_post(&(client->io_lock));
}

guac_client* __guac_alloc_client(GUACIO* io) {

    /* Allocate new client (not handoff) */
    guac_client* client = malloc(sizeof(guac_client));
    memset(client, 0, sizeof(guac_client));

    /* Init new client */
    client->io = io;
    uuid_generate(client->uuid);
    sem_init(&(client->io_lock), 0, 0); /* I/O starts locked */

    return client;
}


guac_client* guac_get_client(int client_fd, guac_client_registry* registry, guac_client_init_handler* client_init, int argc, char** argv) {

    guac_client* client;
    GUACIO* io = guac_open(client_fd);
    guac_instruction instruction;

    /* Make copies of arguments */
    char** safe_argv = malloc(argc * sizeof(char*));
    char** scratch_argv = malloc(argc * sizeof(char*));

    int i;
    for (i=0; i<argc; i++)
        scratch_argv[i] = safe_argv[i] = strdup(argv[i]);

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
                if (registry) {
                    guac_register_client(registry, client);

                    /* Send UUID to web-client */
                    guac_send_uuid(io, client->uuid);
                    guac_flush(client->io);
                }

                if (client_init(client, argc, scratch_argv) != 0)
                    return NULL;

                break;
            }

            /* resume -> resume existing connection (when that connection pauses) */
            if (strcmp(instruction.opcode, "resume") == 0) {

                if (registry) {

                    client = guac_find_client(
                            registry,
                            (unsigned char*) guac_decode_base64_inplace(instruction.argv[0])
                    );

                    if (client) {
                        __guac_set_client_io(client, io);
                        return NULL; /* Returning NULL, so old client loop is used */
                        /* FIXME: Fix semantics of returning NULL vs ptr. This function needs redocumentation, and callers
                         * need to lose their "error" handling. */
                    }
                }

                return NULL;

            }

        }

        if (retval < 0)
            return NULL; /* EOF or error */

        /* Otherwise, retval == 0 implies unfinished instruction */

    }

    /* Free memory used for arg copy */
    for (i=0; i<argc; i++)
        free(safe_argv[i]);
    
    free(safe_argv);
    free(scratch_argv);

    return client;

}


void guac_free_client(guac_client* client, guac_client_registry* registry) {

    if (client->free_handler) {
        if (client->free_handler(client))
            syslog(LOG_ERR, "Error calling client free handler");
    }

    guac_close(client->io);

    guac_remove_client(registry, client->uuid);

    free(client);
}


void guac_start_client(guac_client* client) {

    guac_client client_copy;
    guac_instruction instruction;
    int wait_result;

    /* Maintained copy of client used for all calls to handlers, thus changes to I/O
     * are controlled without starvation-prone blocking
     */
    memcpy(&client_copy, client, sizeof(guac_client));

    /* VNC Client Loop */
    for (;;) {

        /* Accept changes to client I/O only before handling messages */
        if (client_copy.io != client->io) {
            guac_close(client_copy.io); /* Close old I/O */
            client_copy.io = client->io;
        }

        /* Handle server messages */
        if (client->handle_messages) {

            int retval = client->handle_messages(&client_copy);
            if (retval) {
                syslog(LOG_ERR, "Error handling server messages");
                return;
            }

            guac_flush(client_copy.io);
        }

        wait_result = guac_instructions_waiting(client_copy.io);
        if (wait_result > 0) {

            int retval;
            retval = guac_read_instruction(client_copy.io, &instruction); /* 0 if no instructions finished yet, <0 if error or EOF */

            if (retval > 0) {
           
                do {

                    if (strcmp(instruction.opcode, "mouse") == 0) {
                        if (client->mouse_handler)
                            if (
                                    client->mouse_handler(
                                        client,
                                        atoi(instruction.argv[0]), /* x */
                                        atoi(instruction.argv[1]), /* y */
                                        atoi(instruction.argv[2])  /* mask */
                                    )
                               ) {

                                syslog(LOG_ERR, "Error handling mouse instruction");
                                return;

                            }
                    }

                    else if (strcmp(instruction.opcode, "key") == 0) {
                        if (client->key_handler)
                            if (
                                    client->key_handler(
                                        client,
                                        atoi(instruction.argv[0]), /* keysym */
                                        atoi(instruction.argv[1])  /* pressed */
                                    )
                               ) {

                                syslog(LOG_ERR, "Error handling key instruction");
                                return;

                            }
                    }

                    else if (strcmp(instruction.opcode, "clipboard") == 0) {
                        if (client->clipboard_handler)
                            if (
                                    client->clipboard_handler(
                                        client,
                                        guac_unescape_string_inplace(instruction.argv[0]) /* data */
                                    )
                               ) {

                                syslog(LOG_ERR, "Error handling clipboard instruction");
                                return;

                            }
                    }

                    else if (strcmp(instruction.opcode, "pause") == 0) {

                        /* Allow other connection to take over I/O */
                        __guac_release_client_io(client);
                        
                    }

                    else if (strcmp(instruction.opcode, "disconnect") == 0) {
                        syslog(LOG_INFO, "Client requested disconnect");
                        return;
                    }

                } while ((retval = guac_read_instruction(client_copy.io, &instruction)) > 0);

                if (retval < 0) {
                    syslog(LOG_ERR, "Error reading instruction from stream");
                    return;
                }
            }

            if (retval < 0) {
                syslog(LOG_ERR, "Error or end of stream");
                return; /* EOF or error */
            }

            /* Otherwise, retval == 0 implies unfinished instruction */

        }
        else if (wait_result < 0) {
            syslog(LOG_ERR, "Error waiting for next instruction");
            return;
        }

    }

}

