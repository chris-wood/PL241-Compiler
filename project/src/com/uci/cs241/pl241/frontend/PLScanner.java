package com.uci.cs241.pl241.frontend;

import java.io.FileNotFoundException;

public class PLScanner
{
	// Publicly accessible fields
	public int sym;
	public int val;
	public int id;
	
	// Internal FileReader helper
	private PLFileHandler reader;
	
	public PLScanner(String fname) throws FileNotFoundException 
	{
		reader = new PLFileHandler(fname);
		next();
	}
	
	public void next()
	{
		// read the next token...
	}
}
