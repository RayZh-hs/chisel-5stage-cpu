# Chisel: 5-Stage Pipelined CPU

This repository contains a Chisel implementation of a 5-stage pipelined CPU, part of a computer architecture course project at SJTU.

## Running tests

The simulator runs RV32I bare-metal programs from a `.hex` file (one 32-bit word per line) and exits when the program stores its return value to `0xFFFFFFF0` (see `sw/crt0.S`).

Run tests via Mill:
- `mill cpu.test.test` builds C tests in `cpu/test/resources/c/*.c` into `cpu/test/generated/*.hex` using `sw/crt0.S` + `sw/link.ld` (RV32I `-mabi=ilp32`, no libc)
- Then runs them through `core.Runner` and checks exit codes against `cpu/test/resources/expected/*.expected`

Prereq: a RISC-V GCC toolchain providing `riscv32-unknown-elf-gcc` and `riscv32-unknown-elf-objcopy`.
If your toolchain binaries are not on `PATH`, set `RISCV_BIN` to the bin directory prefix, e.g. `/opt/riscv/bin/`.
