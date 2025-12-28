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
    val fetchingPc = RegInit(0.U(32.W))
    val valid = RegInit(false.B)

    io.instAddr := pc

    when (io.pcOverwrite.valid) {
        pc := io.pcOverwrite.bits
        valid := false.B
    } .elsewhen (io.ifOut.ready) {
        pc := pc + 4.U
        fetchingPc := pc
        valid := true.B
    }

    // Output logic, aligned to next cycle
    io.ifOut.bits.pc := fetchingPc
    io.ifOut.bits.inst := io.instData // 1 cycle latency read
    io.ifOut.valid := valid && !io.pcOverwrite.valid 
}
