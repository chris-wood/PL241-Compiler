package com.uci.cs241.pl241.frontend;

import java.io.IOException;

public class PLParser
{
	// terminals: letter digit relOp
	
//	private PLTokenizer stream;
	
	private String sym;
	
//	public PLParser(PLTokenizer stream)
//	{
//		this.stream = stream;
//	}
	
	public void SyntaxError(String msg) throws PLSyntaxErrorException
	{
		throw new PLSyntaxErrorException(msg + ": " + sym);
	}
	
	// this is what's called - starting with the computation non-terminal
	public PLParseResult parse(PLTokenizer stream) throws PLSyntaxErrorException, IOException, PLEndOfFileException
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
			
//			System.out.println("we're here!");
			
		}
		else
		{
			SyntaxError("Computation does not begin with main keyword");
		}
		
		return result;
	}

	// non-terminals
	private PLParseResult parse_ident(PLTokenizer in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
//		if (in.getSymbolType() == PLInputStream.PLInputTokenType.NUMBER)
//		{
//			throw new PLSyntaxErrorException("ident");
//		}
		
		sym = in.next();
		
		return null;
	}

	private PLParseResult parse_number(PLTokenizer in) throws PLSyntaxErrorException
	{
		return null;
	}

	private PLParseResult parse_designator(PLTokenizer in) throws PLSyntaxErrorException
	{
		return null;
	}

	private PLParseResult parse_factor(PLTokenizer in) throws PLSyntaxErrorException
	{
		return null;
	}

	private PLParseResult parse_term(PLTokenizer in) throws PLSyntaxErrorException
	{
		return null;
	}

	private PLParseResult parse_expression(PLTokenizer in) throws PLSyntaxErrorException
	{
		return null;
	}

	private PLParseResult parse_relation(PLTokenizer in) throws PLSyntaxErrorException
	{
		return null;
	}

	private PLParseResult parse_assignment(PLTokenizer in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
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

	private PLParseResult parse_funcCall(PLTokenizer in) throws PLSyntaxErrorException
	{
//		if (!(sym.equals("call")))
//		{
//			throw new PLSyntaxErrorException("funcCall");
//		}
//		else
//		{
//			// TODO
//		}
//		
		return null;
	}

	private PLParseResult parse_ifStatement(PLTokenizer in) throws PLSyntaxErrorException
	{
//		if (!(sym.equals("if")))
//		{
//			throw new PLSyntaxErrorException("ifStatement");
//		}
//		else
//		{
//			// TODO
//		}
		
		return null;
	}

	private PLParseResult parse_whileStatement(PLTokenizer in) throws PLSyntaxErrorException
	{
//		if (!(sym.equals("while")))
//		{
//			throw new PLSyntaxErrorException("whileStatement");
//		}
//		else
//		{
//			// TODO
//		}
		
		return null;
	}

	private PLParseResult parse_returnStatement(PLTokenizer in) throws PLSyntaxErrorException
	{
//		if (!(sym.equals("return")))
//		{
//			throw new PLSyntaxErrorException("returnStatement");
//		}
//		else
//		{
//			// TODO: how to handle expression?
//		}
		
		return null;
	}

	private PLParseResult parse_statement(PLTokenizer in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
		if (sym.equals("let"))
		{
			System.out.println("parsing let statement");
			result = parse_assignment(in);
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
			result = parse_returnStatement(in);
		}
		else
		{
			throw new PLSyntaxErrorException("invalid start of a statement");
		}
		
		return null;
	}

	private PLParseResult parse_statSequence(PLTokenizer in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = parse_statement(in);
		
		while (sym.equals(";"))
		{
			sym = in.next();
			result = parse_statement(in);
		}
		
		return result;
	}

	private PLParseResult parse_typeDecl(PLTokenizer in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
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

	private PLParseResult parse_varDecl(PLTokenizer in) throws PLSyntaxErrorException, IOException, PLEndOfFileException
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

	private PLParseResult parse_funcDecl(PLTokenizer in) throws PLSyntaxErrorException
	{
//		if (sym.equals("function") || sym.equals("procedure"))
//		{
//			in.next();
//			parse_ident(in);
//			
//			if (!(sym.equals(";")))
//			{
//				parse_formalParam(in);
//			}
//			
//			in.next(); // eat semi-colon
//			
//			parse_funcBody(in);
//			
//			if (!(sym.equals("}")))
//			{
//				throw new PLSyntaxErrorException("funcDecl");
//			}
//			else
//			{
//				in.next();
//			}
//		}
//		else
//		{
//			throw new PLSyntaxErrorException("funcDecl");
//		}
		
		return null;
	}

	private PLParseResult parse_formalParam(PLTokenizer in) throws PLSyntaxErrorException
	{
//		if (sym.equals("("))
//		{
//			in.next();
//			parse_ident(in);
//			while (sym.equals(","))
//			{
//				in.next();
//				parse_ident(in);
//			}
//			
//			if (sym.equals(")"))
//			{
//				in.next();
//			}
//			else
//			{
//				throw new PLSyntaxErrorException("formalParam");
//			}
//		}
		
		
		return null;
	}

	private PLParseResult parse_funcBody(PLTokenizer in) throws PLSyntaxErrorException
	{
//		while (!(sym.equals("{")))
//		{
//			parse_varDecl(in);
//		}
//		
//		if (!(sym.equals("}")))
//		{
//			parse_statSequence(in);
//		}
		
		return null;
	}

	private PLParseResult parse_computation(PLTokenizer in) throws PLSyntaxErrorException
	{
		PLParseResult result = null;
		
//		if (!(sym.equals("main")))
//		{
//			throw new PLSyntaxErrorException("computation");
//		}
//		else
//		{
//			in.next();
//			if (!(sym.equals("{")))
//			{
//				while (!(sym.equals("function") || sym.equals("procedure")))
//				{
//					in.next();
//					parse_varDecl(in);
//				}
//				while (!(sym.equals("{")))
//				{
//					in.next();
//					parse_funcDecl(in);
//				}
//			}
//			
//			in.next();
//			parse_statSequence(in);
//			if (!(sym.equals("}")))
//			{
//				throw new PLSyntaxErrorException("computation");
//			}
//			else
//			{
//				in.next();
//			}
//		}
		
		return result;
	}

	// pre-defined functions
	// InputNum()

	// predefined procedures
	// OutputNum(x)
	// OutputNewLine()
}
