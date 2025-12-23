package components

import chisel3._
import chisel3.util._
import utility._
import common._

class Executor extends CycleAwareModule {
    val io = IO(new Bundle {
        val decodedInst = Input(new common.DecodedInstructionBundle)
        val exOut = Output(new common.ExOutBundle())   
    })

    //ALU operation : ADD, SUB, AND, OR, XOR, SLL, SRL, SRA, SLT, SLTU, NOP
    switch(io.decodedInst.aluOp) {
        is(ALUOpEnum.ADD) {
            io.exOut.resultOrAddr := io.decodedInst.rs1Data + io.decodedInst.rs2Data
        }
        is(ALUOpEnum.SUB) {
            io.exOut.resultOrAddr := io.decodedInst.rs1Data - io.decodedInst.rs2Data
        }
        is(ALUOpEnum.AND) {
            io.exOut.resultOrAddr := io.decodedInst.rs1Data & io.decodedInst.rs2Data
        }
        is(ALUOpEnum.OR) {
            io.exOut.resultOrAddr := io.decodedInst.rs1Data | io.decodedInst.rs2Data
        }
        is(ALUOpEnum.XOR) {
            io.exOut.resultOrAddr := io.decodedInst.rs1Data ^ io.decodedInst.rs2Data
        }
        is(ALUOpEnum.SLL) {
            io.exOut.resultOrAddr := io.decodedInst.rs1Data << io.decodedInst.rs2Data(4,0)
        }
        is(ALUOpEnum.SRL) {
            io.exOut.resultOrAddr := io.decodedInst.rs1Data >> io.decodedInst.rs2Data(4,0)
        }
        is(ALUOpEnum.SRA) {
            io.exOut.resultOrAddr := (io.decodedInst.rs1Data.asSInt >> io.decodedInst.rs2Data(4,0)).asUInt
        }
        is(ALUOpEnum.SLT) {
            io.exOut.resultOrAddr := Mux(io.decodedInst.rs1Data.asSInt < io.decodedInst.rs2Data.asSInt, 1.U, 0.U)
        }
        is(ALUOpEnum.SLTU) {
            io.exOut.resultOrAddr := Mux(io.decodedInst.rs1Data < io.decodedInst.rs2Data, 1.U, 0.U)
        }
        is(ALUOpEnum.NOP) {
            io.exOut.resultOrAddr := 0.U
        }
    }
    when(io.decodedInst.memOp =/= MemoryOpEnum.NONE) {
        io.exOut.resultOrAddr := io.decodedInst.rs1Data + io.decodedInst.imm
    }

    io.exOut.memOp := io.decodedInst.memOp
    io.exOut.memWriteData := io.decodedInst.rs2Data
    io.exOut.wdReg := io.decodedInst.rdAddr
}
