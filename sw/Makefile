CROSS_COMPILE = $(RISCV_BIN)riscv32-unknown-elf-
CC = $(CROSS_COMPILE)gcc
OBJCOPY = $(CROSS_COMPILE)objcopy
LDFLAGS = -T link.ld -nostdlib -static
CFLAGS = -march=rv32i -mabi=ilp32 -O2

all: main.hex

main.elf: crt0.S main.c
	$(CC) $(CFLAGS) $(LDFLAGS) crt0.S main.c -o main.elf

main.bin: main.elf
	$(OBJCOPY) -O binary main.elf main.bin

main.hex: main.bin
	od -An -tx4 -v main.bin | tr -s ' ' '\n' | grep -v '^$$' > main.hex

clean:
	rm -f *.elf *.bin *.hex
