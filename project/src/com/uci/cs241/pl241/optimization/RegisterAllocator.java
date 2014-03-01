package com.uci.cs241.pl241.optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.uci.cs241.pl241.frontend.PLSymbolTable;
import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction;
import com.uci.cs241.pl241.ir.PLIRInstruction.InstructionType;
import com.uci.cs241.pl241.ir.PLIRInstruction.OperandType;
import com.uci.cs241.pl241.ir.PLIRInstruction.ResultKind;
import com.uci.cs241.pl241.ir.PLStaticSingleAssignment;

public class RegisterAllocator
{
	public static final int NUM_REGISTERS = 8;
	public InterferenceGraph ig;

	public HashMap<Integer, Integer> regMap = new HashMap<Integer, Integer>();
	public HashSet<Integer> regSet = new HashSet<Integer>();
	
	public HashMap<Integer, PLIRInstruction> constants = new HashMap<Integer, PLIRInstruction>();
	
	public PLSymbolTable scope;
	
	public RegisterAllocator(InterferenceGraph ig, PLSymbolTable scope)
	{
		this.ig = ig;
		this.scope = scope;
	}

	// Graph coloring algorithm
	public void Color(InterferenceGraph graph)
	{
		// Retrieve random node with fewer than NUM_REGISTERS neighbors
		int x = ig.getVertexWithMaxDegree(NUM_REGISTERS);
		if (x == -1)
		{
			x = ig.getSmallestCost();
		}

		// Remove the vertex and recover its neighbors
		regSet.add(x);
		ArrayList<Integer> neighbors = ig.removeVertex(x);

		// If the graph isn't empty, recursively color
		if (ig.isEmpty() == false)
		{
			Color(ig);
		}

		// Add x and its edges back to G
		ig.addVertex(x, neighbors);

		// choose a color for x that is different from its neighbors
		HashSet<Integer> neighborColors = new HashSet<Integer>();
		for (Integer n : neighbors)
		{
			neighborColors.add(regMap.get(n));
		}
		boolean colored = false;
		int color = 1; // register 0, for DLX, is always reserved to be a
						// constant zero
		while (!colored)
		{
			if (neighborColors.contains(color) == false)
			{
				regMap.put(x, color);
				colored = true;
				break;
			}

			// Try the next color
			color++;

			// // We can't use reserved registers... only R1-R27 are general
			// purpose
			// // See DLX spec for R0, R28, R29, R30, R31 roles
			// while (color == 28 || color == 29 || color == 30)
			// {
			// color++;
			// }
		}
		regMap.put(x, color);
		PLStaticSingleAssignment.getInstruction(x).regNum = color;
	}

	public void ComputeLiveRange(PLIRBasicBlock entryBlock)
	{
		HashSet<PLIRInstruction> live = new HashSet<PLIRInstruction>();
		live.addAll(CalcLiveRange(entryBlock, 1, 1));
		live.addAll(CalcLiveRange(entryBlock, 1, 2));
	}

