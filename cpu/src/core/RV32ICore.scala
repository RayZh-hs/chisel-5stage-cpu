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
    // - IF Stage
    instMemory.io.addr := instFetcher.io.instMemoryReadAddr
    // - IF/ID Pipeline
    instDecoder.io.inst := RegNext(instFetcher.io.fetchedInst)
    // - ID/EX Pipeline
    executor.io.decodedInst := RegNext(instDecoder.io.decodedInst)
}