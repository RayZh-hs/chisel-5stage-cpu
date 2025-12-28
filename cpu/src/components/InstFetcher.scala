package components

import chisel3._
import chisel3.util._
import utility._
import common._
import components.InstMemory


class InstFetcher extends CycleAwareModule {
    val io = IO(new Bundle {
        // PC overwrite logic
        val pcOverwrite = Input(Valid(UInt(32.W))) // Note: this will be high with 0 when startup

        // Inst outputs
        val ifOut = Decoupled(new IfOutBundle())

        // Memory interface
        val instAddr = Output(UInt(32.W))
        val instData = Input(UInt(32.W))
    })
    val pc = RegInit(0.U(32.W))
    val nextPc = Wire(UInt(32.W))

    when (io.pcOverwrite.valid) {
        nextPc := io.pcOverwrite.bits
    } .elsewhen (!io.ifOut.ready) {
        nextPc := pc                 
    }.otherwise {
        nextPc := pc + 4.U           
    }
    pc := nextPc
    io.instAddr := nextPc // ready the inst of the pc output next cycle

    // Output logic, aligned to next cycle
    io.ifOut.bits.pc := pc
    io.ifOut.bits.inst := io.instData // 1 cycle latency read
    io.ifOut.valid := !io.pcOverwrite.valid  // not valid if pc is overwritten this cycle(during flush)
}
