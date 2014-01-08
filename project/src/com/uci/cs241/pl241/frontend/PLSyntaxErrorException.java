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
	 * Default constructor for the exception. There is no special
	 * functionality - just call the parent constructor. 
	 * @param message
	 */
	public PLSyntaxErrorException(String message)
	{
		super(message);
	}
}
