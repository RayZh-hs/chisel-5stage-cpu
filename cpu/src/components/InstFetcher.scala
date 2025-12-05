package components

import chisel3._
import chisel3.util._
import utility._
import common.Constants

class InstFetcher extends CycleAwareModule {
    private object InstFetcherState extends ChiselEnum {
        val FETCH, STALL = Value
    }

    private def isBranchInstruction(inst: UInt): Bool = {
        val opcode = inst(6, 0)
        return (opcode | ~"b1100011".U) === "b1111111".U
    }

    val io = IO(new Bundle {
        // PC end stall endpoint
        val pcEndStall = Input(Bool())
        val pcOverwrite = Input(Bool())
        val pcOverwriteAddr = Input(UInt(Constants.memoryBits.W))

        // InstMemory read interface
        val instMemoryReadData = Input(UInt(32.W))
        val instMemoryReadAddr = Output(UInt(Constants.memoryBits.W))

        // Output fetched instruction
        val fetchedInst = Output(UInt(32.W))
    })
    val pc = RegInit(0.U(Constants.memoryBits.W))
    val hazardStall = WireDefault(false.B)
    private val state = RegInit(InstFetcherState.FETCH)

    when (io.pcOverwrite) {
        pc := io.pcOverwriteAddr
    } .otherwise {
        pc := pc + 4.U
    }
    io.instMemoryReadAddr := pc // Fetch instruction from InstMemory using current PC

    io.fetchedInst := io.instMemoryReadData
}
