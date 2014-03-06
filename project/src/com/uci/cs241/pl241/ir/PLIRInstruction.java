package com.uci.cs241.pl241.ir;

import java.util.ArrayList;
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
	
	// used to indicate if this instruction is part of a designator
//	public boolean isDesig = false;
	
	// boolean to flag if phi instruction is for IF or while
	public boolean whilePhi = false;
	public PLIRInstruction jumpInst;
	
	// Helper info
	public String dummyName;
	public int paramNumber;
	
	public int branchDirection = 1; // assume this branch goes to the left unless otherwise specified...
	
	// Wrapping BB
	public PLIRBasicBlock block;
	
	public boolean globalMark = false; 
	public boolean isGlobalVariable = false;
	public boolean isReturn = false;
	
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
	public String origIdent = "";
	public String funcName;
	public String saveName = "";
	
	// Elimination information
	public enum EliminationReason {CSE, DCR};
	public EliminationReason elimReason;
	public boolean isRemoved = false;
	public PLIRInstruction refInst = null;
	
	public void removeInstruction(EliminationReason reason, PLIRInstruction ref)
	{
		PLStaticSingleAssignment.displayInstructions();
		System.err.println("CSE: " + this.id + " referencing " + ref.id);
		
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
		this.origIdent = prev.origIdent;
		
		System.err.println("CSE: " + this.id + " referencing " + ref.id);
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
	
	public PLIRInstruction(PLSymbolTable table)
	{
		// blank slate to be filled in by the parser or static factory methods in this class	
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
//		if (singleOperand.wasIdent)
//		{
//			System.err.println("WAS AN IDENTIFIER!!!: " + singleOperand.origIdent);
//			op1type = OperandType.INST;
//			this.wasIdent = true;
//			this.origIdent = singleOperand.origIdent;
//		}
		
//		id = PLStaticSingleAssignment.addInstruction(this); 
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
			System.err.println("left operand is a constant: " +  left.toString());
			op1type = OperandType.CONST;
			i1 = left.tempVal;
		}
		if (right.kind == ResultKind.CONST)
		{
//			System.err.println("right operand is a constant");
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
	
	public void forceGenerate(PLSymbolTable table)
	{
		if (kind == ResultKind.CONST)
		{
			generated = true;
		}
		
		if ((!generated || overrideGenerate) && (id == 0))
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
	
	private boolean leftProtected = false;
	public void replaceLeftOperand(PLIRInstruction newLeft)
	{
		if (!leftProtected)
		{
			op1 = newLeft;
			op1type = OperandType.INST;
			leftProtected = true;
		}
	}
	
	private boolean rightProtected = false;
	public void replaceRightOperand(PLIRInstruction newRight)
	{
		if (!rightProtected)
		{
			op2 = newRight;
			op2type = OperandType.INST;
			rightProtected = true;
		}
	}
	
	public boolean isNotLiveInstruction()
	{
		return false;
//		switch (opcode)
//		{
//		case BEQ:
//		case BNE:
//		case BLT:
//		case BGT:
//		case BGE:
//		case BLE:
////		case MOVE:
////		case CMP:
//			return true;
//		default:
//			return false;
//		}
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
	
	public static PLIRInstruction create_phi(PLSymbolTable table, PLIRInstruction b1, PLIRInstruction b2, int loc) throws ParserException
	{
		PLIRInstruction inst = new PLIRInstruction(table);
		inst.opcode = InstructionType.PHI;
		inst.kind = ResultKind.VAR;
		
//		if (!(b1.origIdent.equals(b2.origIdent)))
//		{
//			System.out.println("b1 = " + b1.origIdent);
//			System.out.println("b2 = " + b2.origIdent);
//			throw new ParserException("Phi instruction pairs don't refer to the same ident");
//		}
		inst.origIdent = b1.origIdent; // use either b1 or b2 origIndent, they will match at this point
		
		if (b1.kind == ResultKind.CONST)
		{
			inst.op1 = null;
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
			inst.op2 = null;
			inst.op2type = OperandType.CONST;
			inst.i2 = b2.tempVal;
		}
		else
		{
			inst.op2 = b2;
			inst.op2type = OperandType.INST;
		}
		
		inst.isRemoved = false;
		inst.forceGenerate(table, loc);
		inst.type = OperandType.INST;
		
		// override the location
		inst.id = loc + 1;
		
		return inst;
	}
	
	public static PLIRInstruction create_branch(PLSymbolTable table, PLIRInstruction cmp, int token)
	{	
		PLIRInstruction inst = new PLIRInstruction(table);
		inst.op2 = null;
		inst.op2type = OperandType.CONST; // op2 will be an offset
		
		inst.op1type = OperandType.ADDRESS;
		inst.op1 = cmp;
//		inst.i1 = cmp.id;
		
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
		
//		inst.id = PLStaticSingleAssignment.addInstruction(inst);
		inst.forceGenerate(table);
		return inst;
	}
	
	public static PLIRInstruction create_BEQ(PLSymbolTable table, int loc)
	{	
		PLIRInstruction inst = new PLIRInstruction(table);
		inst.i2 = loc;
//		inst.i1 = cmp.fixupLocation;
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
		
		if (opcode == null)
		{
			System.err.println("Dummy placeholder variable encountered: " + dummyName);
		}
		else
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
					s = "phi(" + origIdent + ")";
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
					if (operand.type == null)
					{
						System.err.println(this.opcode);
						System.err.println(this.id);
					}
					switch (operand.type)
					{
						case INST:
							PLIRInstruction op = operand;
							while (op.isRemoved && op.refInst.id != this.id)
							{
//								System.err.println("recursing to: " + op.refInst);
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
//						case BASEADDRESS:
//							s = s + " " + operand.
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
//							System.err.println("recursing from " + this.id + " to: " + op.refInst);
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
//							System.err.println("recursing from " + this.id + " to: " + op.refInst.id);
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
//							System.err.println("recursing from " + this.id + " to: " + op.refInst);
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
