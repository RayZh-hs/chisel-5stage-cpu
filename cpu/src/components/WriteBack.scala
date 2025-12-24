package components

import common._
import chisel3._
import chisel3.util._

class WriteBack extends Module {
    val io = IO(new Bundle {
        val exResult = Input(new common.ExOutBundle())
        val memReadData = Input(UInt(32.W))
        val wbData = Output(UInt(32.W))
        val wbReg = Output(UInt(5.W))
    })

    when(io.exResult.memOp === common.MemoryOpEnum.READ) {
        io.wbData := io.memReadData
    } .otherwise {
        io.wbData := io.exResult.resultOrAddr
    }

    io.wbReg := io.exResult.wdReg
}