package com.uci.cs241.pl241.backend;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.uci.cs241.pl241.backend.DLXInstruction.InstructionFormat;
import com.uci.cs241.pl241.backend.DLXInstruction.InstructionType;
import com.uci.cs241.pl241.frontend.Function;
import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction;
import com.uci.cs241.pl241.ir.PLIRInstruction.OperandType;
import com.uci.cs241.pl241.ir.PLIRInstruction.ResultKind;
import com.uci.cs241.pl241.ir.PLStaticSingleAssignment;

public class DLXGenerator
{
	// Special registers
	public static final int RA = 31;
	public static final int GLOBAL_ADDRESS = 30;
	public static final int SP = 29;
	public static final int FP = 28;
	public static final int R0 = 0;
	public static final int FUNC_RET_REG = 27;
	public static final int LOW_REG = 1;
	public static final int HIGH_REG = 26;

	// Address offset tables
	public HashMap<Integer, Integer> globalOffset;
	public HashMap<String, Integer> globalRefMap;
	public HashMap<String, Integer> globalArrayOffset;

	// Fixup offset maps
	public HashMap<Integer, DLXInstruction> offsetMap = new HashMap<Integer, DLXInstruction>();
	public HashMap<Integer, DLXInstruction> leftOffsetMap = new HashMap<Integer, DLXInstruction>();
	public HashMap<Integer, DLXInstruction> rightOffsetMap = new HashMap<Integer, DLXInstruction>();
	
	// Maps to constants and arrays that are caught by the parser
	public HashMap<Integer, PLIRInstruction> constants = new HashMap<Integer, PLIRInstruction>();
	public HashMap<String, PLIRInstruction> arrays = new HashMap<String, PLIRInstruction>();

	// List of machine instructions accumulated so far
	public ArrayList<DLXInstruction> instructions = new ArrayList<DLXInstruction>();

	// Global variable address table
	public HashMap<String, Integer> functionAddressTable = new HashMap<String, Integer>();
	public HashMap<String, Function> functionMap = new HashMap<String, Function>();
	
	// Exit block (safe keeping)
	public DLXBasicBlock exitBlock;
	
	// Flag to determine if instructions are emitted (false for CSE-eliminated instructions)
	public boolean emitInstruction = true;
	public boolean optimizeForLocals = false;

	// Metadata about each instruction
	public HashMap<InstructionType, Integer> opcodeMap = new HashMap<InstructionType, Integer>();
	public HashMap<InstructionType, InstructionFormat> formatMap = new HashMap<InstructionType, InstructionFormat>();
	public HashMap<Integer, DLXBasicBlock> allBlocks = new HashMap<Integer, DLXBasicBlock>();
	
	public boolean[] regInUse = new boolean[32];

	// The master PC - account for the initial jump and SP/FP initialization at the beginning
	public int pc = 3; 

