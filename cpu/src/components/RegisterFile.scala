package components

import scala.util.chaining._
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

    // Wire the ports
    io.idComm.tap { it =>
        it.regOccupiedParam0.data := regOccupied.read(it.regOccupiedParam0.addr)
        it.regOccupiedParam1.data := regOccupied.read(it.regOccupiedParam1.addr)
        it.regAccessParam0.data := regFile.read(it.regAccessParam0.addr)
        it.regAccessParam1.data := regFile.read(it.regAccessParam1.addr)

        when(it.markBusy.valid) {
            regOccupied.write(it.markBusy.bits, true.B)
        }
    }

    private def sanitizeAddr(addr: UInt): Unit = {
        require(addr.getWidth == 5, "Register address must be 5 bits")
    }
}