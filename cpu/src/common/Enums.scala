package common

import chisel3._
import chisel3.util._

object InstTypeEnum extends ChiselEnum {
    val R_TYPE, I_TYPE, S_TYPE, B_TYPE, U_TYPE, J_TYPE = Value
}

object ALUOpEnum extends ChiselEnum {
    val ADD, SUB, AND, OR, XOR, SLL, SRL, SRA, SLT, SLTU, NOP = Value
}

object MemoryOpEnum extends ChiselEnum {
    val NONE, READ, WRITE = Value
}

object MemoryOpWidthEnum extends ChiselEnum {
    val BYTE, HALFWORD, WORD = Value
}

object ControlOpEnum extends ChiselEnum {
    val NONE, BEQ, BNE, BLT, BGE, BLTU, BGEU, JAL, JALR = Value
}