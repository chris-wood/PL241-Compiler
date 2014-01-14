package com.uci.cs241.pl241.frontend;

public class PLToken
{
	private static String[] relStrings = {"==", "!=", "<", "<=", ">", ">="};
	private static String[] reservedWords = {"let", "call", "if", "then", "else", "fi", "while", "do", 
		"od", "return", "var", "array", "function", "procedure", "main"};
	private static String[] predefinedWords = {"InputNum", "OutputNum", "OutputNewLine"};
	
	// TODO: create alias's for all reserved words here and make them static so they can easily be checked/called upon
	public static String letToken = "let";
	public static String callToken = "call";
	public static String thenToken = "then";
	public static String elseToken = "else";
	public static String fiToken = "fi";
	public static String whileToken = "while";
	public static String doToken = "do";
	public static String odToken = "od";
	public static String returnToken = "return";
	public static String varToken = "var";
	public static String arrayToken = "array";
	public static String functionToken = "function";
	public static String procedureToken = "procedure";
	public static String mainToken = "main";
	
	public static boolean isNumber(String token)
	{
		try 
		{
			int num = Integer.parseInt(token);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
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
	
	// Constructor information
	private String token;
	public PLToken(String token)
	{
		this.token = new String(token);
	}
	
	public boolean typeMatch(String other)
	{
		if (token.equals(other))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
}
