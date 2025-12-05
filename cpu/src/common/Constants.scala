package common

import chisel3._
import chisel3.util._

object Constants {
    // Configurables
    val memoryBits = 10
    val instMemoryBits = 10

    // Derived constants
    val memorySize = 1 << memoryBits
    val instMemorySize = 1 << instMemoryBits
}