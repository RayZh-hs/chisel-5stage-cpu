package common

import chisel3._
import chisel3.util._

/**
  * ### Request-Return Bundle
  *
  * Primarily used for memories, where the response does not come within the cycle
  * 
  * @param addrBits The number of bits for the address
  * @param dataBits The number of bits for the data
  */
class RequestReturnBundle(val addrBits: Int, val dataBits: Int) extends Bundle {
    val addr = Output(UInt(addrBits.W))
    val data = Input(UInt(dataBits.W))

    def readFrom(addr: UInt): UInt = {
        this.addr := addr
        this.data
    }
}

// Comm Bundles

// Communication bundle between ID and REG stages
class IdRegCommBundle extends Bundle {
    val regOccupiedParam0 = new RequestReturnBundle(addrBits = 5, dataBits = 1)
    val regOccupiedParam1 = new RequestReturnBundle(addrBits = 5, dataBits = 1)
    val regAccessParam0 = new RequestReturnBundle(addrBits = 5, dataBits = 32)
    val regAccessParam1 = new RequestReturnBundle(addrBits = 5, dataBits = 32)
    val markBusy = Output(Valid(UInt(5.W)))
}

/**
  * ### Decoded Instruction Bundle
  * 
  * Bundle outputted by the InstDecoder after decoding an instruction,
  * Contains all control signals and immediate values needed for execution.
  * 
  * All the register-related fields have been converted to actual data values.
  * 
  * @param aluOp ALU operation to be performed
  * @param memOp Memory operation to be performed
  * @param op1Src Source of operand 1
  * @param op2Src Source of operand 2
  * @param wbSrc Source of data to be written back to the register file
  * @param regWrite Whether to write back to the register file
  * @param rs1Data Data from source register 1
  * @param rs2Data Data from source register 2
  * @param imm Immediate value extracted from the instruction
  * @param rdAddr Destination register address
  * @param pc Program counter value of the instruction
  */
class DecodedInstructionBundle extends Bundle {
    val aluOp = ALUOpEnum()
    val memOp = MemoryOpEnum()
    val wbSrc = WriteBackSrcEnum()

    val regWrite = Bool()

    val rs1Data = UInt(32.W)
    val rs2Data = UInt(32.W)
    val imm = UInt(32.W)
    val rdAddr = UInt(5.W)
    val pc = UInt(32.W)
}

object DecodedInstructionBundle {
    def ofNoop(): DecodedInstructionBundle = {
        val bundle = WireDefault(0.U.asTypeOf(new DecodedInstructionBundle))
        bundle
    }
}