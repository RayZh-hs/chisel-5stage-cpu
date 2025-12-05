package common

import chisel3._
import chisel3.util._

object InstTypeEnum extends ChiselEnum {
    val R_TYPE, I_TYPE, S_TYPE, B_TYPE, U_TYPE, J_TYPE = Value
}