package components

import common._
import chisel3._
import chisel3.util._

class Memory extends Module {
    val io = IO(new Bundle {
        val exResult = Input(new common.ExOutBundle())
        val wbBundle = Output(new common.MemWbBundle())
    })

    val mem = SyncReadMem(Constants.memorySize, UInt(32.W))
    val wordAddr = io.exResult.resultOrAddr(31, 2)
    val readData = mem.read(wordAddr)// 1 cycle latency
    when(io.exResult.memBundle.memOp === MemoryOpEnum.WRITE) {
        mem.write(wordAddr, io.exResult.memBundle.memWriteData)
    }

    io.wbBundle.data := Mux(io.exResult.memBundle.memOp === MemoryOpEnum.READ, 
                            readData, 
                            io.exResult.resultOrAddr) // also 1 cycle latency
    // delay the wbReg by 1 cycle to match read data latency
    val delayWbReg = RegNext(io.exResult.wdReg)
    io.wbBundle.wbReg := delayWbReg
}