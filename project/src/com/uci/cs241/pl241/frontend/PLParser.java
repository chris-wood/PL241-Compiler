package com.uci.cs241.pl241.frontend;

import java.io.IOException;
import java.util.ArrayList;

import com.uci.cs241.pl241.ir.PLIRBasicBlock;
import com.uci.cs241.pl241.ir.PLIRInstruction;
import com.uci.cs241.pl241.ir.PLIRInstruction.OperandType;
import com.uci.cs241.pl241.ir.PLIRInstruction.PLIRInstructionType;
import com.uci.cs241.pl241.ir.PLStaticSingleAssignment;

public class PLParser
{
	// NODES
	// SUBCLASSES: assignment, func/proc, array, block, designator, functionblock, load/store, main, param 


	
	// Current symbol/token values used for parsing
	private String sym;
	private int toksym;
	
	// Other necessary things
	private PLSymbolTable symTable;
	private PLSymbolTable scope;
	
	public PLParser()
	{
		symTable = new PLSymbolTable();
	}
	
	public void SyntaxError(String msg) throws PLSyntaxErrorException
	{
		throw new PLSyntaxErrorException(msg + ": " + sym);
	}
	
	private void advance(PLScanner in)
	{
		try
		{
			in.next();
			toksym = in.sym;
			sym = in.symstring;
		}
		catch (PLSyntaxErrorException e)
		{
			e.printStackTrace();
		}
	}
	
	// this is what's called - starting with the computation non-terminal
	public PLIRBasicBlock parse(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		advance(in);
		if (toksym == PLToken.mainToken)
		{
			advance(in);
			scope = new PLSymbolTable();
			scope.pushNewScope("main");
			
			if (toksym == PLToken.varToken || toksym == PLToken.arrToken)
			{
				result = this.parse_varDecl(in);
			}
			
			if (toksym == PLToken.funcToken || toksym == PLToken.procToken)
			{
				result = this.parse_funcDecl(in);
			}
			

			if (toksym == PLToken.openBraceToken)
			{
				// eat the sequence of statements that make up the computation
				advance(in);
				result = parse_statSequence(in);
				
				// parse the close of the computation
				if (toksym == PLToken.closeBraceToken)
				{
					advance(in);
					if (toksym == PLToken.periodToken)
					{
						PLIRInstruction inst = new PLIRInstruction(PLIRInstructionType.END);
						PLStaticSingleAssignment.displayInstructions();
					}
					else
					{
						SyntaxError("'.' missing in computation non-terminal");
					}
				}
				else
				{
					SyntaxError("'}' missing in computation non-terminal");
				}
			}
			else
			{
				SyntaxError("'{' missing in computation non-terminal");
			}
		}
		else
		{
			SyntaxError("Computation does not begin with main keyword");
		}
		
		return result;
	}

	// non-terminals
	private PLIRBasicBlock parse_ident(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction inst = scope.getCurrentValue(sym);
		advance(in);
		PLIRBasicBlock block = new PLIRBasicBlock();
		block.instructions.add(inst);
		return block;
	}

