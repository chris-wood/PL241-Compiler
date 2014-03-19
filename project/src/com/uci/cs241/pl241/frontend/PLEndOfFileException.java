package com.uci.cs241.pl241.frontend;

/**
 * Custom exception to indicate EOF when reading from the program source.
 * 
 * @author Christopher Wood, woodc1@uci.edu
 *
 */
public class PLEndOfFileException extends Exception
{
	/**
	 * Default
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor for the exception. There is no special
	 * functionality - just call the parent constructor. 
	 */
	public PLEndOfFileException()
	{
		super("PL241 Error: End of file reached.");
	}
}
