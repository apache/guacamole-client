AUTOMAKE_OPTIONS = gnu

AM_CFLAGS = -Werror -Wall -Iinclude

lib_LTLIBRARIES = libguac.la

libguac_la_SOURCES = client.c clientreg.c guacio.c protocol.c uuidtree.c
include_HEADERS = include/client.h include/guacio.h include/protocol.h include/uuidtree.h

libguac_la_LDFLAGS = -version-info 0:0:0

