package com.uci.cs241.pl241.backend;

import java.util.ArrayList;
import java.util.HashMap;

import com.uci.cs241.pl241.backend.DLXInstruction.InstructionFormat;
import com.uci.cs241.pl241.backend.DLXInstruction.InstructionType;
import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction;
import com.uci.cs241.pl241.ir.PLIRInstruction.OperandType;
import com.uci.cs241.pl241.ir.PLIRInstruction.ResultKind;

public class DLXGenerator
{
	public ArrayList<DLXInstruction> instructions = new ArrayList<DLXInstruction>();
	
	// Metadata about each instruction
	public HashMap<InstructionType, Integer> opcodeMap = new HashMap<InstructionType, Integer>();
	public HashMap<InstructionType, InstructionFormat> formatMap = new HashMap<InstructionType, InstructionFormat>();
	
	public DLXGenerator()
	{
		opcodeMap.put(InstructionType.ADD, 0);
		formatMap.put(InstructionType.ADD, InstructionFormat.F2);
		opcodeMap.put(InstructionType.SUB, 1);
		formatMap.put(InstructionType.SUB, InstructionFormat.F2);
		opcodeMap.put(InstructionType.MUL, 2);
		formatMap.put(InstructionType.MUL, InstructionFormat.F2);
		opcodeMap.put(InstructionType.DIV, 3);
		formatMap.put(InstructionType.DIV, InstructionFormat.F2);
		opcodeMap.put(InstructionType.MOD, 4);
		formatMap.put(InstructionType.MOD, InstructionFormat.F2);
		opcodeMap.put(InstructionType.CMP, 5);
		formatMap.put(InstructionType.CMP, InstructionFormat.F2);
		opcodeMap.put(InstructionType.OR,  8);
		formatMap.put(InstructionType.OR, InstructionFormat.F2);
		opcodeMap.put(InstructionType.AND, 9);
		formatMap.put(InstructionType.AND, InstructionFormat.F2);
		opcodeMap.put(InstructionType.BIC, 10);
		formatMap.put(InstructionType.BIC, InstructionFormat.F2);
		opcodeMap.put(InstructionType.XOR, 11);
		formatMap.put(InstructionType.XOR, InstructionFormat.F2);
		opcodeMap.put(InstructionType.LSH, 12);
		formatMap.put(InstructionType.LSH, InstructionFormat.F2);
		opcodeMap.put(InstructionType.ASH, 13);
		formatMap.put(InstructionType.ASH, InstructionFormat.F2);
		opcodeMap.put(InstructionType.CHK, 14);
		formatMap.put(InstructionType.CHK, InstructionFormat.F2);
		
		opcodeMap.put(InstructionType.ADDI, 16);
		formatMap.put(InstructionType.ADDI, InstructionFormat.F1);
		opcodeMap.put(InstructionType.SUBI, 17);
		formatMap.put(InstructionType.SUBI, InstructionFormat.F1);
		opcodeMap.put(InstructionType.MULI, 18);
		formatMap.put(InstructionType.MULI, InstructionFormat.F1);
		opcodeMap.put(InstructionType.DIVI, 19);
		formatMap.put(InstructionType.DIVI, InstructionFormat.F1);
		opcodeMap.put(InstructionType.MODI, 20);
		formatMap.put(InstructionType.MODI, InstructionFormat.F1);
		opcodeMap.put(InstructionType.CMPI, 21);
		formatMap.put(InstructionType.CMPI, InstructionFormat.F1);
		opcodeMap.put(InstructionType.ORI,  24);
		formatMap.put(InstructionType.ORI, InstructionFormat.F1);
		opcodeMap.put(InstructionType.ANDI, 25);
		formatMap.put(InstructionType.ANDI, InstructionFormat.F1);
		opcodeMap.put(InstructionType.BICI, 26);
		formatMap.put(InstructionType.BICI, InstructionFormat.F1);
		opcodeMap.put(InstructionType.XORI, 27);
		formatMap.put(InstructionType.XORI, InstructionFormat.F1);
		opcodeMap.put(InstructionType.LSHI, 28);
		formatMap.put(InstructionType.LSHI, InstructionFormat.F1);
		opcodeMap.put(InstructionType.ASHI, 29);
		formatMap.put(InstructionType.ASHI, InstructionFormat.F1);
		opcodeMap.put(InstructionType.CHKI, 30);
		formatMap.put(InstructionType.CHKI, InstructionFormat.F1);
		
		opcodeMap.put(InstructionType.LDW, 32);
		formatMap.put(InstructionType.LDW, InstructionFormat.F1);
		opcodeMap.put(InstructionType.LDX, 33);
		formatMap.put(InstructionType.LDX, InstructionFormat.F2);
		opcodeMap.put(InstructionType.POP, 34);
		formatMap.put(InstructionType.POP, InstructionFormat.F1);
		opcodeMap.put(InstructionType.STW, 36);
		formatMap.put(InstructionType.STW, InstructionFormat.F1);
		opcodeMap.put(InstructionType.STX, 37);
		formatMap.put(InstructionType.STX, InstructionFormat.F2);
		opcodeMap.put(InstructionType.PSH, 38);
		formatMap.put(InstructionType.PSH, InstructionFormat.F1);
		
		opcodeMap.put(InstructionType.BEQ, 40);
		formatMap.put(InstructionType.BEQ, InstructionFormat.F1);
		opcodeMap.put(InstructionType.BNE, 41);
		formatMap.put(InstructionType.BNE, InstructionFormat.F1);
		opcodeMap.put(InstructionType.BLT, 42);
		formatMap.put(InstructionType.BLT, InstructionFormat.F1);
		opcodeMap.put(InstructionType.BGE, 43);
		formatMap.put(InstructionType.BGE, InstructionFormat.F1);
		opcodeMap.put(InstructionType.BLE, 44);
		formatMap.put(InstructionType.BLE, InstructionFormat.F1);
		opcodeMap.put(InstructionType.BGT, 45);
		formatMap.put(InstructionType.BGT, InstructionFormat.F1);
		
		opcodeMap.put(InstructionType.BSR, 46);
		formatMap.put(InstructionType.BSR, InstructionFormat.F1);
		opcodeMap.put(InstructionType.JSR, 48);
		formatMap.put(InstructionType.JSR, InstructionFormat.F3);
		opcodeMap.put(InstructionType.RET, 49);
		formatMap.put(InstructionType.RET, InstructionFormat.F2);
		
		opcodeMap.put(InstructionType.RDD, 50);
		formatMap.put(InstructionType.RDD, InstructionFormat.F2);
		opcodeMap.put(InstructionType.WRD, 51);
		formatMap.put(InstructionType.WRD, InstructionFormat.F2);
		opcodeMap.put(InstructionType.WRH, 52);
		formatMap.put(InstructionType.WRH, InstructionFormat.F2);
		opcodeMap.put(InstructionType.WRL, 53);
		formatMap.put(InstructionType.WRL, InstructionFormat.F1);
	}
	
