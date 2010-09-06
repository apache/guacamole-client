
CFLAGS=-O2 -ansi -pedantic -Wall -Werror
LDFLAGS=-lpng -lvncclient

.PHONY: clean

all: guacd

guacd: daemon.o proxy.o guacio.o protocol.o
	$(CC) $(CFLAGS) $(LDFLAGS) daemon.o proxy.o protocol.o guacio.o -o guacd 

proxy.o: proxy.c proxy.h guacio.h
	$(CC) $(CFLAGS) -c proxy.c

protocol.o: protocol.c protocol.h guacio.h
	$(CC) $(CFLAGS) -c protocol.c

guacio.o: guacio.c guacio.h
	$(CC) $(CFLAGS) -c guacio.c

daemon.o: daemon.c proxy.h
	$(CC) $(CFLAGS) -c daemon.c

clean:
	$(RM) *.o guacd

distclean: clean
	$(RM) *~

