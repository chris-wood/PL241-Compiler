package com.uci.cs241.pl241.optimization;

import java.util.ArrayList;
import java.util.HashMap;

import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction.EliminationReason;
import com.uci.cs241.pl241.ir.PLIRInstruction.PLIRInstructionType;
import com.uci.cs241.pl241.ir.PLIRInstruction;

public class CSE 
{	
	public PLIRBasicBlock performCSE(PLIRBasicBlock root)
	{
		PLIRBasicBlock ret = root;
		
		HashMap<PLIRInstructionType, ArrayList<PLIRInstruction>> domList = 
				new HashMap<PLIRInstructionType, ArrayList<PLIRInstruction>>();
		
		// Initialize the map
		domList.put(PLIRInstructionType.NEG, new ArrayList<PLIRInstruction>());
		domList.put(PLIRInstructionType.ADD, new ArrayList<PLIRInstruction>());
		domList.put(PLIRInstructionType.SUB, new ArrayList<PLIRInstruction>());
		domList.put(PLIRInstructionType.MUL, new ArrayList<PLIRInstruction>());
		domList.put(PLIRInstructionType.DIV, new ArrayList<PLIRInstruction>());
		domList.put(PLIRInstructionType.CMP, new ArrayList<PLIRInstruction>());
		
		// Record what's already been visited to prevent loops on while loop BBs
		ArrayList<Integer> visited = new ArrayList<Integer>();
		
		// Start depth-first traversal of the tree, recursively
		cseOnBlock(domList, visited, root);
		
		return ret;
	}
	
	private void cseOnBlock(HashMap<PLIRInstructionType, ArrayList<PLIRInstruction>> parentDomList, 
			ArrayList<Integer> visited, PLIRBasicBlock block)
	{
		if (visited.contains(block.id))
		{
			return;
		}
		else
		{
			HashMap<PLIRInstructionType, ArrayList<PLIRInstruction>> domList = 
					new HashMap<PLIRInstructionType, ArrayList<PLIRInstruction>>();
			
			// Re-populate the map
			for (PLIRInstructionType key : parentDomList.keySet())
			{
				domList.put(key, new ArrayList<PLIRInstruction>());
				for (PLIRInstruction inst : parentDomList.get(key))
				{
					domList.get(key).add(inst);
				}
			}
			
			// Swap out the parent...
			for (PLIRInstruction inst : block.instructions)
			{
				if (domList.containsKey(inst.opcode))
				{
					for (PLIRInstruction parentInst : domList.get(inst.opcode))
					{
						if (inst.equals(parentInst))
						{
							inst.removeInstruction(EliminationReason.CSE, parentInst);
						}
					}
					domList.get(inst.opcode).add(inst);
				}
			}
			
			// Mark as visited
			visited.add(block.id);
			
			// DFS on the children now
			for (PLIRBasicBlock child : block.children)
			{
				cseOnBlock(domList, visited, child);
			}
		}
	}
}
