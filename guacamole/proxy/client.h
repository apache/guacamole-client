#ifndef _CLIENT_H
#define _CLIENT_H

#include <png.h>

#include "guacio.h"

typedef struct guac_client {

    GUACIO* io;
    void* data;

    void (*handle_messages)(struct guac_client* client);
    void (*mouse_handler)(struct guac_client* client, int x, int y, int button_mask);
    void (*key_handler)(struct guac_client* client, int keysym, int pressed);
    void (*free_handler)(void* client);

} guac_client;

guac_client* guac_get_client(int client_fd, void (*client_init)(guac_client* client));
void guac_start_client(guac_client* client);
void guac_free_client(guac_client* client);
png_byte** guac_alloc_png_buffer(int w, int h, int bpp);
void guac_free_png_buffer(png_byte** png_buffer, int h);

#endif
