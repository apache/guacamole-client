
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
#include <png.h>

#include <rfb/rfbclient.h>

#include "guacio.h"
#include "protocol.h"
#include "client.h"

char __guac_password[] = "potato";

static char* __GUAC_VNC_TAG_IO = "GUACIO";
static char* __GUAC_VNC_TAG_PNG_BUFFER = "PNG_BUFFER";
static char* __GUAC_VNC_TAG_PNG_BUFFER_ALPHA = "PNG_BUFFER_ALPHA";


typedef struct vnc_guac_client_data {
    rfbClient* rfb_client;
    png_byte** png_buffer;
    png_byte** png_buffer_alpha;
} vnc_guac_client_data;


void guac_vnc_cursor(rfbClient* client, int x, int y, int w, int h, int bpp) {

    int dx, dy;

    GUACIO* io = rfbClientGetClientData(client, __GUAC_VNC_TAG_IO);
    png_byte** png_buffer = rfbClientGetClientData(client, __GUAC_VNC_TAG_PNG_BUFFER_ALPHA);
    png_byte* row;

    png_byte** png_row_current = png_buffer;

    unsigned int bytesPerRow = bpp * w;
    unsigned char* fb_row_current = client->rcSource;
    unsigned char* fb_mask = client->rcMask;
    unsigned char* fb_row;
    unsigned int v;

    /* Copy image data from VNC client to PNG */
    for (dy = 0; dy<h; dy++) {

        row = *(png_row_current++);

        fb_row = fb_row_current;
        fb_row_current += bytesPerRow;

        for (dx = 0; dx<w; dx++) {

            switch (bpp) {
                case 4:
                    v = *((unsigned int*) fb_row);
                    break;

                case 2:
                    v = *((unsigned short*) fb_row);
                    break;

                default:
                    v = *((unsigned char*) fb_row);
            }

            *(row++) = (v >> client->format.redShift) * 256 / (client->format.redMax+1);
            *(row++) = (v >> client->format.greenShift) * 256 / (client->format.greenMax+1);
            *(row++) = (v >> client->format.blueShift) * 256 / (client->format.blueMax+1);

            /* Handle mask */
            if (*(fb_mask++))
                *(row++) = 255;
            else
                *(row++) = 0;

            fb_row += bpp;

        }
    }

    /* SEND CURSOR */
    guac_send_cursor(io, x, y, png_buffer, w, h);
    guac_flush(io);

}
void guac_vnc_update(rfbClient* client, int x, int y, int w, int h) {

    int dx, dy;

    GUACIO* io = rfbClientGetClientData(client, __GUAC_VNC_TAG_IO);
    png_byte** png_buffer = rfbClientGetClientData(client, __GUAC_VNC_TAG_PNG_BUFFER);
    png_byte* row;

    png_byte** png_row_current = png_buffer;

    unsigned int bpp = client->format.bitsPerPixel/8;
    unsigned int bytesPerRow = bpp * client->width;
    unsigned char* fb_row_current = client->frameBuffer + (y * bytesPerRow) + (x * bpp);
    unsigned char* fb_row;
    unsigned int v;

    /* Copy image data from VNC client to PNG */
    for (dy = y; dy<y+h; dy++) {

        row = *(png_row_current++);

        fb_row = fb_row_current;
        fb_row_current += bytesPerRow;

        for (dx = x; dx<x+w; dx++) {

            switch (bpp) {
                case 4:
                    v = *((unsigned int*) fb_row);
                    break;

                case 2:
                    v = *((unsigned short*) fb_row);
                    break;

                default:
                    v = *((unsigned char*) fb_row);
            }

            *(row++) = (v >> client->format.redShift) * 256 / (client->format.redMax+1);
            *(row++) = (v >> client->format.greenShift) * 256 / (client->format.greenMax+1);
            *(row++) = (v >> client->format.blueShift) * 256 / (client->format.blueMax+1);

            fb_row += bpp;

        }
    }

    guac_send_png(io, x, y, png_buffer, w, h);
    guac_flush(io);

}

void guac_vnc_copyrect(rfbClient* client, int src_x, int src_y, int w, int h, int dest_x, int dest_y) {

    GUACIO* io = rfbClientGetClientData(client, __GUAC_VNC_TAG_IO);

    guac_send_copy(io, src_x, src_y, w, h, dest_x, dest_y);
    guac_flush(io);

}

