package components

import chisel3._
import chisel3.util._

class Executor extends Module {
    val io = IO(new Bundle {
        val decodedInst = Input(new common.DecodedInstBundle)
    })
}
