package com.uci.cs241.pl241.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.uci.cs241.pl241.frontend.PLSymbolTable;
import com.uci.cs241.pl241.frontend.PLToken;
import com.uci.cs241.pl241.frontend.ParserException;

public class PLIRInstruction
{
	// Instruction descriptions
	public int id;
	public InstructionType opcode;
	public OperandType type;
	
	// First operand information
	public int i1;
	public PLIRInstruction op1;
	public OperandType op1type;
	public String op1name;
	
	// Second operand information
	public int i2;
	public PLIRInstruction op2;
	public OperandType op2type;
	public String op2address;
	public String op2name;
	
	public boolean isConstant = false;
	public boolean stale = true;
	public boolean killed = false;
	
	// Pointers to other instructions that need to be considered when this is generated
	public ArrayList<PLIRInstruction> dependents = new ArrayList<PLIRInstruction>();
	public ArrayList<PLIRInstruction> loadInstructions = new ArrayList<PLIRInstruction>();
	public ArrayList<String> guards = new ArrayList<String>();
	
	// boolean to flag if phi instruction is for IF or while
	public boolean whilePhi = false;
	public PLIRInstruction jumpInst;
	
	// Helper info
	public String dummyName;
	public int paramNumber;
	
	// assume this branch goes to the left unless otherwise specified...
	public int branchDirection = 1; 
	
	// Wrapping BB
	public PLIRBasicBlock block;
	
	// Flags to help in parsing
	public boolean globalMark = false; 
	public boolean isGlobalVariable = false;
	public boolean isReturn = false;
	public boolean leftProtected = false;
	public boolean leftWasPhiReplaced = false;
	public boolean rightProtected = false;
	public boolean rightWasPhiReplaced = false;
	
	// Register allocation information
	public int regNum;
	public int depth; // depth in CFG where introduced
	public int uses;
	public int cost;
	
	// 0 unless otherwise set, obviously
	public int tempPosition = 0;
	
	// Array information
	public boolean isArray = false;
	public ArrayList<PLIRInstruction> callOperands;
	public PLIRInstruction storedValue;
	
	// Temporary value for combining constants
	public boolean generated = false;
	public boolean overrideGenerate = false;
	public ResultKind kind;
	public int tempVal;
	public int condcode;
	public int fixupLocation;
	public boolean wasIdent = false;
	
	public HashMap<String, String> ident = new HashMap<String, String>();
	public String funcName;
	public HashMap<String, String> saveName = new HashMap<String, String>();
	
	// Elimination information
	public enum EliminationReason {CSE, DCR};
	public EliminationReason elimReason;
	public boolean isRemoved = false;
	public PLIRInstruction refInst = null;
	
	public void removeInstruction(EliminationReason reason, PLIRInstruction ref)
	{	
		isRemoved = true;
		elimReason = reason;
		refInst = ref;
		PLIRInstruction prev = ref;
		PLIRInstruction base = ref.refInst;
		while (base != null)
		{
			prev = base;
			if (base.refInst == null) base = null;
			else if (base.refInst.id == base.id) base = null;
			else base = base.refInst;
		}
		
		for (String scope : prev.ident.keySet())
		{
			ident.put(scope, prev.ident.get(scope));
		}
	}
	
	public enum InstructionType 
	{
		NEG,
		ADD,
		SUB,
		MUL,
		DIV,
		CMP,
		
		ADDA,
		LOAD,
		STORE,
		MOVE,
		PHI,
		
		PROC,
		FUNC,
		LOADPARAM,
		SAVEGLOBAL,
		GLOBAL,
		RETURN,
		
		END,
		
		BRA,
		BNE,
		BEQ,
		BLE,
		BLT,
		BGE,
		BGT,
		
		READ,
		WRITE,
		WLN
	};
	
	public enum ResultKind
	{
		CONST, VAR, COND
	};
	
	public enum OperandType
	{
		CONST, INST, NULL, ADDRESS, FP, BASEADDRESS, FUNC_PARAM, LOCALVARIABLE
	};
	
	// blank slate to be filled in by the parser or static factory methods in this class
	public PLIRInstruction(PLSymbolTable table)
	{	
	}
	
