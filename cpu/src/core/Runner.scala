package core

import chisel3._
import chisel3.simulator.EphemeralSimulator._

object Runner {
  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      println("Usage: Runner <hex_file> [max_cycles] [verbose]")
      sys.exit(1)
    }
    val hexPath = new java.io.File(args(0)).getAbsolutePath
    val maxCycles = if (args.length > 1) args(1).toInt else 1000
    val verbose = if (args.length > 2) args(2).toBoolean else false

    simulate(new RV32ICore(hexPath, verbose)) { dut =>
      dut.reset.poke(true.B)
      dut.clock.step(5)
      dut.reset.poke(false.B)
      var cycles = 0
      while (!dut.io.exitCode.valid.peek().litToBoolean && cycles < maxCycles) {
        dut.clock.step()
        cycles += 1
      }
      
      if (dut.io.exitCode.valid.peek().litToBoolean) {
        println(s"Program exited with code: ${dut.io.exitCode.bits.peek().litValue}")
      } else {
        println(s"Simulation timed out after $cycles cycles")
        sys.exit(1)
      }
    }
  }
}
