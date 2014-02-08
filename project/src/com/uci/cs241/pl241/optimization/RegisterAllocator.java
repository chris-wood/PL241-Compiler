package com.uci.cs241.pl241.optimization;

import java.util.ArrayList;
import java.util.HashSet;

import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction;
import com.uci.cs241.pl241.ir.PLIRInstruction.PLIRInstructionType;

public class RegisterAllocator 
{
	
	public InterferenceGraph ig;
	
	// Graph coloring algorithm
	public void Color(InterferenceGraph graph)
	{
		
	}
	
	// Global parameters for the liverange calculation and liveset derivation
	ArrayList<PLIRBasicBlock> stack;
	
	public void ComputeLiveRange(PLIRBasicBlock entryBlock)
	{
		stack = new ArrayList<PLIRBasicBlock>();
		ig = new InterferenceGraph();
		
		LRTraverse(entryBlock, 1);
		ig.displayEdges();
	}
	
	private HashSet<PLIRInstruction> LRTraverse(PLIRBasicBlock b, int branch)
	{
		stack.add(b);
		int depth0 = stack.size();
		b.mark = depth0;
		
		HashSet<PLIRInstruction> live = new HashSet<PLIRInstruction>();
		for (int i = 0; i < b.children.size(); i++)
		{
			// if |children| == 1, then i == 0 is branch
			// if |children| == 2, then i == 0 is branch and i == 1 is fail
			PLIRBasicBlock child = b.children.get(i);
			if (child.mark == 0)
			{
				if (i == 0)
				{
					live = LRTraverse(child, 1);
					b.mark = b.mark < child.mark ? b.mark : child.mark;
				}
				else
				{
					live.addAll(LRTraverse(child, 2));
					b.mark = b.mark < child.mark ? b.mark : child.mark;
				}
			}
		}
		
//		if (b.branch != null && b.branch.mark == 0)
//		{
//			live = LRTraverse(b.branch, 1);
//			b.mark = b.mark < b.fail.mark ? b.mark : b.fail.mark;
//		}
//		
//		if (b.fail != null && b.fail.mark == 0)
//		{
//			live.addAll(LRTraverse(b.fail, 2));
//			b.mark = b.mark < b.fail.mark ? b.mark : b.fail.mark;
//		}
		
		b.liveAtEnd = new HashSet<PLIRInstruction>();
		b.liveAtEnd.addAll(live);
		
		// Traverse regular instructions, bottom-up
		for (int i = b.instructions.size() - 1; i >= 0; i--)
		{
			PLIRInstruction inst = b.instructions.get(i);
			if (inst.opcode != PLIRInstructionType.PHI)
			{
				live.remove(inst);
				for (PLIRInstruction x : live)
				{
//					edgeSet.add(new Edge(inst.id, x.id));
					ig.AddEdge(inst.id, x.id);
				}
				if (inst.op1 != null)
				{
					live.add(inst.op1);
				}
				if (inst.op2 != null)
				{
					live.add(inst.op2);
				}
			}
		}
		
		// Traverse phi instructions, bottom-up
		for (int i = b.instructions.size() - 1; i >= 0; i--)
		{
			PLIRInstruction inst = b.instructions.get(i);
			if (inst.opcode == PLIRInstructionType.PHI)
			{
				live.remove(inst);
				for (PLIRInstruction x : live)
				{
//					edgeSet.add(new Edge(inst.id, x.id));
					ig.AddEdge(inst.id, x.id);
				}
				if (branch == 1)
				{
					if (inst.op1 != null)
					{
						live.add(inst.op1);
					}
				}
				else
				{
					if (inst.op2 != null)
					{
						live.add(inst.op2);
					}
				}
			}
		}
		
		// March forward
		if (b.mark == depth0) 
		{
			while (stack.get(stack.size() - 1).id != b.id)
			{
				// b' = pop
				PLIRBasicBlock bb = stack.get(stack.size() - 1);
				stack.remove(stack.size() - 1);
				
				// Add live set to stack (since we went bottom up)
				bb.liveAtEnd.addAll(live);
				
				// live'
				HashSet<PLIRInstruction> liveprime = new HashSet<PLIRInstruction>();
				liveprime.addAll(bb.liveAtEnd);
				
				// Walk backwards, bottom-up again
				for (int i = b.instructions.size() - 1; i >= 0; i--)
				{
					PLIRInstruction inst = b.instructions.get(i);
					if (inst.opcode != PLIRInstructionType.PHI)
					{
						liveprime.remove(inst);
						for (PLIRInstruction x : liveprime)
						{
//							edgeSet.add(new Edge(inst.id, x.id));
							ig.AddEdge(inst.id, x.id);
						}
						if (inst.op1 != null)
						{
							liveprime.add(inst.op1);
						}
						if (inst.op2 != null)
						{
							liveprime.add(inst.op2);
						}
					}
				}
				
				// infinity == -1
				bb.mark = -1;
			}
			
			// pop(stack)
			stack.remove(stack.size() - 1);
			
			// b.mark := infty
			b.mark = -1;
		}
		
		return live;
	}
}
