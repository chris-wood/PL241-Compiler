package com.uci.cs241.pl241.ir;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import com.uci.cs241.pl241.frontend.PLSymbolTable;
import com.uci.cs241.pl241.ir.PLIRInstruction.EliminationReason;
import com.uci.cs241.pl241.ir.PLIRInstruction.PLIRInstructionType;

public class PLStaticSingleAssignment
{
	public static int globalSSAIndex = 0;
	public static ArrayList<PLIRInstruction> instructions = new ArrayList<PLIRInstruction>();
		
	public static void init()
	{
	}
	
	public static int addInstruction(PLSymbolTable table, PLIRInstruction inst)
	{
//		if (globalSSAIndex == 30)
//		{
//			int x = 0;
//		}
//		// Check for common subexpressions first
//		ArrayList<PLIRInstruction> dominated = table.getDominatedInstructions(inst.opcode);
//		if (dominated != null)
//		{
//			boolean common = false;
//			for (int i = 0; i < dominated.size() && !common; i++)
//			{
//				PLIRInstruction other = dominated.get(i);
//				if (other.equals(inst))
//				{
//					inst.removeInstruction(EliminationReason.CSE, other);
//					common = true;
//				}
//			}
//			if (!common)
//			{
//				table.addDominatedInstruction(inst);
////				table.instTypeMap.get(table.getCurrentScope()).get(inst.opcode).add(inst);
////				table.addDominatedInstruction(inst);
//			}
//		}
//		else
//		{
//			table.addDominatedInstruction(inst);
//		}
		
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
		instructions.add(loc,inst);
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
