package com.uci.cs241.pl241.optimization;

import java.util.ArrayList;
import java.util.HashMap;

import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction.EliminationReason;
import com.uci.cs241.pl241.ir.PLIRInstruction.OperandType;
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
		// Avoid cyclic loops from while loops
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
			
			// Check for subexpression elimination on the parent
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
						else if (inst.opcode == parentInst.opcode) // special case to handle commutative operations
						{
					    	switch (inst.opcode)
					    	{
					    	case ADD:
					    		
//					    		// case #1: add const const
//					    		//          add const const
//					    		if ((inst.op1type == OperandType.CONST && parentInst.op1type == OperandType.CONST) &&
//					    			(inst.op2type == OperandType.CONST && parentInst.op2type == OperandType.CONST))	
//					    		{
//					    			if ((inst.i1 == parentInst.i1 && inst.i2 == parentInst.i2) ||
//					    				((inst.i1 == parentInst.i2 && inst.i2 == parentInst.i1)))
//					    			{
//					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
//					    			}
//					    		}
//					    		
//					    		// case #2: add inst inst
//					    		//          add inst inst
//					    		else if ((inst.op1type == OperandType.INST && parentInst.op1type == OperandType.INST) &&
//						    			(inst.op2type == OperandType.INST && parentInst.op2type == OperandType.INST))	
//						    	{
//					    			if ((inst.op1.equals(parentInst.op1) && inst.op2.equals(parentInst.op2)) ||
//					    				((inst.op1.equals(parentInst.op2) && inst.op2.equals(parentInst.op1))))
//					    			{
//					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
//					    			}
//						    	}
//					    		
//					    		// case #3: add inst const
//					    		//          add inst const
//					    		else if ((inst.op1type == OperandType.INST && parentInst.op1type == OperandType.INST) &&
//						    			(inst.op2type == OperandType.CONST && parentInst.op2type == OperandType.CONST))	
//					    		{
//					    			if ((inst.op1.equals(parentInst.op1) && inst.i2 == parentInst.i2 ) ||
//					    				((inst.op1.equals(parentInst.op2) && inst.op2.equals(parentInst.op1))))
//					    			{
//					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
//					    			}
//					    		}
//					    		
//					    		// case #4: add inst const
//					    		//          add const inst
//					    		else if ((inst.op1type == OperandType.INST && parentInst.op1type == OperandType.CONST) &&
//						    			(inst.op2type == OperandType.CONST && parentInst.op2type == OperandType.INST))	
//						    		{
//					    			
//						    		}
//					    		
//					    		// case #5: add const inst
//					    		//          add const inst
//					    		else if ((inst.op1type == OperandType.CONST && parentInst.op1type == OperandType.CONST) &&
//						    			(inst.op2type == OperandType.INST && parentInst.op2type == OperandType.INST))	
//						    		{
//					    			
//						    		}
//					    		
//					    		// case #6: add const inst
//					    		//          add inst const
//					    		else if ((inst.op1type == OperandType.CONST && parentInst.op1type == OperandType.INST) &&
//						    			(inst.op2type == OperandType.INST && parentInst.op2type == OperandType.CONST))	
//						    		{
//					    			
//						    		}
//					    		
//					    		break;
					    	case MUL:
					    		
//					    		// case #1: mul const const
//					    		//          mul const const
//					    		if ((inst.op1type == OperandType.CONST && parentInst.op1type == OperandType.CONST) &&
//					    			(inst.op2type == OperandType.CONST && parentInst.op2type == OperandType.CONST))	
//					    		{
//					    			if ((inst.i1 == parentInst.i1 && inst.i2 == parentInst.i2) ||
//					    				((inst.i1 == parentInst.i2 && inst.i2 == parentInst.i1)))
//					    			{
//					    				inst.removeInstruction(EliminationReason.CSE, parentInst);
//					    			}
//					    		}
//					    		
//					    		// case #2: mul inst inst
//					    		//          mul inst inst
//					    		else if ((inst.op1type == OperandType.INST && parentInst.op1type == OperandType.INST) &&
//						    			(inst.op2type == OperandType.INST && parentInst.op2type == OperandType.INST))	
//						    		{
//					    			
//						    		}
//					    		
//					    		// case #3: mul inst const
//					    		//          mul inst const
//					    		else if ((inst.op1type == OperandType.INST && parentInst.op1type == OperandType.INST) &&
//						    			(inst.op2type == OperandType.CONST && parentInst.op2type == OperandType.CONST))	
//						    		{
//					    			
//						    		}
//					    		
//					    		// case #4: mul inst const
//					    		//          mul const inst
//					    		else if ((inst.op1type == OperandType.INST && parentInst.op1type == OperandType.CONST) &&
//						    			(inst.op2type == OperandType.CONST && parentInst.op2type == OperandType.INST))	
//						    		{
//					    			
//						    		}
//					    		
//					    		// case #5: mul const inst
//					    		//          mul const inst
//					    		else if ((inst.op1type == OperandType.CONST && parentInst.op1type == OperandType.CONST) &&
//						    			(inst.op2type == OperandType.INST && parentInst.op2type == OperandType.INST))	
//						    		{
//					    			
//						    		}
//					    		
//					    		// case #6: mul const inst
//					    		//          mul inst const
//					    		else if ((inst.op1type == OperandType.CONST && parentInst.op1type == OperandType.INST) &&
//						    			(inst.op2type == OperandType.INST && parentInst.op2type == OperandType.CONST))	
//						    		{
//					    			
//						    		}
					    		
					    		break;
					    	default:
					    		break;
					    	}
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
