package core

import chisel3._
import chisel3.util._
import common._
import components._

class RV32ICore(val hexFile: String) extends Module {
    val io = IO(new Bundle {
    })

    // --- Modules ---
    
    val instMemory  = Module(new InstMemory(hexFile))
    val instFetcher = Module(new InstFetcher(instMemory))
    val instDecoder = Module(new InstDecoder)
    val executor    = Module(new Executor)
    val memory      = Module(new Memory)
    val regFile     = Module(new RegisterFile)

    class IfIdBundle extends Bundle {
        val pc = UInt(32.W)
        // val inst = UInt(32.W) // Removed: Inst comes directly from Mem
        val valid = Bool()
    }

    val if_id_reg = RegInit(0.U.asTypeOf(new IfIdBundle))
    val id_ex_reg = RegInit(DecodedInstructionBundle.ofNoop())
    val ex_mem_reg = RegInit(0.U.asTypeOf(new ExOutBundle))

    instFetcher.io.frontendStall := instDecoder.io.invokeStallFrontend
    instFetcher.io.pcOverwrite := instDecoder.io.flushPC
    
    val frontendStalledReg = RegNext(instDecoder.io.invokeStallFrontend, false.B)
    instDecoder.io.frontendIsStalled := frontendStalledReg

    instMemory.io.addr := instFetcher.io.instMemoryReadAddr

    when(instDecoder.io.flushPC.valid) {
        if_id_reg.valid := false.B 
        if_id_reg.pc := 0.U
    } .elsewhen(!instDecoder.io.invokeStallFrontend) {
        if_id_reg.pc := instFetcher.io.instMemoryReadAddr
        if_id_reg.valid := true.B
    } .otherwise {
        // Stall
    }
    instDecoder.io.pc := if_id_reg.pc
    instDecoder.io.inst := Mux(if_id_reg.valid, instMemory.io.inst, 0.U(32.W))

    instDecoder.io.regComm <> regFile.io.idComm
    when(instDecoder.io.flushPC.valid || instDecoder.io.invokeStallFrontend) {
        id_ex_reg := DecodedInstructionBundle.ofNoop()
    } .otherwise {
        id_ex_reg := instDecoder.io.decodedInst
    }

    // 6. EX Stage
    executor.io.decodedInst := id_ex_reg
    ex_mem_reg := executor.io.exOut

    // 7. MEM Stage
    memory.io.exResult := ex_mem_reg
    
    regFile.io.wbReg := memory.io.wbBundle.wbReg
    regFile.io.wbValue := memory.io.wbBundle.data
}