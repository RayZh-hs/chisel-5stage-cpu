package core

import chisel3._
import chisel3.util._
import common._
import components._

class RV32ICore(val hexFile: String) extends Module {


    val instMemory  = Module(new InstMemory(hexFile))
    val instFetcher = Module(new InstFetcher)
    val instDecoder = Module(new InstDecoder)
    val executor    = Module(new Executor)
    val memory      = Module(new Memory)
    val regFile     = Module(new RegisterFile)

    val id_ex_reg = RegInit(DecodedInstructionBundle.ofNoop())
    val ex_mem_reg = RegInit(0.U.asTypeOf(new ExOutBundle))

    instMemory.io.addr := instFetcher.io.instAddr
    instFetcher.io.instData := instMemory.io.inst

    instDecoder.io.ifInput <> instFetcher.io.ifOut
    instDecoder.io.regComm <> regFile.io.idComm

    val flushPipeline = executor.io.jumpTo.valid

    when(flushPipeline) {
        id_ex_reg := DecodedInstructionBundle.ofNoop()
    }.otherwise {
        id_ex_reg := instDecoder.io.decodedInst
    }

    executor.io.decodedInst := id_ex_reg
    instFetcher.io.pcOverwrite := executor.io.jumpTo

    ex_mem_reg := executor.io.exOut
    memory.io.exResult := ex_mem_reg

    // no wb stage
    regFile.io.wbReg   := memory.io.wbBundle.wbReg
    regFile.io.wbValue := memory.io.wbBundle.data
}