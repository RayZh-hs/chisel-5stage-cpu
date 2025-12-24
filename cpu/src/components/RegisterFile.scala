package components

import scala.util.chaining._
import chisel3._
import chisel3.util._
import common._

class RegisterFile extends Module {
    val io = IO(new Bundle {
        val idComm = Flipped(new IdRegCommBundle)
        val rbReg = Input(UInt(5.W))
        val rbValue = Input(UInt(32.W))
    })

    // In chisel, Mem elaborates to a register bank
    val regFile = Mem(32, UInt(32.W))
    val scoreboard = Mem(32, UInt(2.W)) // Can be 0, 1, 2, 3, where 0 is free

    val sbBookeep = new Bundle {
        val sbIncr0 = WireDefault(0.U(5.W))
    }

    // Wire the ports
    io.idComm.tap { it =>
        it.scoreboardParam0.data := scoreboard.read(it.scoreboardParam0.addr)
        it.scoreboardParam1.data := scoreboard.read(it.scoreboardParam1.addr)
        it.regAccessParam0.data := regFile.read(it.regAccessParam0.addr)
        it.regAccessParam1.data := regFile.read(it.regAccessParam1.addr)

        when(it.markBusy.valid) {
            sbBookeep.sbIncr0 := it.markBusy.bits
        }
    }

    // Writeback port
    when(io.rbReg =/= 0.U) {
        sanitizeAddr(io.rbReg)
        regFile.write(io.rbReg, io.rbValue)
    }

    // todo: generate per-cycle write for scoreboard updates
    for (i <- 1 until 32) {
        val incHit = sbBookeep.sbIncr0 === i.U
        val decHit = (io.rbReg === i.U)

        scoreboard.write(i.U, scoreboard(i.U) + incHit.asUInt - decHit.asUInt)
    }

    private def sanitizeAddr(addr: UInt): Unit = {
        require(addr.getWidth == 5, "Register address must be 5 bits")
    }
}