char* guac_vnc_get_password(rfbClient* client) {

    /* Freed after use by libvncclient */
    char* password = malloc(64);
    strncpy(password, __guac_password, 63);

    return password;
}


void vnc_guac_client_handle_messages(guac_client* client) {

    int wait_result;
    rfbClient* rfb_client = ((vnc_guac_client_data*) client->data)->rfb_client;

    wait_result = WaitForMessage(rfb_client, 2000);
    if (wait_result < 0) {
        fprintf(stderr, "WAIT FAIL\n");
        return;
    }

    if (wait_result > 0) {

        if (!HandleRFBServerMessage(rfb_client)) {
            fprintf(stderr, "HANDLE FAIL\n");
            return;
        }

    }

}


void vnc_guac_client_mouse_handler(guac_client* client, int x, int y, int mask) {

    rfbClient* rfb_client = ((vnc_guac_client_data*) client->data)->rfb_client;

    SendPointerEvent(rfb_client, x, y, mask);

}

void vnc_guac_client_key_handler(guac_client* client, int keysym, int pressed) {

    rfbClient* rfb_client = ((vnc_guac_client_data*) client->data)->rfb_client;

    SendKeyEvent(rfb_client, keysym, pressed);

}

void vnc_guac_client_free_handler(guac_client* client) {

    rfbClient* rfb_client = ((vnc_guac_client_data*) client->data)->rfb_client;
    png_byte** png_buffer = ((vnc_guac_client_data*) client->data)->png_buffer;
    png_byte** png_buffer_alpha = ((vnc_guac_client_data*) client->data)->png_buffer_alpha;

    /* Free PNG data */
    guac_free_png_buffer(png_buffer, rfb_client->height);
    guac_free_png_buffer(png_buffer_alpha, rfb_client->height);

    /* Clean up VNC client*/
    rfbClientCleanup(rfb_client);

}


void vnc_guac_client_init(guac_client* client, const char* hostname, int port) {

    char* hostname_copy;

    rfbClient* rfb_client;

    png_byte** png_buffer;
    png_byte** png_buffer_alpha;

    vnc_guac_client_data* vnc_guac_client_data;

    /*** INIT ***/
    rfb_client = rfbGetClient(8, 3, 4); /* 32-bpp client */

    /* Framebuffer update handler */
    rfb_client->GotFrameBufferUpdate = guac_vnc_update;
    /*rfb_client->GotCopyRect = guac_vnc_copyrect;*/

    /* Enable client-side cursor */
    rfb_client->GotCursorShape = guac_vnc_cursor;
    rfb_client->appData.useRemoteCursor = TRUE;

    /* Password */
    rfb_client->GetPassword = guac_vnc_get_password;

    /* Connect */
    hostname_copy = malloc(1024);
    strncpy(hostname_copy, hostname, 1024);

    rfb_client->serverHost = hostname_copy;
    rfb_client->serverPort = port;

    rfbInitClient(rfb_client, NULL, NULL);

    /* Allocate buffers */
    png_buffer = guac_alloc_png_buffer(rfb_client->width, rfb_client->height, 3); /* No-alpha */
    png_buffer_alpha = guac_alloc_png_buffer(rfb_client->width, rfb_client->height, 4); /* With alpha */

    /* Store Guac data in client */
    rfbClientSetClientData(rfb_client, __GUAC_VNC_TAG_IO, client->io);
    rfbClientSetClientData(rfb_client, __GUAC_VNC_TAG_PNG_BUFFER, png_buffer);
    rfbClientSetClientData(rfb_client, __GUAC_VNC_TAG_PNG_BUFFER_ALPHA, png_buffer_alpha);

    /* Set client data */
    vnc_guac_client_data = malloc(sizeof(vnc_guac_client_data));
    vnc_guac_client_data->rfb_client = rfb_client;
    vnc_guac_client_data->png_buffer = png_buffer;
    vnc_guac_client_data->png_buffer_alpha = png_buffer_alpha;
    client->data = vnc_guac_client_data;

    /* Set handlers */
    client->handle_messages = vnc_guac_client_handle_messages;
    client->mouse_handler = vnc_guac_client_mouse_handler;
    client->key_handler = vnc_guac_client_key_handler;

    /* Send name */
    guac_send_name(client->io, rfb_client->desktopName);

    /* Send size */
    guac_send_size(client->io, rfb_client->width, rfb_client->height);
    guac_flush(client->io);

}

