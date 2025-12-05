package components

import chisel3._
import chisel3.util._

/**
 * @brief Arithmetic Logic Unit (ALU) Module for RISC-V Processor
 * 
 * This module implements the ALU functionality for a RISC-V processor.
 * 
 * @input io Bundle containing:
 *   - operandA: First operand for the ALU operation
 *   - 
*/
class ArithmeticLogicUnit extends Module {
    val io = IO(new Bundle {
        // The entire opcode is wired in, but only a section is used for determining ALU operation
        val instType = Input(common.InstTypeEnum())
        val funct3 = Input(UInt(3.W))
        val funct7 = Input(UInt(7.W))
        val source1 = Input(UInt(32.W))
        val source2 = Input(UInt(32.W))
        val aluResult = Output(UInt(32.W))
    })

    // ALU operation based on opcode and funct3
    io.aluResult := 0.U // Default value
    switch(io.instType) {
        is(common.InstTypeEnum.R_TYPE) {
            switch(io.funct3) {
                is("b000".U) { // ADD/SUB
                    when(io.funct7 === "b0100000".U) {
                        io.aluResult := io.source1 - io.source2
                    } .otherwise {
                        io.aluResult := io.source1 + io.source2
                    }
                }
                is("b001".U) { // SLL
                    io.aluResult := (io.source1 << io.source2(4,0))
                }
                is("b010".U) { // SLT
                    io.aluResult := Mux(io.source1.asSInt < io.source2.asSInt, 1.U, 0.U)
                }
                is("b011".U) { // SLTU
                    io.aluResult := Mux(io.source1 < io.source2, 1.U, 0.U)
                }
                is("b100".U) { // XOR
                    io.aluResult := io.source1 ^ io.source2
                }
                is("b101".U) { // SRL/SRA
                    when(io.funct7 === "b0100000".U) {
                        io.aluResult := (io.source1.asSInt >> io.source2(4,0)).asUInt
                    } .otherwise {
                        io.aluResult := (io.source1 >> io.source2(4,0))
                    }
                }
                is("b110".U) { // OR
                    io.aluResult := io.source1 | io.source2
                }
                is("b111".U) { // AND
                    io.aluResult := io.source1 & io.source2
                }
            }
        }
        is(common.InstTypeEnum.I_TYPE) {
            switch(io.funct3) {
                is("b000".U) { // ADDI
                    io.aluResult := io.source1 + io.source2
                }
                is("b010".U) { // SLTI
                    io.aluResult := Mux(io.source1.asSInt < io.source2.asSInt, 1.U, 0.U)
                }
                is("b011".U) { // SLTIU
                    io.aluResult := Mux(io.source1 < io.source2, 1.U, 0.U)
                }
                is("b100".U) { // XORI
                    io.aluResult := io.source1 ^ io.source2
                }
                is("b110".U) { // ORI
                    io.aluResult := io.source1 | io.source2
                }
                is("b111".U) { // ANDI
                    io.aluResult := io.source1 & io.source2
                }
                is("b001".U) { // SLLI
                    io.aluResult := (io.source1 << io.source2(4,0))
                }
                is("b101".U) { // SRLI/SRAI
                    when(io.funct7 === "b0100000".U) {
                        io.aluResult := (io.source1.asSInt >> io.source2(4,0)).asUInt
                    } .otherwise {
                        io.aluResult := (io.source1 >> io.source2(4,0))
                    }
                }
            }
        }
    }
}