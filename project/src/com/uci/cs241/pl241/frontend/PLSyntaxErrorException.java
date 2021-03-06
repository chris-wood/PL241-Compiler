package com.uci.cs241.pl241.frontend;

/**
 * Custom exception to indicate syntax errors during PL241 parsing.
 * 
 * @author Christopher Wood, woodc1@uci.edu
 *
 */
public class PLSyntaxErrorException extends Exception
{
	/**
	 * Default
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor for the exception. There is no special
	 * functionality - just call the parent constructor. 
	 * 
	 * @param message - source of the syntax error
	 */
	public PLSyntaxErrorException(String message)
	{
		super("PL241 Syntax Error: " + message);
	}
}
