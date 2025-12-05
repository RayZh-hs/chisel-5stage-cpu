package components

import common.Constants
import chisel3._
import chisel3.util._

class Memory extends Module {
    val io = IO(new Bundle {
        // All read and write are done via methods
    })

    val mem = SyncReadMem(Constants.memorySize, UInt(8.W))
    
    def readByte(addr: UInt): UInt = {
        sanitizeAddr(addr)
        return mem.read(addr)
    }

    def writeByte(addr: UInt, data: UInt): Unit = {
        sanitizeAddr(addr)
        mem.write(addr, data)
    }

    private def sanitizeAddr(addr: UInt): Unit = {
        require(addr.getWidth == Constants.memoryBits, s"Memory address must be ${Constants.memoryBits} bits")
    }
}