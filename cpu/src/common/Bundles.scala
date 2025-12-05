package common

import chisel3._
import chisel3.util._

class DecodedInstBundle extends Bundle {
    val instType = common.InstTypeEnum()
    val opcode = UInt(7.W)
    val rd = UInt(5.W)
    val funct3 = UInt(3.W)
    val rs1 = UInt(5.W)
    val rs2 = UInt(5.W)
    val funct7 = UInt(7.W)
    val imm = UInt(32.W)
}
