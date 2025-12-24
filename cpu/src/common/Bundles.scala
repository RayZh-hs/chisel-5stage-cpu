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
    val scoreboardParam0 = new RequestReturnBundle(addrBits = 5, dataBits = 1)
    val scoreboardParam1 = new RequestReturnBundle(addrBits = 5, dataBits = 1)
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
  * @param aluOp1 First ALU operand
  * @param aluOp2 Second ALU operand
  * @param regWriteDest Destination register address for write-back (write to 0 to discard write-back)
  * @param memWriteData Data to be written to memory (for store instructions)
  */
class DecodedInstructionBundle extends Bundle {
    val aluOp = ALUOpEnum()
    val memOp = MemoryOpEnum()
    val memOpWidth = MemoryOpWidthEnum()

    val aluOp1 = UInt(32.W)
    val aluOp2 = UInt(32.W)
    val regWriteDest = UInt(5.W)
    val memWriteData = UInt(32.W)
}

object DecodedInstructionBundle {
    def ofNoop(): DecodedInstructionBundle = {
        val bundle = WireDefault(0.U.asTypeOf(new DecodedInstructionBundle))
        bundle
    }
}