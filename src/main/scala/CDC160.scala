//
//    Copyright (c) 2015 Jan Adelsbach <jan@janadelsbach.com>.  
//    All Rights Reserved.
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
//

package cdc160

import Chisel._
import Instructions._

class MemoryBundle extends Bundle {
  val wAddr = UInt(INPUT, width=12)
  val wData = UInt(INPUT, width=12)
  val isWr  = Bool(INPUT)
  val isRdy = Bool(OUTPUT)
  val rData = UInt(OUTPUT, width=12)
}

/*
 * I'm kinda interested how they did that in hardware.
 *
 * Yes I like logical expressions instead of braindead switches.
 * Also this is a nice test for your verilog compiler because
 * the whole thing can be reduced to two lookup tables.
 */
class Decoder extends Module {
  val io = new Bundle {
    val instr  = UInt(INPUT, 6)
    val mode   = UInt(OUTPUT, 3)
    val sinstr = UInt(OUTPUT, 6)
  }

  val reducedNib1 = andR(io.instr(5, 3))
  val reducedNib2 = andR(io.instr(2, 0))

  io.mode := UInt(8)
  io.sinstr := io.instr

 // printf("%b %b (%b %b)\n", reducedNib1, reducedNib2, 
 //        io.instr(5,3), io.instr(2,0))

  // All Instructions until 07 are N also 077, 074 and 076
  when(~orR(io.instr(5,3)) | (reducedNib1 & reducedNib2) | 
       (reducedNib1 & io.instr(2) & ~io.instr(0))) {
    io.mode := UInt(0)
  }
  // The following pattern applies until excluding 060
  .elsewhen(~andR(io.instr(5,4))) {
    when(io.instr(2)) {
      io.sinstr := Cat(io.instr(5,3), Bits("b100"))
    }
    .otherwise {
      io.sinstr := Cat(io.instr(5,3), Bits("b000"))
    }

    // All D is 00 or 04
    when(~orR(io.instr(1,0))) {
      io.mode := UInt(1)
    }
    // All I is 01 or 05
    .elsewhen((!orR(io.instr(2,1)) & io.instr(0)) | 
              (io.instr(2) & io.instr(0) & ~io.instr(1))) {
      io.mode := UInt(2)
    }
    // All F is 02 or 06
    .elsewhen((andR(io.instr(2,1)) & ~io.instr(0)) | 
              (~(io.instr(2) | io.instr(0)) & io.instr(1))) {
      io.mode := UInt(3)
    }
    // All B is 07 or 03
    .elsewhen(reducedNib2 | (~io.instr(2) & andR(io.instr(1,0)))) {
      io.mode := UInt(4)
    }
  }
  // In the 06 space: F from 00 to 03 and B 04 to 07
  .elsewhen(andR(io.instr(5,4)) & ~io.instr(3)) {
    io.sinstr := Cat(io.instr(5,3), UInt(0), io.instr(1,0))

    when(~io.instr(2)) {
      io.mode := UInt(3)
    }
    .otherwise {
      io.mode := UInt(4)
    }
  }
  .otherwise {
    // 071 to 073 are FI
    when(reducedNib1 & ~io.instr(2) & orR(io.instr(1,0))) {
      io.mode := UInt(5)
    }
    // 071
    .elsewhen(reducedNib1 & ~orR(io.instr(2,0))) {
      io.mode := UInt(2)
    }
    .otherwise {
      io.mode := UInt(3)
    }
  }
}

class Memory extends Module {
  val io = new MemoryBundle()
  val M = Mem(UInt(width=12), 4096)

  io.isRdy := Bool(true) // Not yet used

  when(io.isWr) {
    //M(addr:io.wAddr) := io.wData
  }
  .otherwise {
    //io.rData := M(addr:io.wAddr)
  }
}

class ProcessorTop extends Module {
  val io = new Bundle {
    val memory = new MemoryBundle().flip()
  }

  val s_fetch1 :: s_fetch2 :: s_decode :: s_opfetch :: s_indirect :: s_exec :: Nil = Enum(UInt(), 3)

  // Registers
  val Areg = Reg(UInt(width=12), init = UInt(0))
  val Preg = Reg(UInt(width=12), init = UInt(0))
  
  // Custom registers
  val CIreg = Reg(UInt(width=12)) // Current instruction
  val FSMreg = Reg(UInt(width=3), init = s_fetch1)
  val Breg   = Reg(UInt(width=12))

  val decoder = Module(new Decoder())
  decoder.io.instr := CIreg(11, 6)

  switch(FSMreg) {
    is(s_fetch1) {
      io.memory.isWr := Bool(false)
      io.memory.wAddr := Preg
      FSMreg := s_fetch2
    }
    is(s_fetch2) {
      when(io.memory.isRdy) {
        CIreg := io.memory.rData
        FSMreg := s_decode
      }
    }
    is(s_decode) {
      FSMreg := s_opfetch // default
    }
    is(s_opfetch) {
      FSMreg := s_exec
    }
    is(s_indirect) {
      FSMreg := s_opfetch
    }
    is(s_exec) {
      FSMreg := s_fetch1
    }
  }
  
}

class DecoderTests(c: Decoder) extends Tester(c) {
  val allN = Vector(0, 077, 01, 02, 03, 04, 05, 06, 07, 074, 076)
  val allD = Vector(010, 014, 020, 024, 030, 034, 040, 044, 050, 054)
  val allI = Vector(011, 015, 021, 025, 031, 035, 041, 045, 051, 055)
  val allF = Vector(012, 016, 022, 026, 032, 036, 042, 046, 052, 056,
                    060, 061, 062, 063, 075)
  val allB = Vector(013, 017, 023, 027, 033, 037, 043, 047, 053, 057,
                    064, 065, 066, 067)
  val allFi = Vector(071, 072, 073)

  def doall(md: BigInt, lst: Vector[Int]) {
    for(n <- lst) {
      poke(c.io.instr, n)
      step(1)
      expect(c.io.mode, md)
    }
  }

  def mextr(i: Int, md: Int, mx: Int) {
    poke(c.io.instr, i)
    step(1)
    expect(c.io.mode, md)
    expect(c.io.sinstr, mx)
  }

  // Basic decoding
  doall(0, allN)
  doall(1, allD)
  doall(2, allI)
  doall(3, allF)
  doall(4, allB)
  doall(5, allFi)

  // meta instruction extraction
  mextr(07, 0, 07)
  mextr(013, 4, 010)
  mextr(017, 4, 014)
  mextr(064, 4, 060)
  mextr(065, 4, 061)
}

object DecodeTests {
  def main(args: Array[String]): Unit = {
    chiselMainTest(Array[String]("--backend", "c", "--compile", "--test", 
                                 "--genHarness"),
      () => Module(new Decoder())) {c => new DecoderTests(c)}
  }
}
