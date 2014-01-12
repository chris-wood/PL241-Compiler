package com.uci.cs241.pl241.frontend;

public class PLToken
{
	private static String[] relStrings = {"==", "!=", "<", "<=", ">", ">="};
	private static String[] reservedWords = {"let", "call", "if", "then", "else", "fi", "while", "do", "od", "return", "var", "array", "function", "procedure", "main"};
	private static String[] predefinedWords = {"InputNum", "OutputNum", "OutputNewLine"};
	
	public static boolean isRelationalString(String token)
	{
		for (int i = 0; i < relStrings.length; i++)
		{
			if (token.equals(relStrings[i])) return true;
		}
		return false;
	}
	
	public static boolean isReservedWord(String token)
	{
		for (int i = 0; i < reservedWords.length; i++)
		{
			if (token.equals(reservedWords[i])) return true;
		}
		return false;
	}
	
	// TODO: implement queries that check the type of keyword here
}