	public PLIRInstruction(PLSymbolTable table, InstructionType opcode)
	{
		this.opcode = opcode;
		op1 = op2 = null;
		op1type = op2type = OperandType.NULL;
		forceGenerate(table); // always generate right away...
	}
	
	public PLIRInstruction(PLSymbolTable table, InstructionType opcode, PLIRInstruction singleOperand)
	{
		this.opcode = opcode;
		op1 = singleOperand;
		op2 = null;
		op1type = OperandType.INST; // assume by default
		op2type = OperandType.NULL;
		
		if (singleOperand.kind == ResultKind.CONST)
		{
			op1type = OperandType.CONST;
			i1 = singleOperand.tempVal;
		}
		 
		forceGenerate(table); // always generate right away...
	}
	
	public PLIRInstruction(PLSymbolTable table, InstructionType opcode, PLIRInstruction left, PLIRInstruction right)
	{
		this.opcode = opcode;
		op1 = left;
		op2 = right;
		op1type = op2type = OperandType.INST;
		
		if (left.kind == ResultKind.CONST)
		{
			op1type = OperandType.CONST;
			i1 = left.tempVal;
		}
		if (right.kind == ResultKind.CONST)
		{
			op2type = OperandType.CONST;
			i2 = right.tempVal;
		}
		
		// Check to see if we can defer some more...
		if (left.kind == ResultKind.CONST && right.kind == ResultKind.CONST)
		{
			i1 = left.tempVal;
			i2 = right.tempVal;
			kind = ResultKind.CONST;
			switch (opcode)
			{
			case ADD:
				tempVal = i1 + i2;
				kind = ResultKind.CONST;
				break;
			case SUB:
				tempVal = i1 - i2;
				kind = ResultKind.CONST;
				break;
			case MUL:
				tempVal = i1 * i2;
				kind = ResultKind.CONST;
				break;
			case DIV:
				tempVal = i1 / i2;
				kind = ResultKind.CONST;
				break;
			}
		}
		else
		{
			left.forceGenerate(table);
			right.forceGenerate(table);
		}
		
		if (opcode == InstructionType.CMP)
		{
			kind = ResultKind.COND;
			forceGenerate(table);
		}
		if (kind == ResultKind.CONST)
		{
			forceGenerate(table);
		}
	}
	
	public PLIRInstruction(PLSymbolTable table, InstructionType opcode, PLIRInstruction left, OperandType leftType, PLIRInstruction right, OperandType rightType)
	{
		this.opcode = opcode;
		op1 = left;
		op1type = leftType;
		op2 = right;
		op2type = rightType;
		
		if (left.kind == ResultKind.CONST)
		{
			op1type = OperandType.CONST;
			i1 = left.tempVal;
		}
		if (right.kind == ResultKind.CONST)
		{
			op2type = OperandType.CONST;
			i2 = right.tempVal;
		}
		
		// handle computation...
		if (left.kind == ResultKind.CONST && right.kind == ResultKind.CONST)
		{
			i1 = left.tempVal;
			i2 = right.tempVal;
			kind = ResultKind.CONST;
			switch (opcode)
			{
			case ADD:
				tempVal = i1 + i2;
				kind = ResultKind.CONST;
				break;
			case SUB:
				tempVal = i1 - i2;
				kind = ResultKind.CONST;
				break;
			case MUL:
				tempVal = i1 * i2;
				kind = ResultKind.CONST;
				break;
			case DIV:
				tempVal = i1 / i2;
				kind = ResultKind.CONST;
				break;
			}
		}
		else
		{
			left.forceGenerate(table);
			right.forceGenerate(table);
		}
		
		if (opcode == InstructionType.CMP)
		{
			kind = ResultKind.COND;
			forceGenerate(table);
		}
		
		if (kind == ResultKind.CONST)
		{
			forceGenerate(table);
		}
	}
	
