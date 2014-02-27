package com.uci.cs241.pl241.backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.uci.cs241.pl241.backend.DLXInstruction.InstructionFormat;
import com.uci.cs241.pl241.backend.DLXInstruction.InstructionType;
import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction;
import com.uci.cs241.pl241.ir.PLIRInstruction.OperandType;
import com.uci.cs241.pl241.ir.PLIRInstruction.ResultKind;
import com.uci.cs241.pl241.ir.PLStaticSingleAssignment;

public class DLXGenerator
{
	public static final int GLOBAL_ADDRESS = 30;
	public static final int SP = 29;
	public static final int FP = 28;
	public static final int R0 = 0;
	
	public int branchOffset = 0;
	
	public HashMap<Integer, DLXInstruction> offsetMap = new HashMap<Integer, DLXInstruction>(); 
	
	public ArrayList<DLXInstruction> instructions = new ArrayList<DLXInstruction>();
	
	// Global variable address table
	public HashMap<Integer, Integer> addressTable = new HashMap<Integer, Integer>();
	
	// Metadata about each instruction
	public HashMap<InstructionType, Integer> opcodeMap = new HashMap<InstructionType, Integer>();
	public HashMap<InstructionType, InstructionFormat> formatMap = new HashMap<InstructionType, InstructionFormat>();
	public HashMap<Integer, DLXBasicBlock> allBlocks = new HashMap<Integer, DLXBasicBlock>();
	
	int pc = 0;
	
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
	
	public void populateGlobalAddressTable(HashMap<String, PLIRInstruction> globals)
	{
		int offset = 0;
		for (String v : globals.keySet())
		{
			addressTable.put(globals.get(v).id, offset);
			offset += 4; // addresses are 4 bytes
		}
	}
	
	public void fixup(ArrayList<DLXInstruction> instructions)
	{
		// Reset pc for each instruction
		int pc = 0;
		for (DLXInstruction inst : instructions)
		{
			inst.pc = pc++;
			System.out.println(inst);
		}
		
		// Fix branches
		for (DLXInstruction inst : instructions)
		{
			if (DLXInstruction.isBranch(inst))
			{
				if (inst.rc < 0)
				{
					int refId = inst.ssaInst + inst.rc;
					PLIRInstruction refInst = PLStaticSingleAssignment.getInstruction(refId);
					int offset = offsetMap.get(refInst.id).pc;
					inst.rc = offset - inst.pc + inst.offset; // this needs to be + # of phi's added...
				}
				else // positive offset
				{
					int refId = inst.ssaInst + inst.rc;
					PLIRInstruction refInst = PLStaticSingleAssignment.getInstruction(refId);
					int offset = offsetMap.get(refInst.id).pc;
					inst.rc = offset - inst.pc;
				}
				inst.encodedForm = encodeInstruction(inst);
			}
		}
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
		
		if (code == 0)
		{
			System.err.println("invalid encoding");
			System.exit(-1);
		}
		
		return code;
	}
	
//	public void prependInstructionToBlock(DLXBasicBlock block, DLXInstruction inst)
//	{
//		inst.encodedForm = encodeInstruction(inst);
//		block.instructions.add(0, inst);
//		inst.pc = pc++;
//	}

	public void appendInstructionToBlock(DLXBasicBlock block, DLXInstruction inst)
	{
		inst.encodedForm = encodeInstruction(inst);
		block.instructions.add(inst);
		inst.pc = pc++;
	}
	
	public void appendInstructionToEndBlock(DLXBasicBlock block, DLXInstruction inst)
	{
		inst.encodedForm = encodeInstruction(inst);
		block.endInstructions.add(inst);
		inst.pc = pc++;
	}
	
	public void appendInstructionToBlockFromOffset(DLXBasicBlock block, DLXInstruction inst, int offset)
	{
		inst.encodedForm = encodeInstruction(inst);
		block.instructions.add(block.instructions.size() + offset, inst);
		inst.pc = pc++;
	}
	
