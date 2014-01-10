package com.uci.cs241.pl241.frontend;

import java.io.IOException;

/**
 * Main class responsible for parsing the input on character at a time to 
 * generate tokens used in the parsing step.
 * 
 * @author Christopher A. Wood, woodc1@uci.edu 
 * 
 */
public class PLTokenizer
{
	private PLFileHandler fh;
	
	public PLTokenizer(PLFileHandler fh)
	{
		this.fh = fh;
	}
	
	public String next() throws IOException
	{
		// TODO: do the FSM for number/ident here...
		return "" + fh.readChar();
	}
}
