package com.uci.cs241.pl241.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.uci.cs241.pl241.ir.PLIRInstruction.PLIRInstructionType;

public class PLIRInstructionManager
{
//	public boolean[] regTable;
	
	public ArrayList<PLIRInstruction> buf;
	public HashMap<String, PLIRInstruction> valueMap;
	
	public PLIRInstructionManager()
	{
		buf = new ArrayList<PLIRInstruction>();
		valueMap = new HashMap<String, PLIRInstruction>();
	}
	
//	public void PutF1(int op, int a, int b, int c)
//	{
//		
//	}
	
	public int allocateReg()
	{
		return 0;
	}
	
//	public List<PLIRInstruction> load(PLParseResult result)
//	{
//		List<PLIRInstruction> instructions = new ArrayList<PLIRInstruction>();
//		if (result.kind == PLParseResultKind.VAR)
//		{
//			result.regno = allocateReg();
//			
//			PLIRInstruction left = null;
//			PLIRInstruction right = null;
//			
//			PLIRInstruction inst = new PLIRInstruction(PLIRInstructionType.LOAD, left, right);
//			instructions.add(inst);
//			result.kind = PLParseResultKind.REG;
//		}
//		else if (result.kind == PLParseResultKind.CONST)
//		{
//			if (result.val == 0)
//			{
//				result.regno = 0;
//			}
//			else
//			{
//				result.regno = allocateReg();
//				
//				PLIRInstruction left = null;
//				PLIRInstruction right = null;
//				
//				PLIRInstruction inst = new PLIRInstruction(PLIRInstructionType.ADD, left, right);
//				instructions.add(inst);
//			}
//		}
//		
//		return instructions;
//	}
	
}