	private PLIRBasicBlock parse_number(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{	
		// Result: add 0 tokenValue
		PLIRInstruction li = new PLIRInstruction(PLIRInstructionType.ADD, 0, Integer.parseInt(sym)); 
		advance(in);
		PLIRBasicBlock block = new PLIRBasicBlock();
		block.instructions.add(li);
		return block;
	}

	private PLIRBasicBlock parse_designator(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		
		result = parse_ident(in);
		while (toksym == PLToken.openBracketToken)
		{
			advance(in);
			result = parse_expression(in);
			if (toksym != PLToken.closeBracketToken)
			{
				SyntaxError("']' missing from designator non-terminal.");
			}
			advance(in);
		}
		
		return result;
	}

	private PLIRBasicBlock parse_factor(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock factor = null;
		if (toksym == PLToken.openParenToken)
		{
			advance(in);
			
			// Parse the inner expression and make sure the parens match afterwards
			factor = parse_expression(in);
			if (toksym != PLToken.closeParenToken)
			{
				SyntaxError("')' missing from factor non-terminal");
			}
			
			// They do, so eat the token and advance
			advance(in);
		}
		else if (toksym == PLToken.callToken)
		{
			factor = parse_expression(in);
		}
		else if (toksym == PLToken.number)
		{
			factor = parse_number(in);
		}
		else if (toksym == PLToken.ident)
		{
			factor = parse_designator(in); 
		}
		else
		{
			SyntaxError("Invalid case in parse_factor");
		}
		
		return factor;
	}

	private PLIRBasicBlock parse_term(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock factor = parse_factor(in);
		if (toksym == PLToken.timesToken || toksym == PLToken.divToken)
		{
////			PLIRBasicBlock termNode = new PLIRBasicBlock(toksym);
//			PLIRBasicBlock termNode = new PLIRBasicBlock();
//			advance(in);
//			
//			// Now parse the right factor and build the resulting node
//			PLIRBasicBlock rightNode = parse_factor(in);
//			
//			
////			termNode.setLeft(factor);
////			termNode.setRight(rightNode);
//			return termNode;
			
			int operator = toksym;
//			System.out.println("here now");
			PLIRBasicBlock termNode = new PLIRBasicBlock();
			advance(in);
			
			// Now parse the right term and build the resulting node 
			PLIRBasicBlock rightNode = parse_term(in);
			
			// Form the expression instruction
			PLIRInstruction leftValue = factor.instructions.get(factor.instructions.size() - 1);
			PLIRInstruction rightValue = rightNode.instructions.get(rightNode.instructions.size() - 1);
			PLIRInstructionType opcode = operator == PLToken.timesToken ? PLIRInstructionType.MUL : PLIRInstructionType.DIV;
			
			
			PLIRInstruction termInst = new PLIRInstruction(opcode, leftValue, rightValue);
			termInst.forceGenerate();
			termNode.addInstruction(termInst);
			
//			exprNode.setLeft(term);
//			exprNode.setRight(rightNode);
			return termNode;
		}
		else
		{
			return factor;
		}
	}

	private PLIRBasicBlock parse_expression(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock term = parse_term(in);
		if (toksym == PLToken.plusToken || toksym == PLToken.minusToken)
		{
//			PLIRBasicBlock exprNode = new PLIRBasicBlock(toksym);
			int operator = toksym;
//			System.out.println("here now");
			PLIRBasicBlock exprNode = new PLIRBasicBlock();
			advance(in);
			
			// Now parse the right term and build the resulting node 
			PLIRBasicBlock rightNode = parse_term(in);
			
			// Form the expression instruction
			PLIRInstruction leftValue = term.instructions.get(term.instructions.size() - 1);
			PLIRInstruction rightValue = rightNode.instructions.get(rightNode.instructions.size() - 1);
			PLIRInstructionType opcode = operator == PLToken.plusToken ? PLIRInstructionType.ADD : PLIRInstructionType.SUB;
			
			
			PLIRInstruction exprInst = new PLIRInstruction(opcode, leftValue, rightValue);
			exprInst.forceGenerate();
			exprNode.addInstruction(exprInst);
			
//			exprNode.setLeft(term);
//			exprNode.setRight(rightNode);
			return exprNode;
		}
		else
		{
			for (PLIRInstruction inst : term.instructions)
			{
				inst.forceGenerate();
			}
			return term;
		}
	}

	private PLIRBasicBlock parse_relation(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock left = parse_expression(in);
		if (PLToken.isRelationalToken(toksym) == false)
		{
			SyntaxError("Invalid relational character");
		}
		
		// TOOD: save the relational code
		int condcode = toksym;
		
		// Eat the relational token
		advance(in);
		
		PLIRBasicBlock right = parse_expression(in);
		
		// Build the comparison instruction
		PLIRInstruction leftInst = left.instructions.get(left.instructions.size() - 1);
		PLIRInstruction rightInst = right.instructions.get(right.instructions.size() - 1);
		PLIRInstruction inst = new PLIRInstruction(PLIRInstructionType.CMP, leftInst, rightInst);
		inst.condcode = condcode;
		inst.fixupLocation = 0;
		
		// Create the relation block containing the instruction
		PLIRBasicBlock relation = new PLIRBasicBlock();
		relation.addInstruction(leftInst);
		relation.addInstruction(rightInst);
		relation.addInstruction(inst);
		
		return relation;
	}

	private PLIRBasicBlock parse_assignment(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		
		if (toksym != PLToken.letToken)
		{
			throw new PLSyntaxErrorException("assignment");
		}
		else
		{
			advance(in);
			String varName = sym;
			
			// Check to make sure these variables are in scope before being used
			if (scope.isVarInScope(varName))
			{
				result = parse_designator(in);
				if (toksym == PLToken.becomesToken)
				{
					advance(in);
					result = parse_expression(in);
					
					// The last instruction added to the BB is the one that holds the value for this assignment
					PLIRInstruction inst = result.instructions.get(result.instructions.size() - 1);
					scope.updateSymbol(varName, inst); // (SSA ID) := expr
				}
				else
				{
					SyntaxError("'<-' character missing in assignment statement");
				}
			}
			else
			{
				SyntaxError("Variable " + varName + " not in current scope: " + scope.getCurrentScope());
			}
		}
		
		return result;
	}

	private PLIRBasicBlock parse_funcCall(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		
		if (toksym != PLToken.callToken)
		{
			SyntaxError("Invalid start to funcCall non-terminal");
		}
		else
		{
			advance(in);
			
			// Save function identifier in case it's a predefined function with a special instruction
			String funcName = sym;
			result = parse_ident(in);
			
			if (toksym == PLToken.openParenToken)
			{
				advance(in);
				
				if (toksym != PLToken.semiToken && toksym != PLToken.elseToken
						&& toksym != PLToken.fiToken && toksym != PLToken.odToken 
						&& toksym != PLToken.closeBraceToken && toksym != PLToken.closeParenToken)
				{
					result = parse_expression(in);
					
					// Special case for single parameter
					if (toksym == PLToken.commaToken && funcName.equals("OutputNum"))
					{
						SyntaxError("Function OutputNum only takes a single parameter");
					}
					else if (toksym != PLToken.commaToken && funcName.equals("OutputNum"))
					{
						PLIRInstruction inst = new PLIRInstruction(PLIRInstructionType.WRITE, result.instructions.get(result.instructions.size() - 1));
						// TODO: this is the instruction that outputs the number, but where does it go in the BB?
					}
					else
					{
						// Read in the rest of the instructions
						while (toksym == PLToken.commaToken)
						{
							advance(in);
							result = parse_expression(in);
						}
					}
				}
				else if (toksym == PLToken.closeParenToken && funcName.equals("InputNum"))
				{
					PLIRInstruction inst = new PLIRInstruction(PLIRInstructionType.READ);
					// TODO: this is the instruction that outputs the number, but where does it go in the BB?
				}
				else if (toksym == PLToken.closeParenToken && funcName.equals("OutputNewLine"))
				{
					PLIRInstruction inst = new PLIRInstruction(PLIRInstructionType.WLN);
					// TODO: this is the instruction that outputs the number, but where does it go in the BB?
				}
			}
			
			// Eat the last token and proceed
			advance(in);
		}
		
		return result;
	}

	private PLIRBasicBlock parse_whileStatement(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		
		if (toksym != PLToken.whileToken)
		{
			SyntaxError("Invalid start to ifStatement non-terminal");
		}
		else
		{
			advance(in);
			result = parse_relation(in);
			
			if (toksym != PLToken.doToken)
			{
				SyntaxError("Missing 'do' in while statement");
			}
			
			advance(in);
			result  = parse_statSequence(in);
			
			if (toksym != PLToken.odToken)
			{
				SyntaxError("Missing 'od' in while statement");
			}
			
			advance(in);
		}
		
		return result;
	}

	private PLIRBasicBlock parse_returnStatement(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		
		if (toksym != PLToken.returnToken)
		{
			SyntaxError("Invalid start to ifStatement non-terminal");
		}
		else
		{
			advance(in);
			if (toksym != PLToken.semiToken && toksym != PLToken.elseToken
					&& toksym != PLToken.fiToken && toksym != PLToken.odToken && toksym != PLToken.closeBraceToken)
			{
				result = parse_expression(in);
			}
		}
		
		return result;
	}

	private PLIRBasicBlock parse_statement(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		
		if (toksym == PLToken.letToken)
		{
			result = parse_assignment(in);
		}
		else if (toksym == PLToken.callToken)
		{
			result = parse_funcCall(in);
		}
		else if (toksym == PLToken.ifToken)
		{
			PLIRInstruction follow = new PLIRInstruction();
			follow.fixupLocation = 0;
			
			advance(in);
			
			PLIRBasicBlock entry = parse_relation(in);
			PLIRInstruction x = entry.instructions.get(entry.instructions.size() - 1);
			CondNegBraFwd(x);
			
			// Check for follow through branch
			if (toksym != PLToken.thenToken)
			{
				SyntaxError("Missing then clause");
			}
			advance(in);
			
			// Parse the follow-through and add it as the first child of the entry block
			PLIRBasicBlock thenBlock = parse_statSequence(in);
			entry.children.add(thenBlock);
			
			// Create the artificual join node and make it a child of the follow-through branch,
			// and also the exit/join block of the entry block
			PLIRBasicBlock joinNode = new PLIRBasicBlock();
			thenBlock.children.add(joinNode);
			entry.exitNode = entry.joinNode = joinNode;
			
			// Check for an else branch
			if (toksym == PLToken.elseToken)
			{
				UnCondBraFwd(follow);
				Fixup(x.fixupLocation);
				advance(in);
				
				// Parse the else block
				PLIRBasicBlock elseBlock = parse_statSequence(in);
				entry.children.add(elseBlock);
				elseBlock.children.add(joinNode);
			}
			else
			{
				Fixup(x.fixupLocation);
			}
			
			// Check for fi token
			if (toksym != PLToken.fiToken)
			{
				SyntaxError("Missing 'fi' close to if statement");
			}
			
			advance(in);
			
			System.err.println("Fixing: " + follow.fixupLocation);
			Fixup(follow.fixupLocation);
			
			result = entry;
		}
		else if (toksym == PLToken.whileToken)
		{
			result = parse_whileStatement(in);
		}
		else if (toksym == PLToken.returnToken)
		{
			result = parse_returnStatement(in);
		}
		else 
		{
			SyntaxError("Invalid start of a statement");
		}
		
		return result;
	}

	private PLIRBasicBlock parse_statSequence(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = parse_statement(in);
		
		while (toksym == PLToken.semiToken)
		{
			advance(in);
			result = parse_statement(in);
		}
		
		return result;
	}

	private PLIRBasicBlock parse_typeDecl(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		
		if (toksym == PLToken.varToken)
		{
			advance(in);
			return null;
		}
		else if (toksym == PLToken.arrToken)
		{
			advance(in);
			if (toksym == PLToken.openBracketToken)
			{
				advance(in);
				result = parse_number(in);
				if (toksym == PLToken.closeBracketToken)
				{
					advance(in);
					while (toksym == PLToken.openBracketToken)
					{
						advance(in);
						result = parse_number(in);
						if (toksym == PLToken.closeBracketToken)
						{
							SyntaxError("']' missing from typeDecl non-terminal");
						}
						advance(in);
					}
				}
				else
				{
					SyntaxError("']' missing from typeDecl non-terminal");
				}
			}
			else
			{
				SyntaxError("'[' missing from typeDecl non-terminal");
			}
		}
		else
		{
			SyntaxError("Invalid start to varDecl non-terminal");
		}
		
		return null;
	}

	private PLIRBasicBlock parse_varDecl(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{	
		PLIRBasicBlock result = parse_typeDecl(in);
		scope.addVarToScope(sym);
		result = parse_ident(in);
		while (toksym == PLToken.commaToken)
		{
			advance(in);
			scope.addVarToScope(sym);
			result = parse_ident(in); 
		}
		if (toksym != PLToken.semiToken)
		{
			SyntaxError("';' missing from varDecl");
		}
		advance(in);
		
		return result;
	}

	private PLIRBasicBlock parse_funcDecl(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		if (toksym == PLToken.funcToken || toksym == PLToken.procToken)
		{
			advance(in);
			scope.pushNewScope(sym);
			result = parse_ident(in);
			
			if (toksym != PLToken.semiToken)
			{
				result = parse_formalParam(in);
			}
			
			advance(in);  // eat semi-colon
			result = parse_funcBody(in);
			
			if (toksym != PLToken.semiToken)
			{
				SyntaxError("'}' missing from funcDecl non-terminal");
			}
			
			String leavingScope = scope.popScope();
			System.err.println("Leaving scope: " + leavingScope);
			advance(in);
		}
		else
		{
			SyntaxError("Invalid start to funcDecl non-terminal");
		}
		
		return result;
	}

	private PLIRBasicBlock parse_formalParam(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		
		if (toksym == PLToken.openParenToken)
		{
			advance(in);
			result = parse_ident(in);
			while (toksym == PLToken.commaToken)
			{
				advance(in);
				result = parse_ident(in);
			}
			
			if (toksym != PLToken.closeParenToken)
			{
				SyntaxError("')' missing from formalParam");
			}
			
			advance(in);
		}
		
		return result;
	}

	private PLIRBasicBlock parse_funcBody(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRBasicBlock result = null;
		
		if (toksym == PLToken.varToken || toksym == PLToken.arrToken)
		{
			result = parse_varDecl(in);
		}
		
		if (toksym != PLToken.openBraceToken)
		{
			SyntaxError("'{' missing from funcBody non-terminal.");
		}
		
		if (toksym != PLToken.closeBraceToken)
		{
			// must be a statSequence here
			result = parse_statSequence(in);
			if (toksym != PLToken.closeBraceToken)
			{
				SyntaxError("'}' missing from statSequence non-terminal in funcBody");
			}
		}
		
		return result;
	}
	
	// per spec
	private void CondNegBraFwd(PLIRInstruction x)
	{
		x.fixupLocation = PLStaticSingleAssignment.globalSSAIndex;
		PLIRInstruction.create_branch(x, x.condcode);
	}
	
	// per spec
	private void UnCondBraFwd(PLIRInstruction x)
	{
		PLIRInstruction.create_BEQ(x.fixupLocation);
		x.fixupLocation = PLStaticSingleAssignment.globalSSAIndex - 1;
	}

	// set the address to which we jump...
	// buffer[loc] = (pc - loc);
	// per spec
	private void Fixup(int loc)
	{
		PLStaticSingleAssignment.instructions.get(loc).i2 = (PLStaticSingleAssignment.globalSSAIndex - loc);
	}
}
