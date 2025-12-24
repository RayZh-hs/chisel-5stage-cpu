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
            io.exOut.resultOrAddr := io.decodedInst.aluOp1 + io.decodedInst.aluOp2
        }
        is(ALUOpEnum.SUB) {
            io.exOut.resultOrAddr := io.decodedInst.aluOp1 - io.decodedInst.aluOp2
        }
        is(ALUOpEnum.AND) {  
            io.exOut.resultOrAddr := io.decodedInst.aluOp1 & io.decodedInst.aluOp2
        }
        is(ALUOpEnum.OR) {
            io.exOut.resultOrAddr := io.decodedInst.aluOp1 | io.decodedInst.aluOp2
        }
        is(ALUOpEnum.XOR) {
            io.exOut.resultOrAddr := io.decodedInst.aluOp1 ^ io.decodedInst.aluOp2
        }
        is(ALUOpEnum.SLL) {
            io.exOut.resultOrAddr := io.decodedInst.aluOp1 << io.decodedInst.aluOp2(4,0)
        }
        is(ALUOpEnum.SRL) {
            io.exOut.resultOrAddr := io.decodedInst.aluOp1 >> io.decodedInst.aluOp2(4,0)
        }
        is(ALUOpEnum.SRA) {
            io.exOut.resultOrAddr := (io.decodedInst.aluOp1.asSInt >> io.decodedInst.aluOp2(4,0)).asUInt
        }
        is(ALUOpEnum.SLT) {
            io.exOut.resultOrAddr := Mux(io.decodedInst.aluOp1.asSInt < io.decodedInst.aluOp2.asSInt, 1.U, 0.U)
        }
        is(ALUOpEnum.SLTU) {
            io.exOut.resultOrAddr := Mux(io.decodedInst.aluOp1 < io.decodedInst.aluOp2, 1.U, 0.U)
        }
        is(ALUOpEnum.NOP) {
            io.exOut.resultOrAddr := 0.U
        }
    }
    when(io.decodedInst.memOp =/= MemoryOpEnum.NONE) {
        io.exOut.resultOrAddr := io.decodedInst.aluOp1 + io.decodedInst.aluOp2
    }

    io.exOut.memOp := io.decodedInst.memOp
    io.exOut.memWriteData := io.decodedInst.memWriteData
    io.exOut.wdReg := io.decodedInst.regWriteDest
}