	public ArrayList<DLXInstruction> convertToStraightLineCode(DLXBasicBlock entry, int stopBlock, HashSet<Integer> visited)
	{ 
		ArrayList<DLXInstruction> instructions = new ArrayList<DLXInstruction>();
		
		if (entry.id == stopBlock) return instructions;
		
		if (visited.contains(entry.id) == false)
		{
			visited.add(entry.id);
			for (DLXInstruction inst : entry.instructions)
			{
				instructions.add(inst);
			}
			instructions.get(instructions.size() - 1).offset = entry.endInstructions.size();
			for (DLXInstruction inst : entry.endInstructions)
			{
				instructions.add(instructions.size() - 1, inst);
			}
			
			boolean end = entry.left == null && entry.right == null;
			
			// Find the join
			DLXBasicBlock join = null;
			if (entry.left != null && entry.right != null)
			{
				// DFS on left to build visited set
				ArrayList<DLXBasicBlock> leftStack = new ArrayList<DLXBasicBlock>();
				HashMap<Integer, DLXBasicBlock> joinSeen = new HashMap<Integer, DLXBasicBlock>();
				leftStack.add(entry.left);
				while (leftStack.isEmpty() == false)
				{
					DLXBasicBlock curr = leftStack.get(leftStack.size() - 1);
					leftStack.remove(leftStack.size() - 1);
					if (joinSeen.containsKey(curr.id) == false)
					{
						joinSeen.put(curr.id, curr);
						if (curr.right != null)
						{
							leftStack.add(curr.right);
						}
						if (curr.left != null)
						{
							leftStack.add(curr.left);
						}
					}
				}
				
				// Find common join point
				ArrayList<DLXBasicBlock> rightStack = new ArrayList<DLXBasicBlock>();
				rightStack.add(entry.right);
				while (rightStack.isEmpty() == false)
				{
					DLXBasicBlock curr = rightStack.get(rightStack.size() - 1);
					rightStack.remove(rightStack.size() - 1);
					if (joinSeen.containsKey(curr.id))
					{
						join = joinSeen.get(curr.id);
					}
					else
					{
						if (curr.right != null)
						{
							rightStack.add(curr.right);
						}
						if (curr.left != null)
						{
							rightStack.add(curr.left);
						}
					}
				}
				
				// if join == null (there is no common point), then left is a 
				// while body and right is the join... handle accordingly
				if (join != null)
				{
					if (entry.left != null && entry.left.id != stopBlock)
					{
						instructions.addAll(convertToStraightLineCode(entry.left, join.id, visited));
					}
					if (entry.right != null && entry.right.id != stopBlock)
					{
						instructions.addAll(convertToStraightLineCode(entry.right, -1, visited));
					}
				}
				else 
				{
					if (entry.left != null && entry.left.id != stopBlock)
					{
						instructions.addAll(convertToStraightLineCode(entry.left, entry.id, visited));
					}
					if (entry.right != null && entry.right.id != stopBlock)
					{
						instructions.addAll(convertToStraightLineCode(entry.right, -1, visited));
					}
				}
			}
			else if (entry.left != null)
			{
				instructions.addAll(convertToStraightLineCode(entry.left, stopBlock, visited));
			}
			else if (entry.right != null)
			{
				instructions.addAll(convertToStraightLineCode(entry.right, stopBlock, visited));
			}
			else
			{
				// end.... do nothing else
			}
		}
		
		return instructions;
	}
	
