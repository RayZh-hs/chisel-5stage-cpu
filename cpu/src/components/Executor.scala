package components

import chisel3._
import chisel3.util._
import utility._
import common._

class Executor extends CycleAwareModule {
    val io = IO(new Bundle {
        val decodedInst = Input(new common.DecodedInstructionBundle)
        val exOut       = Output(new common.ExOutBundle())   
        val jumpTo      = Output(Valid(UInt(32.W)))
    })

    io.exOut.resultOrAddr := 0.U 
    io.exOut.memOp        := io.decodedInst.memOp
    io.exOut.memWriteData := io.decodedInst.op2
    io.exOut.wdReg        := io.decodedInst.regWriteDest
    
    io.jumpTo.valid       := false.B
    io.jumpTo.bits        := 0.U

    val aluResult = Wire(UInt(32.W))
    aluResult := 0.U // Default

    switch(io.decodedInst.aluOp) {
        is(ALUOpEnum.ADD) { aluResult := io.decodedInst.op1 + io.decodedInst.op2 }
        is(ALUOpEnum.SUB) { aluResult := io.decodedInst.op1 - io.decodedInst.op2 }
        is(ALUOpEnum.AND) { aluResult := io.decodedInst.op1 & io.decodedInst.op2 }
        is(ALUOpEnum.OR)  { aluResult := io.decodedInst.op1 | io.decodedInst.op2 }
        is(ALUOpEnum.XOR) { aluResult := io.decodedInst.op1 ^ io.decodedInst.op2 }
        is(ALUOpEnum.SLL) { aluResult := io.decodedInst.op1 << io.decodedInst.op2(4,0) }
        is(ALUOpEnum.SRL) { aluResult := io.decodedInst.op1 >> io.decodedInst.op2(4,0) }
        is(ALUOpEnum.SRA) { aluResult := (io.decodedInst.op1.asSInt >> io.decodedInst.op2(4,0)).asUInt }
        is(ALUOpEnum.SLT) { aluResult := Mux(io.decodedInst.op1.asSInt < io.decodedInst.op2.asSInt, 1.U, 0.U) }
        is(ALUOpEnum.SLTU){ aluResult := Mux(io.decodedInst.op1 < io.decodedInst.op2, 1.U, 0.U) }
    }

    val branchTarget = io.decodedInst.pc + io.decodedInst.imm
    val jalrTarget   = (io.decodedInst.op1 + io.decodedInst.imm) & ~1.U(32.W) 

    val conditionMet = WireDefault(false.B)
    val isJALR       = io.decodedInst.branchOp === ControlOpEnum.JALR
    val isJAL        = io.decodedInst.branchOp === ControlOpEnum.JAL

    switch(io.decodedInst.branchOp){
        is(ControlOpEnum.BEQ) { conditionMet := io.decodedInst.op1 === io.decodedInst.op2 }
        is(ControlOpEnum.BNE) { conditionMet := io.decodedInst.op1 =/= io.decodedInst.op2 }
        is(ControlOpEnum.BLT) { conditionMet := io.decodedInst.op1.asSInt < io.decodedInst.op2.asSInt }
        is(ControlOpEnum.BGE) { conditionMet := io.decodedInst.op1.asSInt >= io.decodedInst.op2.asSInt }
        is(ControlOpEnum.BLTU){ conditionMet := io.decodedInst.op1 < io.decodedInst.op2 }
        is(ControlOpEnum.BGEU){ conditionMet := io.decodedInst.op1 >= io.decodedInst.op2 }
        is(ControlOpEnum.JAL) { conditionMet := true.B }
        is(ControlOpEnum.JALR){ conditionMet := true.B }
    }

    when(conditionMet) {
        io.jumpTo.valid := true.B
        io.jumpTo.bits := Mux(isJALR, jalrTarget, branchTarget)
    }
    when(isJAL || isJALR) {
        io.exOut.resultOrAddr := io.decodedInst.pc + 4.U
    }.elsewhen(io.decodedInst.memOp =/= MemoryOpEnum.NONE) {
        io.exOut.resultOrAddr := io.decodedInst.op1 + io.decodedInst.imm
    }.otherwise {
        io.exOut.resultOrAddr := aluResult
    }
}