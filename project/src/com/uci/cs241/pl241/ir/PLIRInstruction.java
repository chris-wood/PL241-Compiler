package com.uci.cs241.pl241.ir;

import java.util.ArrayList;
import java.util.List;

import com.uci.cs241.pl241.frontend.PLToken;

public class PLIRInstruction
{
	// Instruction descriptions
	public int id;
	public PLIRInstructionType opcode;
	public int i1;
	public PLIRInstruction op1;
	public OperandType op1type;
	public int i2;
	public PLIRInstruction op2;
	public OperandType op2type;
	
	// temporary value for combining constants
//	public PLIRInstructionGenerateType generate = PLIRInstructionGenerateType.NOW;
	public boolean generated = false;
	public ResultKind kind;
	public int tempVal;
	public int condcode;
	public int fixupLocation;
	public boolean wasIdent = false;
	public String origIdent = "";
	
	// Unique identifier for each instruction that's generated
//	public static ArrayList<PLIRInstruction> ssaInstructions = new ArrayList<PLIRInstruction>();
	
	public enum PLIRInstructionType 
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
		CONST, INST, NULL, ADDRESS
	};
	
	public PLIRInstruction()
	{
		// blank slate to be filled in by the parser or static factory methods in this class	
	}
	
	public PLIRInstruction(PLIRInstructionType opcode)
	{
		this.opcode = opcode;
		op1 = op2 = null;
		op1type = op2type = OperandType.NULL;
//		id = PLStaticSingleAssignment.addInstruction(this); 
		forceGenerate(); // always generate right away...
	}
	
	public PLIRInstruction(PLIRInstructionType opcode, PLIRInstruction singleOperand)
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
		forceGenerate(); // always generate right away...
	}
	
	public PLIRInstruction(PLIRInstructionType opcode, PLIRInstruction left, PLIRInstruction right)
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
			left.forceGenerate();
			right.forceGenerate();
		}
		
		if (opcode == PLIRInstructionType.CMP)
		{
			kind = ResultKind.COND;
			forceGenerate();
		}
		if (kind == ResultKind.CONST)
		{
			forceGenerate();
		}
	}
	
	public PLIRInstruction(PLIRInstructionType opcode, PLIRInstruction left, OperandType leftType, PLIRInstruction right, OperandType rightType)
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
			left.forceGenerate();
			right.forceGenerate();
		}
		
		if (opcode == PLIRInstructionType.CMP)
		{
			kind = ResultKind.COND;
			forceGenerate();
		}
		
		if (kind == ResultKind.CONST)
		{
			forceGenerate();
		}
	}
	
	public PLIRInstruction(PLIRInstructionType opcode, PLIRInstruction left, OperandType leftType, int right)
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
			left.forceGenerate();
		}
		
		if (opcode == PLIRInstructionType.CMP)
		{
			kind = ResultKind.COND;
			forceGenerate();
		}
		
		if (kind == ResultKind.CONST)
		{
			forceGenerate();
		}
	}
	
	public PLIRInstruction(PLIRInstructionType opcode, int left, int right)
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
			forceGenerate();
		}
	}
	
	public void forceGenerate()
	{
		if (kind == ResultKind.CONST)
		{
			generated = true;
		} 
		
		if (!generated)
		{
			kind = ResultKind.VAR;
			generated = true;
			id = PLStaticSingleAssignment.addInstruction(this);
		}
	}
	
	public void replaceLeftOperand(PLIRInstruction newLeft)
	{
		op1 = newLeft;
		op1type = OperandType.INST;
	}
	
	public void replaceRightOperand(PLIRInstruction newRight)
	{
		op2 = newRight;
		op2type = OperandType.INST;
	}
	
	public static PLIRInstruction create_cmp(PLIRInstruction left, PLIRInstruction right)
	{
		PLIRInstruction inst = new PLIRInstruction();
		inst.opcode = PLIRInstructionType.CMP;
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
		
		inst.forceGenerate();
		
		return inst;
	}
	
	public static PLIRInstruction create_phi(PLIRInstruction b1, PLIRInstruction b2)
	{
		PLIRInstruction inst = new PLIRInstruction();
		inst.opcode = PLIRInstructionType.PHI;
		inst.kind = ResultKind.VAR;
		
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
		
//		inst.id = PLStaticSingleAssignment.addInstruction(inst);
//		inst.generated = true;
		inst.forceGenerate();
		
		return inst;
	}
	
	public static PLIRInstruction create_branch(PLIRInstruction cmp, int token)
	{	
		PLIRInstruction inst = new PLIRInstruction();
		inst.op2 = null;
		inst.op2type = OperandType.CONST; // op2 will be an offset
		
		inst.op1type = OperandType.ADDRESS;
		inst.i1 = cmp.id;
		
		// We negate the logic in order to make fall-through work correctly
		switch (token)
		{
		case PLToken.eqlToken:
			inst.opcode = PLIRInstructionType.BNE;
			break;
		case PLToken.neqToken:
			inst.opcode = PLIRInstructionType.BEQ;
			break;
		case PLToken.lssToken:
			inst.opcode = PLIRInstructionType.BGE;
			break;
		case PLToken.geqToken:
			inst.opcode = PLIRInstructionType.BLT;
			break;
		case PLToken.leqToken:
			inst.opcode = PLIRInstructionType.BGT;
			break;
		case PLToken.gttToken:
			inst.opcode = PLIRInstructionType.BLE;
			break;
		}
		
		inst.id = PLStaticSingleAssignment.addInstruction(inst);
//		inst.forceGenerate();
		return inst;
	}
	
	public static PLIRInstruction create_BEQ(int loc)
	{	
		PLIRInstruction inst = new PLIRInstruction();
		inst.i2 = loc;
//		inst.i1 = cmp.fixupLocation;
		inst.op1 = inst.op2 = null;
		inst.op1type = OperandType.CONST; // op1 will be 0
		inst.op2type = OperandType.CONST; // op2 will be an offset
		inst.opcode = PLIRInstructionType.BEQ;
		inst.forceGenerate();
		return inst;
	}
	
	public static PLIRInstruction createInputInstruction()
	{
		return null;
	}
	
	public static PLIRInstruction createOutputNumInstruction(PLIRInstruction val)
	{
		return null;
	}
	
	public static PLIRInstruction createOutputLineInstruction()
	{
		return null;
	}
	
	@Override
	public String toString()
	{
		String s = "";
		
		switch (opcode)
		{
			case ADD:
				s = "add";
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
				s = "phi";
				break;
			// TODO: fill in the remaining ones here
		}
		
		switch (op1type)
		{
			case INST:
				s = s + " (" + op1.id + ")";
				break;
			case CONST:
				s = s + " #" + i1;
				break;
			case ADDRESS:
				s = s + " (" + i1 + ")";
				break;
		}
		
		switch (op2type)
		{
			case INST:
				s = s + " (" + op2.id + ")";
				break;
			case CONST:
				s = s + " #" + i2;
				break;
			case ADDRESS:
				s = s + " (" + i2 + ")";
				break;
		}
		
		return s;
	}
}
