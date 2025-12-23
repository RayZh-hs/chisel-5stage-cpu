package components

import chisel3._
import chisel3.util._
import utility._

class Executor extends CycleAwareModule {
    val io = IO(new Bundle {
        val decodedInst = Input(new common.DecodedInstructionBundle)
    })
}
