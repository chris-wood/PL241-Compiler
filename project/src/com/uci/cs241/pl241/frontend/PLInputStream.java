package com.uci.cs241.pl241.frontend;

import java.util.Scanner;

/**
 * An "intelligent" wrapper for the token input stream.  
 * 
 * @author Christopher Wood, woodc1@uci.edu
 * 
 */
public class PLInputStream 
{
	// Head of the input stream
	private String sym;
	
	// Scanner object containing the program input string
	private Scanner scan;
	
	/**
	 * Create the input stream wrapper from the long stringified representation
	 * of the input program. This is assumed to be passed in from a file reader 
	 * filter prior to parsing.
	 * 
	 * @param stream - stringified version of the program
	 */
	public PLInputStream(String stream) 
	{
		scan = new Scanner(stream);
	}
	
	/**
	 * Advance the input stream to the next token, discarding the current symbol.
	 * 
	 * @param none
	 * @return nothing
	 */
	public void next()
	{
		sym = scan.next();
	}
	
	/**
	 * Return the current symbol (i.e., the head of the input stream).
	 * 
	 * @return sym
	 */
	public String getSymbol()
	{
		return sym;
	}
}