	public void generateBlockTreeInstructons(DLXBasicBlock edb, PLIRBasicBlock b, int branch, HashSet<Integer> visited)
	{
		if (visited.contains(b.id) == false)
		{
			visited.add(b.id);
			
			for (int i = 0; i < b.instructions.size(); i++)
			{
				PLIRInstruction ssaInst = b.instructions.get(i);
				
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
							
							// ra = ra, i2 --> ra = i1 + i2
							newInst.opcode = InstructionType.ADDI;
							newInst.format = formatMap.get(InstructionType.ADDI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.regNum;
							newInst.rc = ssaInst.i2;
							
							branchOffset++;
							offsetMap.put(ssaInst.id, preInst);
							appendInstructionToBlock(edb, preInst);
							appendInstructionToBlock(edb, newInst);
						}
						else if (leftConst)
						{
							newInst.opcode = InstructionType.ADDI;
							newInst.format = formatMap.get(InstructionType.ADDI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op2.regNum;
							newInst.rc = ssaInst.i1;
							
							offsetMap.put(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);
						}
						else if (rightConst)
						{
							newInst.opcode = InstructionType.ADDI;
							newInst.format = formatMap.get(InstructionType.ADDI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.i2;
							
							offsetMap.put(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);
						}
						else
						{
							newInst.opcode = InstructionType.ADD;
							newInst.format = formatMap.get(InstructionType.ADD);
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.op2.regNum;
							
							offsetMap.put(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);
						}
						
						break;
					case SUB:
						if (leftConst && rightConst)
						{
							// ra = 0, i1
							DLXInstruction preInst = new DLXInstruction();
							preInst.opcode = InstructionType.ADDI;
							preInst.format = formatMap.get(InstructionType.ADDI);
							preInst.ra = ssaInst.regNum;
							preInst.rb = 0;
							preInst.rc = ssaInst.i1;
							
							// ra = ra - i2 --> ra = i1 - i2
							newInst.opcode = InstructionType.SUBI;
							newInst.format = formatMap.get(InstructionType.SUBI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.regNum;
							newInst.rc = ssaInst.i2;
							
							branchOffset++;
							offsetMap.put(ssaInst.id, preInst);
							appendInstructionToBlock(edb, preInst);
							appendInstructionToBlock(edb, newInst);
						}
						else if (leftConst)
						{
							System.err.println("Invalid sub order");
							System.exit(-1);
						}
						else if (rightConst)
						{
							newInst.opcode = InstructionType.SUBI;
							newInst.format = formatMap.get(InstructionType.SUBI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.i2;
							
							offsetMap.put(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);
						}
						else
						{
							newInst.opcode = InstructionType.SUB;
							newInst.format = formatMap.get(InstructionType.SUB);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.op2.regNum;
							
							offsetMap.put(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);
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
							
							// ra = ra, i2 --> ra = i1 + i2
							newInst.opcode = InstructionType.MULI;
							newInst.format = formatMap.get(InstructionType.MULI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.regNum;
							newInst.rc = ssaInst.i2;
							
							branchOffset++;
							offsetMap.put(ssaInst.id, preInst);
							appendInstructionToBlock(edb, preInst);
							appendInstructionToBlock(edb, newInst);
						}
						else if (leftConst)
						{
							newInst.opcode = InstructionType.MULI;
							newInst.format = formatMap.get(InstructionType.MULI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op2.regNum;
							newInst.rc = ssaInst.i1;
							
							offsetMap.put(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);
						}
						else if (rightConst)
						{
							newInst.opcode = InstructionType.MULI;
							newInst.format = formatMap.get(InstructionType.MULI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.i2;
							
							offsetMap.put(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);
						}
						else
						{
							newInst.opcode = InstructionType.MUL;
							newInst.format = formatMap.get(InstructionType.MUL);
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.op2.regNum;
							
							offsetMap.put(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);
						}
						break;
						
					case DIV:
						if (leftConst && rightConst)
						{
							// ra = 0, i1
							DLXInstruction preInst = new DLXInstruction();
							preInst.opcode = InstructionType.ADDI;
							preInst.format = formatMap.get(InstructionType.ADDI);
							preInst.ra = ssaInst.regNum;
							preInst.rb = 0;
							preInst.rc = ssaInst.i1;
							
							// ra = ra - i2 --> ra = i1 - i2
							newInst.opcode = InstructionType.DIVI;
							newInst.format = formatMap.get(InstructionType.DIVI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.regNum;
							newInst.rc = ssaInst.i2;
							
							branchOffset++;
							offsetMap.put(ssaInst.id, preInst);
							appendInstructionToBlock(edb, preInst);
							appendInstructionToBlock(edb, newInst);
						}
						else if (leftConst)
						{
							System.err.println("Invalid div order");
							System.exit(-1);
						}
						else if (rightConst)
						{
							newInst.opcode = InstructionType.DIVI;
							newInst.format = formatMap.get(InstructionType.DIVI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.i2;
							
							offsetMap.put(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);
						}
						else
						{
							newInst.opcode = InstructionType.DIV;
							newInst.format = formatMap.get(InstructionType.DIV);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.op2.regNum;
							
							offsetMap.put(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);
						}
						break;
						
					case ADDA:
						// TODO
						break;
						
					case WRITE:
						newInst.opcode = InstructionType.WRD;
						newInst.format = formatMap.get(InstructionType.WRD);
						newInst.ra = 0;
						newInst.rc = 0;
						
						if (ssaInst.op1type == OperandType.ADDRESS || ssaInst.op1type == OperandType.INST)
						{
							newInst.rb = ssaInst.op1.regNum;
							offsetMap.put(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);
						}
						else // constant, push regNum onto stack, load constant into rb, pop off of stack
						{
							DLXInstruction pushInst = new DLXInstruction();
							pushInst.opcode = InstructionType.PSH;
							pushInst.format = formatMap.get(InstructionType.PSH);
							pushInst.ra = ssaInst.regNum; // save contents of ssaInst.regNum
							pushInst.rb = SP;
							pushInst.rc = 4; // word size
							pushInst.encodedForm = encodeInstruction(pushInst);
							
							DLXInstruction loadInst = new DLXInstruction();
							loadInst.opcode = InstructionType.ADDI;
							loadInst.format = formatMap.get(InstructionType.ADDI);
							loadInst.ra = ssaInst.regNum;
							loadInst.rb = 0;
							loadInst.rc = ssaInst.i1;
							loadInst.encodedForm = encodeInstruction(loadInst);
							
							newInst.rb = ssaInst.regNum; // the actual write instruction
							
							DLXInstruction popInst = new DLXInstruction();
							popInst.opcode = InstructionType.POP;
							popInst.format = formatMap.get(InstructionType.POP);
							popInst.ra = ssaInst.regNum; // save contents of ssaInst.regNum
							popInst.rb = SP;
							popInst.rc = -4; // word size
							popInst.encodedForm = encodeInstruction(popInst);
							
							branchOffset += 3;
							offsetMap.put(ssaInst.id, pushInst);
							appendInstructionToBlock(edb, pushInst);
							appendInstructionToBlock(edb, loadInst);
							appendInstructionToBlock(edb, newInst);
							appendInstructionToBlock(edb, popInst);
						}
						break;
					case READ:
						newInst.opcode = InstructionType.RDD;
						newInst.format = formatMap.get(InstructionType.RDD);
						newInst.ra = ssaInst.regNum;
						newInst.rb = 0;
						newInst.rc = 0;
						
						offsetMap.put(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);
						break;
					case WLN:
						newInst.opcode = InstructionType.WRL;
						newInst.format = formatMap.get(InstructionType.WRL);
						newInst.ra = 0;
						newInst.rb = 0;
						newInst.rc = 0;
						
						offsetMap.put(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);
						break;
					case END:
						newInst.opcode = InstructionType.RET;
						newInst.format = formatMap.get(InstructionType.RET);
						newInst.ra = 0;
						newInst.rb = 0;
						newInst.rc = 0;
						
						offsetMap.put(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);
						break;
					case CMP:
						if (leftConst && rightConst)
						{
							// ra = 0, i1
							DLXInstruction preInst = new DLXInstruction();
							preInst.opcode = InstructionType.ADDI;
							preInst.format = formatMap.get(InstructionType.ADDI);
							preInst.ra = ssaInst.regNum;
							preInst.rb = 0;
							preInst.rc = ssaInst.i1;
							
							// ra = ra - i2 --> ra = i1 - i2
							newInst.opcode = InstructionType.CMPI;
							newInst.format = formatMap.get(InstructionType.CMPI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.regNum;
							newInst.rc = ssaInst.i2;
							
							branchOffset++;
							offsetMap.put(ssaInst.id, preInst);
							appendInstructionToBlock(edb, preInst);
							appendInstructionToBlock(edb, newInst);
						}
						else if (leftConst)
						{
							System.err.println("left const in comparison");
							System.exit(-1);
						}
						else if (rightConst)
						{
							newInst.opcode = InstructionType.CMPI;
							newInst.format = formatMap.get(InstructionType.CMPI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.i2;
							
							offsetMap.put(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);
						}
						else
						{
							newInst.opcode = InstructionType.CMP;
							newInst.format = formatMap.get(InstructionType.CMP);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.op2.regNum;
							
							offsetMap.put(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);
						}
						break;
						
						
					case BEQ:
						newInst.opcode = InstructionType.BEQ;
						newInst.format = formatMap.get(InstructionType.BEQ);
						if (ssaInst.op1type == OperandType.CONST) // unconditional branch...
						{
							newInst.ra = 0; // e.g. BEQ #0 #4
						}
						else
						{
							newInst.ra = ssaInst.op1.regNum; // e.g. BEQ (0) #4
						}
						newInst.rc = ssaInst.i2;
						newInst.ssaInst = ssaInst.id;
						
						offsetMap.put(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);
						break;
						
					case BNE:
						newInst.opcode = InstructionType.BNE;
						newInst.format = formatMap.get(InstructionType.BNE);
						newInst.ra = ssaInst.op1.regNum; // BNE (0) #4
						newInst.rc = ssaInst.i2;
						newInst.ssaInst = ssaInst.id;
						
						offsetMap.put(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);
						break;
						
					case BLT:
						newInst.opcode = InstructionType.BLT;
						newInst.format = formatMap.get(InstructionType.BLT);
						newInst.ra = ssaInst.op1.regNum; // BLT (0) #4
						newInst.rc = ssaInst.i2;
						newInst.ssaInst = ssaInst.id;
						
						offsetMap.put(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);
						break;
						
					case BGE:
						newInst.opcode = InstructionType.BGE;
						newInst.format = formatMap.get(InstructionType.BGE);
						newInst.ra = ssaInst.op1.regNum; // BGE (0) #4
						newInst.rc = ssaInst.i2;
						newInst.ssaInst = ssaInst.id;
						
						offsetMap.put(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);
						break;
						
					case BLE:
						newInst.opcode = InstructionType.BLE;
						newInst.format = formatMap.get(InstructionType.BLE);
						newInst.ra = ssaInst.op1.regNum; // BLE (0) #4
						newInst.rc = ssaInst.i2;
						newInst.ssaInst = ssaInst.id;
						
						offsetMap.put(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);
						break;
						
					case BGT:
						newInst.opcode = InstructionType.BGT;
						newInst.format = formatMap.get(InstructionType.BGT);
						newInst.ra = ssaInst.op1.regNum; // BGT (0) #4
						newInst.rc = ssaInst.i2;
						newInst.ssaInst = ssaInst.id;
						
						offsetMap.put(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);
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
						DLXBasicBlock insertBlock = null;
						if (ssaInst.whilePhi)
						{
							// Find the end of the while body
							ArrayList<DLXBasicBlock> leftStack = new ArrayList<DLXBasicBlock>();
							HashSet<Integer> seen = new HashSet<Integer>();
							seen.add(edb.id);
							leftStack.add(edb.left);
							DLXBasicBlock curr = null;
							while (leftStack.isEmpty() == false)
							{
								DLXBasicBlock tmp = leftStack.get(leftStack.size() - 1);
								leftStack.remove(leftStack.size() - 1);
								if (seen.contains(tmp.id) == false)
								{
									curr = tmp;
									seen.add(tmp.id);
									if (curr.right != null) leftStack.add(curr.right);
									if (curr.left != null) leftStack.add(curr.left);
								}
							}
							insertBlock = curr;
							
							if (!(ssaInst.op1.regNum == ssaInst.op2.regNum && ssaInst.regNum == ssaInst.op1.regNum))
							{
								if (ssaInst.regNum != ssaInst.op1.regNum) // move ssaInst.op1.regNum to ssaInst.regNum
								{
									DLXInstruction leftInst = new DLXInstruction();
									leftInst.opcode = InstructionType.ADD;
									leftInst.format = formatMap.get(InstructionType.ADD);
									leftInst.ra = ssaInst.regNum; // BGT (0) #4
									leftInst.rb = 0;
									leftInst.rc = ssaInst.op1.regNum;
									
									branchOffset++;
									offsetMap.put(ssaInst.id, leftInst);
									appendInstructionToBlock(edb, leftInst);
								}
								if (ssaInst.regNum != ssaInst.op2.regNum) // move ssaInst.op1.regNum to ssaInst.regNum
								{
									DLXInstruction rightInst = new DLXInstruction();
									rightInst.opcode = InstructionType.ADD;
									rightInst.format = formatMap.get(InstructionType.ADD);
									rightInst.ra = ssaInst.regNum; // BGT (0) #4
									rightInst.rb = 0;
									rightInst.rc = ssaInst.op2.regNum;
									
									branchOffset++;
//									offsetMap.put(ssaInst.id, rightInst);
//									appendInstructionToBlockFromOffset(insertBlock, rightInst, -1);
									appendInstructionToEndBlock(insertBlock, rightInst);
								}
							}
						}
						else
						{
							if (!(ssaInst.op1.regNum == ssaInst.op2.regNum && ssaInst.regNum == ssaInst.op1.regNum))
							{
								if (ssaInst.regNum != ssaInst.op1.regNum) // move ssaInst.op1.regNum to ssaInst.regNum
								{
									DLXInstruction leftInst = new DLXInstruction();
									leftInst.opcode = InstructionType.ADD;
									leftInst.format = formatMap.get(InstructionType.ADD);
									leftInst.ra = ssaInst.regNum; // BGT (0) #4
									leftInst.rb = 0;
									leftInst.rc = ssaInst.op1.regNum;
									insertBlock = edb.parents.get(branch);
									
									branchOffset++;
									offsetMap.put(ssaInst.id, leftInst);
//									prependInstructionToBlock(insertBlock, leftInst);
									appendInstructionToBlock(insertBlock, leftInst);
								}
								if (ssaInst.regNum != ssaInst.op2.regNum) // move ssaInst.op1.regNum to ssaInst.regNum
								{
									DLXInstruction rightInst = new DLXInstruction();
									rightInst.opcode = InstructionType.ADD;
									rightInst.format = formatMap.get(InstructionType.ADD);
									rightInst.ra = ssaInst.regNum; // BGT (0) #4
									rightInst.rb = 0;
									rightInst.rc = ssaInst.op2.regNum;
									insertBlock = edb.parents.get(branch);
									
									branchOffset++;
									offsetMap.put(ssaInst.id, rightInst);
//									prependInstructionToBlock(insertBlock, rightInst);
									appendInstructionToBlock(insertBlock, rightInst);
								}
							}
						}
						break;
				}
			}
			
			if (b.leftChild != null)
			{
				generateBlockTreeInstructons(edb.left, b.leftChild, 0, visited);
			}
			if (b.rightChild != null)
			{
				generateBlockTreeInstructons(edb.right, b.rightChild, 1, visited);
			}
		}
	}
	
	public DLXBasicBlock generateBlockTree(DLXBasicBlock parent, PLIRBasicBlock b, HashSet<Integer> visited)
	{
		DLXBasicBlock block = null;
		
		if (allBlocks.containsKey(b.id))
		{
			block = allBlocks.get(b.id);
		}
		else
		{
			block = new DLXBasicBlock(b.id);
			allBlocks.put(b.id, block);
		}
		block.parents.add(parent);
		
		if (visited.contains(b.id) == false)
		{
			visited.add(b.id);
			
			// Recurse to children
			if (b.leftChild != null)
			{
				block.left = generateBlockTree(block, b.leftChild, visited);
			}
			if (b.rightChild != null)
			{
				block.right = generateBlockTree(block, b.rightChild, visited);
			}
		}
		
		
		return block;
	}

}
