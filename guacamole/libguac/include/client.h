
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

/**
 * Guacamole proxy client.
 *
 * Represents a Guacamole proxy client (the client which communicates to
 * a server on behalf of Guacamole, on behalf of the web-client).
 */
typedef struct guac_client {

    /**
     * The GUACIO structure to be used to communicate with the web-client. It is
     * expected that the implementor of any Guacamole proxy client will provide
     * their own mechanism of I/O for their protocol. The GUACIO structure is
     * used only to communicate conveniently with the Guacamole web-client.
     */
    GUACIO* io;

    /**
     * Arbitrary reference to proxy client-specific data. Implementors of a
     * Guacamole proxy client can store any data they want here, which can then
     * be retrieved as necessary in the message handlers.
     */
    void* data;

    void (*handle_messages)(struct guac_client* client);
    void (*mouse_handler)(struct guac_client* client, int x, int y, int button_mask);
    void (*key_handler)(struct guac_client* client, int keysym, int pressed);
    void (*clipboard_handler)(struct guac_client* client, char* copied);
    void (*free_handler)(void* client);

} guac_client;

guac_client* guac_get_client(int client_fd, void (*client_init)(guac_client* client, const char* hostname, int port), const char* hostname, int port);
void guac_start_client(guac_client* client);
void guac_free_client(guac_client* client);
png_byte** guac_alloc_png_buffer(int w, int h, int bpp);
void guac_free_png_buffer(png_byte** png_buffer, int h);

#endif
