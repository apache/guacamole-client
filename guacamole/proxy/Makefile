
.PHONY: clean

all: guacd

guacd: daemon.o proxy.o base64.o
	$(CC) daemon.o proxy.o base64.o -l png -o guacd 

proxy.o: proxy.c proxy.h base64.h
	$(CC) -c -ansi -pedantic proxy.c

base64.o: base64.c base64.h
	$(CC) -c -ansi -pedantic base64.c

daemon.o: daemon.c proxy.h
	$(CC) -c -ansi -pedantic daemon.c

clean:
	$(RM) *.o guacd

distclean: clean
	$(RM) *~