	public long encodeInstruction(DLXInstruction inst)
	{
		long code = 0;
		long opcode = 0;
		long a = 0;
		long b = 0;
		long c = 0;
		
		switch (inst.format)
		{
			case F1:
				opcode = opcodeMap.get(inst.opcode);
				a = inst.ra;
				b = inst.rb;
				c = inst.rc;
				code = (long)((opcode & 0x3F) << 26) | ((a & 0x1F) << 21) | ((b & 0x1F) << 16) | (c & 0xFFFF);
				break;
			case F2:
				opcode = opcodeMap.get(inst.opcode);
				a = inst.ra;
				b = inst.rb;
				c = inst.rc;
				code = (long)((opcode & 0x3F) << 26) | ((a & 0x1F) << 21) | ((b & 0x1F) << 16) | (c & 0x1F);
				break;
			case F3:
				opcode = opcodeMap.get(inst.opcode);
				c = inst.rc;
				code = (long)((opcode & 0x3F) << 26) | (c & 0x3FFFFFF);
				break;
		}
		
		return code;
	}

	public ArrayList<DLXBasicBlock> convertFromColoredSSA(ArrayList<PLIRBasicBlock> blocks)
	{
		ArrayList<DLXBasicBlock> dlxBlocks = new ArrayList<DLXBasicBlock>();
		for (PLIRBasicBlock entry : blocks)
		{
			PLIRBasicBlock join = entry;
			while (join.joinNode != null)
			{
				join = join.joinNode;
			}
			
			ArrayList<DLXBasicBlock> dlxStack = new ArrayList<DLXBasicBlock>();
			ArrayList<PLIRBasicBlock> stack = new ArrayList<PLIRBasicBlock>();
			stack.add(join);
			
			DLXBasicBlock exit = new DLXBasicBlock();
			dlxStack.add(exit);
			DLXBasicBlock dlxBlock = null;
			
			while (!stack.isEmpty())
			{
				dlxBlock = dlxStack.get(dlxStack.size() - 1);
				dlxStack.remove(dlxStack.size() - 1);
				PLIRBasicBlock currBlock = stack.get(stack.size() - 1);
				stack.remove(stack.size() - 1);
				
				for (int i = currBlock.instructions.size() - 1; i >= 0; i--)
				{
					PLIRInstruction ssaInst = currBlock.instructions.get(i);
					
					// Dummy instruction to generate
					DLXInstruction newInst = new DLXInstruction();
					newInst.ra = ssaInst.regNum;
					
					// Determine if the instruction contains an immediate value
					boolean isImmediate = false;
					boolean leftConst = false;
					boolean rightConst = false;
					if (ssaInst.kind == ResultKind.CONST)
					{
						isImmediate = true;
					}
					if (ssaInst.op1type == OperandType.CONST)
					{
						isImmediate = true;
						leftConst = true;
					}
					if (ssaInst.op2type == OperandType.CONST)
					{
						isImmediate = true;
						rightConst = true;
					}
					
					// Create the instruction accordingly
					switch (ssaInst.opcode)
					{
						case ADD:
							if (leftConst && rightConst)
							{
								// ra = 0, i1
								DLXInstruction preInst = new DLXInstruction();
								preInst.opcode = InstructionType.ADDI;
								preInst.format = formatMap.get(InstructionType.ADDI);
								preInst.ra = ssaInst.regNum;
								preInst.rb = 0;
								preInst.rc = ssaInst.i1;
								preInst.encodedForm = encodeInstruction(preInst);
								
								// ra = ra, i2 --> ra = i1 + i2
								newInst.opcode = InstructionType.ADDI;
								newInst.format = formatMap.get(InstructionType.ADDI);
								newInst.ra = ssaInst.regNum;
								newInst.rb = ssaInst.regNum;
								newInst.rc = ssaInst.i2;
								newInst.encodedForm = encodeInstruction(newInst);
								
								dlxBlock.instructions.add(0, newInst);
								dlxBlock.instructions.add(0, preInst);
							}
							else if (leftConst)
							{
								newInst.opcode = InstructionType.ADDI;
								newInst.format = formatMap.get(InstructionType.ADDI);
								newInst.ra = ssaInst.regNum;
								newInst.rb = ssaInst.op2.regNum;
								newInst.rc = ssaInst.i1;
								newInst.encodedForm = encodeInstruction(newInst);
								dlxBlock.instructions.add(0, newInst);
							}
							else if (rightConst)
							{
								newInst.opcode = InstructionType.ADDI;
								newInst.format = formatMap.get(InstructionType.ADDI);
								newInst.ra = ssaInst.regNum;
								newInst.rb = ssaInst.op1.regNum;
								newInst.rc = ssaInst.i2;
								newInst.encodedForm = encodeInstruction(newInst);
								dlxBlock.instructions.add(0, newInst);
							}
							else
							{
								newInst.opcode = InstructionType.ADD;
								newInst.format = formatMap.get(InstructionType.ADD);
								newInst.rb = ssaInst.op1.regNum;
								newInst.rc = ssaInst.op2.regNum;
								newInst.encodedForm = encodeInstruction(newInst);
								dlxBlock.instructions.add(0, newInst);
							}
							
							break;
						case SUB:
							if (leftConst && rightConst)
							{
								// ra = 0, i1
								DLXInstruction preInst = new DLXInstruction();
								preInst.opcode = InstructionType.ADDI;
								preInst.ra = ssaInst.regNum;
								preInst.rb = 0;
								preInst.rc = ssaInst.i1;
								
								// ra = ra, i2 --> ra = i1 + i2
								newInst.opcode = InstructionType.SUBI;
								newInst.ra = ssaInst.regNum;
								newInst.rb = ssaInst.regNum;
								newInst.rc = ssaInst.i2;
								
								dlxBlock.instructions.add(0, newInst);
								dlxBlock.instructions.add(0, preInst);
							}
							else if (leftConst)
							{
								// TODO: ensure order of operations here
								
								newInst.opcode = InstructionType.SUBI;
								newInst.ra = ssaInst.regNum;
								newInst.rb = ssaInst.op2.regNum;
								newInst.rc = ssaInst.i1;
								dlxBlock.instructions.add(0, newInst);
							}
							else if (rightConst)
							{
								newInst.opcode = InstructionType.SUBI;
								newInst.ra = ssaInst.regNum;
								newInst.rb = ssaInst.op1.regNum;
								newInst.rc = ssaInst.i2;
								dlxBlock.instructions.add(0, newInst);
							}
							else
							{
								newInst.opcode = InstructionType.SUBI;
								newInst.rb = ssaInst.op1.regNum;
								newInst.rc = ssaInst.op2.regNum;
								dlxBlock.instructions.add(0, newInst);
							}
							break;
							
						case MUL:
							if (leftConst && rightConst)
							{
								// ra = 0, i1
								DLXInstruction preInst = new DLXInstruction();
								preInst.opcode = InstructionType.MULI;
								preInst.format = formatMap.get(InstructionType.MULI);
								preInst.ra = ssaInst.regNum;
								preInst.rb = 0;
								preInst.rc = ssaInst.i1;
								preInst.encodedForm = encodeInstruction(preInst);
								
								// ra = ra, i2 --> ra = i1 + i2
								newInst.opcode = InstructionType.MULI;
								newInst.format = formatMap.get(InstructionType.MULI);
								newInst.ra = ssaInst.regNum;
								newInst.rb = ssaInst.regNum;
								newInst.rc = ssaInst.i2;
								newInst.encodedForm = encodeInstruction(newInst);
								
								dlxBlock.instructions.add(0, newInst);
								dlxBlock.instructions.add(0, preInst);
							}
							else if (leftConst)
							{
								newInst.opcode = InstructionType.MULI;
								newInst.format = formatMap.get(InstructionType.MULI);
								newInst.ra = ssaInst.regNum;
								newInst.rb = ssaInst.op2.regNum;
								newInst.rc = ssaInst.i1;
								newInst.encodedForm = encodeInstruction(newInst);
								dlxBlock.instructions.add(0, newInst);
							}
							else if (rightConst)
							{
								newInst.opcode = InstructionType.MULI;
								newInst.format = formatMap.get(InstructionType.MULI);
								newInst.ra = ssaInst.regNum;
								newInst.rb = ssaInst.op1.regNum;
								newInst.rc = ssaInst.i2;
								newInst.encodedForm = encodeInstruction(newInst);
								dlxBlock.instructions.add(0, newInst);
							}
							else
							{
								newInst.opcode = InstructionType.MUL;
								newInst.format = formatMap.get(InstructionType.MUL);
								newInst.rb = ssaInst.op1.regNum;
								newInst.rc = ssaInst.op2.regNum;
								newInst.encodedForm = encodeInstruction(newInst);
								dlxBlock.instructions.add(0, newInst);
							}
							break;
							
						case DIV:
							break;
						case ADDA:
							break;
						case WRITE:
							newInst.opcode = InstructionType.WRD;
							newInst.format = formatMap.get(InstructionType.WRD);
							newInst.ra = 0;
							
							if (ssaInst.op1type == OperandType.ADDRESS || ssaInst.op1type == OperandType.INST)
							{
								newInst.rb = ssaInst.op1.regNum;
							}
							else
							{
								System.err.print("need to load ra contents");
								System.exit(-1);
							}
							
							newInst.rc = 0;
							newInst.encodedForm = encodeInstruction(newInst);
							dlxBlock.instructions.add(0, newInst);
							break;
						case READ:
							newInst.opcode = InstructionType.RDD;
							newInst.format = formatMap.get(InstructionType.RDD);
							newInst.ra = ssaInst.regNum;
							newInst.rb = 0;
							newInst.rc = 0;
							newInst.encodedForm = encodeInstruction(newInst);
							dlxBlock.instructions.add(0, newInst);
							break;
						case WLN:
							newInst.opcode = InstructionType.WRL;
							newInst.format = formatMap.get(InstructionType.WRL);
							newInst.ra = 0;
							newInst.rb = 0;
							newInst.rc = 0;
							newInst.encodedForm = encodeInstruction(newInst);
							dlxBlock.instructions.add(0, newInst);
							break;
						case END:
							newInst.opcode = InstructionType.RET;
							newInst.format = formatMap.get(InstructionType.RET);
							newInst.ra = 0;
							newInst.rb = 0;
							newInst.rc = 0;
							newInst.encodedForm = encodeInstruction(newInst);
							dlxBlock.instructions.add(0, newInst);
							break;
						case CMP:
							break;
						case BEQ:
							break;
						case BNE:
							break;
						case BLT:
							break;
						case BGE:
							break;
						case BLE:
							break;
						case BGT:
							break;
						case FUNC:
							break;
						case PROC:
							break;
						case LOADPARAM:
							break;
						case LOAD:
							break;
						case STORE:
							break;
						case PHI:
							break;
					}
				}
				
				// Handle the parents
				if (currBlock.parents != null && currBlock.parents.size() > 0)
				{
					for (PLIRBasicBlock parent : currBlock.parents)
					{
						dlxStack.add(new DLXBasicBlock());
						stack.add(parent);
					}
				}
			}
			
			dlxBlocks.add(dlxBlock);
		}
		
		return dlxBlocks;
	}

}
