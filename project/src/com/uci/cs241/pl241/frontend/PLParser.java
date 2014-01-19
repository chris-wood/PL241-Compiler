package com.uci.cs241.pl241.frontend;

import java.io.IOException;

import com.uci.cs241.pl241.ir.PLIRInstruction;
import com.uci.cs241.pl241.ir.PLSymbolTable;
import com.uci.cs241.pl241.ir.PLIRInstruction.PLIRInstructionOperandType;
import com.uci.cs241.pl241.ir.PLIRInstruction.PLIRInstructionType;

public class PLParser
{
	// NODES
	// SUBCLASSES: assignment, func/proc, array, block, designator, functionblock, load/store, main, param 
	
	// terminals: letter digit relOp
	
	// Current symbol/token values used for parsing
	private String sym;
	private int toksym;
	
	// Other necessary things
	private PLSymbolTable symTable;
	private PLScope scope;
	
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
	public PLIRInstruction parse(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction result = null;
		advance(in);
		if (toksym == PLToken.mainToken)
		{
			advance(in);
			scope = new PLScope();
			
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
				result = this.parse_statSequence(in);
				
				// parse the close of the computation
				if (toksym == PLToken.closeBraceToken)
				{
					advance(in);
					if (toksym == PLToken.periodToken)
					{
						
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
	private PLIRInstruction parse_ident(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		// TODO
		advance(in);
		
		// TODO: create the node
		
		return null;
	}

	private PLIRInstruction parse_number(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{	
		// add 0 tokenValue
		PLIRInstruction li = new PLIRInstruction(PLIRInstructionType.ADD, 0, Integer.parseInt(sym)); 
		advance(in);
		return li;
	}

	private PLIRInstruction parse_designator(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction result = null;
		
		result = parse_ident(in);
//		while (sym.equals("["))
		while (toksym == PLToken.openBracketToken)
		{
			advance(in);
			result = parse_expression(in);
//			if (!(sym.equals("]")))
			if (toksym != PLToken.closeBracketToken)
			{
				SyntaxError("']' missing from designator non-terminal.");
			}
//			in.next(); sym = in.symstring;
			advance(in);
		}
		
		return null;
	}

	private PLIRInstruction parse_factor(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction factor = null;
		
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

	private PLIRInstruction parse_term(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction factor = parse_factor(in);
		if (toksym == PLToken.timesToken || toksym == PLToken.divToken)
		{
			PLIRInstruction termNode = new PLIRInstruction(toksym);
			advance(in);
			
			// Now parse the right factor and build the resulting node
			PLIRInstruction rightNode = parse_factor(in);
//			termNode.setLeft(factor);
//			termNode.setRight(rightNode);
			return termNode;
		}
		else
		{
			return factor;
		}
	}

	private PLIRInstruction parse_expression(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction term = parse_term(in);
		if (toksym == PLToken.plusToken || toksym == PLToken.minusToken)
		{
			PLIRInstruction exprNode = new PLIRInstruction(toksym);
			advance(in);
			
			// Now parse the right term and build the resulting node 
			PLIRInstruction rightNode = parse_term(in);
//			exprNode.setLeft(term);
//			exprNode.setRight(rightNode);
			return exprNode;
		}
		else
		{
			return term;
		}
	}

	private PLIRInstruction parse_relation(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction result = null;
		
		result = parse_expression(in);
		// TODO: combine
		
//		if (true) // TODO (!(PLToken.isRelationalString(sym)))
		if (PLToken.isRelationalToken(toksym) == false)
		{
			SyntaxError("Invalid relational character");
		}
		advance(in);
		
		// TODO: save the relational code here
		
		// TODO: combine
		result = parse_expression(in);
		
		return result;
	}

	private PLIRInstruction parse_assignment(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction result = null;
		
//		if (!(sym.equals("let")))
		if (toksym != PLToken.letToken)
		{
			throw new PLSyntaxErrorException("assignment");
		}
		else
		{
//			in.next(); sym = in.symstring;
			advance(in);
			String varName = sym;
			
			// Check to make sure these variables are in scope before being used
//			if (scope.isVarInScope())
			
			
			result = parse_designator(in);
//			if (sym.equals("<-"))
			if (toksym == PLToken.becomesToken)
			{
				advance(in);
				result = parse_expression(in);
				
				System.out.println("result: " + result.i1 + "," + result.i2);
				
				// caw
				
//				symTable.updateSymbol(varName, result);
			}
			else
			{
				SyntaxError("'<-' character missing in assignment statement");
			}
		}
		
		return result;
	}

	private PLIRInstruction parse_funcCall(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction result = null;
		
//		if (!(sym.equals("call")))
		if (toksym != PLToken.callToken)
		{
			SyntaxError("Invalid start to funcCall non-terminal");
		}
		else
		{
			advance(in);
			result = parse_ident(in);
			
//			if (sym.equals("("))
			if (toksym == PLToken.openParenToken)
			{
//				in.next(); sym = in.symstring;
				advance(in);
				
//				if (!(sym.equals(";")) && !(sym.equals("else")) && !(sym.equals("fi")) && !(sym.equals("od")) && !(sym.equals("}")))
				if (toksym != PLToken.semiToken && toksym != PLToken.elseToken
						&& toksym != PLToken.fiToken && toksym != PLToken.odToken && toksym != PLToken.closeBraceToken)
				{
					result = parse_expression(in);
//					while (sym.equals(";"))
					while (toksym == PLToken.semiToken);
					{
//						in.next(); sym = in.symstring;
						advance(in);
						result = parse_expression(in);
					}
				}
			}
			
			// TODO: necessary?
//			in.next(); sym = in.symstring;
			advance(in);
		}
		
		return result;
	}

	private PLIRInstruction parse_ifStatement(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction result = null;
		
//		if (!(sym.equals("if")))
		if (toksym != PLToken.ifToken)
		{
			SyntaxError("Invalid start to ifStatement non-terminal");
		}
		else
		{
//			in.next(); sym = in.symstring;
			advance(in);
			result = parse_relation(in);
			
//			if (!(sym.equals("then")))
			if (toksym != PLToken.thenToken)
			{
				SyntaxError("Missing then clause");
			}
//			in.next(); sym = in.symstring;
			advance(in);
			result = parse_statSequence(in);
			
//			if (sym.equals("else"))
			if (toksym == PLToken.elseToken)
			{
//				in.next(); sym = in.symstring;
				advance(in);
				result = parse_statSequence(in);
			}
//			else if (!(sym.equals("fi")))
			else if (toksym != PLToken.fiToken)
			{
				SyntaxError("Missing 'fi' close to if statement");
			}
			
//			in.next(); sym = in.symstring;
			advance(in);
		}
		
		return result;
	}

	private PLIRInstruction parse_whileStatement(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction result = null;
		
//		if (!(sym.equals("while")))
		if (toksym != PLToken.whileToken)
		{
			SyntaxError("Invalid start to ifStatement non-terminal");
		}
		else
		{
//			in.next(); sym = in.symstring;
			advance(in);
			result = parse_relation(in);
			
//			if (!(sym.equals("do")))
			if (toksym != PLToken.doToken)
			{
				SyntaxError("Missing 'do' in while statement");
			}
			
//			in.next(); sym = in.symstring;
			advance(in);
			result = parse_statSequence(in);
			
//			if (!(sym.equals("od")))
			if (toksym != PLToken.odToken)
			{
				SyntaxError("Missing 'od' in while statement");
			}
			
//			in.next(); sym = in.symstring;
			advance(in);
		}
		
		return result;
	}

	private PLIRInstruction parse_returnStatement(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction result = null;
		
//		if (!(sym.equals("return")))
		if (toksym != PLToken.returnToken)
		{
			SyntaxError("Invalid start to ifStatement non-terminal");
		}
		else
		{
//			in.next(); sym = in.symstring;
			advance(in);
//			System.out.println("Checking: " + sym);
//			if (!(sym.equals(";")) && !(sym.equals("else")) && !(sym.equals("fi")) && !(sym.equals("od")) && !(sym.equals("}")))
			if (toksym != PLToken.semiToken && toksym != PLToken.elseToken
					&& toksym != PLToken.fiToken && toksym != PLToken.odToken && toksym != PLToken.closeBraceToken)
			{
				result = parse_expression(in);
			}
		}
		
		return result;
	}

	private PLIRInstruction parse_statement(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction result = null;
		
//		if (sym.equals("let"))
		if (toksym == PLToken.letToken)
		{
//			System.out.println("parsing let statement");
			result = parse_assignment(in);
//			System.out.println(sym);
		}
//		else if (sym.equals("call"))
		else if (toksym == PLToken.callToken)
		{
			result = parse_funcCall(in);
		}
//		else if (sym.equals("if"))
		else if (toksym == PLToken.ifToken)
		{
			result = parse_ifStatement(in);
		}
//		else if (sym.equals("while"))
		else if (toksym == PLToken.whileToken)
		{
			result = parse_whileStatement(in);
		}
//		else if (sym.equals("return"))
		else if (toksym == PLToken.returnToken)
		{
//			System.out.println("parsing return statement");
			result = parse_returnStatement(in);
		}
		else
		{
			throw new PLSyntaxErrorException("invalid start of a statement");
		}
		
		return null;
	}

	private PLIRInstruction parse_statSequence(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction result = parse_statement(in);
		
//		while (sym.equals(";"))
		while (toksym == PLToken.semiToken)
		{
//			System.out.println("parsing next statement");
//			in.next(); sym = in.symstring;
			advance(in);
			result = parse_statement(in);
		}
		
//		in.next(); sym = in.symstring;
		
		return result;
	}

	private PLIRInstruction parse_typeDecl(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction result = null;
		
//		if (sym.equals("var"))
		if (toksym == PLToken.varToken)
		{
//			in.next(); sym = in.symstring;
			advance(in);
			return null;
		}
//		else if (sym.equals("array"))
		else if (toksym == PLToken.arrToken)
		{
//			in.next(); sym = in.symstring;
			advance(in);
//			if (sym.equals("["))
			if (toksym == PLToken.openBracketToken)
			{
//				in.next(); sym = in.symstring;
				advance(in);
				result = parse_number(in);
//				if (sym.equals("]"))
				if (toksym == PLToken.closeBracketToken)
				{
//					in.next(); sym = in.symstring;
					advance(in);
//					while (sym.equals("["))
					while (toksym == PLToken.openBracketToken)
					{
//						in.next(); sym = in.symstring;
						advance(in);
						result = parse_number(in);
//						if (sym.equals("]") == false)
						if (toksym == PLToken.closeBracketToken)
						{
							SyntaxError("']' missing from typeDecl non-terminal");
						}
//						in.next(); sym = in.symstring;
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

	private PLIRInstruction parse_varDecl(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction result = parse_typeDecl(in);
		result = parse_ident(in);
//		in.next(); sym = in.symstring;
//		System.out.println("here: " + sym);
//		while (sym.equals(","))
		while (toksym == PLToken.commaToken)
		{
//			in.next(); sym = in.symstring;
			advance(in);
			result = parse_ident(in); 
//			in.next(); sym = in.symstring;
		}
//		System.out.println(sym);
//		if (sym.equals(";") == false)
		if (toksym != PLToken.semiToken)
		{
			SyntaxError("';' missing from varDecl");
		}
//		in.next(); sym = in.symstring;
		advance(in);
		
		return result;
	}

	private PLIRInstruction parse_funcDecl(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction result = null;
		
//		if (sym.equals("function") || sym.equals("procedure"))
		if (toksym == PLToken.funcToken || toksym == PLToken.procToken)
		{
//			in.next(); sym = in.symstring;
			advance(in);
			result = parse_ident(in);
			
//			if (!(sym.equals(";")))
			if (toksym != PLToken.semiToken)
			{
				result = parse_formalParam(in);
			}
			
//			in.next(); sym = in.symstring; // eat semi-colon
			advance(in);
			result = parse_funcBody(in);
			
//			if (!(sym.equals(";")))
			if (toksym != PLToken.semiToken)
			{
				SyntaxError("'}' missing from funcDecl non-terminal");
			}
			
//			in.next(); sym = in.symstring;
			advance(in);
		}
		else
		{
			SyntaxError("Invalid start to funcDecl non-terminal");
		}
		
		return result;
	}

	private PLIRInstruction parse_formalParam(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction result = null;
		
//		if (sym.equals("("))
		if (toksym == PLToken.openParenToken)
		{
//			in.next(); sym = in.symstring;
			advance(in);
			result = parse_ident(in);
			
//			while (sym.equals(","))
			while (toksym == PLToken.commaToken)
			{
//				in.next(); sym = in.symstring;
				advance(in);
				result = parse_ident(in);
			}
			
//			if (!(sym.equals(")")))
			if (toksym != PLToken.closeParenToken)
			{
				SyntaxError("')' missing from formalParam");
			}
			
//			in.next(); sym = in.symstring;
			advance(in);
		}
		
		return result;
	}

	private PLIRInstruction parse_funcBody(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRInstruction result = null;
		
//		if (sym.equals("var") || sym.equals("array"))
		if (toksym == PLToken.varToken || toksym == PLToken.arrToken)
		{
			result = parse_varDecl(in);
		}
		
//		if (!(sym.equals("{")))
		if (toksym != PLToken.openBraceToken)
		{
			SyntaxError("'{' missing from funcBody non-terminal.");
		}
		
//		if (!(sym.equals("}")))
		if (toksym != PLToken.closeBraceToken)
		{
			// must be a statSequence here
			result = parse_statSequence(in);
//			if (!(sym.equals("}")))
			if (toksym != PLToken.closeBraceToken)
			{
				SyntaxError("'}' missing from statSequence non-terminal in funcBody");
			}
		}
		
		return result;
	}

	// pre-defined functions
	// InputNum()

	// predefined procedures
	// OutputNum(x)
	// OutputNewLine()
}
