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
		throw new PLSyntaxErrorException(msg);
	}
	
	// this is what's called - starting with the computation non-terminal
	public PLParseResult parse(PLTokenizer stream) throws PLSyntaxErrorException, IOException, PLEndOfFileException
	{
		PLParseResult result = null;
		
		sym = stream.next();
		if (sym.equals("main"))
		{
			System.out.println("we're here!");
		}
		else
		{
			SyntaxError("Computation does not begin with main keyword");
		}
		
		return result;
	}

	// non-terminals
	private PLParseResult parse_ident(PLInputStream in) throws PLSyntaxErrorException
	{
		if (in.getSymbolType() == PLInputStream.PLInputTokenType.NUMBER)
		{
			throw new PLSyntaxErrorException("ident");
		}
		
		
		
		return null;
	}

	private PLParseResult parse_number(PLInputStream in) throws PLSyntaxErrorException
	{
		return null;
	}

	private PLParseResult parse_designator(PLInputStream in) throws PLSyntaxErrorException
	{
		return null;
	}

	private PLParseResult parse_factor(PLInputStream in) throws PLSyntaxErrorException
	{
		return null;
	}

	private PLParseResult parse_term(PLInputStream in) throws PLSyntaxErrorException
	{
		return null;
	}

	private PLParseResult parse_expression(PLInputStream in) throws PLSyntaxErrorException
	{
		return null;
	}

	private PLParseResult parse_relation(PLInputStream in) throws PLSyntaxErrorException
	{
		return null;
	}

	private PLParseResult parse_assignment(PLInputStream in) throws PLSyntaxErrorException
	{
		if (!(in.getSymbol().equals("let")))
		{
			throw new PLSyntaxErrorException("assignment");
		}
		else
		{
			// TODO
		}
		
		return null;
	}

	private PLParseResult parse_funcCall(PLInputStream in) throws PLSyntaxErrorException
	{
		if (!(in.getSymbol().equals("call")))
		{
			throw new PLSyntaxErrorException("funcCall");
		}
		else
		{
			// TODO
		}
		
		return null;
	}

	private PLParseResult parse_ifStatement(PLInputStream in) throws PLSyntaxErrorException
	{
		if (!(in.getSymbol().equals("if")))
		{
			throw new PLSyntaxErrorException("ifStatement");
		}
		else
		{
			// TODO
		}
		
		return null;
	}

	private PLParseResult parse_whileStatement(PLInputStream in) throws PLSyntaxErrorException
	{
		if (!(in.getSymbol().equals("while")))
		{
			throw new PLSyntaxErrorException("whileStatement");
		}
		else
		{
			// TODO
		}
		
		return null;
	}

	private PLParseResult parse_returnStatement(PLInputStream in) throws PLSyntaxErrorException
	{
		if (!(in.getSymbol().equals("return")))
		{
			throw new PLSyntaxErrorException("returnStatement");
		}
		else
		{
			// TODO: how to handle expression?
		}
		
		return null;
	}

	private PLParseResult parse_statement(PLInputStream in) throws PLSyntaxErrorException
	{
		if (in.getSymbol().equals("let"))
		{
			parse_assignment(in);
		}
		else if (in.getSymbol().equals("call"))
		{
			parse_funcCall(in);
		}
		else if (in.getSymbol().equals("if"))
		{
			parse_ifStatement(in);
		}
		else if (in.getSymbol().equals("while"))
		{
			parse_whileStatement(in);
		}
		else if (in.getSymbol().equals("return"))
		{
			parse_returnStatement(in);
		}
		else
		{
			throw new PLSyntaxErrorException("statement");
		}
		
		return null;
	}

	private PLParseResult parse_statSequence(PLInputStream in) throws PLSyntaxErrorException
	{
		parse_statement(in);
		while (in.getSymbol().equals(";"))
		{
			in.next();
			parse_statement(in);
		}
		
		return null;
	}

	private PLParseResult parse_typeDecl(PLInputStream in) throws PLSyntaxErrorException
	{
		return null;
	}

	private PLParseResult parse_varDecl(PLInputStream in) throws PLSyntaxErrorException
	{
		return null;
	}

	private PLParseResult parse_funcDecl(PLInputStream in) throws PLSyntaxErrorException
	{
		if (in.getSymbol().equals("function") || in.getSymbol().equals("procedure"))
		{
			in.next();
			parse_ident(in);
			
			if (!(in.getSymbol().equals(";")))
			{
				parse_formalParam(in);
			}
			
			in.next(); // eat semi-colon
			
			parse_funcBody(in);
			
			if (!(in.getSymbol().equals("}")))
			{
				throw new PLSyntaxErrorException("funcDecl");
			}
			else
			{
				in.next();
			}
		}
		else
		{
			throw new PLSyntaxErrorException("funcDecl");
		}
		
		return null;
	}

	private PLParseResult parse_formalParam(PLInputStream in) throws PLSyntaxErrorException
	{
		if (in.getSymbol().equals("("))
		{
			in.next();
			parse_ident(in);
			while (in.getSymbol().equals(","))
			{
				in.next();
				parse_ident(in);
			}
			
			if (in.getSymbol().equals(")"))
			{
				in.next();
			}
			else
			{
				throw new PLSyntaxErrorException("formalParam");
			}
		}
		return null;
	}

	private PLParseResult parse_funcBody(PLInputStream in) throws PLSyntaxErrorException
	{
		while (!(in.getSymbol().equals("{")))
		{
			parse_varDecl(in);
		}
		
		if (!(in.getSymbol().equals("}")))
		{
			parse_statSequence(in);
		}
		
		return null;
	}

	private PLParseResult parse_computation(PLInputStream in) throws PLSyntaxErrorException
	{
		PLParseResult result = null;
		
		if (!(in.getSymbol().equals("main")))
		{
			throw new PLSyntaxErrorException("computation");
		}
		else
		{
			in.next();
			if (!(in.getSymbol().equals("{")))
			{
				while (!(in.getSymbol().equals("function") || in.getSymbol().equals("procedure")))
				{
					in.next();
					parse_varDecl(in);
				}
				while (!(in.getSymbol().equals("{")))
				{
					in.next();
					parse_funcDecl(in);
				}
			}
			
			in.next();
			parse_statSequence(in);
			if (!(in.getSymbol().equals("}")))
			{
				throw new PLSyntaxErrorException("computation");
			}
			else
			{
				in.next();
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
