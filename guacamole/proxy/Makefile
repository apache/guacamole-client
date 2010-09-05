
.PHONY: clean

all: guacd

guacd: daemon.o proxy.o guacio.o
	$(CC) daemon.o proxy.o guacio.o -l png -o guacd 

proxy.o: proxy.c proxy.h guacio.h
	$(CC) -c -ansi -pedantic proxy.c

guacio.o: guacio.c guacio.h
	$(CC) -c -ansi -pedantic guacio.c

daemon.o: daemon.c proxy.h
	$(CC) -c -ansi -pedantic daemon.c

clean:
	$(RM) *.o guacd

distclean: clean
	$(RM) *~

