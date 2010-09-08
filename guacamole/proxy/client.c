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

guac_client* guac_get_client(int client_fd, void (*client_init)(guac_client* client)) {

    guac_client* client = malloc(sizeof(guac_client));

    client->io = guac_open(client_fd);

    client_init(client);

    return client;

}

void guac_free_client(guac_client* client) {
    if (client->free_handler)
        client->free_handler(client);

    guac_close(client->io);

    free(client);
}


void guac_start_client(guac_client* client) {

    GUACIO* io = client->io;
    int wait_result;

    /* VNC Client Loop */
    for (;;) {

        /* Handle server messages */
        if (client->handle_messages)
            client->handle_messages(client);

        wait_result = guac_instructions_waiting(io);
        if (wait_result > 0) {

            guac_instruction* instruction;
           
            while ((instruction = guac_read_instruction(io))) {

                if (strcmp(instruction->opcode, "mouse") == 0) {
                    if (client->mouse_handler)
                        client->mouse_handler(
                            client,
                            atoi(instruction->argv[0]), /* x */
                            atoi(instruction->argv[1]), /* y */
                            atoi(instruction->argv[2])  /* mask */
                        );
                }

                else if (strcmp(instruction->opcode, "key") == 0) {
                    if (client->key_handler)
                        client->key_handler(
                            client,
                            atoi(instruction->argv[0]), /* keysym */
                            atoi(instruction->argv[1])  /* pressed */
                        );
                }

                guac_free_instruction(instruction);
            }
        }

    }

}

