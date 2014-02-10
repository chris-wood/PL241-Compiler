package com.uci.cs241.pl241.optimization;

import java.util.ArrayList;
import java.util.HashSet;

import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction;
import com.uci.cs241.pl241.ir.PLIRInstruction.PLIRInstructionType;
import com.uci.cs241.pl241.ir.PLStaticSingleAssignment;

public class RegisterAllocator 
{
	public static final int NUM_REGISTERS = 8;
	public InterferenceGraph ig;
	
	// Graph coloring algorithm
	public void Color(InterferenceGraph graph)
	{
		// Retrieve random node with fewer than NUM_REGISTERS neighbors
		int x = ig.getNeighborCount(NUM_REGISTERS);
		if (x == -1)
		{
			// else take hte node with the lowest count => and spill to memory
		}
		
	}
	
	// Global parameters for the liverange calculation and liveset derivation
	ArrayList<PLIRBasicBlock> stack;
	
	public void ComputeLiveRange(PLIRBasicBlock entryBlock)
	{
		stack = new ArrayList<PLIRBasicBlock>();
		ig = new InterferenceGraph(PLStaticSingleAssignment.instructions);
		
		LRTraverse(entryBlock, 1);
		ig.displayEdges();
	}
	
	private HashSet<Integer> LRTraverse(PLIRBasicBlock b, int branch)
	{
		stack.add(b);
		int depth0 = stack.size();
		
		
//		branch = b.mark == 0 ? 1 : 2;
		b.mark = depth0;
		
		
		System.out.println("block " + b.id + " mark = " + b.mark);
		
		HashSet<Integer> live = new HashSet<Integer>();
		for (int i = 0; i < b.children.size(); i++)
		{
			// if |children| == 1, then i == 0 is branch
			// if |children| == 2, then i == 0 is branch and i == 1 is fail
			PLIRBasicBlock child = b.children.get(i);
			if (child.mark == 0)
			{
				if (i == 0)
				{
					System.out.println("Traversing left to: " + child.id);
					live = LRTraverse(child, 1);
					b.mark = b.mark < child.mark ? b.mark : child.mark;
				}
				else
				{
					System.out.println("Traversing right to: " + child.id);
					live.addAll(LRTraverse(child, 2));
					b.mark = b.mark < child.mark ? b.mark : child.mark;
				}
			}
		}
		
		b.liveAtEnd = new HashSet<Integer>();
		b.liveAtEnd.addAll(live);
		System.out.println("Live at end of " + b.id + ": " + b.liveAtEnd + ",  mark = " + b.mark);
		
		// Traverse regular instructions, bottom-up
//		if (b.mark != Integer.MAX_VALUE)
		{
		for (int i = b.instructions.size() - 1; i >= 0; i--)
		{
			PLIRInstruction inst = b.instructions.get(i);
			while (inst.refInst != null)
			{
				inst = inst.refInst;
			}
			if (PLStaticSingleAssignment.isIncluded(inst.id) && inst.isNotLiveInstruction() == false && inst.opcode != PLIRInstructionType.PHI)
			{	
				live.remove(inst.id);
				for (Integer x : live)
				{
//					edgeSet.add(new Edge(inst.id, x.id));
//					System.err.println("Adding edge: " + inst.toString() + " " + x.toString());
					ig.addEdge(inst.id, x);
				}
				if (inst.op1 != null && PLStaticSingleAssignment.isIncluded(inst.op1.id))
				{
					PLIRInstruction op = inst.op1;
					while (op.refInst != null)
					{
						op = op.refInst;
					}
					live.add(op.id);
				}
				if (inst.op2 != null  && PLStaticSingleAssignment.isIncluded(inst.op2.id))
				{
					PLIRInstruction op = inst.op2;
					while (op.refInst != null)
					{
						op = op.refInst;
					}
					live.add(op.id);
				}
				
				System.out.println("After " + inst.id + ": " + live);
			}
		}
		}
		
		// if we already visited this node, then this time we MUST have come from the right
//		branch = b.mark == Integer.MAX_VALUE ? 2 : 1;
		if (b.id == 30)
		{
			System.out.println("end node, ");
		}
		
		// Traverse phi instructions, bottom-up
		for (int i = b.instructions.size() - 1; i >= 0; i--)
		{
			PLIRInstruction inst = b.instructions.get(i);
			while (inst.refInst != null)
			{
				inst = inst.refInst;
			}
			if (PLStaticSingleAssignment.isIncluded(inst.id) && inst.isNotLiveInstruction() == false && inst.opcode == PLIRInstructionType.PHI)
			{
				live.remove(inst.id);
				for (Integer x : live)
				{
//					edgeSet.add(new Edge(inst.id, x.id));
					ig.addEdge(inst.id, x);
				}
				if (branch == 1)
				{
					if (inst.op1 != null  && PLStaticSingleAssignment.isIncluded(inst.op1.id))
					{
						PLIRInstruction op = inst.op1;
						while (op.refInst != null)
						{
							op = op.refInst;
						}
						live.add(op.id);
					}
				}
				else
				{
					if (inst.op2 != null  && PLStaticSingleAssignment.isIncluded(inst.op2.id))
					{
						PLIRInstruction op = inst.op2;
						while (op.refInst != null)
						{
							op = op.refInst;
						}
						live.add(op.id);
					}
				}
				
				System.out.println("After phi " + inst.id + ": " + live);
			}
		}
		
		// March forward
		if (b.mark == depth0) 
		{
			PLIRBasicBlock bb = stack.get(stack.size() - 1);
			while (bb.id != b.id)
			{
				// b' = pop
				stack.remove(stack.size() - 1);
				
				// Add live set to stack (since we went bottom up)
				bb.liveAtEnd.addAll(live);
				
				// live'
				HashSet<Integer> liveprime = new HashSet<Integer>();
				liveprime.addAll(bb.liveAtEnd);
				
				// Walk backwards, bottom-up again
				for (int i = b.instructions.size() - 1; i >= 0; i--)
				{
					PLIRInstruction inst = b.instructions.get(i);
					while (inst.refInst != null)
					{
						inst = inst.refInst;
					}
					if (PLStaticSingleAssignment.isIncluded(inst.id) && inst.isNotLiveInstruction() == false && inst.opcode != PLIRInstructionType.PHI)
					{
						liveprime.remove(inst.id);
						
						
						for (Integer x : liveprime)
						{
//							edgeSet.add(new Edge(inst.id, x.id));
							ig.addEdge(inst.id, x);
						}
						if (inst.op1 != null  && PLStaticSingleAssignment.isIncluded(inst.op1.id))
						{
							PLIRInstruction op = inst.op1;
							while (op.refInst != null)
							{
								op = op.refInst;
							}
							liveprime.add(op.id);
						}
						if (inst.op2 != null  && PLStaticSingleAssignment.isIncluded(inst.op2.id))
						{
							PLIRInstruction op = inst.op2;
							while (op.refInst != null)
							{
								op = op.refInst;
							}
							liveprime.add(op.id);
						}
						
						System.out.println("Prime after " + inst.id + ": " + liveprime);
					}
				}
				
				// infinity == -1
				System.out.println("Setting bprime" + bb.id + " mark = " + bb.mark);
				bb.mark = Integer.MAX_VALUE;
			}
			
			// pop(stack)
			stack.remove(stack.size() - 1);
			
			// b.mark := infty
			System.out.println("Setting b " + b.id + " mark = " + b.mark);
			b.mark = Integer.MAX_VALUE;
		}
		else
		{
			System.out.println("b.mark != depth0");
		}
		
		return live;
	}
}