	public PLIRInstruction(PLSymbolTable table, InstructionType opcode, PLIRInstruction left, OperandType leftType, int right)
	{
		this.opcode = opcode;
		op1 = left;
		op1type = leftType;
		op2 = null;
		op2type = OperandType.CONST;
		
		i2 = right;
		if (left.kind == ResultKind.CONST)
		{
			op1type = OperandType.CONST;
			i1 = left.tempVal;
		}
		
		// handle computation...
		if (left.kind == ResultKind.CONST)
		{
			i1 = left.tempVal;
			kind = ResultKind.CONST;
			switch (opcode)
			{
			case ADD:
				tempVal = i1 + right;
				kind = ResultKind.CONST;
				break;
			case SUB:
				tempVal = i1 - right;
				kind = ResultKind.CONST;
				break;
			case MUL:
				tempVal = i1 * right;
				kind = ResultKind.CONST;
				break;
			case DIV:
				tempVal = i1 / right;
				kind = ResultKind.CONST;
				break;
			}
		}
		else
		{
			left.forceGenerate(table);
		}
		
		if (opcode == InstructionType.CMP)
		{
			kind = ResultKind.COND;
			forceGenerate(table);
		}
		
		if (kind == ResultKind.CONST)
		{
			forceGenerate(table);
		}
	}
	
	public PLIRInstruction(PLSymbolTable table, InstructionType opcode, int left, int right)
	{
		this.opcode = opcode;
		op1 = op2 = null;
		op1type = op2type = OperandType.CONST;
		i1 = left;
		i2 = right;
		
		switch (opcode)
		{
		case ADD:
			tempVal = i1 + i2;
			kind = ResultKind.CONST;
			break;
		case SUB:
			tempVal = i1 - i2;
			kind = ResultKind.CONST;
			break;
		case MUL:
			tempVal = i1 * i2;
			kind = ResultKind.CONST;
			break;
		case DIV:
			tempVal = i1 / i2;
			kind = ResultKind.CONST;
			break;
		}

		if (kind != ResultKind.CONST)
		{
			forceGenerate(table);
		}
	}
	
	public void forceGenerate(PLSymbolTable table, int loc)
	{
		if (kind == ResultKind.CONST)
		{
			generated = true;
		} 
		if (!generated)
		{
			kind = ResultKind.VAR;
			generated = true;
			id = PLStaticSingleAssignment.injectInstruction(this, table, loc);
		}
	}
	
	public PLIRInstruction evaluate(int placement, int offset, PLIRInstruction newOp, PLSymbolTable table, ArrayList<PLIRInstruction> visitedInsts, int branch)
	{
		if (op1 != null && branch == 1)
		{
			String thisIdent = op1.ident.get(table.getCurrentScope());
			String thatIdent = newOp.ident.get(table.getCurrentScope());
			if (thisIdent != null && thatIdent != null && thisIdent.equals(thatIdent))
			{
				newOp.overrideGenerate = true;
				newOp.tempPosition += offset;
				newOp.forceGenerate(table);
				op1type = OperandType.INST;
				op1 = newOp;
			}
			else if (visitedInsts.contains(op1) == false)
			{
				visitedInsts.add(op1);
				op1 = op1.evaluate(placement, offset, newOp, table, visitedInsts, branch);
				op1type = op1.type == OperandType.FUNC_PARAM ? OperandType.INST : op1.type;
			}
		}
			
		if (op2 != null && !(newOp.opcode == InstructionType.PHI && opcode == InstructionType.PHI && branch == 2))
		{
			String thisIdent = op2.ident.get(table.getCurrentScope());
			String thatIdent = newOp.ident.get(table.getCurrentScope());
			if (thisIdent != null && thatIdent != null && thisIdent.equals(thatIdent))
			{
				newOp.overrideGenerate = true;
				newOp.tempPosition += offset;
				newOp.forceGenerate(table);
				op2type = OperandType.INST;
				op2 = newOp;
			}
			else if (visitedInsts.contains(op2) == false)
			{
				visitedInsts.add(op2);
				op2 = op2.evaluate(placement, offset, newOp, table, visitedInsts, branch);
				op2type = op2.type == OperandType.FUNC_PARAM ? OperandType.INST : op2.type;
			}
		}
			
		// re-evaluate the node
		if (op1 != null)
		{
			if (op1.kind == ResultKind.CONST || op1type == OperandType.CONST)
			{
				op1type = OperandType.CONST;
				i1 = op1.tempVal;
			}
		}
		if (op2 != null)
		{
			if (op2.kind == ResultKind.CONST || op2type == OperandType.CONST)
			{
				op2type = OperandType.CONST;
				i2 = op2.tempVal;
			}
		}
		
		// handle computation...
		if (op1 != null && op2 != null)
		{
			if (op1.kind == ResultKind.CONST && op2.kind == ResultKind.CONST)
			{
				i1 = op1.tempVal;
				i2 = op2.tempVal;
				kind = ResultKind.CONST;
				switch (opcode)
				{
				case ADD:
					tempVal = i1 + i2;
					kind = ResultKind.CONST;
					break;
				case SUB:
					tempVal = i1 - i2;
					kind = ResultKind.CONST;
					break;
				case MUL:
					tempVal = i1 * i2;
					kind = ResultKind.CONST;
					break;
				case DIV:
					tempVal = i1 / i2;
					kind = ResultKind.CONST;
					break;
				}
			}
			else if (op1.kind == ResultKind.CONST && op2.opcode != InstructionType.GLOBAL) // right is not constant
			{
				type = OperandType.INST;
				kind = ResultKind.VAR;
				
				op2.overrideGenerate = true;
				op2.forceGenerate(table);
				
				int pos = placement < 0 ? tempPosition + offset : placement;
				this.tempPosition = pos;
				op1.overrideGenerate = true;
				forceGenerate(table);
			}
			else if (op2.kind == ResultKind.CONST && op1.opcode != InstructionType.GLOBAL)
			{
				type = OperandType.INST;
				kind = ResultKind.VAR;
				
				op1.overrideGenerate = true;
				op1.forceGenerate(table);
				
				int pos = placement < 0 ? tempPosition + offset : placement;
				this.tempPosition = pos;
				this.overrideGenerate = true;
				forceGenerate(table);
			}
			else if (op2.opcode != InstructionType.GLOBAL && op1.opcode != InstructionType.GLOBAL)
			{
				type = OperandType.INST;
				kind = ResultKind.VAR;
				
				op1.overrideGenerate = true;
				op1.forceGenerate(table);
				op2.overrideGenerate = true;
				op2.forceGenerate(table);
				
				int pos = placement < 0 ? tempPosition + offset : placement;
				this.tempPosition = pos;
				op1.overrideGenerate = true;
				forceGenerate(table);
			}
		}
		
		if (opcode == InstructionType.CMP)
		{
			kind = ResultKind.COND;
			forceGenerate(table);
		}
		
		if (kind == ResultKind.CONST)
		{
			forceGenerate(table);
		}
		
		return this;
	}
	
