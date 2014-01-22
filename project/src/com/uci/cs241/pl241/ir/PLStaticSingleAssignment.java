package com.uci.cs241.pl241.ir;

import java.util.ArrayList;

public class PLStaticSingleAssignment
{
	public static int globalSSAIndex = 0;
	public static ArrayList<PLIRInstruction> instructions = new ArrayList<PLIRInstruction>();
	
	public static int addInstruction(PLIRInstruction inst)
	{
//		System.out.println("Adding: " + inst.toString());
		boolean success = instructions.add(inst);
		if (!success) return -1;
		return globalSSAIndex++;
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
