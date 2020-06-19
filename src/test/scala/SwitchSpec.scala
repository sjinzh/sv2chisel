// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
// Copyright 2020 The sv2chisel Authors. All rights reserved.

package sv2chiselTests

import sv2chiselTests.utils._
import sv2chisel._

import logger._

import scala.util.Random
import org.scalatest._

class SwitchSpec extends Sv2ChiselSpec {
  Logger.setLevel(LogLevel.Warn)
  
  /*
  
  TODO: refacto type management with traits
  
  - main question being : shall we totally decouple hw types from sw types ?
  
  - one trait TypeKind 
    - HwTypeKind 
    - SwTypeKind
    - MixedTypeKind ?
    
  - one trait TypeClass
    - BoolTypeClass
    - NumTypeClass
    - DataTypeClass (hw on which you can do asUInt / asTypeOf)
    - 
  - there can be numeric sw and numeric hw types (or numeric mixed ?)
  
  - how to have a function that can passed a trait as argument to now whether to cast or not
  - for example a conditionally requires a BoolType
  
  
  
  */
  
  "SwitchSpec" should "should be properly emitted" in {
    val result = emitInModule("""
      |wire a, b, c, d, e; 
      |wire [31:0] mem, prev; 
      |reg [31:0] res; 
      |
      |always @(posedge clk) begin
      |  case (1'b1)
      |    a:
      |      res <= prev;
      |    |{b, c}:
      |      res <= mem[31:12] << 12;
      |    |{a, d, e}:
      |      res <= $signed(mem[31:20]);
      |    &{a, b}:
      |      res <= $signed({mem[31], mem[7], mem[30:25], mem[11:8], 1'b0});
      |    b:
      |      res <= $signed({mem[31:25], mem[11:7]});
      |    default:
      |      res <= 1'bx;
      |  endcase
      |end
      """.stripMargin
    )
    debug(result)
    result should contains ("class Test() extends MultiIOModule {")
    
    result should contains ("val mem = Wire(Vec(32, Bool()))",
                            "val prev = Wire(UInt(32.W))",
                            "val res = Reg(UInt(32.W))")
    
    result should contains ("when(true.B === a) {",
                              "res := prev",
                            "} .elsewhen (true.B === Concat(b, c).orR()) {",
                              "res := mem(31,12).asUInt<<12",
                            "} .elsewhen (true.B === Concat(a, d, e).orR()) {",
                              "res := mem(31,20).asTypeOf(SInt(32.W))",
                            "} .elsewhen (true.B === Concat(a, b).andR()) {",
                              "res := (Concat(mem(31), mem(7), mem(30,25).asUInt, mem(11,8).asUInt, b\"0\".U(1.W))).asTypeOf(SInt(32.W))",
                            "} .elsewhen (true.B === b) {",
                              "res := (Concat(mem(31,25).asUInt, mem(11,7).asUInt)).asTypeOf(SInt(32.W))",
                            "} .otherwise {",
                              "res := b\"0\".U(1.W)",
                            "}")
    
  }

}