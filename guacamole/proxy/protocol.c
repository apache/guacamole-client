#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <png.h>

#include "guacio.h"
#include "protocol.h"

char* guac_escape_string(const char* str) {

    char* escaped;
    char* current;

    int i;
    int length = 0;

    /* Determine length */
    for (i=0; str[i] != '\0'; i++) {
        switch (str[i]) {

            case ';':
            case ',':
            case '\\':
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

            case '\\':
                *(current++) = '\\';
                *(current++) = '\\';
                break;

            default:
                *(current++) = str[i];
        }

    }

    return escaped;

}

void guac_send_name(GUACIO* io, const char* name) {

    char* escaped = guac_escape_string(name); 

    guac_write_string(io, "name:");
    guac_write_string(io, name);
    guac_write_string(io, ";");

    free(escaped);

}

void guac_send_size(GUACIO* io, int w, int h) {
    guac_write_string(io, "size:");
    guac_write_int(io, w);
    guac_write_string(io, ",");
    guac_write_int(io, h);
    guac_write_string(io, ";");
}

void guac_send_copy(GUACIO* io, int srcx, int srcy, int w, int h, int dstx, int dsty) {
    guac_write_string(io, "copy:");
    guac_write_int(io, srcx);
    guac_write_string(io, ",");
    guac_write_int(io, srcy);
    guac_write_string(io, ",");
    guac_write_int(io, w);
    guac_write_string(io, ",");
    guac_write_int(io, h);
    guac_write_string(io, ",");
    guac_write_int(io, dstx);
    guac_write_string(io, ",");
    guac_write_int(io, dsty);
    guac_write_string(io, ";");
}

void __guac_write_png(png_structp png, png_bytep data, png_size_t length) {

    if (guac_write_base64((GUACIO*) png->io_ptr, data, length) < 0) {
        perror("Error writing PNG");
        png_error(png, "Error writing PNG");
        return;
    }

}

void __guac_write_flush(png_structp png) {
}

void guac_send_png(GUACIO* io, int x, int y, png_byte** png_rows, int w, int h) {

    png_structp png;
    png_infop png_info;

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

    png_set_write_fn(png, io, __guac_write_png, __guac_write_flush);

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


int __guac_fill_messagebuf(GUACIO* io) {

    int retval;
    
    /* Attempt to fill buffer */
    retval = read(
            io->fd,
            io->messagebuf + io->messagebuf_used_length,
            io->messagebuf_size - io->messagebuf_used_length
    );

    if (retval < 0)
        return retval;

    io->messagebuf_used_length += retval;

    /* Expand buffer if necessary */
    if (io->messagebuf_used_length > io->messagebuf_size / 2) {
        io->messagebuf_size *= 2;
        io->messagebuf = realloc(io->messagebuf, io->messagebuf_size);
    }

    return retval;

}

guac_message* guac_read_message(GUACIO* io) {

    guac_message* parsed_message;
    int retval;
    int i = 0;
    
    /* Loop until a message is read */
    for (;;) {

        /* Search for end of message */
        for (; i < io->messagebuf_used_length; i++) {

            if (io->messagebuf[i] == ';') {

                /* Parse new message */
                char* message = malloc(i+1);
                memcpy(message, io->messagebuf, i+1);
                message[i] = '\0'; /* Replace semicolon with null terminator. */

                parsed_message = malloc(sizeof(guac_message));
                parsed_message->opcode = message;
                parsed_message->argc = 0;
                parsed_message->argv = NULL;

                /* Found. Reset buffer */
                memmove(io->messagebuf, io->messagebuf + i + 1, io->messagebuf_used_length - i - 1);
                io->messagebuf_used_length -= i + 1;

                /* Done */
                return parsed_message;
            }

        }

        /* No message yet? Get more data ... */
        retval = guac_select(io, 1000);
        if (retval < 0)
            return NULL;

        /* Break if descriptor doesn't have enough data */
        if (retval == 0)
            return NULL; /* SOFT FAIL: No message ... yet, but is still in buffer */

        retval = __guac_fill_messagebuf(io);
        if (retval < 0 && errno != EAGAIN && errno != EWOULDBLOCK)
            return NULL;

    }

}

void guac_free_message(guac_message* message) {
    free(message->opcode);

    if (message->argv)
        free(message->argv);

    free(message);
}


int guac_messages_waiting(GUACIO* io) {

    if (io->messagebuf_used_length > 0)
        return 1;

    return guac_select(io, 1000);
}

