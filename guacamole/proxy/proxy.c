#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <png.h>

#include <rfb/rfbclient.h>

#include "guacio.h"
#include "protocol.h"
#include "proxy.h"

char __guac_password[] = "potato";

char* __GUAC_VNC_TAG_IO = "GUACIO";
char* __GUAC_VNC_TAG_PNG_ROWS = "PNG_ROWS";

void guac_vnc_update(rfbClient* client, int x, int y, int w, int h) {

    int dx, dy;

    GUACIO* io = rfbClientGetClientData(client, __GUAC_VNC_TAG_IO);
    png_byte** png_rows = rfbClientGetClientData(client, __GUAC_VNC_TAG_PNG_ROWS);
    png_byte* row;

    png_byte** png_row_current = png_rows;

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

    guac_send_png(io, x, y, png_rows, w, h);
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

    fprintf(stderr, "Sending password: %s\n", password);

    return password;
}

void proxy(int client_fd) {

    char* hostname;
    int wait_result;
    rfbClient* rfb_client;

    png_byte** png_rows;
    png_byte* row;
    int y;

    GUACIO* io = guac_open(client_fd);

    /*** INIT ***/

    rfb_client = rfbGetClient(8, 3, 4); /* 32-bpp client */

    /* Framebuffer update handler */
    rfb_client->GotFrameBufferUpdate = guac_vnc_update;
    /*rfb_client->GotCopyRect = guac_vnc_copyrect;*/

    /* Password */
    rfb_client->GetPassword = guac_vnc_get_password;

    hostname = malloc(64);
    strcpy(hostname, "localhost");

    rfb_client->serverHost = hostname;
    rfb_client->serverPort = 5902;

    if (rfbInitClient(rfb_client, NULL, NULL)) {
        fprintf(stderr, "SUCCESS.\n");
    }

    /* Allocate rows for PNG */
    png_rows = (png_byte**) malloc(rfb_client->height * sizeof(png_byte*));
    for (y=0; y<rfb_client->width; y++) {
        row = (png_byte*) malloc(sizeof(png_byte) * 3 * rfb_client->width);
        png_rows[y] = row;
    }

    /* Store Guac data in client */
    rfbClientSetClientData(rfb_client, __GUAC_VNC_TAG_IO, io);
    rfbClientSetClientData(rfb_client, __GUAC_VNC_TAG_PNG_ROWS, png_rows);

    /* Send name */
    guac_send_name(io, rfb_client->desktopName);

    /* Send size */
    guac_send_size(io, rfb_client->width, rfb_client->height);
    guac_flush(io);

    /* VNC Client Loop */
    for (;;) {

        wait_result = WaitForMessage(rfb_client, 2000);
        if (wait_result < 0) {
            fprintf(stderr, "WAIT FAIL\n");
            break;
        }

        if (wait_result > 0) {

            if (!HandleRFBServerMessage(rfb_client)) {
                fprintf(stderr, "HANDLE FAIL\n");
                break;
            }

        }
    }

    /* Free PNG data */
    for (y = 0; y<rfb_client->height; y++)
        free(png_rows[y]);
    free(png_rows);

    /* Clean up VNC client*/

    rfbClientCleanup(rfb_client);

    guac_write_string(io, "error:Test finished.;");
    guac_close(io);
}

