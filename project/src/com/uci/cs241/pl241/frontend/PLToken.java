package com.uci.cs241.pl241.frontend;

public class PLToken
{	
	public static final int errorToken = 0;
	public static final int timesToken = 1;
	public static final int divToken = 2;
	public static final int plusToken = 11;
	public static final int minusToken = 12;
	public static final int eqlToken = 20;
	public static final int newToken = 21;
	public static final int lssToken = 22;
	public static final int geqToken = 23;
	public static final int leqToken = 24;
	public static final int gttToken = 25;
	public static final int periodToken = 30;
	public static final int commaToken = 31;
	public static final int openBracketToken = 32;
	public static final int closeBracketToken = 24;
	public static final int closeParenToken = 35;
	public static final int becomesToken = 40;
	public static final int thenToken = 41;
	public static final int doToken = 42;
	public static final int openParenToken = 50;
	public static final int number = 60;
	public static final int ident = 61;
	public static final int semiToken = 70;
	public static final int endToken = 80;
	public static final int odToken = 81;
	public static final int fiToken = 82;
	public static final int elseToken = 90;
	public static final int letToken = 100;
	public static final int callToken = 101;
	public static final int ifToken = 102;
	public static final int whileToken = 103;
	public static final int returnToken = 104;
	public static final int varToken = 110;
	public static final int arrToken = 111;
	public static final int funcToken = 112;
	public static final int procToken = 113;
	public static final int beginToken = 150;
	public static final int mainToken = 200;
	public static final int eofToken = 255;
	
	
//	private static String[] relStrings = {"==", "!=", "<", "<=", ">", ">="};
//	private static String[] reservedWords = {"let", "call", "if", "then", "else", "fi", "while", "do", 
//		"od", "return", "var", "array", "function", "procedure", "main"};
//	private static String[] predefinedWords = {"InputNum", "OutputNum", "OutputNewLine"};
//	
//	// TODO: create alias's for all reserved words here and make them static so they can easily be checked/called upon
//	public static String letToken = "let";
//	public static String callToken = "call";
//	public static String thenToken = "then";
//	public static String elseToken = "else";
//	public static String fiToken = "fi";
//	public static String whileToken = "while";
//	public static String doToken = "do";
//	public static String odToken = "od";
//	public static String returnToken = "return";
//	public static String varToken = "var";
//	public static String arrayToken = "array";
//	public static String functionToken = "function";
//	public static String procedureToken = "procedure";
//	public static String mainToken = "main";
	
//	public static boolean isNumber(String token)
//	{
//		try 
//		{
//			int num = Integer.parseInt(token);
//			return true;
//		}
//		catch (NumberFormatException e)
//		{
//			return false;
//		}
//	}
//	
//	public static boolean isRelationalString(String token)
//	{
//		for (int i = 0; i < relStrings.length; i++)
//		{
//			if (token.equals(relStrings[i])) return true;
//		}
//		return false;
//	}
//	
//	public static boolean isReservedWord(String token)
//	{
//		for (int i = 0; i < reservedWords.length; i++)
//		{
//			if (token.equals(reservedWords[i])) return true;
//		}
//		return false;
//	}
//	
//	// TODO: implement queries that check the type of keyword here
//	
//	// Constructor information
//	private String token;
//	public PLToken(String token)
//	{
//		this.token = new String(token);
//	}
//	
//	public boolean typeMatch(String other)
//	{
//		if (token.equals(other))
//		{
//			return true;
//		}
//		else
//		{
//			return false;
//		}
//	}
}
