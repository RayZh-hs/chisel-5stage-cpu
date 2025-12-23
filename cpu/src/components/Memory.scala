package components

import common.Constants
import chisel3._
import chisel3.util._

class Memory extends Module {
    val io = IO(new Bundle {
    })

    val mem = SyncReadMem(Constants.memorySize, UInt(8.W))

    private def sanitizeAddr(addr: UInt): Unit = {
        require(addr.getWidth == Constants.memoryBits, s"Memory address must be ${Constants.memoryBits} bits")
    }
}