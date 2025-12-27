package components

import scala.util.chaining._
import chisel3._
import chisel3.util._
import common._

class RegisterFile extends Module {
    val io = IO(new Bundle {
        val idComm = Flipped(new IdRegCommBundle)
        val wbReg = Input(UInt(5.W))
        val wbValue = Input(UInt(32.W))
    })

    // In chisel, Mem elaborates to a register bank
    val regFile = Mem(32, UInt(32.W))
    val scoreboard = RegInit(VecInit(Seq.fill(32)(0.U(2.W))))

    val sbBookeep = new Bundle {
        val sbIncr0 = WireDefault(0.U(5.W))
    }

    // Wire the ports
    io.idComm.tap { it =>
        it.scoreboardParam0.data := scoreboard(it.scoreboardParam0.addr)
        it.scoreboardParam1.data := scoreboard(it.scoreboardParam1.addr)
        it.regAccessParam0.data := regFile.read(it.regAccessParam0.addr)
        it.regAccessParam1.data := regFile.read(it.regAccessParam1.addr)

        when(it.markBusy.valid) {
            sbBookeep.sbIncr0 := it.markBusy.bits
        }
    }

    // Writeback port
    when(io.wbReg =/= 0.U) {
        sanitizeAddr(io.wbReg)
        regFile.write(io.wbReg, io.wbValue)
    }

    for (i <- 1 until 32) {
        val incHit = sbBookeep.sbIncr0 === i.U
        val decHit = (io.wbReg === i.U)
        
        when (incHit || decHit) {
            scoreboard(i.U) := scoreboard(i.U) + incHit.asUInt - decHit.asUInt
        }
    }

    private def sanitizeAddr(addr: UInt): Unit = {
        require(addr.getWidth == 5, "Register address must be 5 bits")
    }
}