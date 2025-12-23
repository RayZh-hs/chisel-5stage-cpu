package components

import chisel3._
import chisel3.util._
import utility._
import common.Constants

class InstFetcher extends CycleAwareModule {
    private def isBranchInstruction(inst: UInt): Bool = {
        val opcode = inst(6, 0)
        return (opcode | ~"b1100011".U) === "b1111111".U
    }

    val io = IO(new Bundle {
        // PC end stall endpoint
        val frontendStall = Input(Bool())
        val pcOverwrite = Input(Valid(UInt(32.W)))

        // InstMemory read interface
        val instMemoryReadAddr = Output(UInt(32.W))
    })
    val pc = RegInit(0.U(32.W))

    val nextPc = Wire(UInt(32.W))
    when (io.frontendStall) {
        printf("[%d] (InstFetcher) FE Stalled at PC = 0x%x\n", cycleCount, pc)
        nextPc := pc
    } .elsewhen (io.pcOverwrite.valid) {
        printf("[%d] (InstFetcher) PC Overwrite to 0x%x\n", cycleCount, io.pcOverwrite.bits)
        nextPc := io.pcOverwrite.bits
    } .otherwise {
        printf("[%d] (InstFetcher) PC Increment to 0x%x\n", cycleCount, pc + 4.U)
        nextPc := pc + 4.U
    }

    pc := nextPc
    io.instMemoryReadAddr := nextPc
}
