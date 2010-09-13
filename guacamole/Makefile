
.PHONY: client proxy clean libguac vnc

all: client proxy vnc

client:
	$(MAKE) -C client all

proxy: libguac
	$(MAKE) -C proxy all

libguac:
	$(MAKE) -C libguac all

vnc: libguac
	$(MAKE) -C vnc all

clean:
	$(MAKE) -C client clean
	$(MAKE) -C proxy clean
	$(MAKE) -C libguac clean
	$(MAKE) -C vnc clean 

