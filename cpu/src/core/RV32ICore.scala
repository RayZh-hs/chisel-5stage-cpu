package core

import chisel3._
import chisel3.util._
import common._
import components._

class RV32ICore(val hexFile: String, val verbose: Boolean = true) extends Module {
    val io = IO(new Bundle {
        val exitCode = Output(Valid(UInt(32.W)))
    })

    val instMemory  = Module(new InstMemory(hexFile))
    val instFetcher = Module(new InstFetcher)
    val instDecoder = Module(new InstDecoder)
    val executor    = Module(new Executor)
    val memory      = Module(new Memory)
    val regFile     = Module(new RegisterFile)

    val id_ex_reg = RegInit(DecodedInstructionBundle.ofNoop())
    val ex_mem_reg = RegInit(0.U.asTypeOf(new ExOutBundle))

    val flushSignal = Wire(Valid(UInt(32.W)))
    val startUp = RegNext(false.B, true.B)
    flushSignal := executor.io.jumpTo
    when (startUp) {
        flushSignal.valid := true.B
        flushSignal.bits  := 0.U
    }


    instMemory.io.addr := instFetcher.io.instAddr
    instFetcher.io.instData := instMemory.io.inst

    instDecoder.io.ifInput <> instFetcher.io.ifOut
    instDecoder.io.regComm <> regFile.io.idComm
    
    when(flushSignal.valid) {
        id_ex_reg := DecodedInstructionBundle.ofNoop()
    }.otherwise {
        id_ex_reg := instDecoder.io.decodedInst
    }

    executor.io.decodedInst := id_ex_reg
    instFetcher.io.pcOverwrite := flushSignal

    if (verbose) {
        printf(p"PC: ${instFetcher.io.ifOut.bits.pc} Inst: ${Hexadecimal(instFetcher.io.ifOut.bits.inst)} Valid: ${instFetcher.io.ifOut.valid} Ready: ${instFetcher.io.ifOut.ready}\n")
    }

    ex_mem_reg := executor.io.exOut
    memory.io.exResult := ex_mem_reg

    // no wb stage
    regFile.io.wbReg   := memory.io.wbBundle.wbReg
    regFile.io.wbValue := memory.io.wbBundle.data

    // Exit code contract: write to 0xFFFFFFF0
    io.exitCode.valid := ex_mem_reg.resultOrAddr === "hFFFFFFF0".U && 
                         ex_mem_reg.memBundle.memOp === MemoryOpEnum.WRITE
    io.exitCode.bits  := ex_mem_reg.memBundle.memWriteData
}