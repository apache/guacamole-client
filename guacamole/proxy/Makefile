
.PHONY: clean

all: guacd

guacd: daemon.o proxy.o
	$(CC) daemon.o proxy.o -o guacd 

proxy.o: proxy.c proxy.h
	$(CC) -c -ansi -pedantic proxy.c

daemon.o: daemon.c proxy.h
	$(CC) -c -ansi -pedantic daemon.c

clean:
	$(RM) *.o guacd

distclean: clean
	$(RM) *~

