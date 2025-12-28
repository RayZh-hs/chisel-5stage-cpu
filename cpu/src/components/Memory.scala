package components

import common._
import chisel3._
import chisel3.util._

class Memory extends Module {
    val io = IO(new Bundle {
        val exResult = Input(new common.ExOutBundle())
        val wbBundle = Output(new common.MemWbBundle())
    })

    val mem = SyncReadMem(Constants.memorySize, Vec(4, UInt(8.W)))
    val addr = io.exResult.resultOrAddr
    val wordAddr = addr(31, 2)
    val byteOffset = addr(1, 0)

    val writeData = io.exResult.memBundle.memWriteData
    val writeVec = Wire(Vec(4, UInt(8.W)))
    val writeMask = Wire(Vec(4, Bool()))

    writeVec := VecInit(Seq.fill(4)(0.U(8.W)))
    writeMask := VecInit(Seq.fill(4)(false.B))

    switch(io.exResult.memBundle.memOpWidth) {
        is(MemoryOpWidthEnum.BYTE) {
            writeVec(byteOffset) := writeData(7, 0)
            writeMask(byteOffset) := true.B
        }
        is(MemoryOpWidthEnum.HALFWORD) {
            // Note: RISC-V allows misaligned access in some implementations, 
            // but here we assume aligned or handle only the lower bit for halfword
            writeVec(Cat(byteOffset(1), 0.U(1.W))) := writeData(7, 0)
            writeVec(Cat(byteOffset(1), 1.U(1.W))) := writeData(15, 8)
            writeMask(Cat(byteOffset(1), 0.U(1.W))) := true.B
            writeMask(Cat(byteOffset(1), 1.U(1.W))) := true.B
        }
        is(MemoryOpWidthEnum.WORD) {
            for (i <- 0 until 4) {
                writeVec(i) := writeData(8 * i + 7, 8 * i)
                writeMask(i) := true.B
            }
        }
    }

    when(io.exResult.memBundle.memOp === MemoryOpEnum.WRITE) {
        mem.write(wordAddr, writeVec, writeMask)
    }

    val readVec = mem.read(wordAddr) // 1 cycle latency
    val delayByteOffset = RegNext(byteOffset)
    val delayMemOpWidth = RegNext(io.exResult.memBundle.memOpWidth)
    val delayMemReadSigned = RegNext(io.exResult.memBundle.memReadSigned)

    val rawReadData = readVec.asUInt
    val shiftedReadData = rawReadData >> (delayByteOffset << 3)
    
    val finalReadData = WireDefault(0.U(32.W))
    switch(delayMemOpWidth) {
        is(MemoryOpWidthEnum.BYTE) {
            val b = shiftedReadData(7, 0)
            finalReadData := Mux(delayMemReadSigned, Cat(Fill(24, b(7)), b), Cat(0.U(24.W), b))
        }
        is(MemoryOpWidthEnum.HALFWORD) {
            val h = shiftedReadData(15, 0)
            finalReadData := Mux(delayMemReadSigned, Cat(Fill(16, h(15)), h), Cat(0.U(16.W), h))
        }
        is(MemoryOpWidthEnum.WORD) {
            finalReadData := rawReadData
        }
    }

    val delayResultOrAddr = RegNext(io.exResult.resultOrAddr)
    io.wbBundle.data := Mux(RegNext(io.exResult.memBundle.memOp === MemoryOpEnum.READ), 
                            finalReadData, 
                            delayResultOrAddr)
    // delay the wbReg by 1 cycle to match read data latency
    val delayWbReg = RegNext(io.exResult.wdReg)
    io.wbBundle.wbReg := delayWbReg
}