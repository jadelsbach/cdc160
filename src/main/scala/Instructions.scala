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
import Node._

object Instructions {
  def OP_ERR = Bits("b000000")
  def OP_HLT = Bits("b111111")
  
  def OP_SHA = Bits("0000001")
 
  // Logical Product
  def OP_LPN = Bits("b000010")
  def OP_LPD = Bits("b001000")
  def OP_LPI = Bits("b001001")
  def OP_LPF = Bits("b001010")
  def OP_LPB = Bits("b001011")

  // Logical Sum
  def OP_LSN = Bits("b000011")
  def OP_LSD = Bits("b001100")
  def OP_LSI = Bits("b001101")
  def OP_LSF = Bits("b001110")
  def OP_LSB = Bits("b001111")

  // Load
  def OP_LDN = Bits("b000100")
  def OP_LDD = Bits("b010000")
  def OP_LDI = Bits("b010001")
  def OP_LDF = Bits("b010010")
  def OP_LDB = Bits("b010011")

}
