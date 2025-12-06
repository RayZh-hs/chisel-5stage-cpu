package components

import common.Constants
import chisel3._
import chisel3.util._

class Memory extends Module {
    val io = IO(new Bundle {
        // All read and write are done via methods
    })

    val mem = SyncReadMem(Constants.memorySize, UInt(8.W))

    /**
     * Reads a byte from memory at the specified address.
     * @param addr The address to read from (UInt wire)
     * @return The byte at the given address (UInt wire, available next cycle)
     * 
     * @note For use with multiple modules, wire in the containing module. Example:
     * ```
     * val memory = Module(new Memory)
     * val requestSender = Module(new RequestSender)
     * val responseReceiver = Module(new ResponseReceiver)
     * responseReceiver.memoryInput := memory.readByte(requestSender.memoryAddressOutput)
     * ```
    */
    def readByte(addr: UInt): UInt = {
        sanitizeAddr(addr)
        return mem.read(addr)
    }

    /**
      * Reads a half-word (2 bytes) from memory at the specified address.
      * @param addr The address to read from (UInt wire)
      * @return The half-word at the given address (UInt wire, available next cycle)
      * @note For use with multiple modules, wire in the containing module. Example:
      * ```
      * val memory = Module(new Memory)
      * val requestSender = Module(new RequestSender)
      * val responseReceiver = Module(new ResponseReceiver)
      * responseReceiver.memoryInput := memory.readHalfWord(requestSender.memoryAddressOutput)
      * ```
    */
    def readHalfWord(addr: UInt): UInt = {
        sanitizeAddr(addr)
        val b0 = mem.read(addr)
        val b1 = mem.read(addr + 1.U)
        return Cat(b1, b0)
    }

    /**
      * Reads a word (4 bytes) from memory at the specified address.
      * @param addr The address to read from (UInt wire)
      * @return The word at the given address (UInt wire, available next cycle)
      * @note For use with multiple modules, wire in the containing module. Example:
      * ```
      * val memory = Module(new Memory)
      * val requestSender = Module(new RequestSender)
      * val responseReceiver = Module(new ResponseReceiver)
      * responseReceiver.memoryInput := memory.readWord(requestSender.memoryAddressOutput)
      * ```
    */
    def readWord(addr: UInt): UInt = {
        sanitizeAddr(addr)
        val b0 = mem.read(addr)
        val b1 = mem.read(addr + 1.U)
        val b2 = mem.read(addr + 2.U)
        val b3 = mem.read(addr + 3.U)
        return Cat(b3, b2, b1, b0)
    }

    /**
     * Writes a byte to memory at the specified address.
     * @param addr The address to write to (UInt wire)
     * @param data The byte to write (UInt wire)
    */
    def writeByte(addr: UInt, data: UInt): Unit = {
        sanitizeAddr(addr)
        mem.write(addr, data)
    }

    /**
      * Writes a half-word (2 bytes) to memory at the specified address.
      * @param addr The address to write to (UInt wire)
      * @param data The half-word to write (UInt wire)
    */
    def writeHalfWord(addr: UInt, data: UInt): Unit = {
        sanitizeAddr(addr)
        mem.write(addr, data(7, 0))
        mem.write(addr + 1.U, data(15, 8))
    }

    /**
      * Writes a word (4 bytes) to memory at the specified address.
      * @param addr The address to write to (UInt wire)
      * @param data The word to write (UInt wire)
    */
    def writeWord(addr: UInt, data: UInt): Unit = {
        sanitizeAddr(addr)
        mem.write(addr, data(7, 0))
        mem.write(addr + 1.U, data(15, 8))
        mem.write(addr + 2.U, data(23, 16))
        mem.write(addr + 3.U, data(31, 24))
    }

    private def sanitizeAddr(addr: UInt): Unit = {
        require(addr.getWidth == Constants.memoryBits, s"Memory address must be ${Constants.memoryBits} bits")
    }
}