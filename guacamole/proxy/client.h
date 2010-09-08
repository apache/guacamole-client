
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

guac_client* guac_get_client(int client_fd, void (*client_init)(guac_client* client, const char* hostname, int port), const char* hostname, int port);
void guac_start_client(guac_client* client);
void guac_free_client(guac_client* client);
png_byte** guac_alloc_png_buffer(int w, int h, int bpp);
void guac_free_png_buffer(png_byte** png_buffer, int h);

#endif
