
CFLAGS+=-O2 -fPIC -pedantic -Wall -Werror -Iinclude
LDFLAGS=-lpng -luuid

.PHONY: clean doc

all: libguac.so

doc:
	doxygen

clean-doc:
	$(RM) -R doc

libguac.so: client.o guacio.o protocol.o clientreg.o uuidtree.o
	$(CC) -shared $(LDFLAGS) -Wl,-soname,libguac.so.0 -o libguac.so client.o protocol.o guacio.o clientreg.o uuidtree.o

client.o: client.c include/client.h include/guacio.h
	$(CC) $(CFLAGS) -c client.c

clientreg.o: clientreg.c include/uuidtree.h include/client.h
	$(CC) $(CFLAGS) -c clientreg.c

uuidtree.o: uuidtree.c include/uuidtree.h
	$(CC) $(CFLAGS) -c uuidtree.c

protocol.o: protocol.c include/protocol.h include/guacio.h
	$(CC) $(CFLAGS) -c protocol.c

guacio.o: guacio.c include/guacio.h
	$(CC) $(CFLAGS) -c guacio.c

clean:
	$(RM) *.o *.so

distclean: clean
	$(RM) *~

