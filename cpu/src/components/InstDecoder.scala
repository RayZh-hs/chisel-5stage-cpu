package components

import chisel3._
import chisel3.util._
import common.Constants

class InstDecoder extends Module {
    val io = IO(new Bundle {
        val inst = Input(UInt(32.W))
        val decodedInst = Output(new common.DecodedInstBundle)
    })

    io.decodedInst.opcode := io.inst(6, 0)
    io.decodedInst.rd := io.inst(11, 7)
    io.decodedInst.funct3 := io.inst(14, 12)
    io.decodedInst.rs1 := io.inst(19, 15)
    io.decodedInst.rs2 := io.inst(24, 20)
    io.decodedInst.funct7 := io.inst(31, 25)

    // Immediate extraction
    val immI = Cat(Fill(20, io.inst(31)), io.inst(31, 20))
    val immS = Cat(Fill(20, io.inst(31)), io.inst(31, 25), io.inst(11, 7))
    val immB = Cat(Fill(19, io.inst(31)), io.inst(31), io.inst(7), io.inst(30, 25), io.inst(11, 8), 0.U(1.W))
    val immU = Cat(io.inst(31, 12), 0.U(12.W))
    val immJ = Cat(Fill(11, io.inst(31)), io.inst(31), io.inst(19, 12), io.inst(20), io.inst(30, 21), 0.U(1.W))

    // Identify hazards
    // TODO

    // Default values
    io.decodedInst.instType := common.InstTypeEnum.I_TYPE
    io.decodedInst.imm := immI

    // Set inst type and immediate based on opcode
    switch(io.decodedInst.opcode) {
        is("b0110011".U) { // R-Type
            io.decodedInst.instType := common.InstTypeEnum.R_TYPE
            io.decodedInst.imm := 0.U
        }
        is("b0010011".U) { // I-Type (ALU immediate)
            io.decodedInst.instType := common.InstTypeEnum.I_TYPE
            io.decodedInst.imm := immI
        }
        is("b0000011".U) { // I-Type (Load)
            io.decodedInst.instType := common.InstTypeEnum.I_TYPE
            io.decodedInst.imm := immI
        }
        is("b0100011".U) { // S-Type (Store)
            io.decodedInst.instType := common.InstTypeEnum.S_TYPE
            io.decodedInst.imm := immS
        }
        is("b1100011".U) { // B-Type (Branch)
            io.decodedInst.instType := common.InstTypeEnum.B_TYPE
            io.decodedInst.imm := immB
        }
        is("b0110111".U) { // U-Type (LUI)
            io.decodedInst.instType := common.InstTypeEnum.U_TYPE
            io.decodedInst.imm := immU
        }
        is("b0010111".U) { // U-Type (AUIPC)
            io.decodedInst.instType := common.InstTypeEnum.U_TYPE
            io.decodedInst.imm := immU
        }
        is("b1101111".U) { // J-Type (JAL)
            io.decodedInst.instType := common.InstTypeEnum.J_TYPE
            io.decodedInst.imm := immJ
        }
        is("b1100111".U) { // I-Type (JALR)
            io.decodedInst.instType := common.InstTypeEnum.I_TYPE
            io.decodedInst.imm := immI
        }
    }
}
