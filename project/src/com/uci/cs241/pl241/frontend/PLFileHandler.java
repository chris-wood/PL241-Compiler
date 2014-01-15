package com.uci.cs241.pl241.frontend;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class PLFileHandler
{
	// Publicly accessible symbol - front of the file
	public char sym = 1; // valid to start!
	
	// Constants
	public static final int SYM_ERROR = 0;
	public static final int SYM_EOF = 255;
	
	// Internal file I/O handler
	private BufferedReader reader;
	
	public PLFileHandler(String fname) throws FileNotFoundException
	{
		reader = new BufferedReader(new FileReader(fname));
		next();
	}
	
	public void next()
	{
		if (sym != SYM_ERROR && sym != SYM_EOF)
		{
			try
			{
				sym = (char)reader.read();
				if (sym == -1)
				{
					sym = SYM_EOF; 
				}
//				if (sym == 0)
//				{
//					System.err.println("?!?!?!?!?!");
//				}
			}
			catch (IOException e)
			{
				System.err.println("Error: FileHandler encountered an I/O " +
						"exception when advanceding to the next symbol.");
				sym = SYM_ERROR;
			}
		}
	}
}
