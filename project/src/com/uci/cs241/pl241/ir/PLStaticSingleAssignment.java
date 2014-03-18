package com.uci.cs241.pl241.ir;

import java.util.ArrayList;

import com.uci.cs241.pl241.frontend.PLSymbolTable;
import com.uci.cs241.pl241.ir.PLIRInstruction.InstructionType;

public class PLStaticSingleAssignment
{
	public static int globalSSAIndex = 0;
	public static ArrayList<PLIRInstruction> instructions = new ArrayList<PLIRInstruction>();
		
	public static void init()
	{
	}
	
//	public static void endInstructions()
//	{
//		boolean remove = false;
//		ArrayList<Integer> toRemove = new ArrayList<Integer>(); 
//		for (int i = 0; i < instructions.size(); i++)
//		{
//			if (remove) toRemove.add(i);
//			if (instructions.get(i).opcode == InstructionType.END)
//			{
//				remove = true;
//			}
//		}
//		for (Integer i : toRemove)
//		{
//			System.out.println("Removing: " + instructions.get(i));
//			instructions.remove(i);
//		}
//	}
	
	public static void finish()
	{
		for (int i = 0; i < instructions.size(); i++)
		{
			instructions.get(i).id = i;
		}
		for (PLIRInstruction inst : instructions)
		{
			inst.cost = inst.depth * inst.uses;
		}
		
		// put any other finalization logic here 
	}
	
	public static boolean isIncluded(int id)
	{
		for (PLIRInstruction inst : instructions)
		{
			if (inst.id == id)
			{
				return true;
			}
		}
		return false;
	}
	
	public static PLIRInstruction getInstruction(int id)
	{
		for (PLIRInstruction inst : instructions)
		{
			if (inst.id == id)
			{
				return inst;
			}
		}
		return null;
//		return instructions.get(id - 1);
	}
	
	public static int addInstruction(PLSymbolTable table, PLIRInstruction inst)
	{
		System.out.println("Adding " + inst);
		inst.depth = table.scopeDepth();
		boolean success = instructions.add(inst);
		if (!success) return -1;
		globalSSAIndex++;
		return globalSSAIndex;
	}
	
	public static int injectInstruction(PLIRInstruction inst, PLSymbolTable table, int loc)
	{
		for (int i = loc; i < instructions.size(); i++)
		{
			PLIRInstruction next = instructions.get(i);
			next.id++;
			next.fixupLocation++;
		}
		if (loc > instructions.size())
		{
			loc = instructions.size();
			instructions.add(inst);
		}
		else
		{
			instructions.add(loc,inst);
		}
		globalSSAIndex++;
		return loc + 1;
	}
	
	public static void updateInstruction(int id, PLIRInstruction inst)
	{
		System.err.println("Replacing: " + instructions.get(id).toString() + " with " + inst.toString());
		instructions.set(id, inst);
	}
	
	public static String renderInstructions()
	{
		StringBuilder builder = new StringBuilder();
		for (PLIRInstruction inst : instructions)
		{
			builder.append(inst.id + " := " + inst.toString() + "\n");
		}
		return builder.toString();
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