	public HashSet<PLIRInstruction> CalcLiveRange(PLIRBasicBlock b, int branch, int pass)
	{
		HashSet<PLIRInstruction> live = new HashSet<PLIRInstruction>();

		if (b == null)
		{
			return live;
		}
		else
		{
			if (b.visitNumber >= pass)
			{
				live.addAll(b.liveAtEnd);
			}
			else
			{
				// inc(b.visit#)
				b.visitNumber++;

				// for all enclosing loop headers h in b
				// b.live = b.live + h.live
				if (b.visitNumber == 2)
				{
					for (PLIRBasicBlock h : b.wrappedLoopHeaders)
					{
						b.liveAtEnd.addAll(h.liveAtEnd);
						live.addAll(h.liveAtEnd);
					}
				}

				// recursively add children to the live set
				if (b.leftChild != null)
				{
					live.addAll(CalcLiveRange(b.leftChild, 1, pass));
				}
				if (b.rightChild != null)
				{
					live.addAll(CalcLiveRange(b.rightChild, 2, pass));
				}

				// for all non-phis
				for (int i = b.instructions.size() - 1; i >= 0; i--)
				{
					PLIRInstruction inst = b.instructions.get(i);
					if (inst.opcode == InstructionType.WRITE && inst.id == 8) 
					{
						System.err.println("write");
					}
					if (inst.opcode != InstructionType.PHI && inst.isNotLiveInstruction() == false)
					{
						
						while (inst.refInst != null)
						{
							inst = inst.refInst;
						}

						// live = live - {i}
						live.remove(inst);

						// Add edge between new var and all live variables
						for (PLIRInstruction liveInst : live)
						{
							ig.addEdge(inst.id, liveInst.id);
						}
						
						if (inst.id == 10)
						{
							System.err.println("here mk");
						}

						// live = live + {j,k}
						if (inst.op1 != null && PLStaticSingleAssignment.isIncluded(inst.op1.id))
						{
							PLIRInstruction op = inst.op1;
							while (op.refInst != null)
							{
								op = op.refInst;
							}
							live.add(op);
						}
						else if (inst.op1type == OperandType.CONST && inst.i1 != 0)
						{
							if (constants.containsKey(inst.i1))
							{
								live.add(constants.get(inst.i1));
							}
							else
							{
								PLIRInstruction constInst = new PLIRInstruction(scope);
								constInst.id = PLStaticSingleAssignment.globalSSAIndex;
								ig.addVertex(constInst.id);
								PLStaticSingleAssignment.addInstruction(scope, constInst);
								constants.put(inst.i1, constInst);
								live.add(constInst);
							}
						}
						if (inst.op2 != null && PLStaticSingleAssignment.isIncluded(inst.op2.id))
						{
							PLIRInstruction op = inst.op2;
							while (op.refInst != null)
							{
								op = op.refInst;
							}
							live.add(op);
						}
						else if (inst.op2type == OperandType.CONST && inst.i2 != 0)
						{
							if (constants.containsKey(inst.i2))
							{
								live.add(constants.get(inst.i2));
							}
							else
							{
								PLIRInstruction constInst = new PLIRInstruction(scope);
								constInst.id = PLStaticSingleAssignment.globalSSAIndex;
								ig.addVertex(constInst.id);
								PLStaticSingleAssignment.addInstruction(scope, constInst);
								constants.put(inst.i2, constInst);
								live.add(constInst);
							}
						}
						
						// All operands must live in some register (derp!)
						if (inst.callOperands != null)
						{
							for (PLIRInstruction op : inst.callOperands)
							{
								live.add(op);
							}
						}
					}
				}

				// b.liveAtEnd = live
				b.liveAtEnd.addAll(live);
			}

			// for all phis
			for (int i = b.instructions.size() - 1; i >= 0; i--)
			{
				PLIRInstruction inst = b.instructions.get(i);
				if (inst.opcode == InstructionType.PHI)
				{
					// live = live - {i}
					live.remove(inst);

					// Add edge between new var and all live variables
					for (PLIRInstruction liveInst : live)
					{
						ig.addEdge(inst.id, liveInst.id);
					}

					// live = live + {j,k}, if j/k are actual values and not constants
					// but only add the phi operand indexed by the branch number
					if (branch == 1 && inst.op1 != null && PLStaticSingleAssignment.isIncluded(inst.op1.id))
					{
						PLIRInstruction op = inst.op1;
						while (op.refInst != null)
						{
							op = op.refInst;
						}
						live.add(op);
					}
					if (branch == 2 && inst.op2 != null && PLStaticSingleAssignment.isIncluded(inst.op2.id))
					{
						PLIRInstruction op = inst.op2;
						while (op.refInst != null)
						{
							op = op.refInst;
						}
						live.add(op);
					}
				}
			}
		}

		return live;
	}
}
