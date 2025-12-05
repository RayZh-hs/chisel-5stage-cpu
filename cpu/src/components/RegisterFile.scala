package components

import chisel3._
import chisel3.util._

class RegisterFile(val readPortCount: Int = 2, val writePortCount: Int = 1, val readOccupiedCount: Int = 0, val writeOccupiedCount: Int = 0) extends Module {
    val io = IO(new Bundle {
        val readAddr = Input(Vec(readPortCount, UInt(5.W)))
        val readData = Output(Vec(readPortCount, UInt(32.W)))
        val write = Input(Vec(writePortCount, new Bundle {
            val addr = UInt(5.W)
            val data = UInt(32.W)
            val enable = Bool()
        }))
        val readOccupiedAddr = Input(Vec(readOccupiedCount, UInt(5.W)))
        val readOccupiedData = Output(Vec(readOccupiedCount, Bool()))
        val writeOccupied = Input(Vec(writeOccupiedCount, new Bundle {
            val addr = UInt(5.W)
            val occupy = Bool()
            val enable = Bool()
        }))
    })

    val registers = Mem(32, UInt(32.W))
    val isOccupied = Mem(32, Bool())

    for (i <- 0 until readPortCount) {
        io.readData(i) := registers(io.readAddr(i))
    }
    for (i <- 0 until writePortCount) {
        when(io.write(i).enable && (io.write(i).addr =/= 0.U)) {
            registers(io.write(i).addr) := io.write(i).data
        }
    }
    for (i <- 0 until readOccupiedCount) {
        io.readOccupiedData(i) := isOccupied(io.readOccupiedAddr(i))
    }
    for (i <- 0 until writeOccupiedCount) {
        when(io.writeOccupied(i).enable && (io.writeOccupied(i).addr =/= 0.U)) {
            isOccupied(io.writeOccupied(i).addr) := io.writeOccupied(i).occupy
        }
    }
}