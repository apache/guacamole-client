#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <png.h>

#include <rfb/rfbclient.h>

#include "guacio.h"
#include "proxy.h"

char __guac_password[] = "potato";

char* __GUAC_VNC_TAG_IO = "GUACIO";
char* __GUAC_VNC_TAG_PNG_ROWS = "PNG_ROWS";

char* guac_escape_string(const char* str) {

    char* escaped;
    char* current;

    int i;
    int length = 0;

    /* Determine length */
    for (i=0; str[i] != '\0'; i++) {
        switch (str[i]) {

            case ';':
                length += 2;
                break;

            case ',':
                length += 2;
                break;

            default:
                length++;

        }
    }

    /* Allocate new */
    escaped = malloc(length+1);

    current = escaped;
    for (i=0; str[i] != '\0'; i++) {
        switch (str[i]) {

            case ';':
                *(current++) = '\\';
                *(current++) = 's';
                break;

            case ',':
                *(current++) = '\\';
                *(current++) = 'c';
                break;

            default:
                *(current++) = str[i];
        }

    }

    return escaped;

}

void guac_write_png(png_structp png, png_bytep data, png_size_t length) {

    if (guac_write_base64((GUACIO*) png->io_ptr, data, length) < 0) {
        perror("Error writing PNG");
        png_error(png, "Error writing PNG");
        return;
    }

}

void guac_write_flush(png_structp png) {
}

void guac_send_name(GUACIO* io, const char* name) {
    guac_write_string(io, "name:");
    guac_write_string(io, name);
    guac_write_string(io, ";");
}

void guac_send_size(GUACIO* io, int w, int h) {
    guac_write_string(io, "size:");
    guac_write_int(io, w);
    guac_write_string(io, ",");
    guac_write_int(io, h);
    guac_write_string(io, ";");
}

void guac_send_png(GUACIO* io, int x, int y, png_byte** png_rows, int w, int h) {

    png_structp png;
    png_infop png_info;
    png_byte* row;

    /* For now, generate random test image  */
    for (y=0; y<h; y++) {

        row = png_rows[y];

        for (x=0; x<w; x++) {
            *row++ = random() % 0xFF;
            *row++ = random() % 0xFF;
            *row++ = random() % 0xFF;
        }
    }

    /* Write image */

    /* Set up PNG writer */
    png = png_create_write_struct(PNG_LIBPNG_VER_STRING, NULL, NULL, NULL);
    if (!png) {
        perror("Error initializing libpng write structure");
        return;
    }

    png_info = png_create_info_struct(png);
    if (!png_info) {
        perror("Error initializing libpng info structure");
        png_destroy_write_struct(&png, NULL);
        return;
    }

    /* Set error handler */
    if (setjmp(png_jmpbuf(png))) {
        perror("Error setting handler");
        png_destroy_write_struct(&png, &png_info);
        return;
    }

    png_set_write_fn(png, io, guac_write_png, guac_write_flush);

    /* Set PNG IHDR */
    png_set_IHDR(
            png,
            png_info,
            w,
            h,
            8,
            PNG_COLOR_TYPE_RGB,
            PNG_INTERLACE_NONE,
            PNG_COMPRESSION_TYPE_DEFAULT,
            PNG_FILTER_TYPE_DEFAULT
    );
    
    guac_write_string(io, "png:");
    guac_write_int(io, x);
    guac_write_string(io, ",");
    guac_write_int(io, y);
    guac_write_string(io, ",");
    png_set_rows(png, png_info, png_rows);
    png_write_png(png, png_info, PNG_TRANSFORM_IDENTITY, NULL);

    if (guac_flush_base64(io) < 0) {
        perror("Error flushing PNG");
        png_error(png, "Error flushing PNG");
        return;
    }

    png_destroy_write_struct(&png, &png_info);

    guac_write_string(io, ";");

}

void guac_vnc_update(rfbClient* client, int x, int y, int w, int h) {

    GUACIO* io = rfbClientGetClientData(client, __GUAC_VNC_TAG_IO);
    png_byte** png_rows = rfbClientGetClientData(client, __GUAC_VNC_TAG_PNG_ROWS);

    guac_send_png(io, x, y, png_rows, 100, 100);
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
    char* escaped;
    int wait_result;
    rfbClient* rfb_client;

    png_byte** png_rows;
    png_byte* row;
    int x, y;

    GUACIO* io = guac_open(client_fd);

    /*** INIT ***/

    /* Allocate rows for PNG */
    png_rows = (png_byte**) malloc(100 /* height */ * sizeof(png_byte*));
    for (y=0; y<100 /* height */; y++) {
        row = (png_byte*) malloc(sizeof(png_byte) * 3 * 100 /* width */);
        png_rows[y] = row;
    }

    rfb_client = rfbGetClient(8, 3, 4); /* 32-bpp client */

    /* Framebuffer update handler */
    rfb_client->GotFrameBufferUpdate = guac_vnc_update;

    /* Password */
    rfb_client->GetPassword = guac_vnc_get_password;

    hostname = malloc(64);
    strcpy(hostname, "localhost");

    rfb_client->serverHost = hostname;
    rfb_client->serverPort = 5902;

    if (rfbInitClient(rfb_client, NULL, NULL)) {
        fprintf(stderr, "SUCCESS.\n");
    }

    /* Store Guac data in client */
    rfbClientSetClientData(rfb_client, __GUAC_VNC_TAG_IO, io);
    rfbClientSetClientData(rfb_client, __GUAC_VNC_TAG_PNG_ROWS, png_rows);

    /* Send name */
    escaped = guac_escape_string(rfb_client->desktopName);
    guac_send_name(io, escaped);
    free(escaped);

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

    /* Clean up */

    rfbClientCleanup(rfb_client);

    guac_write_string(io, "error:Test finished.;");

    /* Free PNG data */
    for (y = 0; y<100 /* height */; y++)
        free(png_rows[y]);
    free(png_rows);

    guac_close(io);
}

