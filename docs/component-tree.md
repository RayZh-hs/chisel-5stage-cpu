# Component Tree

- InstFetcher (manage PC)
    - InstMemory
    <- InstMemory
- InstDecoder
- Executor
    - ArithmeticLogicUnit
    <- RegisterFile
- MemoryAccessor
    - Memory
    <- Memory
- WritebackUnit
    -> RegisterFile
    -> Memory
