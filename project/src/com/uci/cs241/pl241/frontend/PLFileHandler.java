package com.uci.cs241.pl241.frontend;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class PLFileHandler
{
	private BufferedReader reader;
	private char[] buffer = new char[1];
	
	public PLFileHandler(String fname) throws FileNotFoundException
	{
		reader = new BufferedReader(new FileReader(fname));
	}
	
//	public void hasNext()
	
	public char readChar() throws IOException
	{
		reader.read(buffer);
		return buffer[0];
	}
}
