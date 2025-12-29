# Chisel: 5-Stage Pipelined CPU

This repository contains a Chisel implementation of a 5-stage pipelined CPU, part of a computer architecture course project at SJTU.

## Running tests

The simulator runs RV32I bare-metal programs from a `.hex` file (one 32-bit word per line) and exits when the program stores its return value to `0xFFFFFFF0` (see `cpu/test/resources/linkage/crt0.S`).

Run tests via Mill:
- `mill cpu.test.test` builds C tests in `cpu/test/resources/c/*.c` into `cpu/test/generated/*.hex` using the startup code and linker script in `cpu/test/resources/linkage/` (RV32I `-mabi=ilp32`, no libc).
- Then runs them through `core.Runner` and checks exit codes against `cpu/test/resources/expected/*.expected`.

Prereq: a RISC-V GCC toolchain. The test suite automatically searches for common prefixes like `riscv32-unknown-elf-`, `riscv64-unknown-elf-`, or `riscv64-linux-gnu-`.
If your toolchain binaries are not on `PATH`, set `RISCV_BIN` to the bin directory (e.g. `/opt/riscv/bin/`).
