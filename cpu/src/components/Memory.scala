package components

import common._
import chisel3._
import chisel3.util._

class Memory extends Module {
    val io = IO(new Bundle {
        val exResult = Input(new common.ExOutBundle())
        val wbData = Output(UInt(32.W))
        val wbReg = Output(UInt(5.W))
    })

    val mem = SyncReadMem(Constants.memorySize, UInt(8.W))
    val addr = io.exResult.resultOrAddr
    sanitizeAddr(addr)
    when(io.exResult.memOp === common.MemoryOpEnum.READ) {
        val byte0 = mem.read(addr)
        val byte1 = mem.read(addr + 1.U)
        val byte2 = mem.read(addr + 2.U)
        val byte3 = mem.read(addr + 3.U)
        io.wbData := Cat(byte3, byte2, byte1, byte0)
    } .elsewhen(io.exResult.memOp === common.MemoryOpEnum.WRITE) {
        val data = io.exResult.memWriteData
        mem.write(addr, data(7,0))
        mem.write(addr + 1.U, data(15,8))
        mem.write(addr + 2.U, data(23,16))
        mem.write(addr + 3.U, data(31,24))
        io.wbData := 0.U
    } .otherwise {
        io.wbData := 0.U
    }

    io.wbReg := io.exResult.wdReg
    private def sanitizeAddr(addr: UInt): Unit = {
        require(addr.getWidth == Constants.memoryBits, s"Memory address must be ${Constants.memoryBits} bits")
    }
}