	public DLXGenerator(HashMap<Integer, Integer> globalOffset, HashMap<String, Integer> globalArrayOffset,
			HashMap<String, Integer> globalRefMap, HashMap<Integer, PLIRInstruction> constants, HashMap<String, PLIRInstruction> arrays)
	{
		this.globalOffset = globalOffset;
		this.globalArrayOffset = globalArrayOffset;
		this.globalRefMap = globalRefMap;
		this.constants = constants;
		this.arrays = arrays;
		
		for (int i = 0; i < regInUse.length; i++)
		{
			regInUse[i] = false;
		}

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
		opcodeMap.put(InstructionType.OR, 8);
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
		opcodeMap.put(InstructionType.ORI, 24);
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

	public void fixup(ArrayList<DLXInstruction> instructions)
	{
		// Reset pc for each instruction
		int localPc = this.pc - instructions.size();
		for (DLXInstruction inst : instructions)
		{
			inst.pc = localPc++;
		}

		// Fix branches
		for (DLXInstruction inst : instructions)
		{
			if (DLXInstruction.isBranch(inst))
			{
				if (inst.rc < 0) // jump to the exact compare instruction that was saved during the parsing stage
				{
					int refId = inst.ssaInst.id + inst.rc;
					PLIRInstruction refInst = PLStaticSingleAssignment.getInstruction(refId);
					
					// If we branch forward to a PHI, then...
					int offset = 0;
					DLXBasicBlock jumpToBlock = null;
					if (refInst.opcode == PLIRInstruction.InstructionType.PHI)
					{
						// Find pc of the first non-phi, since that's where we really want to 
						// jump because all of the PHI moves have already been taken care of
						int loc = -1;
						for (int i = refInst.id + 1; i < PLStaticSingleAssignment.instructions.size(); i++)
						{
							if (PLStaticSingleAssignment.getInstruction(i).opcode != PLIRInstruction.InstructionType.PHI)
							{
								loc = i;
								break;
							}
						}
						
						// Use this offset instead
						PLIRInstruction realJump = PLStaticSingleAssignment.getInstruction(loc);
						offset = offsetMap.get(realJump.id).pc;
						inst.rc = offset - inst.pc;
					}
					else
					{
						offset = offsetMap.get(refInst.id).pc;
						inst.rc = offset - inst.pc;
					}
				}
				else // positive offset
				{
					int refId = inst.ssaInst.id + inst.rc;
					PLIRInstruction refInst = PLStaticSingleAssignment.getInstruction(refId);	
					
					// If we branch forward to a PHI, then...
					int offset = 0;
					DLXBasicBlock jumpToBlock = null;
					if (refInst.opcode == PLIRInstruction.InstructionType.PHI)
					{
						if (inst.ssaInst.branchDirection == 1)
						{
							if (inst.block.left.instructions.size() > 0)
							{
								offset = inst.block.left.instructions.get(0).pc;
								jumpToBlock = inst.block.left;
							}
							else if (inst.block.left.endInstructions.size() > 0)
							{
								offset = inst.block.left.endInstructions.get(0).pc;
								jumpToBlock = inst.block.left;
							}
							else
							{
								ArrayList<DLXBasicBlock> queue = new ArrayList<DLXBasicBlock>();
								queue.add(inst.block.left);
								while (queue.isEmpty() == false)
								{
									DLXBasicBlock curr = queue.get(0);
									queue.remove(0);
									while (curr == null)
									{
										curr = queue.get(0);
										queue.remove(0);
									}
									if (curr.instructions.size() > 0)
									{
										offset = curr.instructions.get(0).pc;
										jumpToBlock = curr;
									}
									else if (curr.endInstructions.size() > 0)
									{
										offset = curr.endInstructions.get(0).pc;
										jumpToBlock = curr;
									}
									else
									{
										queue.add(curr.left);
										queue.add(curr.right);
									}
								}
							}
						}
						else
						{
							if (inst.block.right.instructions.size() > 0)
							{
								offset = inst.block.right.instructions.get(0).pc;
								jumpToBlock = inst.block.right;
							}
							else if (inst.block.right.endInstructions.size() > 0)
							{
								offset = inst.block.right.endInstructions.get(0).pc;
								jumpToBlock = inst.block.right;
							}
							else // DFS to find start of next non-empty block
							{
								ArrayList<DLXBasicBlock> queue = new ArrayList<DLXBasicBlock>();
								queue.add(inst.block.right);
								while (queue.isEmpty() == false)
								{
									DLXBasicBlock curr = queue.get(0);
									queue.remove(0);
									while (curr == null)
									{
										curr = queue.get(0);
										queue.remove(0);
									}
									if (curr.instructions.size() > 0)
									{
										offset = curr.instructions.get(0).pc;
										jumpToBlock = curr;
									}
									else if (curr.endInstructions.size() > 0)
									{
										offset = curr.endInstructions.get(0).pc;
										jumpToBlock = curr;
									}
									else
									{
										queue.add(curr.left);
										queue.add(curr.right);
									}
								}
							}
						}
					}
					else
					{
						offset = offsetMap.get(refInst.id).pc;
						jumpToBlock = offsetMap.get(refInst.id).block;
					}
					
					int index = 0;
					for (int i = 0; i < jumpToBlock.instructions.size(); i++)
					{
						if (jumpToBlock.instructions.get(i).pc == offset)
						{
							index = i;
						}
					}
					
					if (jumpToBlock.endInstructions.size() > 0 && index == jumpToBlock.instructions.size() - 1)
					{
						offset -= jumpToBlock.endInstructions.size();
					}
					if (refInst.opcode == PLIRInstruction.InstructionType.RETURN)
					{
						offset -= jumpToBlock.offsetSize;
					}
					
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
				BigInteger cop = new BigInteger(Long.toBinaryString(c), 2);
				code = (long) ((opcode & 0x3F) << 26) | ((a & 0x1F) << 21) | ((b & 0x1F) << 16) | (cop.longValue() & 0xFFFF);
				break;
			case F2:
				opcode = opcodeMap.get(inst.opcode);
				a = inst.ra;
				b = inst.rb;
				c = inst.rc;
				code = (long) ((opcode & 0x3F) << 26) | ((a & 0x1F) << 21) | ((b & 0x1F) << 16) | (c & 0x1F);
				break;
			case F3:
				opcode = opcodeMap.get(inst.opcode);
				c = inst.rc;
				code = (long) ((opcode & 0x3F) << 26) | (c & 0x3FFFFFF);
				break;
		}

		if (code == 0)
		{
			System.err.println("invalid encoding");
			System.exit(-1);
		}

		return code;
	}
	
	public void fixOffset(int ssaInstId, DLXInstruction dlxInst)
	{
		offsetMap.put(ssaInstId, dlxInst);
	}

	public void appendInstructionToBlock(DLXBasicBlock block, DLXInstruction inst)
	{
		inst.block = block;
		inst.encodedForm = encodeInstruction(inst);
		block.instructions.add(inst);
		inst.pc = pc++;
		inst.emit = emitInstruction;
	}
	
	public void appendInstructionToStartBlock(DLXBasicBlock block, DLXInstruction inst)
	{
		inst.block = block;
		inst.emit = emitInstruction;
		inst.encodedForm = encodeInstruction(inst);
		block.startInstructions.add(inst);
		inst.pc = pc++;
	}

	public void appendInstructionToEndBlock(DLXBasicBlock block, DLXInstruction inst)
	{
		inst.block = block;
		inst.emit = emitInstruction;
		inst.encodedForm = encodeInstruction(inst);
		block.endInstructions.add(inst);
		inst.pc = pc++;
	}

	public void appendInstructionToBlockFromOffset(DLXBasicBlock block, DLXInstruction inst, int offset)
	{
		inst.block = block;
		inst.emit = emitInstruction;
		inst.encodedForm = encodeInstruction(inst);
		block.instructions.add(block.instructions.size() + offset, inst);
		inst.pc = pc++;
	}
	
	public DLXBasicBlock findJoin(DLXBasicBlock parent, DLXBasicBlock left, DLXBasicBlock right)
	{
		DLXBasicBlock join = null;
		if (left != null && right != null)
		{
			// DFS on left to build visited set
			ArrayList<DLXBasicBlock> leftStack = new ArrayList<DLXBasicBlock>();
			HashMap<Integer, DLXBasicBlock> joinSeen = new HashMap<Integer, DLXBasicBlock>();
			leftStack.add(left);
			ArrayList<Integer> seen = new ArrayList<Integer>();
			seen.add(parent.id);
			boolean looped = false;
			while (leftStack.isEmpty() == false && !looped)
			{
				DLXBasicBlock curr = leftStack.get(leftStack.size() - 1);
				leftStack.remove(leftStack.size() - 1);
				
				if (seen.contains(curr.id))
				{
					looped = true;
				}
				else if (joinSeen.containsKey(curr.id) == false)
				{
					seen.add(curr.id);
					joinSeen.put(curr.id, curr);
					if (curr.left != null)
					{
						leftStack.add(curr.left);
					}
					if (curr.right != null)
					{
						leftStack.add(curr.right);
					}
				}
				
			}

			// Find furthest common join point
			ArrayList<DLXBasicBlock> rightStack = new ArrayList<DLXBasicBlock>();
			rightStack.add(right);
			seen.clear();
			seen.add(parent.id);
			join = null;
			while (rightStack.isEmpty() == false)
			{
				DLXBasicBlock curr = rightStack.get(rightStack.size() - 1);
				rightStack.remove(rightStack.size() - 1);
				
				if (seen.contains(curr.id))
				{
					continue;
				}
				else
				{
					seen.add(curr.id);
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
			}
		}

		return join;
	}

	DLXInstruction lastRefInst = null;
	public ArrayList<DLXInstruction> convertToStraightLineCode(DLXBasicBlock entry, Function func, ArrayList<Integer> stopBlocks, HashSet<Integer> visited)
	{
		ArrayList<DLXInstruction> instructions = new ArrayList<DLXInstruction>();

		if (stopBlocks.contains(entry.id))
		{
			return instructions;
		}

		if (visited.contains(entry.id) == false)
		{
			visited.add(entry.id);
			for (DLXInstruction inst : entry.startInstructions)
			{
				if (inst.emit) instructions.add(inst);
			}
			for (DLXInstruction inst : entry.instructions)
			{
				if (inst.emit) instructions.add(inst);
			}
			int index = instructions.size() - 1;
			if (entry.instructions.size() > 0 && DLXInstruction.isBranch(entry.instructions.get(entry.instructions.size() - 1)) == false)
			{
				index = entry.instructions.size();
			}
			for (DLXInstruction inst : entry.endInstructions)
			{
				if (instructions.size() > 0)
				{
					if (inst.emit) instructions.add(index++, inst);
				}
				else
				{
					if (inst.emit)
					{
						instructions.add(inst);
						index = 1;
					}
				}
			}
			if (instructions.size() > 0)
			{
				instructions.get(instructions.size() - 1).offset = entry.endInstructions.size();
			}
			
			DLXBasicBlock join = findJoin(entry, entry.left, entry.right);
			if (entry.left != null && entry.right != null)
			{
				// If the join is not null then we have encountered an if statement control flow
				if (join != null) 
				{
					stopBlocks.add(join.id);
					if (entry.left != null)
					{
						instructions.addAll(convertToStraightLineCode(entry.left, func, stopBlocks, visited));
					}
					if (entry.right != null)
					{
						instructions.addAll(convertToStraightLineCode(entry.right, func, stopBlocks, visited));
					}
					
					// After visiting the left and right, continue at the join node
					instructions.addAll(convertToStraightLineCode(join, func, new ArrayList<Integer>(), visited));
				}
				
				// if join == null (there is no common point), then left is a
				// while body and right is the join... handle accordingly
				else
				{
					stopBlocks.add(entry.id);
					if (entry.left != null)
					{
						instructions.addAll(convertToStraightLineCode(entry.left, func, stopBlocks, visited));
					}
					if (entry.right != null)
					{
						instructions.addAll(convertToStraightLineCode(entry.right, func, new ArrayList<Integer>(), visited));
					}
				}
			}
			else // end.... add stack cleanup instructions if this is the end of a procedure or function
			{
				if (func != null && func.hasReturn == false)
				{
					exitBlock = entry;
				}
			}
		}

		return instructions;
	}

	public boolean checkForGlobalLoad(DLXBasicBlock edb, PLIRInstruction usingInst, int refId, int regNum, int offset, boolean fixOffset)
	{
		// Check for functions without procedures
		if (optimizeForLocals) return false;
		
		// Short circuit for arrays
		String name, saveName;
		if (usingInst.block == null || (usingInst.block != null && usingInst.block.label == null))
		{
			name = usingInst.ident.get("main");
			saveName = usingInst.saveName.get("main");
		}
		else if (usingInst.block.scopeName == null)
		{
			name = usingInst.ident.get(usingInst.block.label);
			saveName = usingInst.saveName.get(usingInst.block.label);
		}
		else
		{
			name = usingInst.ident.get(usingInst.block.scopeName);
			saveName = usingInst.saveName.get(usingInst.block.scopeName);
		}
		
		if (globalArrayOffset.containsKey(name))
		{
			return false;
		}
		
		boolean loaded = false;
		if (!loaded && globalOffset.containsKey(refId))
		{
			DLXInstruction loadInst = new DLXInstruction();
			loadInst.opcode = InstructionType.LDW;
			loadInst.format = formatMap.get(InstructionType.LDW);
			loadInst.ra = regNum; // save contents of ssaInst.regNum
			loadInst.rb = GLOBAL_ADDRESS;

			loadInst.rc = -4 * (globalOffset.get(globalRefMap.get(name)) + 1); // word size

			if (fixOffset)
			{
				fixOffset(offset, loadInst);
			}
			appendInstructionToBlock(edb, loadInst);
			return true;
		}
		else if (!loaded && globalRefMap.containsKey(name))
		{
			DLXInstruction loadInst = new DLXInstruction();
			loadInst.opcode = InstructionType.LDW;
			loadInst.format = formatMap.get(InstructionType.LDW);
			loadInst.ra = regNum; // save contents of ssaInst.regNum
			loadInst.rb = GLOBAL_ADDRESS;
			
			int ref = 0;
			if (name == null || name.length() == 0)
			{
				ref = globalRefMap.get(saveName);
			}
			else
			{
				ref = globalRefMap.get(name);
			}
			
			loadInst.rc = -4 * (globalOffset.get(ref) + 1);  

			if (fixOffset)
			{
				fixOffset(offset, loadInst);
			}
			appendInstructionToBlock(edb, loadInst);
			return true;
		}
		return false;
	}

	public boolean checkForGlobalStore(DLXBasicBlock edb, PLIRInstruction usingInst, boolean appendToStart, boolean reallyStart)
	{
		// Check for functions without procedures
		if (optimizeForLocals) return false;
		
		// Short circuit for arrays
		String name, saveName;
		if (usingInst.block == null || (usingInst.block != null && usingInst.block.label == null))
		{
			name = usingInst.ident.get("main");
			saveName = usingInst.saveName.get("main");
		}
		else if (usingInst.block.scopeName == null)
		{
			name = usingInst.ident.get(usingInst.block.label);
			saveName = usingInst.saveName.get(usingInst.block.label);
		}
		else
		{
			name = usingInst.ident.get(usingInst.block.scopeName);
			saveName = usingInst.saveName.get(usingInst.block.scopeName);
		}
		
		// Short circuit for arrays - code to store is automatically generated by parser
		if (globalArrayOffset.containsKey(name))
		{
			return false;
		}
		
		boolean store = false;
		if (!store && globalOffset.containsKey(usingInst.id))
		{
			DLXInstruction storeInst = new DLXInstruction();
			storeInst.opcode = InstructionType.STW;
			storeInst.format = formatMap.get(InstructionType.STW);
			storeInst.ra = usingInst.regNum; // save contents of ssaInst.regNum
			storeInst.rb = GLOBAL_ADDRESS;
			storeInst.rc = -4 * (globalOffset.get(usingInst.id) + 1); // word size

			// Determine where in the block to place the store
			if (appendToStart)
			{
				appendInstructionToBlock(edb, storeInst);
			}
			else
			{
				appendInstructionToEndBlock(edb, storeInst);
			}
			
			// success
			return true;
		}
		else if (!store && (this.globalRefMap.containsKey(name)))
		{
			DLXInstruction storeInst = new DLXInstruction();
			storeInst.opcode = InstructionType.STW;
			storeInst.format = formatMap.get(InstructionType.STW);
			storeInst.ra = usingInst.regNum; // save contents of ssaInst.regNum
			storeInst.rb = GLOBAL_ADDRESS;
			
			int ref = 0;
			if (name == null || name.length() == 0)
			{
				ref = globalRefMap.get(saveName);
			}
			else
			{
				ref = globalRefMap.get(name);
			}
			storeInst.rc = -4 * (globalOffset.get(ref) + 1); // word size

			// Determine where in the block to place the store
			if (reallyStart)
			{
				appendInstructionToStartBlock(edb, storeInst);
			}
			else if (appendToStart)
			{
				appendInstructionToBlock(edb, storeInst);
			}
			else
			{
				appendInstructionToEndBlock(edb, storeInst);
			}
			
			// success
			return true;
		}
		
		return false;
	}
	
	public void setupStack(DLXBasicBlock edb, PLIRBasicBlock b, Function func)
	{
		// Push array space onto the stack
		for (String array : func.arraySizes.keySet())
		{
			int dimension = 1;
			for (Integer d : func.arraySizes.get(array))
			{
				dimension *= d;
			}
			
			// Push the arrays onto the stack
			for (int d = 0; d < dimension; d++)
			{
				DLXInstruction pushInst = new DLXInstruction();
				pushInst.opcode = InstructionType.PSH;
				pushInst.format = formatMap.get(InstructionType.PSH);
				pushInst.ra = 0; // save contents of ssaInst.regNum
				pushInst.rb = SP;
				pushInst.rc = 4; // word size
				pushInst.encodedForm = encodeInstruction(pushInst);
				appendInstructionToBlock(edb, pushInst);
			}
		}
	}
	
	public void tearDownStack(DLXBasicBlock edb, PLIRBasicBlock b, Function func)
	{
		int offset = 0;
		
		// Push array space onto the stack
		for (String array : func.arraySizes.keySet())
		{
			int dimension = 1;
			for (Integer d : func.arraySizes.get(array))
			{
				dimension *= d;
			}
			
			// Pop the arrays off of the stack
			for (int d = 0; d < dimension; d++)
			{
				DLXInstruction popInst = new DLXInstruction();
				popInst.opcode = InstructionType.POP;
				popInst.format = formatMap.get(InstructionType.POP);
				popInst.ra = 0; // save contents of ssaInst.regNum
				popInst.rb = SP;
				popInst.rc = -4; // word size
				popInst.encodedForm = encodeInstruction(popInst);
				appendInstructionToBlock(edb, popInst);
				offset++;
			}
		}
		
		edb.offsetSize = offset;
	}

	public void generateBlockTreeInstructons(DLXBasicBlock edb, PLIRBasicBlock b, Function func, boolean isMain, HashSet<Integer> visited)
	{	
		// Be sure to visit each block at most once
		if (visited.contains(b.id) == false)
		{
			visited.add(b.id);

			for (int i = 0; i < b.instructions.size(); i++)
			{
				PLIRInstruction ssaInst = b.instructions.get(i);
				
				emitInstruction = true;
				if (ssaInst.refInst != null)
				{
//					emitInstruction = false;
//					continue;
				}
				
				// Walk back to the original instruction operands if they were eliminated by CSE
				while (ssaInst.op1 != null && ssaInst.op1.refInst != null)
				{
					ssaInst.op1 = ssaInst.op1.refInst;
				}
				while (ssaInst.op2 != null && ssaInst.op2.refInst != null)
				{
					ssaInst.op2 = ssaInst.op2.refInst;
				}

				// Dummy instruction to generate
				DLXInstruction newInst = new DLXInstruction();
				newInst.ra = ssaInst.regNum;

				// Determine if the instruction contains an immediate value
				boolean leftConst = false;
				boolean rightConst = false;
				boolean isArrayAdd = ssaInst.op1type == OperandType.FP || ssaInst.op1type == OperandType.BASEADDRESS;
				if (ssaInst.op1type == OperandType.CONST)
				{
					leftConst = true;
				}
				if (ssaInst.op2type == OperandType.CONST)
				{
					rightConst = true;
				}

				// Create the instruction accordingly
				switch (ssaInst.opcode)
				{
					case ADD:
						if (leftConst && rightConst)
						{
							if (ssaInst.i1 != 0)
							{
								// ra = 0, i1
								DLXInstruction preInst = new DLXInstruction();
								preInst.opcode = InstructionType.ADDI;
								preInst.format = formatMap.get(InstructionType.ADDI);
								preInst.ra = constants.get(ssaInst.i1).regNum;
								preInst.rb = 0;
								preInst.rc = ssaInst.i1;

								// ra = ra, i2 --> ra = i1 + i2
								newInst.opcode = InstructionType.ADDI;
								newInst.format = formatMap.get(InstructionType.ADDI);
								newInst.ra = ssaInst.regNum;
								newInst.rb = constants.get(ssaInst.i1).regNum;
								newInst.rc = ssaInst.i2;

								fixOffset(ssaInst.id, preInst);
								appendInstructionToBlock(edb, preInst);
								appendInstructionToBlock(edb, newInst);

								checkForGlobalStore(edb, ssaInst, true, false);
							}
							else
							{
								// ra = ra, i2 --> ra = i1 + i2
								newInst.opcode = InstructionType.ADDI;
								newInst.format = formatMap.get(InstructionType.ADDI);
								newInst.ra = ssaInst.regNum;
								newInst.rb = 0;
								newInst.rc = ssaInst.i2;

								fixOffset(ssaInst.id, newInst);
								appendInstructionToBlock(edb, newInst);

								checkForGlobalStore(edb, ssaInst, true, false);
							}
						}
						else if (leftConst)
						{
							newInst.opcode = InstructionType.ADDI;
							newInst.format = formatMap.get(InstructionType.ADDI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op2.regNum;
							newInst.rc = ssaInst.i1;
							
							if (checkForGlobalLoad(edb, ssaInst.op2, ssaInst.op2.id, ssaInst.op2.regNum, ssaInst.id, true))
							{
								appendInstructionToBlock(edb, newInst);
							}
							else
							{
								fixOffset(ssaInst.id, newInst);
								appendInstructionToBlock(edb, newInst);
							}
							
							checkForGlobalStore(edb, ssaInst, true, false);
						}
						else if (rightConst)
						{
							newInst.opcode = InstructionType.ADDI;
							newInst.format = formatMap.get(InstructionType.ADDI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.i2;
							
							if (checkForGlobalLoad(edb, ssaInst.op1, ssaInst.op1.id, ssaInst.op1.regNum, ssaInst.id, true))
							{
								appendInstructionToBlock(edb, newInst);
							}
							else
							{
								fixOffset(ssaInst.id, newInst);
								appendInstructionToBlock(edb, newInst);
							}
							
							checkForGlobalStore(edb, ssaInst, true, false);
						}
						else if (!isArrayAdd)
						{
							newInst.opcode = InstructionType.ADD;
							newInst.format = formatMap.get(InstructionType.ADD);
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.op2.regNum;
							
							boolean fixed = false;
							if (checkForGlobalLoad(edb, ssaInst.op1, ssaInst.op1.id, ssaInst.op1.regNum, ssaInst.id, true))
							{
								fixed = true;
							}
							if (checkForGlobalLoad(edb, ssaInst.op2, ssaInst.op2.id, ssaInst.op2.regNum, ssaInst.id, true))
							{
								fixed = true;
							}
							
							if (!fixed)
							{
								fixOffset(ssaInst.id, newInst);
							}
							appendInstructionToBlock(edb, newInst);

							checkForGlobalStore(edb, ssaInst, true, false);
						}
						else // addition for an array
						{
							newInst.ra = ssaInst.regNum;

							// Determine if this is a global thing or not...
							String ident = ssaInst.op2address.substring(0, ssaInst.op2address.indexOf("_"));
							if (globalArrayOffset.containsKey(ident)) 
							{
								newInst.opcode = InstructionType.ADDI;
								newInst.format = formatMap.get(InstructionType.ADDI);
								newInst.rb = GLOBAL_ADDRESS;
								newInst.rc = -4 * (globalArrayOffset.get(ident));
							}
							else // local array on the stack
							{
								newInst.opcode = InstructionType.ADDI;
								newInst.format = formatMap.get(InstructionType.ADDI);
								newInst.rb = FP;
								newInst.rc = func.getStackOffset(ident);
							}

							// Store the offset
							fixOffset(ssaInst.id, newInst);
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

							fixOffset(ssaInst.id, preInst);
							appendInstructionToBlock(edb, preInst);
							appendInstructionToBlock(edb, newInst);

							checkForGlobalStore(edb, ssaInst, true, false);
						}
						else if (leftConst) // X - Y = (Y * -1) + X
						{
							// ra = 0, i1
							DLXInstruction preInst = new DLXInstruction();
							preInst.opcode = InstructionType.MULI;
							preInst.format = formatMap.get(InstructionType.MULI);
							preInst.ra = ssaInst.regNum;
							preInst.rb = ssaInst.op2.regNum;
							preInst.rc = -1;

							// ra = ra - i2 --> ra = i1 - i2
							newInst.opcode = InstructionType.ADDI;
							newInst.format = formatMap.get(InstructionType.ADDI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.regNum;
							newInst.rc = ssaInst.i1;

							fixOffset(ssaInst.id, preInst);
							appendInstructionToBlock(edb, preInst);
							appendInstructionToBlock(edb, newInst);
						}
						else if (rightConst)
						{
							newInst.opcode = InstructionType.SUBI;
							newInst.format = formatMap.get(InstructionType.SUBI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.i2;

							fixOffset(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);

							checkForGlobalStore(edb, ssaInst, true, false);
						}
						else
						{
							newInst.opcode = InstructionType.SUB;
							newInst.format = formatMap.get(InstructionType.SUB);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.op2.regNum;

							fixOffset(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);

							checkForGlobalStore(edb, ssaInst, true, false);
						}
						break;

					case MUL:
						if (leftConst && rightConst)
						{
							// ra = 0, i1
							DLXInstruction preInst1 = new DLXInstruction();
							preInst1.opcode = InstructionType.ADDI;
							preInst1.format = formatMap.get(InstructionType.ADDI);
							preInst1.ra = ssaInst.regNum;
							preInst1.rb = 0;
							preInst1.rc = ssaInst.i1;

							// ra = ra, i2 --> ra = i1 * i2
							newInst.opcode = InstructionType.MULI;
							newInst.format = formatMap.get(InstructionType.MULI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.regNum;
							newInst.rc = ssaInst.i2;

							fixOffset(ssaInst.id, preInst1);
							appendInstructionToBlock(edb, preInst1);
							appendInstructionToBlock(edb, newInst);

							checkForGlobalStore(edb, ssaInst, true, false);
						}
						else if (leftConst)
						{
							newInst.opcode = InstructionType.MULI;
							newInst.format = formatMap.get(InstructionType.MULI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op2.regNum;
							newInst.rc = ssaInst.i1;

							fixOffset(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);

							checkForGlobalStore(edb, ssaInst, true, false);
						}
						else if (rightConst)
						{
							newInst.opcode = InstructionType.MULI;
							newInst.format = formatMap.get(InstructionType.MULI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.i2;

							fixOffset(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);

							checkForGlobalStore(edb, ssaInst, true, false);
						}
						else
						{
							newInst.opcode = InstructionType.MUL;
							newInst.format = formatMap.get(InstructionType.MUL);
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.op2.regNum;

							fixOffset(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);

							checkForGlobalStore(edb, ssaInst, true, false);
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

							fixOffset(ssaInst.id, preInst);
							appendInstructionToBlock(edb, preInst);
							appendInstructionToBlock(edb, newInst);

							checkForGlobalStore(edb, ssaInst, true, false);
						}
						else if (leftConst)
						{
							DLXInstruction preInst = new DLXInstruction();
							preInst.opcode = InstructionType.ADDI;
							preInst.format = formatMap.get(InstructionType.ADDI);
							preInst.ra = constants.get(ssaInst.i1).regNum;
							preInst.rb = 0;
							preInst.rc = ssaInst.i1;
							
							newInst.opcode = InstructionType.DIV;
							newInst.format = formatMap.get(InstructionType.DIV);
							newInst.ra = ssaInst.regNum;
							newInst.rb = preInst.ra;
							newInst.rc = ssaInst.op2.regNum;

							fixOffset(ssaInst.id, preInst);
							appendInstructionToBlock(edb, preInst);
							appendInstructionToBlock(edb, newInst);
						}
						else if (rightConst)
						{
							newInst.opcode = InstructionType.DIVI;
							newInst.format = formatMap.get(InstructionType.DIVI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.i2;

							fixOffset(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);

							checkForGlobalStore(edb, ssaInst, true, false);
						}
						else
						{
							newInst.opcode = InstructionType.DIV;
							newInst.format = formatMap.get(InstructionType.DIV);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.op2.regNum;

							fixOffset(ssaInst.id, newInst);
							appendInstructionToBlock(edb, newInst);

							checkForGlobalStore(edb, ssaInst, true, false);
						}
						break;

					case ADDA:
						newInst.opcode = InstructionType.ADD;
						newInst.format = formatMap.get(InstructionType.ADD);
						newInst.ra = ssaInst.regNum;						
						newInst.rb = ssaInst.op1.regNum;
						newInst.rc = ssaInst.op2.regNum;
						
						// Store the offset
						fixOffset(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);

						break;

					case WRITE:
						newInst.opcode = InstructionType.WRD;
						newInst.format = formatMap.get(InstructionType.WRD);
						newInst.ra = 0;
						newInst.rc = 0;

						if (!(ssaInst.op1type == OperandType.CONST))
						{
							newInst.rb = ssaInst.op1.regNum;
							if (checkForGlobalLoad(edb, ssaInst.op1, ssaInst.op1.id, ssaInst.op1.regNum, ssaInst.id, true))
							{
								appendInstructionToBlock(edb, newInst);
							}
							else
							{
								fixOffset(ssaInst.id, newInst);
								appendInstructionToBlock(edb, newInst);
							}
						}
						else
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

							fixOffset(ssaInst.id, pushInst);
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
						fixOffset(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);

						// Check to see if the value needs to be stored at the
						// global address
						checkForGlobalStore(edb, ssaInst, true, false);

						break;
					case WLN:
						newInst.opcode = InstructionType.WRL;
						newInst.format = formatMap.get(InstructionType.WRL);
						newInst.ra = 0;
						newInst.rb = 0;
						newInst.rc = 0;

						fixOffset(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);
						break;
					case END:
						newInst.opcode = InstructionType.RET;
						newInst.format = formatMap.get(InstructionType.RET);
						newInst.ra = 0;
						newInst.rb = 0;
						newInst.rc = 0;

						fixOffset(ssaInst.id, newInst);
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

							fixOffset(ssaInst.id, preInst);
							appendInstructionToBlock(edb, preInst);
							appendInstructionToBlock(edb, newInst);
						}
						else if (leftConst)
						{
							DLXInstruction preInst = new DLXInstruction();
							preInst.opcode = InstructionType.ADDI;
							preInst.format = formatMap.get(InstructionType.ADDI);
							preInst.ra = ssaInst.regNum;
							preInst.rb = 0;
							preInst.rc = ssaInst.i1;

							// ra = ra - i2 --> ra = i1 - i2
							newInst.opcode = InstructionType.CMP;
							newInst.format = formatMap.get(InstructionType.CMPI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.regNum;
							newInst.rc = ssaInst.op2.regNum;

							// Check for loading global
							fixOffset(ssaInst.id, preInst);
							appendInstructionToBlock(edb, preInst);
							checkForGlobalLoad(edb, ssaInst.op2, ssaInst.op2.id, ssaInst.op2.regNum, ssaInst.id, false);
							appendInstructionToBlock(edb, newInst);
						}
						else if (rightConst)
						{
							newInst.opcode = InstructionType.CMPI;
							newInst.format = formatMap.get(InstructionType.CMPI);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.i2;

							// Check for loading global
							if (!(checkForGlobalLoad(edb, ssaInst.op1, ssaInst.op1.id, ssaInst.op1.regNum, ssaInst.id, true)))
							{
								fixOffset(ssaInst.id, newInst);
							}
							appendInstructionToBlock(edb, newInst);
						}
						else
						{
							newInst.opcode = InstructionType.CMP;
							newInst.format = formatMap.get(InstructionType.CMP);
							newInst.ra = ssaInst.regNum;
							newInst.rb = ssaInst.op1.regNum;
							newInst.rc = ssaInst.op2.regNum;

							// Check for loading global of left
							boolean fixOffset = true;
							if (checkForGlobalLoad(edb, ssaInst.op1, ssaInst.op1.id, ssaInst.op1.regNum, ssaInst.id, fixOffset))
							{
								fixOffset = false;
							}

							if (checkForGlobalLoad(edb, ssaInst.op2, ssaInst.op2.id, ssaInst.op2.regNum, ssaInst.id, fixOffset))
							{
								fixOffset = false;
							}

							if (fixOffset)
							{
								fixOffset(ssaInst.id, newInst);
							}
							appendInstructionToBlock(edb, newInst);
						}
						break;

					case BEQ:
						newInst.opcode = InstructionType.BEQ;
						newInst.format = formatMap.get(InstructionType.BEQ);
						if (ssaInst.op1type == OperandType.CONST) // unconditional branch
						{
							newInst.ra = 0; // e.g. BEQ #0 #4
						}
						else
						{
							newInst.ra = ssaInst.op1.regNum; // e.g. BEQ (0) #4
						}
						newInst.rc = ssaInst.i2;
						newInst.ssaInst = ssaInst;

						fixOffset(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);
						break;

					case BNE:
						newInst.opcode = InstructionType.BNE;
						newInst.format = formatMap.get(InstructionType.BNE);
						newInst.ra = ssaInst.op1.regNum; // BNE (0) #4
						newInst.rc = ssaInst.i2;
						newInst.ssaInst = ssaInst;

						fixOffset(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);
						break;

					case BLT:
						newInst.opcode = InstructionType.BLT;
						newInst.format = formatMap.get(InstructionType.BLT);
						newInst.ra = ssaInst.op1.regNum; // BLT (0) #4
						newInst.rc = ssaInst.i2;
						newInst.ssaInst = ssaInst;

						fixOffset(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);
						break;

					case BGE:
						newInst.opcode = InstructionType.BGE;
						newInst.format = formatMap.get(InstructionType.BGE);
						newInst.ra = ssaInst.op1.regNum; // BGE (0) #4
						newInst.rc = ssaInst.i2;
						newInst.ssaInst = ssaInst;

						fixOffset(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);
						break;

					case BLE:
						newInst.opcode = InstructionType.BLE;
						newInst.format = formatMap.get(InstructionType.BLE);
						newInst.ra = ssaInst.op1.regNum; // BLE (0) #4
						newInst.rc = ssaInst.i2;
						newInst.ssaInst = ssaInst;

						fixOffset(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);
						break;

					case BGT:
						newInst.opcode = InstructionType.BGT;
						newInst.format = formatMap.get(InstructionType.BGT);
						newInst.ra = ssaInst.op1.regNum;
						newInst.rc = ssaInst.i2;
						newInst.ssaInst = ssaInst;

						fixOffset(ssaInst.id, newInst);
						appendInstructionToBlock(edb, newInst);
						break;

					case RETURN:
						// Clean up the stack
						tearDownStack(edb, b, func);
						
						boolean savedReturn = false;
						if (ssaInst.op1 != null)
						{
							DLXInstruction retParamInst = new DLXInstruction();
							retParamInst.opcode = InstructionType.PSH;
							retParamInst.format = formatMap.get(InstructionType.PSH);
							retParamInst.ra = ssaInst.op1.regNum;
							retParamInst.rb = SP;

							fixOffset(ssaInst.id, retParamInst);
							retParamInst.rc = 4;
							savedReturn = true;
							appendInstructionToBlock(edb, retParamInst);
						}
						else if (ssaInst.op1 != null && ssaInst.op1type == OperandType.CONST)
						{
							DLXInstruction preInst = new DLXInstruction();
							preInst.opcode = InstructionType.ADDI;
							preInst.format = formatMap.get(InstructionType.ADDI);
							preInst.ra = ssaInst.op1.regNum;
							preInst.rb = 0;
							preInst.rc = ssaInst.i1;
							
							DLXInstruction retParamInst = new DLXInstruction();
							retParamInst.opcode = InstructionType.PSH;
							retParamInst.format = formatMap.get(InstructionType.PSH);
							retParamInst.ra = ssaInst.op1.regNum;
							retParamInst.rb = SP;

							fixOffset(ssaInst.id, retParamInst);
							retParamInst.rc = 4;
							savedReturn = true;
							appendInstructionToBlock(edb, retParamInst);
						}

						DLXInstruction retInst = new DLXInstruction();
						retInst.opcode = InstructionType.RET;
						retInst.format = formatMap.get(InstructionType.RET);
						retInst.ra = 0;
						retInst.rb = 0;
						retInst.rc = RA; // jump to RA

						if (!savedReturn)
						{
							fixOffset(ssaInst.id, retInst);
						}
						appendInstructionToBlock(edb, retInst);

						break;

					case FUNC:
					{
						// Save priors if constants
						for (String var : functionMap.get(ssaInst.funcName).constantsToSave.keySet())
						{
							int constant = functionMap.get(ssaInst.funcName).constantsToSave.get(var);
							DLXInstruction preInst = new DLXInstruction();
							preInst.opcode = InstructionType.ADDI;
							preInst.format = formatMap.get(InstructionType.ADDI);
							preInst.ra = ssaInst.regNum;
							preInst.rb = 0;
							preInst.rc = constant;
							appendInstructionToBlock(edb, preInst);
							
							DLXInstruction storeInst = new DLXInstruction();
							storeInst.opcode = InstructionType.STW;
							storeInst.format = formatMap.get(InstructionType.STW);
							storeInst.ra = ssaInst.regNum; // save contents of ssaInst.regNum
							storeInst.rb = GLOBAL_ADDRESS;
							storeInst.rc = -4 * (globalOffset.get(globalRefMap.get(var)) + 1); // word size
							appendInstructionToBlock(edb, storeInst);
						}
						
						// Push all inuse registers onto the stack
						for (int ri = LOW_REG; ri <= HIGH_REG; ri++)
						{
							DLXInstruction regPush = new DLXInstruction();
							regPush.opcode = InstructionType.PSH;
							regPush.format = formatMap.get(InstructionType.PSH);
							regPush.ra = ri; // save contents of ssaInst.regNum
							regPush.rb = SP; //
							regPush.rc = 4; // word size
							regPush.encodedForm = encodeInstruction(regPush);
							fixOffset(ssaInst.id, regPush);
							appendInstructionToBlock(edb, regPush);
						}

						// Push RA onto the stack
						DLXInstruction push1 = new DLXInstruction();
						push1.opcode = InstructionType.PSH;
						push1.format = formatMap.get(InstructionType.PSH);
						push1.ra = RA; // save contents of ssaInst.regNum
						push1.rb = SP; //
						push1.rc = 4; // word size
						push1.encodedForm = encodeInstruction(push1);
						fixOffset(ssaInst.id, push1);
						appendInstructionToBlock(edb, push1);

						// Push all operands onto the stack (callee recovers the proper order)
						for (int opIndex = ssaInst.callOperands.size() - 1; opIndex >= 0; opIndex--)
						{	
							PLIRInstruction operand = ssaInst.callOperands.get(opIndex);
							
							int srcReg = operand.regNum;
							if (operand.kind == ResultKind.CONST)
							{
								DLXInstruction preInst = new DLXInstruction();
								preInst.opcode = InstructionType.ADDI;
								preInst.format = formatMap.get(InstructionType.ADDI);
								
								if (constants.containsKey(operand.tempVal))
								{
									srcReg = constants.get(operand.tempVal).regNum;	
								}
								preInst.ra = srcReg;
								preInst.rb = 0;
								preInst.rc = operand.tempVal;
								preInst.encodedForm = encodeInstruction(preInst);
								appendInstructionToBlock(edb, preInst);
							}
							
							DLXInstruction push3 = new DLXInstruction();
							push3.opcode = InstructionType.PSH;
							push3.format = formatMap.get(InstructionType.PSH);

							push3.ra = srcReg; // save contents of ssaInst.regNum
							push3.rb = SP; 
							push3.rc = 4; // word size
							push3.encodedForm = encodeInstruction(push3);
							appendInstructionToBlock(edb, push3);
						}

						// Push current FP onto the stack (to become to old FP)
						DLXInstruction push2 = new DLXInstruction();
						push2.opcode = InstructionType.PSH;
						push2.format = formatMap.get(InstructionType.PSH);
						push2.ra = FP; // save contents of ssaInst.regNum
						push2.rb = SP; 
						push2.rc = 4; // word size
						push2.encodedForm = encodeInstruction(push2);
						appendInstructionToBlock(edb, push2);

						// Set FP = SP
						DLXInstruction fpChange = new DLXInstruction();
						fpChange.opcode = InstructionType.ADD;
						fpChange.format = formatMap.get(InstructionType.ADD);
						fpChange.ra = FP;
						fpChange.rb = 0;
						fpChange.rc = SP;
						fpChange.encodedForm = encodeInstruction(fpChange);
						appendInstructionToBlock(edb, fpChange);

						// Add the JSR routine
						DLXInstruction jsrInst = new DLXInstruction();
						jsrInst.opcode = InstructionType.JSR;
						jsrInst.format = formatMap.get(InstructionType.JSR);
						jsrInst.ra = newInst.rb = 0;
						if (functionAddressTable.containsKey(ssaInst.funcName))
						{
							jsrInst.rc = functionAddressTable.get(ssaInst.funcName) * 4;
						}
						else
						{
							jsrInst.jumpNeedsFix = true;
							jsrInst.refFunc = ssaInst.funcName;
						}
						jsrInst.encodedForm = encodeInstruction(jsrInst);
						fixOffset(ssaInst.id, jsrInst);
						appendInstructionToBlock(edb, jsrInst);

						// Store the result, which is expected to be saved in R27
						DLXInstruction retPop = new DLXInstruction();
						retPop.opcode = InstructionType.POP;
						retPop.format = formatMap.get(InstructionType.POP);
						retPop.ra = FUNC_RET_REG;
						retPop.rb = SP;
						retPop.rc = -4; // word size
						retPop.encodedForm = encodeInstruction(retPop);
						appendInstructionToBlock(edb, retPop);

						// Recover the FP
						DLXInstruction pop1 = new DLXInstruction();
						pop1.opcode = InstructionType.POP;
						pop1.format = formatMap.get(InstructionType.POP);
						pop1.ra = FP;
						pop1.rb = SP;
						pop1.rc = -4; // word size
						pop1.encodedForm = encodeInstruction(pop1);
						appendInstructionToBlock(edb, pop1);

						// Pop operands off of the stack
						for (int opIndex = 0; opIndex < ssaInst.callOperands.size(); opIndex++)
						{
							PLIRInstruction operand = ssaInst.callOperands.get(opIndex);
							DLXInstruction pop2 = new DLXInstruction();
							pop2.opcode = InstructionType.POP;
							pop2.format = formatMap.get(InstructionType.POP);
							pop2.ra = operand.regNum; // toss away the operands that were passed onto the stack
							pop2.rb = SP;
							pop2.rc = -4; // word size
							pop2.encodedForm = encodeInstruction(pop2);
							appendInstructionToBlock(edb, pop2);
						}

						// Reset the old RA
						DLXInstruction pop3 = new DLXInstruction();
						pop3.opcode = InstructionType.POP;
						pop3.format = formatMap.get(InstructionType.POP);
						pop3.ra = RA;
						pop3.rb = SP;
						pop3.rc = -4; // word size
						pop3.encodedForm = encodeInstruction(pop3);
						appendInstructionToBlock(edb, pop3);

						// Pop all inuse registers from the stack
						for (int ri = HIGH_REG; ri >= LOW_REG; ri--)
						{
							DLXInstruction regPop = new DLXInstruction();
							regPop.opcode = InstructionType.POP;
							regPop.format = formatMap.get(InstructionType.POP);
							regPop.ra = ri; // save contents of ssaInst.regNum
							regPop.rb = SP; //
							regPop.rc = -4; // word size
							regPop.encodedForm = encodeInstruction(regPop);
							fixOffset(ssaInst.id, regPop);
							appendInstructionToBlock(edb, regPop);
						}

						// Move the result from the function into the
						// appropriate register
						DLXInstruction moveInst = new DLXInstruction();
						moveInst.opcode = InstructionType.ADD;
						moveInst.format = formatMap.get(InstructionType.ADD);
						moveInst.ra = ssaInst.regNum;
						moveInst.rb = 0;
						moveInst.rc = FUNC_RET_REG; // word size
						moveInst.encodedForm = encodeInstruction(moveInst);
						appendInstructionToBlock(edb, moveInst);

						// Check for store here...
						checkForGlobalStore(edb, ssaInst, true, false);

						break;
					}
					case PROC:
					{
						// Save priors if constants
						Function callFunc = functionMap.get(ssaInst.funcName); 
						for (String var : callFunc.constantsToSave.keySet())
						{
							int constant = functionMap.get(ssaInst.funcName).constantsToSave.get(var);
							DLXInstruction preInst = new DLXInstruction();
							preInst.opcode = InstructionType.ADDI;
							preInst.format = formatMap.get(InstructionType.ADDI);
							preInst.ra = ssaInst.regNum;
							preInst.rb = 0;
							preInst.rc = constant;
							appendInstructionToBlock(edb, preInst);
							
							DLXInstruction storeInst = new DLXInstruction();
							storeInst.opcode = InstructionType.STW;
							storeInst.format = formatMap.get(InstructionType.STW);
							storeInst.ra = ssaInst.regNum; // save contents of ssaInst.regNum
							storeInst.rb = GLOBAL_ADDRESS;
							storeInst.rc = -4 * (globalOffset.get(globalRefMap.get(var)) + 1); // word size
							appendInstructionToBlock(edb, storeInst);
						}
						
						// Push all inuse registers onto the stack
						for (int ri = LOW_REG; ri <= HIGH_REG; ri++)
						{
							DLXInstruction regPush = new DLXInstruction();
							regPush.opcode = InstructionType.PSH;
							regPush.format = formatMap.get(InstructionType.PSH);
							regPush.ra = ri; // save contents of ssaInst.regNum
							regPush.rb = SP; 
							regPush.rc = 4; // word size
							regPush.encodedForm = encodeInstruction(regPush);
							fixOffset(ssaInst.id, regPush);
							appendInstructionToBlock(edb, regPush);
						}

						// Push RA onto the stack
						DLXInstruction push1 = new DLXInstruction();
						push1.opcode = InstructionType.PSH;
						push1.format = formatMap.get(InstructionType.PSH);
						push1.ra = RA; // save contents of ssaInst.regNum
						push1.rb = SP; //
						push1.rc = 4; // word size
						push1.encodedForm = encodeInstruction(push1);
						fixOffset(ssaInst.id, push1);
						appendInstructionToBlock(edb, push1);

						// Push all operands onto the stack (callee recovers the
						// proper order)
						for (int opIndex = ssaInst.callOperands.size() - 1; opIndex >= 0; opIndex--)
						{							
							PLIRInstruction operand = ssaInst.callOperands.get(opIndex);
							
							int srcReg = operand.regNum;
							if (operand.kind == ResultKind.CONST)
							{
								DLXInstruction preInst = new DLXInstruction();
								preInst.opcode = InstructionType.ADDI;
								preInst.format = formatMap.get(InstructionType.ADDI);
								
								if (constants.containsKey(operand.tempVal))
								{
									srcReg = constants.get(operand.tempVal).regNum;	
								}
								preInst.ra = srcReg;
								preInst.rb = 0;
								preInst.rc = operand.tempVal;
								preInst.encodedForm = encodeInstruction(preInst);
								appendInstructionToBlock(edb, preInst);
							}
							DLXInstruction push3 = new DLXInstruction();
							push3.opcode = InstructionType.PSH;
							push3.format = formatMap.get(InstructionType.PSH);
							push3.ra = srcReg; 
							push3.rb = SP; 
							push3.rc = 4; // word size
							push3.encodedForm = encodeInstruction(push3);
							appendInstructionToBlock(edb, push3);
						}

						// Push current FP onto the stack (to become to old FP)
						DLXInstruction push2 = new DLXInstruction();
						push2.opcode = InstructionType.PSH;
						push2.format = formatMap.get(InstructionType.PSH);
						push2.ra = FP; // save contents of ssaInst.regNum
						push2.rb = SP; //
						push2.rc = 4; // word size
						push2.encodedForm = encodeInstruction(push2);
						appendInstructionToBlock(edb, push2);

						// Set FP = SP
						DLXInstruction fpChange = new DLXInstruction();
						fpChange.opcode = InstructionType.ADD;
						fpChange.format = formatMap.get(InstructionType.ADD);
						fpChange.ra = FP;
						fpChange.rb = 0;
						fpChange.rc = SP;
						fpChange.encodedForm = encodeInstruction(fpChange);
						appendInstructionToBlock(edb, fpChange);

						// Add the JSR routine
						DLXInstruction jsrInst = new DLXInstruction();
						jsrInst.opcode = InstructionType.JSR;
						jsrInst.format = formatMap.get(InstructionType.JSR);
						jsrInst.ra = newInst.rb = 0;
						jsrInst.rc = functionAddressTable.get(ssaInst.funcName) * 4;
						jsrInst.encodedForm = encodeInstruction(jsrInst);
						fixOffset(ssaInst.id, jsrInst);
						appendInstructionToBlock(edb, jsrInst);

						// Recover the FP
						DLXInstruction pop1 = new DLXInstruction();
						pop1.opcode = InstructionType.POP;
						pop1.format = formatMap.get(InstructionType.POP);
						pop1.ra = FP;
						pop1.rb = SP;
						pop1.rc = -4; // word size
						pop1.encodedForm = encodeInstruction(pop1);
						appendInstructionToBlock(edb, pop1);

						// Pop operands off of the stack
						for (int opIndex = 0; opIndex < ssaInst.callOperands.size(); opIndex++)
						{
							PLIRInstruction operand = ssaInst.callOperands.get(opIndex);
							DLXInstruction pop2 = new DLXInstruction();
							pop2.opcode = InstructionType.POP;
							pop2.format = formatMap.get(InstructionType.POP);
							pop2.ra = operand.regNum; // toss away the operands that were passed onto the stack
							pop2.rb = SP;
							pop2.rc = -4; // word size
							pop2.encodedForm = encodeInstruction(pop2);
							appendInstructionToBlock(edb, pop2);
						}

						// Reset the old RA
						DLXInstruction pop3 = new DLXInstruction();
						pop3.opcode = InstructionType.POP;
						pop3.format = formatMap.get(InstructionType.POP);
						pop3.ra = RA;
						pop3.rb = SP;
						pop3.rc = -4; // word size
						pop3.encodedForm = encodeInstruction(pop3);
						appendInstructionToBlock(edb, pop3);

						// Pop all inuse registers from the stack
						for (int ri = HIGH_REG; ri >= LOW_REG; ri--)
						{
							DLXInstruction regPop = new DLXInstruction();
							regPop.opcode = InstructionType.POP;
							regPop.format = formatMap.get(InstructionType.POP);
							regPop.ra = ri; // save contents of ssaInst.regNum
							regPop.rb = SP; //
							regPop.rc = -4; // word size
							regPop.encodedForm = encodeInstruction(regPop);
							fixOffset(ssaInst.id, regPop);
							appendInstructionToBlock(edb, regPop);
						}

						break;
					}

					case LOADPARAM:
						// loadparam operand#
						// i1 for ssaInst is the operand#
						// load from stack
						// LDW a, b, c R.a := Mem[R.b + c]
						DLXInstruction loadInst = new DLXInstruction();
						loadInst.opcode = InstructionType.LDW;
						loadInst.format = formatMap.get(InstructionType.LDW);
						loadInst.ra = ssaInst.regNum;
						loadInst.rb = SP;
						loadInst.rc = -4 * (ssaInst.paramNumber + 1 + func.getTotalStackOffset());
						appendInstructionToBlock(edb, loadInst);
						break;

					case LOAD:
						DLXInstruction ldwInst = new DLXInstruction();
						ldwInst.opcode = InstructionType.LDX;
						ldwInst.format = formatMap.get(InstructionType.LDX);
						ldwInst.ra = ssaInst.regNum;
						ldwInst.rb = 0;
						ldwInst.rc = ssaInst.op1.regNum;

						fixOffset(ssaInst.id, ldwInst);
						appendInstructionToBlock(edb, ldwInst);
						
						checkForGlobalStore(edb, ssaInst, true, false);

						break;

					case STORE:
						boolean addedToOffsetMap = false;
						if (ssaInst.op2type == OperandType.CONST && ssaInst.i2 != 0)
						{
							// ra = 0, i1
							DLXInstruction preInst = new DLXInstruction();
							preInst.opcode = InstructionType.ADDI;
							preInst.format = formatMap.get(InstructionType.ADDI);
							preInst.ra = constants.get(ssaInst.i2).regNum;
							preInst.rb = 0;
							preInst.rc = ssaInst.i2;
							fixOffset(ssaInst.id, preInst);
							addedToOffsetMap = true;
							appendInstructionToBlock(edb, preInst);
						}

						DLXInstruction stwInst = new DLXInstruction();
						stwInst.opcode = InstructionType.STX;
						stwInst.format = formatMap.get(InstructionType.STX);
						if (ssaInst.op2type == OperandType.CONST && ssaInst.i2 != 0)
						{
							stwInst.ra = constants.get(ssaInst.i2).regNum;
						}
						else
						{
							stwInst.ra = ssaInst.op2.regNum;
						}

						stwInst.rb = 0;
						stwInst.rc = ssaInst.op1.regNum; // word size

						if (!addedToOffsetMap)
						{
							fixOffset(ssaInst.id, stwInst);
						}
						appendInstructionToBlock(edb, stwInst);

						break;

					case SAVEGLOBAL:
						DLXInstruction storeInst = new DLXInstruction();
						storeInst.opcode = InstructionType.STW;
						storeInst.format = formatMap.get(InstructionType.STW);
						storeInst.ra = ssaInst.op1.regNum; // save contents of ssaInst.regNum
						storeInst.rb = GLOBAL_ADDRESS;
						storeInst.rc = -4 * (globalOffset.get(ssaInst.op2.id) + 1); // word size

						fixOffset(ssaInst.id, storeInst);
						appendInstructionToBlock(edb, storeInst);
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
							DLXBasicBlock prev = null;
							while (leftStack.isEmpty() == false)
							{
								DLXBasicBlock tmp = leftStack.get(leftStack.size() - 1);
								leftStack.remove(leftStack.size() - 1);
								if (tmp.id == edb.id)
								{
									break;
								}
								if (seen.contains(tmp.id) == false)
								{
									prev = curr;
									curr = tmp;
									seen.add(tmp.id);
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
							insertBlock = curr;

							// Handle constants in the phi functions specially
							if (ssaInst.op1type == OperandType.CONST && ssaInst.op2type == OperandType.CONST)
							{
								DLXInstruction leftInst = new DLXInstruction();
								leftInst.opcode = InstructionType.ADDI;
								leftInst.format = formatMap.get(InstructionType.ADDI);
								leftInst.ra = ssaInst.regNum;
								leftInst.rb = 0;
								leftInst.rc = ssaInst.i1;

								leftOffsetMap.put(ssaInst.id, leftInst);
								appendInstructionToBlock(edb, leftInst);
								checkForGlobalStore(edb, ssaInst, true, false);

								DLXInstruction rightInst = new DLXInstruction();
								rightInst.opcode = InstructionType.ADDI;
								rightInst.format = formatMap.get(InstructionType.ADDI);
								rightInst.ra = ssaInst.regNum;
								rightInst.rb = 0;
								rightInst.rc = ssaInst.i2;

								rightOffsetMap.put(ssaInst.id, rightInst);
								appendInstructionToEndBlock(insertBlock, rightInst);
								checkForGlobalStore(insertBlock, ssaInst, false, false);
							}
							else if (ssaInst.op1type == OperandType.CONST) // left side is a constant
							{
								DLXInstruction leftInst = new DLXInstruction();
								leftInst.opcode = InstructionType.ADDI;
								leftInst.format = formatMap.get(InstructionType.ADDI);
								leftInst.ra = ssaInst.regNum;
								leftInst.rb = 0;
								leftInst.rc = ssaInst.i1;

								leftOffsetMap.put(ssaInst.id, leftInst);
								appendInstructionToBlock(edb, leftInst);
								checkForGlobalStore(edb, ssaInst, true, false);

								if (ssaInst.regNum != ssaInst.op2.regNum)
								{
									insertBlock.skipped++;
									DLXInstruction rightInst = new DLXInstruction();
									rightInst.opcode = InstructionType.ADD;
									rightInst.format = formatMap.get(InstructionType.ADD);
									rightInst.ra = ssaInst.regNum;
									rightInst.rb = 0;
									rightInst.rc = ssaInst.op2.regNum;

									rightOffsetMap.put(ssaInst.id, rightInst);
									appendInstructionToEndBlock(insertBlock, rightInst);
									checkForGlobalStore(insertBlock, ssaInst, false, false);
								}
							}
							else if (ssaInst.op2type == OperandType.CONST) // right side is a constant
							{
								boolean fixedOffset = false;
								if (ssaInst.regNum != ssaInst.op1.regNum)
								{
									DLXInstruction leftInst = new DLXInstruction();
									leftInst.opcode = InstructionType.ADD;
									leftInst.format = formatMap.get(InstructionType.ADD);
									leftInst.ra = ssaInst.regNum;
									leftInst.rb = 0;
									leftInst.rc = ssaInst.op1.regNum;

									leftOffsetMap.put(ssaInst.id, leftInst);
									
									appendInstructionToBlock(edb, leftInst);
									checkForGlobalStore(edb, ssaInst, true, false);
									fixedOffset = true;
									edb.skipped++;
								}

								DLXInstruction rightInst = new DLXInstruction();
								rightInst.opcode = InstructionType.ADDI;
								rightInst.format = formatMap.get(InstructionType.ADDI);
								rightInst.ra = ssaInst.regNum;
								rightInst.rb = 0;
								rightInst.rc = ssaInst.i2;

								if (!fixedOffset)
								{
									rightOffsetMap.put(ssaInst.id, rightInst);
								}						
								appendInstructionToEndBlock(insertBlock, rightInst);
								checkForGlobalStore(insertBlock, ssaInst, false, false);
							}
							else // both sides are registers.
							{
								boolean fixedOffset = false;
								if (ssaInst.regNum != ssaInst.op1.regNum)
								{
									DLXInstruction leftInst = new DLXInstruction();
									leftInst.opcode = InstructionType.ADD;
									leftInst.format = formatMap.get(InstructionType.ADD);
									leftInst.ra = ssaInst.regNum;
									leftInst.rb = 0;
									leftInst.rc = ssaInst.op1.regNum;

									fixedOffset = true;
									leftOffsetMap.put(ssaInst.id, leftInst);
									appendInstructionToBlock(edb, leftInst);
									checkForGlobalStore(edb, ssaInst, true, false);
									insertBlock.skipped++;
								}
								if (ssaInst.regNum != ssaInst.op2.regNum)
								{
									DLXInstruction rightInst = new DLXInstruction();
									rightInst.opcode = InstructionType.ADD;
									rightInst.format = formatMap.get(InstructionType.ADD);
									rightInst.ra = ssaInst.regNum;
									rightInst.rb = 0;
									rightInst.rc = ssaInst.op2.regNum;

									fixedOffset = true;
									rightOffsetMap.put(ssaInst.id, rightInst);
									appendInstructionToEndBlock(insertBlock, rightInst);
									checkForGlobalStore(insertBlock, ssaInst, false, false);
									
									insertBlock.skipped++;
								}
							}
						}
						else // PHI for an if-statement, contained in the join node
						{	
							// Determine left and right parents and offsets
							DLXBasicBlock leftParent = edb.parents.get(0);
							DLXBasicBlock rightParent = edb.parents.get(1);
							
							// Handle constants in the phi functions specially
							if (ssaInst.op1type == OperandType.CONST && ssaInst.op2type == OperandType.CONST)
							{
								DLXInstruction leftInst = new DLXInstruction();
								leftInst.opcode = InstructionType.ADDI;
								leftInst.format = formatMap.get(InstructionType.ADDI);
								leftInst.ra = ssaInst.regNum;
								leftInst.rb = 0;
								leftInst.rc = ssaInst.i1;
								
								leftOffsetMap.put(ssaInst.id, leftInst);
								appendInstructionToEndBlock(leftParent, leftInst);
								checkForGlobalStore(leftParent, ssaInst, false, false);

								DLXInstruction rightInst = new DLXInstruction();
								rightInst.opcode = InstructionType.ADDI;
								rightInst.format = formatMap.get(InstructionType.ADDI);
								rightInst.ra = ssaInst.regNum;
								rightInst.rb = 0;
								rightInst.rc = ssaInst.i2;

								rightOffsetMap.put(ssaInst.id, rightInst);
								appendInstructionToEndBlock(rightParent, rightInst);
								checkForGlobalStore(rightParent, ssaInst, false, false);
							}
							else if (ssaInst.op1type == OperandType.CONST) // left side is a constant
							{
								DLXInstruction leftInst = new DLXInstruction();
								leftInst.opcode = InstructionType.ADDI;
								leftInst.format = formatMap.get(InstructionType.ADDI);
								leftInst.ra = ssaInst.regNum;
								leftInst.rb = 0;
								leftInst.rc = ssaInst.i1;

								leftOffsetMap.put(ssaInst.id, leftInst);
								appendInstructionToEndBlock(leftParent, leftInst);
								checkForGlobalStore(leftParent, ssaInst, false, false);

								if (ssaInst.regNum != ssaInst.op2.regNum)
								{
									DLXInstruction rightInst = new DLXInstruction();
									rightInst.opcode = InstructionType.ADD;
									rightInst.format = formatMap.get(InstructionType.ADD);
									rightInst.ra = ssaInst.regNum;
									rightInst.rb = 0;
									rightInst.rc = ssaInst.op2.regNum;

									rightOffsetMap.put(ssaInst.id, rightInst);
									appendInstructionToEndBlock(rightParent, rightInst);
									checkForGlobalStore(rightParent, ssaInst, false, false);
								}
							}
							else if (ssaInst.op2type == OperandType.CONST) // right side is a constant
							{
								boolean fixedOffset = false;
								if (ssaInst.regNum != ssaInst.op1.regNum)
								{
									DLXInstruction leftInst = new DLXInstruction();
									leftInst.opcode = InstructionType.ADD;
									leftInst.format = formatMap.get(InstructionType.ADD);
									leftInst.ra = ssaInst.regNum;
									leftInst.rb = 0;
									leftInst.rc = ssaInst.op1.regNum;

									leftOffsetMap.put(ssaInst.id, leftInst);
									appendInstructionToEndBlock(leftParent, leftInst);
									checkForGlobalStore(leftParent, ssaInst, false, false);
									fixedOffset = true;
									edb.skipped++;
								}

								DLXInstruction rightInst = new DLXInstruction();
								rightInst.opcode = InstructionType.ADDI;
								rightInst.format = formatMap.get(InstructionType.ADDI);
								rightInst.ra = ssaInst.regNum;
								rightInst.rb = 0;
								rightInst.rc = ssaInst.i2;

								rightOffsetMap.put(ssaInst.id, rightInst);
								appendInstructionToEndBlock(rightParent, rightInst);
								checkForGlobalStore(rightParent, ssaInst, false, false);
							}
							else // both sides are registers.
							{
								if (ssaInst.regNum != ssaInst.op1.regNum)
								{
									DLXInstruction leftInst = new DLXInstruction();
									leftInst.opcode = InstructionType.ADD;
									leftInst.format = formatMap.get(InstructionType.ADD);
									leftInst.ra = ssaInst.regNum;
									leftInst.rb = 0;
									leftInst.rc = ssaInst.op1.regNum;

									leftOffsetMap.put(ssaInst.id, leftInst);
									appendInstructionToEndBlock(leftParent, leftInst);
									checkForGlobalStore(leftParent, ssaInst, false, false);
								}
								if (ssaInst.regNum != ssaInst.op2.regNum)
								{
									DLXInstruction rightInst = new DLXInstruction();
									rightInst.opcode = InstructionType.ADD;
									rightInst.format = formatMap.get(InstructionType.ADD);
									rightInst.ra = ssaInst.regNum;
									rightInst.rb = 0;
									rightInst.rc = ssaInst.op2.regNum;

									rightOffsetMap.put(ssaInst.id, rightInst);
									appendInstructionToEndBlock(rightParent, rightInst);
									checkForGlobalStore(rightParent, ssaInst, false, false);
								}
							}
						}
						break;
				}
			}

			// Continue walking the basic block tree and generating instructions
			if (b.leftChild != null)
			{
				generateBlockTreeInstructons(edb.left, b.leftChild, func, isMain, visited);
			}
			if (b.rightChild != null)
			{
				generateBlockTreeInstructons(edb.right, b.rightChild, func, isMain, visited);
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
