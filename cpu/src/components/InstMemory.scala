package components

import chisel3._
import chisel3.util._
import common.Constants
import chisel3.util.experimental.loadMemoryFromFile

class InstMemory(hexFile: String) extends Module {
    val io = IO(new Bundle {
        val addr = Input(UInt(32.W))
        val inst = Output(UInt(32.W))
    })
    

    // when(!reset.asBool) {
    //     chisel3.assert(io.addr(1,0) === 0.U, "InstMemory address must be word-aligned")
    //     chisel3.assert(io.addr < (Constants.instMemorySize * 4).U, "InstMemory address out of bounds")
    // }

    val mem = SyncReadMem(Constants.instMemorySize, UInt(32.W))
    if (hexFile.nonEmpty) {
        loadMemoryFromFile(mem, hexFile)
    }
    io.inst := mem.read(io.addr >> 2)
}
