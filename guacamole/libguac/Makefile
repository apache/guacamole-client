
CFLAGS=-g -fPIC -pedantic -Wall -Werror -Iinclude
LDFLAGS=-lpng -luuid

.PHONY: clean doc

all: libguac.so

doc:
	doxygen

clean-doc:
	$(RM) -R doc

libguac.so: client.o guacio.o protocol.o
	$(CC) $(CFLAGS) -shared $(LDFLAGS) -Wl,-soname,libguac.so.0 -o libguac.so client.o protocol.o guacio.o

client.o: client.c include/client.h include/guacio.h
	$(CC) $(CFLAGS) -c client.c

protocol.o: protocol.c include/protocol.h include/guacio.h
	$(CC) $(CFLAGS) -c protocol.c

guacio.o: guacio.c include/guacio.h
	$(CC) $(CFLAGS) -c guacio.c

clean:
	$(RM) *.o *.so

distclean: clean
	$(RM) *~

