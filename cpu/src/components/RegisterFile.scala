package components

import chisel3._
import chisel3.util._

class RegisterFile extends Module {
    val io = IO(new Bundle {
        // All read and write are done via methods
    })

    // In chisel, Mem elaborates to a register bank
    val regFile = Mem(32, UInt(32.W))
    val regOccupied = Mem(32, Bool())

    def readReg(addr: UInt): UInt = {
        sanitizeAddr(addr)
        return regFile.read(addr)
    }

    def writeReg(addr: UInt, data: UInt): Unit = {
        sanitizeAddr(addr)
        regFile.write(addr, data)
    }

    def isRegOccupied(addr: UInt): Bool = {
        sanitizeAddr(addr)
        return regOccupied(addr)
    }

    def setRegOccupied(addr: UInt, occupied: Bool): Unit = {
        sanitizeAddr(addr)
        regOccupied.write(addr, occupied)
    }

    private def sanitizeAddr(addr: UInt): Unit = {
        require(addr.getWidth == 5, "Register address must be 5 bits")
    }
}