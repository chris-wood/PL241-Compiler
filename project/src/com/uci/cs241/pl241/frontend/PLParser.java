package com.uci.cs241.pl241.frontend;

import java.io.IOException;

import com.uci.cs241.pl241.ir.PLParseResult;

public class PLParser
{
	// terminals: letter digit relOp
	
//	private PLScanner stream;
	
	private String sym;
	
//	public PLParser(PLScanner stream)
//	{
//		this.stream = stream;
//	}
	
	public void SyntaxError(String msg) throws PLSyntaxErrorException
	{
		throw new PLSyntaxErrorException(msg + ": " + sym);
	}
	
	// this is what's called - starting with the computation non-terminal
	public PLParseResult parse(PLScanner stream) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
		sym = stream.next();
		if (sym.equals("main"))
		{
			sym = stream.next();
			
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
				sym = stream.next();
				result = this.parse_statSequence(stream);
				
				// parse the close of the computation
				if (sym.equals("}"))
				{
					sym = stream.next();
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
	private PLParseResult parse_ident(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		// TODO
		sym = in.next();
		
		return null;
	}

	private PLParseResult parse_number(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		// TODO
		sym = in.next();
		System.out.println(sym);
		
		return null;
	}

	private PLParseResult parse_designator(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
		result = parse_ident(in);
		while (sym.equals("["))
		{
			sym = in.next();
			result = parse_expression(in);
			if (!(sym.equals("]")))
			{
				SyntaxError("']' missing froim designator non-terminal.");
			}
			sym = in.next();
		}
		
		return null;
	}

	private PLParseResult parse_factor(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
		if (sym.equals("("))
		{
			sym = in.next();
			result = parse_expression(in);
			if (!(sym.equals(")")))
			{
				SyntaxError("')' missing from factor non-terminal");
			}
		}
		else if (sym.equals("call"))
		{
			result = parse_expression(in);
		}
		else 
		{
			if (PLToken.isNumber(sym))
			{
				System.out.println("number! " + sym);
				result = parse_number(in);
			}
			else
			{
				System.out.println("designator! " + sym);
				result = parse_designator(in); // only other possibility...
			}
		}
		
		return null;
	}

	private PLParseResult parse_term(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
		result = parse_factor(in);
		while (sym.equals("*") || sym.equals("/"))
		{
			sym = in.next();
			result = parse_factor(in);
		}
		
		return result;
	}

	private PLParseResult parse_expression(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
		result = parse_term(in);
		while (sym.equals("+") || sym.equals("-"))
		{
			sym = in.next();
			result = parse_term(in);
		}
		
		return result;
	}

	private PLParseResult parse_relation(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
		result = parse_expression(in);
		// TODO: combine
		
		if (!(PLToken.isRelationalString(sym)))
		{
			SyntaxError("Invalid relational character");
		}
		sym = in.next();
		
		// TODO: save the relational code here
		
		// TODO: combine
		result = parse_expression(in);
		
		return result;
	}

	private PLParseResult parse_assignment(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
		if (!(sym.equals("let")))
		{
			throw new PLSyntaxErrorException("assignment");
		}
		else
		{
			sym = in.next();
			result = parse_designator(in);
			if (sym.equals("<-"))
			{
				sym = in.next();
				result = parse_expression(in);
			}
			else
			{
				SyntaxError("'<-' character missing in assignment statement");
			}
		}
		
		return result;
	}

	private PLParseResult parse_funcCall(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
		if (!(sym.equals("call")))
		{
			SyntaxError("Invalid start to funcCall non-terminal");
		}
		else
		{
			sym = in.next();
			result = parse_ident(in);
			
			if (sym.equals("("))
			{
				sym = in.next();
				
				if (!(sym.equals(";")) && !(sym.equals("else")) && !(sym.equals("fi")) && !(sym.equals("od")) && !(sym.equals("}")))
				{
					result = parse_expression(in);
					while (sym.equals(";"))
					{
						sym = in.next();
						result = parse_expression(in);
					}
				}
			}
			
			// TODO: necessary?
			sym = in.next();
		}
		
		return result;
	}

	private PLParseResult parse_ifStatement(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
		if (!(sym.equals("if")))
		{
			SyntaxError("Invalid start to ifStatement non-terminal");
		}
		else
		{
			sym = in.next();
			result = parse_relation(in);
			
			if (!(sym.equals("then")))
			{
				SyntaxError("Missing then clause");
			}
			sym = in.next();
			result = parse_statSequence(in);
			
			if (sym.equals("else"))
			{
				sym = in.next();
				result = parse_statSequence(in);
			}
			else if (!(sym.equals("fi")))
			{
				SyntaxError("Missing 'fi' close to if statement");
			}
			
			sym = in.next();
		}
		
		return result;
	}

	private PLParseResult parse_whileStatement(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
		if (!(sym.equals("while")))
		{
			SyntaxError("Invalid start to ifStatement non-terminal");
		}
		else
		{
			sym = in.next();
			result = parse_relation(in);
			
			if (!(sym.equals("do")))
			{
				SyntaxError("Missing 'do' in while statement");
			}
			
			sym = in.next();
			result = parse_statSequence(in);
			
			if (!(sym.equals("od")))
			{
				SyntaxError("Missing 'od' in while statement");
			}
			
			sym = in.next();
		}
		
		return result;
	}

	private PLParseResult parse_returnStatement(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
		if (!(sym.equals("return")))
		{
			SyntaxError("Invalid start to ifStatement non-terminal");
		}
		else
		{
			sym = in.next();
			System.out.println("CHecking: " + sym);
			if (!(sym.equals(";")) && !(sym.equals("else")) && !(sym.equals("fi")) && !(sym.equals("od")) && !(sym.equals("}")))
			{
				result = parse_expression(in);
			}
		}
		
		return result;
	}

	private PLParseResult parse_statement(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
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

	private PLParseResult parse_statSequence(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = parse_statement(in);
		
		while (sym.equals(";"))
		{
			System.out.println("parsing next statement");
			sym = in.next();
			result = parse_statement(in);
		}
		
//		sym = in.next();
		
		return result;
	}

	private PLParseResult parse_typeDecl(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
		if (sym.equals("var"))
		{
			sym = in.next();
			return null;
		}
		else if (sym.equals("array"))
		{
			sym = in.next();
			if (sym.equals("["))
			{
				sym = in.next();
				result = parse_number(in);
				if (sym.equals("]"))
				{
					sym = in.next();
					while (sym.equals("["))
					{
						sym = in.next();
						result = parse_number(in);
						if (sym.equals("]") == false)
						{
							SyntaxError("']' missing from typeDecl non-terminal");
						}
						sym = in.next();
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

	private PLParseResult parse_varDecl(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = parse_typeDecl(in);
		result = parse_ident(in);
//		sym = in.next();
//		System.out.println("here: " + sym);
		while (sym.equals(","))
		{
			sym = in.next();
			result = parse_ident(in); 
//			sym = in.next();
		}
//		System.out.println(sym);
		if (sym.equals(";") == false)
		{
			SyntaxError("';' missing from varDecl");
		}
		sym = in.next();
		
		return result;
	}

	private PLParseResult parse_funcDecl(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
		if (sym.equals("function") || sym.equals("procedure"))
		{
			sym = in.next();
			result = parse_ident(in);
			
			if (!(sym.equals(";")))
			{
				result = parse_formalParam(in);
			}
			
			sym = in.next(); // eat semi-colon
			result = parse_funcBody(in);
			
			if (!(sym.equals(";")))
			{
				SyntaxError("'}' missing from funcDecl non-terminal");
			}
			
			sym = in.next();
		}
		else
		{
			SyntaxError("Invalid start to funcDecl non-terminal");
		}
		
		return result;
	}

	private PLParseResult parse_formalParam(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
		if (sym.equals("("))
		{
			sym = in.next();
			result = parse_ident(in);
			
			while (sym.equals(","))
			{
				sym = in.next();
				result = parse_ident(in);
			}
			
			if (!(sym.equals(")")))
			{
				SyntaxError("')' missing from formalParam");
			}
			
			sym = in.next();
		}
		
		
		return result;
	}

	private PLParseResult parse_funcBody(PLScanner in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
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
