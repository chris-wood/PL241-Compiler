package com.uci.cs241.pl241.frontend;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class PLScanner
{
	// Publicly accessible fields
	public int sym;
	public int val;
	public int id;
	
	// Identifier table
	public static List<String> identifiers;
	
	// Internal FileReader helper
	private PLFileHandler reader;
	private char lastChar;
	
	// Special relational characters
	private char[] relChars = { '=', '!', '<', '>' };
	private char[] arithChars = {'+', '-', '*', '/'};
	private char[] structuralChars = {',', ';', '{', '}', '[', ']', '.', '(', ')'};
	private String[] relStrings = {"==", "!=", "<", "<=", ">", ">="};
	
	private boolean inCharSet(char[] set, char c)
	{
		for (int i = 0; i < set.length; i++)
		{
			if (c == set[i]) return true;
		}
		return false;
	}

	private boolean isRelationalCharacter(char c)
	{
		return inCharSet(relChars, c);
	}
	
	private boolean isArithmeticCharacter(char c)
	{
		return inCharSet(arithChars, c);
	}
	
	private boolean isStructuralCharacter(char c)
	{
		return inCharSet(structuralChars, c);
	}
	
	private boolean isRelationalString(String s)
	{
		for (int i = 0; i < relStrings.length; i++)
		{
			if (s.equals(relStrings[i]))
			{
				return true;
			}
		}
		return false;
	}
	
	public PLScanner(String fname) throws FileNotFoundException 
	{
		// Populate the identifier table with reserved keywords
		identifiers = new ArrayList<String>();
		
		// Create the file handle and pull the first token into the sym variable
		reader = new PLFileHandler(fname);
		try
		{
			nextToken();
		}
		catch (PLSyntaxErrorException e)
		{
			System.err.println("Token error: " + e.getMessage());
		}
	}
	
	public String next() throws PLSyntaxErrorException
	{
		String upnext = nextToken();
		while (upnext.trim().length() == 0)
		{
			upnext = nextToken();
		}
		return upnext;
	}
	
	public String nextToken() throws PLSyntaxErrorException
	{
		StringBuilder token = new StringBuilder();
		
		// And so it goes...
		lastChar = 0;
		char nextChar = reader.sym;
		
		// Skip over leading whitespace
		while (nextChar == ' ')  
		{
			reader.next();
			nextChar = reader.sym;
		}
		
		// Check to see if we have a relational character, number, digit, or potential arithmetic operation
		if (Character.isDigit(nextChar))
		{
			token.append(nextChar);
			reader.next();
			nextChar = reader.sym;
			while (Character.isDigit(nextChar))
			{
				token.append(nextChar);
				reader.next();
				nextChar = reader.sym;
			}
		}
		else if (Character.isLetter(nextChar))
		{
			token.append(nextChar);
			reader.next();
			nextChar = reader.sym;
			while (Character.isDigit(nextChar) || Character.isLetter(nextChar))
			{
				token.append(nextChar);
				reader.next();
				nextChar = reader.sym;
			}
		}
		else if (isRelationalCharacter(nextChar))
		{
			token.append(nextChar);
			lastChar = nextChar;
			reader.next();
			nextChar = reader.sym;
			if (isRelationalString("" + lastChar + nextChar))
			{
				token.append(nextChar);
				
				// Make sure the next thing read is a space
				reader.next();
				nextChar = reader.sym;
				if (nextChar != ' ')
				{
					SyntaxError("Invalid relational operator: " + token.toString() + nextChar);
				}
			}
			else if (nextChar == '-')
			{
				token.append(nextChar);
				reader.next();
			}
			else if (nextChar == ' ')
			{
				reader.next();
			}
		}
		else if (isArithmeticCharacter(nextChar))
		{
			lastChar = nextChar;
			token.append(nextChar);
			reader.next();
			nextChar = reader.sym;
			
			// check for the special case of being the start of a comment
			if (nextChar == '/') 
			{
				System.err.println(">>> Tokenizer encountered the start of a line comment");
				System.err.print("" + lastChar + nextChar);
				reader.next();
				nextChar = reader.sym;
				while (nextChar != '\n')
				{
					System.err.print(nextChar);
					reader.next();
					nextChar = reader.sym;
				}
				System.err.println();
				reader.next(); // skip over the newline
				token = new StringBuilder();
			}
		}
		else if (isStructuralCharacter(nextChar))
		{
			token.append(nextChar);
			reader.next();
		}
		else if (nextChar == '\n')
		{ 
			reader.next(); // skip over the newline
			token = new StringBuilder();
		}
		else
		{
			SyntaxError("Illegal start of token: " + nextChar);
		}
		
		// TODO: map token string/characters to the token ID
		return token.toString();
	}
	
	public void SyntaxError(String msg) throws PLSyntaxErrorException
	{
		throw new PLSyntaxErrorException(msg + ": " + sym);
	}
}
