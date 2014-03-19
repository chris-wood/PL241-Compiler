package com.uci.cs241.pl241.frontend;

public class PLToken
{	
	public static final int errorToken = 0;
	public static final int timesToken = 1;
	public static final int divToken = 2;
	public static final int plusToken = 11;
	public static final int minusToken = 12;
	public static final int eqlToken = 20;
	public static final int neqToken = 21;
	public static final int lssToken = 22;
	public static final int geqToken = 23;
	public static final int leqToken = 24;
	public static final int gttToken = 25;
	public static final int periodToken = 30;
	public static final int commaToken = 31;
	public static final int openBracketToken = 32;
	public static final int closeBracketToken = 34;
	public static final int closeParenToken = 35;
	public static final int openBraceToken = 36;
	public static final int closeBraceToken = 37;
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
	
	public static int tokenToId(String tok)
	{
		if (tok.equals("*")) return timesToken;
		else if (tok.equals("/")) return divToken;
		else if (tok.equals("+")) return plusToken;
		else if (tok.equals("-")) return minusToken;
		else if (tok.equals("==")) return eqlToken;
		else if (tok.equals("!=")) return neqToken;
		else if (tok.equals("<")) return lssToken;
		else if (tok.equals(">=")) return geqToken;
		else if (tok.equals("<=")) return leqToken;
		else if (tok.equals(">")) return gttToken;
		else if (tok.equals(".")) return periodToken;
		else if (tok.equals(",")) return commaToken;
		else if (tok.equals("[")) return openBracketToken;
		else if (tok.equals("]")) return closeBracketToken;
		else if (tok.equals(")")) return closeParenToken;
		else if (tok.equals("{")) return openBraceToken;
		else if (tok.equals("}")) return closeBraceToken;
		else if (tok.equals("<-")) return becomesToken;
		else if (tok.equals("then")) return thenToken;
		else if (tok.equals("do")) return doToken;
		else if (tok.equals("(")) return openParenToken;
		else if (tok.equals(";")) return semiToken;
		else if (tok.equals("end")) return endToken;
		else if (tok.equals("od")) return odToken;
		else if (tok.equals("fi")) return fiToken;
		else if (tok.equals("else")) return elseToken;
		else if (tok.equals("let")) return letToken;
		else if (tok.equals("call")) return callToken;
		else if (tok.equals("if")) return ifToken;
		else if (tok.equals("while")) return whileToken;
		else if (tok.equals("return")) return returnToken;
		else if (tok.equals("var")) return varToken;
		else if (tok.equals("array")) return arrToken;
		else if (tok.equals("function")) return funcToken;
		else if (tok.equals("procedure")) return procToken;
		else if (tok.equals("begin")) return beginToken;
		else if (tok.equals("main")) return mainToken;
		else return -1;
	}
	
	public static boolean isRelationalToken(int tok)
	{
		if (tok == eqlToken || tok == neqToken || tok == lssToken ||
				tok == geqToken || tok == leqToken || tok == gttToken)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
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
}
