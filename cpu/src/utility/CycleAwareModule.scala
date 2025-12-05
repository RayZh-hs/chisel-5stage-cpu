package utility

import chisel3._
import chisel3.util._

class CycleAwareModule extends Module {
    val cycleCount = RegInit(0.U(64.W))

    // Increment cycle count every clock cycle
    cycleCount := cycleCount + 1.U
}
