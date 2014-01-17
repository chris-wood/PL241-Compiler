package com.uci.cs241.pl241.frontend;

import java.io.IOException;

import com.uci.cs241.pl241.ir.PLIRNode;
import com.uci.cs241.pl241.ir.PLIRNode;

public class PLParser
{
	// NODES
	// SUBCLASSES: assignment, func/proc, array, block, designator, functionblock, load/store, main, param 
	
	// terminals: letter digit relOp
	
//	private PLScanner stream;
	
	private String sym;
	private int toksym;
	
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
	public PLIRNode parse(PLScanner stream) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode result = null;
		
		stream.next();
		sym = stream.symstring;
		if (sym.equals("main"))
		{
			stream.next();
			sym = stream.symstring;
			
			if (sym.equals("var") || sym.equals("array"))
			{
				result = this.parse_varDecl(stream);
			}
			
			if (sym.equals("function") || sym.equals("procedure"))
			{
				result = this.parse_funcDecl(stream);
			}
			
			if (sym.equals("{"))
			{
				// eat the sequence of statements that make up the computation
				stream.next();
				sym = stream.symstring;
				result = this.parse_statSequence(stream);
				
				// parse the close of the computation
				if (sym.equals("}"))
				{
					stream.next();
					sym = stream.symstring;
					if (sym.equals("."))
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
	private PLIRNode parse_ident(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		// TODO
		advance(in);
		
		return null;
	}

	private PLIRNode parse_number(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		// TODO
		advance(in);
		System.out.println(sym);
		
		return null;
	}

	private PLIRNode parse_designator(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode result = null;
		
		result = parse_ident(in);
		while (sym.equals("["))
		{
			advance(in);
			result = parse_expression(in);
			if (!(sym.equals("]")))
			{
				SyntaxError("']' missing froim designator non-terminal.");
			}
			in.next(); sym = in.symstring;
		}
		
		return null;
	}

	private PLIRNode parse_factor(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode factor = null;
		
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
			System.out.println("number! " + sym);
			factor = parse_number(in);
		}
		else if (toksym == PLToken.ident)
		{
			System.out.println("designator! " + sym);
			factor = parse_designator(in); // only other possibility...
		}
		else
		{
			SyntaxError("Invalid case in parse_factor");
		}
		
		return factor;
	}

	private PLIRNode parse_term(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode factor = parse_factor(in);
		if (toksym == PLToken.timesToken || toksym == PLToken.divToken)
		{
			PLIRNode termNode = new PLIRNode(toksym);
			advance(in);
			
			// Now parse the right factor and build the resulting node
			PLIRNode rightNode = parse_factor(in);
			termNode.setLeft(factor);
			termNode.setRight(rightNode);
			return termNode;
		}
		else
		{
			return factor;
		}
	}

	private PLIRNode parse_expression(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode term = parse_term(in);
		if (toksym == PLToken.plusToken || toksym == PLToken.minusToken)
		{
			PLIRNode exprNode = new PLIRNode(toksym);
			advance(in);
			
			// Now parse the right term and build the resulting node 
			PLIRNode rightNode = parse_term(in);
			exprNode.setLeft(term);
			exprNode.setRight(rightNode);
			return exprNode;
		}
		else
		{
			return term;
		}
	}

	private PLIRNode parse_relation(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode result = null;
		
		result = parse_expression(in);
		// TODO: combine
		
		if (true) // TODO (!(PLToken.isRelationalString(sym)))
		{
			SyntaxError("Invalid relational character");
		}
		in.next(); sym = in.symstring;
		
		// TODO: save the relational code here
		
		// TODO: combine
		result = parse_expression(in);
		
		return result;
	}

	private PLIRNode parse_assignment(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode result = null;
		
		if (!(sym.equals("let")))
		{
			throw new PLSyntaxErrorException("assignment");
		}
		else
		{
			in.next(); sym = in.symstring;
			result = parse_designator(in);
			if (sym.equals("<-"))
			{
				in.next(); sym = in.symstring;
				result = parse_expression(in);
			}
			else
			{
				SyntaxError("'<-' character missing in assignment statement");
			}
		}
		
		return result;
	}

	private PLIRNode parse_funcCall(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode result = null;
		
		if (!(sym.equals("call")))
		{
			SyntaxError("Invalid start to funcCall non-terminal");
		}
		else
		{
			in.next(); sym = in.symstring;
			result = parse_ident(in);
			
			if (sym.equals("("))
			{
				in.next(); sym = in.symstring;
				
				if (!(sym.equals(";")) && !(sym.equals("else")) && !(sym.equals("fi")) && !(sym.equals("od")) && !(sym.equals("}")))
				{
					result = parse_expression(in);
					while (sym.equals(";"))
					{
						in.next(); sym = in.symstring;
						result = parse_expression(in);
					}
				}
			}
			
			// TODO: necessary?
			in.next(); sym = in.symstring;
		}
		
		return result;
	}

	private PLIRNode parse_ifStatement(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode result = null;
		
		if (!(sym.equals("if")))
		{
			SyntaxError("Invalid start to ifStatement non-terminal");
		}
		else
		{
			in.next(); sym = in.symstring;
			result = parse_relation(in);
			
			if (!(sym.equals("then")))
			{
				SyntaxError("Missing then clause");
			}
			in.next(); sym = in.symstring;
			result = parse_statSequence(in);
			
			if (sym.equals("else"))
			{
				in.next(); sym = in.symstring;
				result = parse_statSequence(in);
			}
			else if (!(sym.equals("fi")))
			{
				SyntaxError("Missing 'fi' close to if statement");
			}
			
			in.next(); sym = in.symstring;
		}
		
		return result;
	}

	private PLIRNode parse_whileStatement(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode result = null;
		
		if (!(sym.equals("while")))
		{
			SyntaxError("Invalid start to ifStatement non-terminal");
		}
		else
		{
			in.next(); sym = in.symstring;
			result = parse_relation(in);
			
			if (!(sym.equals("do")))
			{
				SyntaxError("Missing 'do' in while statement");
			}
			
			in.next(); sym = in.symstring;
			result = parse_statSequence(in);
			
			if (!(sym.equals("od")))
			{
				SyntaxError("Missing 'od' in while statement");
			}
			
			in.next(); sym = in.symstring;
		}
		
		return result;
	}

	private PLIRNode parse_returnStatement(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode result = null;
		
		if (!(sym.equals("return")))
		{
			SyntaxError("Invalid start to ifStatement non-terminal");
		}
		else
		{
			in.next(); sym = in.symstring;
			System.out.println("CHecking: " + sym);
			if (!(sym.equals(";")) && !(sym.equals("else")) && !(sym.equals("fi")) && !(sym.equals("od")) && !(sym.equals("}")))
			{
				result = parse_expression(in);
			}
		}
		
		return result;
	}

	private PLIRNode parse_statement(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode result = null;
		
		if (sym.equals("let"))
		{
			System.out.println("parsing let statement");
			result = parse_assignment(in);
			System.out.println(sym);
		}
		else if (sym.equals("call"))
		{
			result = parse_funcCall(in);
		}
		else if (sym.equals("if"))
		{
			result = parse_ifStatement(in);
		}
		else if (sym.equals("while"))
		{
			result = parse_whileStatement(in);
		}
		else if (sym.equals("return"))
		{
			System.out.println("parsing return statement");
			result = parse_returnStatement(in);
		}
		else
		{
			throw new PLSyntaxErrorException("invalid start of a statement");
		}
		
		return null;
	}

	private PLIRNode parse_statSequence(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode result = parse_statement(in);
		
		while (sym.equals(";"))
		{
			System.out.println("parsing next statement");
			in.next(); sym = in.symstring;
			result = parse_statement(in);
		}
		
//		in.next(); sym = in.symstring;
		
		return result;
	}

	private PLIRNode parse_typeDecl(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode result = null;
		
		if (sym.equals("var"))
		{
			in.next(); sym = in.symstring;
			return null;
		}
		else if (sym.equals("array"))
		{
			in.next(); sym = in.symstring;
			if (sym.equals("["))
			{
				in.next(); sym = in.symstring;
				result = parse_number(in);
				if (sym.equals("]"))
				{
					in.next(); sym = in.symstring;
					while (sym.equals("["))
					{
						in.next(); sym = in.symstring;
						result = parse_number(in);
						if (sym.equals("]") == false)
						{
							SyntaxError("']' missing from typeDecl non-terminal");
						}
						in.next(); sym = in.symstring;
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

	private PLIRNode parse_varDecl(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode result = parse_typeDecl(in);
		result = parse_ident(in);
//		in.next(); sym = in.symstring;
//		System.out.println("here: " + sym);
		while (sym.equals(","))
		{
			in.next(); sym = in.symstring;
			result = parse_ident(in); 
//			in.next(); sym = in.symstring;
		}
//		System.out.println(sym);
		if (sym.equals(";") == false)
		{
			SyntaxError("';' missing from varDecl");
		}
		in.next(); sym = in.symstring;
		
		return result;
	}

	private PLIRNode parse_funcDecl(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode result = null;
		
		if (sym.equals("function") || sym.equals("procedure"))
		{
			in.next(); sym = in.symstring;
			result = parse_ident(in);
			
			if (!(sym.equals(";")))
			{
				result = parse_formalParam(in);
			}
			
			in.next(); sym = in.symstring; // eat semi-colon
			result = parse_funcBody(in);
			
			if (!(sym.equals(";")))
			{
				SyntaxError("'}' missing from funcDecl non-terminal");
			}
			
			in.next(); sym = in.symstring;
		}
		else
		{
			SyntaxError("Invalid start to funcDecl non-terminal");
		}
		
		return result;
	}

	private PLIRNode parse_formalParam(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode result = null;
		
		if (sym.equals("("))
		{
			in.next(); sym = in.symstring;
			result = parse_ident(in);
			
			while (sym.equals(","))
			{
				in.next(); sym = in.symstring;
				result = parse_ident(in);
			}
			
			if (!(sym.equals(")")))
			{
				SyntaxError("')' missing from formalParam");
			}
			
			in.next(); sym = in.symstring;
		}
		
		return result;
	}

	private PLIRNode parse_funcBody(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLIRNode result = null;
		
		if (sym.equals("var") || sym.equals("array"))
		{
			result = parse_varDecl(in);
		}
		
		if (!(sym.equals("{")))
		{
			SyntaxError("'{' missing from funcBody non-terminal.");
		}
		
		if (!(sym.equals("}")))
		{
			// must be a statSequence here
			result = parse_statSequence(in);
			if (!(sym.equals("}")))
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
