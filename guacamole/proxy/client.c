
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

guac_client* guac_get_client(int client_fd, void (*client_init)(guac_client* client, const char* hostname, int port), const char* hostname, int port) {

    guac_client* client = malloc(sizeof(guac_client));

    client->io = guac_open(client_fd);

    client_init(client, hostname, port);
    guac_flush(client->io);

    return client;

}

void guac_free_client(guac_client* client) {
    if (client->free_handler)
        client->free_handler(client);

    guac_close(client->io);

    free(client);
}


void guac_start_client(guac_client* client) {

    guac_instruction instruction;
    GUACIO* io = client->io;
    int wait_result;

    /* VNC Client Loop */
    for (;;) {

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

                } while ((retval = guac_read_instruction(io, &instruction)) > 0);

                if (retval < 0)
                    return;
            }

            if (retval < 0)
                return; /* EOF or error */

            /* Otherwise, retval == 0 implies unfinished instruction */

            /* Handle server messages */
            if (client->handle_messages) {
                client->handle_messages(client);
                guac_flush(client->io);
            }

        }
        else if (wait_result < 0)
            return;

    }

}

