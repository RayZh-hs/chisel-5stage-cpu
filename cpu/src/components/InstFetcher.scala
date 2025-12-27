package components

import chisel3._
import chisel3.util._
import utility._
import common._
import components.InstMemory


class InstFetcher(mem: InstMemory) extends CycleAwareModule {
    val io = IO(new Bundle {
        // PC overwrite logic
        val pcOverwrite = Input(Valid(UInt(32.W))) // Note: this will be high with 0 when startup

        // Inst outputs
        val ifOut = Output(Decoupled(new IfOutBundle()))
    })
    val pc = Reg(UInt(32.W))
    val nextPc = Wire(UInt(32.W))

    when (io.pcOverwrite.valid) {
        nextPc := io.pcOverwrite.bits
    } .elsewhen (!io.ifOut.ready) {
        nextPc := pc                 
    }.otherwise {
        nextPc := pc + 4.U           
    }
    pc := nextPc
    mem.io.addr := nextPc // ready the inst of the pc output next cycle

    // Output logic, aligned to next cycle
    io.ifOut.bits.pc := pc
    io.ifOut.bits.inst := mem.io.inst // 1 cycle latency read
    io.ifOut.valid := !io.pcOverwrite.valid  // not valid if pc is overwritten this cycle(during flush)
}
