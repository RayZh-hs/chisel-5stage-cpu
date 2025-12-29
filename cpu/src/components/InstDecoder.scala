package components

import chisel3._
import chisel3.util._
import utility._
import common._

class InstDecoder extends CycleAwareModule {
    val io = IO(new Bundle {
        // Input from InstFetcher (Decoupled handshake)
        val ifInput = Flipped(Decoupled(new IfOutBundle()))

        // Interface with Register File / Scoreboard
        val regComm = new IdRegCommBundle()

        // Output to ID/EX Pipeline Register
        val decodedInst = Output(new DecodedInstructionBundle)

        val flush = Input(Bool())
    })

    io.regComm.scoreboardParam0.addr := 0.U
    io.regComm.scoreboardParam1.addr := 0.U
    io.regComm.regAccessParam0.addr := 0.U
    io.regComm.regAccessParam1.addr := 0.U

    val inst = io.ifInput.bits.inst
    val pc   = io.ifInput.bits.pc

    val opcode = inst(6, 0)
    val rd     = inst(11, 7)
    val funct3 = inst(14, 12)
    val rs1    = inst(19, 15)
    val rs2    = inst(24, 20)
    val funct7 = inst(31, 25)

    val immI = Cat(Fill(20, inst(31)), inst(31, 20))
    val immS = Cat(Fill(20, inst(31)), inst(31, 25), inst(11, 7))
    val immB = Cat(Fill(19, inst(31)), inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W))
    val immU = Cat(inst(31, 12), 0.U(12.W))
    val immJ = Cat(Fill(11, inst(31)), inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W))

    val isRType = opcode === "b0110011".U
    val isIType = opcode === "b0010011".U
    val isLoad  = opcode === "b0000011".U
    val isStore = opcode === "b0100011".U
    val isBr    = opcode === "b1100011".U
    val isLui   = opcode === "b0110111".U
    val isAuipc = opcode === "b0010111".U
    val isJal   = opcode === "b1101111".U
    val isJalr  = opcode === "b1100111".U

    val usesRs1 = isRType || isIType || isLoad || isStore || isBr || isJalr
    val usesRs2 = isRType || isStore || isBr

    val rs1Addr = Mux(usesRs1, rs1, 0.U)
    val rs2Addr = Mux(usesRs2, rs2, 0.U)

    val rs1Busy = io.regComm.scoreboardParam0.readFrom(rs1Addr)
    val rs2Busy = io.regComm.scoreboardParam1.readFrom(rs2Addr)
    val hasHazard = rs1Busy.asBool || rs2Busy.asBool

    io.ifInput.ready := !hasHazard

    val decoded = WireInit(DecodedInstructionBundle.ofNoop())

    when(io.ifInput.valid && !hasHazard) {
        decoded.pc := pc
        val rs1Data = io.regComm.regAccessParam0.readFrom(rs1Addr)
        val rs2Data = io.regComm.regAccessParam1.readFrom(rs2Addr)

        decoded.op1 := rs1Data 
        
        // Mux op2: R-Type uses Reg, I-Type/Stores use Imm
        when(isRType || isBr || isStore) {
            decoded.op2 := rs2Data
        }.otherwise {
            // I-Type ALU, Loads, JALR, etc.
            decoded.op2 := Mux(isLui || isAuipc, immU,
                           Mux(isJal, immJ, immI)) // Default to I-immediate
        }

        when(isStore)     { decoded.imm := immS }
        .elsewhen(isBr)   { decoded.imm := immB }
        .elsewhen(isLui || isAuipc) { decoded.imm := immU }
        .elsewhen(isJal)  { decoded.imm := immJ }
        .otherwise        { decoded.imm := immI }

        decoded.regWriteDest := Mux(isStore || isBr, 0.U, rd)
        // ALU Ops
        when(isRType) {
            decoded.aluOp := getALUOp(funct3, funct7, true.B)
        }.elsewhen(isIType) {
            decoded.aluOp := getALUOp(funct3, funct7, false.B) 
        }.elsewhen(isLui) {
            decoded.aluOp := ALUOpEnum.LUI
        }.elsewhen(isAuipc) {
            decoded.aluOp := ALUOpEnum.AUIPC
        }

        when(isBr) {
            decoded.branchOp := getBranchOp(funct3)
        }.elsewhen(isJal) {
            decoded.branchOp := ControlOpEnum.JAL
        }.elsewhen(isJalr) {
            decoded.branchOp := ControlOpEnum.JALR
        }
        when(isLoad) {
            decoded.memOp := MemoryOpEnum.READ
            decoded.memOpWidth := getMemOpWidth(funct3)
            decoded.memReadSigned := !funct3(2)
        }.elsewhen(isStore) {
            decoded.memOp := MemoryOpEnum.WRITE
            decoded.memOpWidth := getMemOpWidth(funct3)
            decoded.op2 := rs2Data // Store data comes from rs2
        }
        when(decoded.regWriteDest =/= 0.U && !io.flush) {
            io.regComm.markBusy.valid := true.B
            io.regComm.markBusy.bits  := decoded.regWriteDest
        }
    }

    io.decodedInst := decoded
    
    io.regComm.markBusy.valid := false.B
    io.regComm.markBusy.bits := 0.U
    when(io.ifInput.valid && !hasHazard && decoded.regWriteDest =/= 0.U && !io.flush) {
        io.regComm.markBusy.valid := true.B
        io.regComm.markBusy.bits  := decoded.regWriteDest
    }

    // --- Helpers ---
    def getALUOp(f3: UInt, f7: UInt, isR: Bool): ALUOpEnum.Type = {
        val op = WireDefault(ALUOpEnum.ADD)
        switch(f3) {
            is("b000".U) { op := Mux(isR && f7(5), ALUOpEnum.SUB, ALUOpEnum.ADD) }
            is("b001".U) { op := ALUOpEnum.SLL }
            is("b010".U) { op := ALUOpEnum.SLT }
            is("b011".U) { op := ALUOpEnum.SLTU }
            is("b100".U) { op := ALUOpEnum.XOR }
            is("b101".U) { op := Mux(f7(5), ALUOpEnum.SRA, ALUOpEnum.SRL) }
            is("b110".U) { op := ALUOpEnum.OR }
            is("b111".U) { op := ALUOpEnum.AND }
        }
        op
    }

    def getBranchOp(f3: UInt): ControlOpEnum.Type = {
        val op = WireDefault(ControlOpEnum.BEQ)
        switch(f3) {
            is("b000".U) { op := ControlOpEnum.BEQ }
            is("b001".U) { op := ControlOpEnum.BNE }
            is("b100".U) { op := ControlOpEnum.BLT }
            is("b101".U) { op := ControlOpEnum.BGE }
            is("b110".U) { op := ControlOpEnum.BLTU }
            is("b111".U) { op := ControlOpEnum.BGEU }
        }
        op
    }

    def getMemOpWidth(f3: UInt): MemoryOpWidthEnum.Type = {
        val w = WireDefault(MemoryOpWidthEnum.WORD)
        switch(f3) {
            is("b000".U) { w := MemoryOpWidthEnum.BYTE }
            is("b001".U) { w := MemoryOpWidthEnum.HALFWORD }
            is("b010".U) { w := MemoryOpWidthEnum.WORD }
            is("b100".U) { w := MemoryOpWidthEnum.BYTE }
            is("b101".U) { w := MemoryOpWidthEnum.HALFWORD }
        }
        w
    }
}