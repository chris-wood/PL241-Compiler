package com.uci.cs241.pl241.ir;

import java.util.ArrayList;

public class PLStaticSingleAssignment
{
	public static int globalSSAIndex = 0;
	public static ArrayList<PLIRInstruction> instructions = new ArrayList<PLIRInstruction>();
	
	public static int addInstruction(PLIRInstruction inst)
	{
		if (inst.toString().equals("read"))
		{
			int i = 0; // catch a break
		}
		boolean success = instructions.add(inst);
		if (!success) return -1;
		return globalSSAIndex++;
	}
	
	public static void updateInstruction(int id, PLIRInstruction inst)
	{
		System.err.println("Replacing: " + instructions.get(id).toString() + " with " + inst.toString());
		instructions.set(id, inst);
	}
	
	public static void displayInstructions()
	{
		StringBuilder builder = new StringBuilder();
		int index = 0;
		System.out.println("Instructions:");
		for (PLIRInstruction inst : instructions)
		{
			builder.append((index++) + " := " + inst.toString() + "\n");
		}
		System.out.println(builder.toString());
	}
}
