package com.uci.cs241.pl241.frontend;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Main class responsible for parsing the input on character at a time to
 * generate tokens used in the parsing step.
 * 
 * @author Christopher A. Wood, woodc1@uci.edu
 * 
 */
public class PLTokenizer
{
	// private PLFileHandler fh = null;
	private BufferedReader in;
	private ArrayList<String> lines;
	private int lineNumber; // stream index
	private int lineOffset; // offset in the current line

	// Special relational characters
	private char[] relChars = { '=', '!', '<', '>' };

	private boolean isRelationalCharacter(char c)
	{
		for (int i = 0; i < relChars.length; i++)
		{
			if (c == relChars[i])
				return true;
		}
		return false;
	}

	// private char[] formatChars = {'\n', '\t', '\r', ';'};
	// private boolean isFormatCharacter(char c)
	// {
	// for (int i = 0; i < relChars.length; i++)
	// {
	// if (c == relChars[i]) return true;
	// }
	// return false;
	// }

	private void preProcessInput()
	{

	}

	// public PLTokenizer(PLFileHandler fh)
	// {
	// this.fh = fh;
	// }
	//
	public PLTokenizer(BufferedReader in) throws IOException
	{
		lines = new ArrayList<String>();
		
		String line;
		while ((line = in.readLine()) != null)
		{
			// Strip comments
			if (line.startsWith("//"))
			{
				// pass over this line...
			}
			else
			{
				line.replace('\n', ' ').replace('\r', ' ');
				lines.add(line);
				System.out.println(line);
			}
		}
		
		System.out.println("-----");
		System.out.println("Tokenizing results below...");
		System.out.println("-----");
		
		this.lineNumber = 0;
		this.lineOffset = 0;

		// this.stream = in;
		// this.streamIndex = 0;
	}

	public String next() throws IOException, PLSyntaxErrorException, PLEndOfFileException
	{
		StringBuilder token = new StringBuilder("");	
		String line = lines.get(lineNumber);
		if (lineOffset == line.length())
		{
			if (lineNumber == lines.size() - 1)
			{
				throw new PLEndOfFileException();
			}
			line = lines.get(++lineNumber);
			lineOffset = 0;
		}
		char nextChar = line.charAt(lineOffset++);
//		System.out.println("      " + nextChar);

		// Now build the token by parsing character-by-character with a FSM
		if (Character.isDigit(nextChar)) // must be a digit
		{
			token.append(nextChar);
			nextChar = line.charAt(lineOffset++);
//			System.out.println("      " + nextChar);
			while (Character.isDigit(nextChar))
			{
				token.append(nextChar);
				nextChar = line.charAt(lineOffset++);
			}
//			System.out.println(token.toString());
		} 
		else if (Character.isLetter(nextChar))
		{
			token.append(nextChar);
			nextChar = line.charAt(lineOffset++);
//			System.out.println("      " + nextChar);
			while (Character.isDigit(nextChar) || Character.isLetter(nextChar))
			{
				token.append(nextChar);
				nextChar = line.charAt(lineOffset++);
			}
//			System.out.println(token.toString() + " - " + lineOffset);
		} 
		else 
		{
			token.append(nextChar);
		}

		return token.toString();
	}
}
