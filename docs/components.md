# Component Tree

- InstFetcher
    -> InstMemory
- InstDecoder
    <- InstMemory
- Executor
    - ArithmeticLogicUnit
    <- RegisterFile
- MemoryAccessor
    - Memory
    <- Memory
- WritebackUnit
    -> RegisterFile
    -> Memory

# Component Descriptions

## InstFetcher

Manages the Program Counter. When not Stalled (FRONTEND_STALL), it first updates the program counter (if PC_OVERWRITE is high, it uses the provided address; otherwise, increment by 4), then sends an updated request to InstMemory to fetch the instruction (which after one cycle will be available to InstDecoder).

> Initial State: PC_OVERWRITE = 1, PC_OVERWRITE_ADDR = 0

## InstDecoder

Fetches the instruction from InstMemory and decodes it into a DecodedInstBundle for further processing in the pipeline.

If the either:
- The instruction is a branch instruction (opcode 1100011)
- The instruction is a jump instruction (opcode 1101111 or 1100111)

The frontend stall flag will set, until the corresponding destination address is calculated and sent back to InstFetcher to update the PC.
