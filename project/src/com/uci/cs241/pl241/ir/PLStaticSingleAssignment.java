package com.uci.cs241.pl241.ir;

import java.util.ArrayList;
import java.util.HashMap;

import com.uci.cs241.pl241.ir.PLIRInstruction.EliminationReason;
import com.uci.cs241.pl241.ir.PLIRInstruction.PLIRInstructionType;

public class PLStaticSingleAssignment
{
	public static int globalSSAIndex = 0;
	public static ArrayList<PLIRInstruction> instructions = new ArrayList<PLIRInstruction>();
	
	public static HashMap<PLIRInstructionType, ArrayList<PLIRInstruction>> instTypeMap = new HashMap<PLIRInstructionType, ArrayList<PLIRInstruction>>();
	
	public static void init()
	{
		instTypeMap.put(PLIRInstructionType.NEG, new ArrayList<PLIRInstruction>());
		instTypeMap.put(PLIRInstructionType.ADD, new ArrayList<PLIRInstruction>());
		instTypeMap.put(PLIRInstructionType.SUB, new ArrayList<PLIRInstruction>());
		instTypeMap.put(PLIRInstructionType.MUL, new ArrayList<PLIRInstruction>());
		instTypeMap.put(PLIRInstructionType.DIV, new ArrayList<PLIRInstruction>());
		instTypeMap.put(PLIRInstructionType.CMP, new ArrayList<PLIRInstruction>());
	}
	
	public static int addInstruction(PLIRInstruction inst)
	{
		// Check for common subexpression first
		if (instTypeMap.containsKey(inst.opcode))
		{
			boolean common = false;
			for (int i = 0; i < instTypeMap.get(inst.opcode).size() && !common; i++)
			{
				PLIRInstruction other = instTypeMap.get(inst.opcode).get(i);
				if (other.equals(inst))
				{
					inst.removeInstruction(EliminationReason.CSE, other);
					common = true;
				}
			}
			if (!common)
			{
				instTypeMap.get(inst.opcode).add(inst);	
			}
		}
		
		boolean success = instructions.add(inst);
		if (!success) return -1;
		globalSSAIndex++;
		return globalSSAIndex;
	}
	
	public static int injectInstruction(PLIRInstruction inst, int loc)
	{
		for (int i = loc; i < instructions.size(); i++)
		{
			PLIRInstruction next = instructions.get(i);
			next.id++;
			next.fixupLocation++;
		}
		instructions.add(loc,inst);
		globalSSAIndex++;
		return loc + 1;
	}
	
	public static void updateInstruction(int id, PLIRInstruction inst)
	{
		System.err.println("Replacing: " + instructions.get(id).toString() + " with " + inst.toString());
		instructions.set(id, inst);
	}
	
	public static void displayInstructions()
	{
		StringBuilder builder = new StringBuilder();
		for (PLIRInstruction inst : instructions)
		{
			builder.append(inst.id + " := " + inst.toString() + "\n");
		}
		System.out.println(builder.toString());
	}
}