	public boolean checkForReplace(PLIRInstruction replacement, String var, String scope)
	{
		for (int ii = 0; ii < dependents.size(); ii++)
		{
			PLIRInstruction dependent = dependents.get(ii);
			if (dependent.ident.get(scope) != null && dependent.ident.get(scope).equals(var))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public void forceGenerate(PLSymbolTable table)
	{
		if (kind == ResultKind.CONST)
		{
			generated = true;
		}
		
		if ((!generated || overrideGenerate) && id == 0)
		{
			kind = ResultKind.VAR;
			generated = true;
			overrideGenerate = false;
			if (tempPosition > 0)
			{
				id = PLStaticSingleAssignment.injectInstruction(this, table, tempPosition);
			}
			else
			{
				id = PLStaticSingleAssignment.addInstruction(table, this);
			}
		}
	}
	
	public boolean replaceLeftOperand(PLIRInstruction newLeft)
	{
		if (!leftProtected)
		{
			op1 = newLeft;
			op1type = OperandType.INST;
			leftProtected = true;
			
			if (opcode == InstructionType.PHI && newLeft.opcode == InstructionType.PHI)
			{
				leftWasPhiReplaced = true;
			}
			return true;
		}
		return false;
	}
	
	public boolean replaceRightOperand(PLIRInstruction newRight)
	{
		if (!rightProtected && !(leftWasPhiReplaced && opcode == InstructionType.PHI))
		{
			op2 = newRight;
			op2type = OperandType.INST;
			rightProtected = true;
			return true;
		}
		return false;
	}
	
	public boolean isNotLiveInstruction()
	{
		return false;
	}
	
	public static PLIRInstruction create_cmp(PLSymbolTable table, PLIRInstruction left, PLIRInstruction right)
	{
		PLIRInstruction inst = new PLIRInstruction(table);
		inst.opcode = InstructionType.CMP;
		inst.kind = ResultKind.VAR;
		
		if (left.kind == ResultKind.CONST)
		{
			inst.op1 = left;
			inst.op1type = OperandType.CONST;
			inst.i1 = left.tempVal;
		}
		else
		{
			inst.op1 = left;
			inst.op1type = OperandType.INST;
		}
		
		if (right.kind == ResultKind.CONST)
		{
			inst.op2 = right;
			inst.op2type = OperandType.CONST;
			inst.i2 = right.tempVal;
		}
		else
		{
			inst.op2 = right;
			inst.op2type = OperandType.INST;
		}
		
		inst.forceGenerate(table);
		
		return inst;
	}
	
	public static PLIRInstruction create_phi(PLSymbolTable table, String var, PLIRInstruction b1, PLIRInstruction b2, int loc, boolean generate) throws ParserException
	{
		PLIRInstruction inst = new PLIRInstruction(table);
		inst.opcode = InstructionType.PHI;
		inst.kind = ResultKind.VAR;
		inst.ident.put(table.getCurrentScope(), var);
		inst.saveName.put(table.getCurrentScope(), var);
		
		if (b1.kind == ResultKind.CONST)
		{
			inst.op1 = b1;
			inst.op1type = OperandType.CONST;
			inst.i1 = b1.tempVal;
		}
		else
		{
			inst.op1 = b1;
			inst.op1type = OperandType.INST;
		}
		
		if (b2.kind == ResultKind.CONST)
		{
			inst.op2 = b2;
			inst.op2type = OperandType.CONST;
			inst.i2 = b2.tempVal;
		}
		else
		{
			inst.op2 = b2;
			inst.op2type = OperandType.INST;
		}
		
		inst.isRemoved = false;
		if (generate)
		{
			inst.forceGenerate(table, loc);
			inst.id = loc + 1;
		}
		inst.type = OperandType.INST;
		
		return inst;
	}
	
	public static PLIRInstruction create_branch(PLSymbolTable table, PLIRInstruction cmp, int token)
	{	
		PLIRInstruction inst = new PLIRInstruction(table);
		inst.op2 = null;
		inst.op2type = OperandType.CONST; // op2 will be an offset
		
		inst.op1type = OperandType.ADDRESS;
		inst.op1 = cmp;
		
		// We negate the logic in order to make fall-through work correctly
		switch (token)
		{
		case PLToken.eqlToken:
			inst.opcode = InstructionType.BNE;
			break;
		case PLToken.neqToken:
			inst.opcode = InstructionType.BEQ;
			break;
		case PLToken.lssToken:
			inst.opcode = InstructionType.BGE;
			break;
		case PLToken.geqToken:
			inst.opcode = InstructionType.BLT;
			break;
		case PLToken.leqToken:
			inst.opcode = InstructionType.BGT;
			break;
		case PLToken.gttToken:
			inst.opcode = InstructionType.BLE;
			break;
		}
		
		inst.forceGenerate(table);
		return inst;
	}
	
	public static PLIRInstruction create_BEQ(PLSymbolTable table, int loc)
	{	
		PLIRInstruction inst = new PLIRInstruction(table);
		inst.i2 = loc;
		inst.op1 = inst.op2 = null;
		inst.op1type = OperandType.CONST; // op1 will be 0
		inst.op2type = OperandType.CONST; // op2 will be an offset
		inst.opcode = InstructionType.BEQ;
		inst.forceGenerate(table);
		return inst;
	}
	
	public static PLIRInstruction create_call(PLSymbolTable table, String funcName, boolean isFunc, ArrayList<PLIRInstruction> ops)
	{
		PLIRInstruction newInst = new PLIRInstruction(table); 
		newInst.funcName = funcName;
		newInst.callOperands = new ArrayList<PLIRInstruction>();
		for (PLIRInstruction inst : ops)
		{
			newInst.callOperands.add(inst);
		}
		
		if (isFunc)
		{
			newInst.opcode = InstructionType.FUNC;
			newInst.type = OperandType.INST;
		}
		else
		{
			newInst.opcode = InstructionType.PROC;
			newInst.type = OperandType.NULL;
		}
		
		// Generate the special instruction now
		newInst.overrideGenerate = true;
		newInst.forceGenerate(table);
		
		return newInst;
	}
	
	public static boolean isBranch(PLIRInstruction inst)
	{
		switch (inst.opcode)
		{
			case BNE:
			case BEQ:
			case BGE:
			case BLT:
			case BGT:
			case BLE:
				return true;
			default:
				return false;
		}
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o == null) return false;
	    if (o == this) return true;
	    if (!(o instanceof PLIRInstruction)) return false;
	    PLIRInstruction other = (PLIRInstruction)o;
	    
	    // the wonderful benefit of SSA :-)
	    if (this.id == other.id)
	    {
	    	return true;
	    }
	    else
	    {
	    	return false;
	    }
	}
	
	@Override
	public String toString()
	{
		String s = "[" + id + "] ";
		
		// Short-circuit for instructions that have already been deleted
		if (this.isRemoved && this.refInst != null)
		{
			return "(" + this.refInst.id + ")";
		}
		
		if (opcode != null)
		{
			switch (opcode)
			{
				case ADD:
					s = "add";
					break;
				case SUB:
					s = "sub";
					break;
				case MUL:
					s = "mul";
					break;
				case DIV:
					s = "div";
					break;
				case ADDA:
					s = "adda";
					break;
				case WRITE:
					s = "write";
					break;
				case READ:
					s = "read";
					break;
				case WLN:
					s = "wln";
					break;
				case END:
					s = "end";
					break;
				case CMP:
					s = "cmp";
					break;
				case BEQ:
					s = "beq";
					break;
				case BNE:
					s = "bne";
					break;
				case BLT:
					s = "blt";
					break;
				case BGE:
					s = "bge";
					break;
				case BLE:
					s = "ble";
					break;
				case BGT:
					s = "bgt";
					break;
				case PHI:
					s = "phi(" + saveName + ")";
					break;
				case FUNC:
					s = "func";
					break;
				case PROC:
					s = "proc";
					break;
				case LOADPARAM:
					s = "loadparam " + paramNumber;
					break;
				case SAVEGLOBAL:
					s = "saveglobal";
					break;
				case LOAD:
					s = "load";
					break;
				case STORE:
					s = "store";
					break;
				case GLOBAL:
					s = "global";
					break;
				case RETURN:
					s = "return";
					break;
			}
			
			if (callOperands != null)
			{
				s = s + " " + funcName;
				
				// Append the operands
				for (PLIRInstruction operand : callOperands)
				{
					switch (operand.type)
					{
						case INST:
							PLIRInstruction op = operand;
							while (op.isRemoved && op.refInst.id != this.id)
							{
								op = op.refInst;
							}
							s = s + " (" + op.id + ")";
							break;
						case CONST:
							s = s + " #" + operand.tempVal;
							break;
						case ADDRESS:
							s = s + " (" + operand.id + ")";
							break;
						case FP:
							s = s + " FP";
							break;
					}
				}
			}
			else if (op2type == null && op1type != null) // single operand instruction
			{
				switch (op1type)
				{
					case INST:
						PLIRInstruction op = op1;
						while (op.isRemoved && op.id != this.id)
						{
							op = op.refInst;
						}
						s = s + " (" + op.id + ")";
						break;
					case CONST:
						s = s + " #" + i1;
						break;
					case ADDRESS:
						s = s + " (" + op1.id + ")";
						break;
					case FP:
						s = s + " FP";
						break;
				}
			}
			else if (op2type != null && op1type != null) // this is a normal IR instruction, so render it as usual
			{
				switch (op1type)
				{
					case INST:
						PLIRInstruction op = op1;
						while (op.isRemoved && op.id != this.id && op.refInst.id != this.id)
						{
							op = op.refInst;
						}
						s = s + " (" + op.id + ")";
						break;
					case CONST:
						s = s + " #" + i1;
						break;
					case ADDRESS:
						s = s + " (" + op1.id + ")";
						break;
					case FP:
						s = s + " FP";
						break;
				}
				
				switch (op2type)
				{
					case INST:
						PLIRInstruction op = op2;
						while (op.isRemoved && op.id != this.id)
						{
							op = op.refInst;
						}
						s = s + " (" + op.id + ")";
						break;
					case CONST:
						s = s + " #" + i2;
						break;
					case ADDRESS:
						s = s + " (" + op2.id + ")";
						break;
					case BASEADDRESS:
						s = s + " " + op2address;
						break;
				}
			}
		}
		
		return s;
	}
}
