package components

import chisel3._
import chisel3.util._
import common._

class RegisterFile extends Module {
    val io = IO(new Bundle {
        val idComm = Flipped(new IdRegCommBundle)
    })

    // In chisel, Mem elaborates to a register bank
    val regFile = Mem(32, UInt(32.W))
    val regOccupied = Mem(32, Bool())

    private def sanitizeAddr(addr: UInt): Unit = {
        require(addr.getWidth == 5, "Register address must be 5 bits")
    }
}