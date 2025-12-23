package core

import chisel3._
import chisel3.util._
import common.Constants
import components._

class RV32ICore(val hexFile: String) extends Module {
    val memory = Module(new Memory)
    val instMemory = Module(new InstMemory(hexFile))
    val regFile = Module(new RegisterFile)

    val instFetcher = Module(new InstFetcher)
    val instDecoder = Module(new InstDecoder)
    val executor = Module(new Executor)

    // Connect Components
    // - IF/ID Memory Access (Handled via instruction memory)
    instMemory.io.addr := instFetcher.io.instMemoryReadAddr
    instDecoder.io.inst := instMemory.io.inst
    // - Frontend Interleaving
    instFetcher.io.pcOverwrite <> RegNext(instDecoder.io.flushPC)
    // - ID Stage Register File Access
    instDecoder.io.regComm <> regFile.io.idComm
    // - ID/EX Pipeline
    executor.io.decodedInst := RegNext(instDecoder.io.decodedInst)
}