import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.uci.cs241.pl241.frontend.PLEndOfFileException;
import com.uci.cs241.pl241.frontend.PLParser;
import com.uci.cs241.pl241.frontend.PLScanner;
import com.uci.cs241.pl241.frontend.PLSyntaxErrorException;
import com.uci.cs241.pl241.frontend.PLTokenizer;


public class PLC
{
	public static void main(String[] args) throws IOException, PLSyntaxErrorException, PLEndOfFileException
	{
		if (args.length != 1)
		{
			System.err.println("usage: PLC <program.txt>");
			System.exit(-1);
		}
		
		// Scanner test
		PLScanner scanner = new PLScanner(args[0]);
		ArrayList<String> tokens = new ArrayList<String>();
		try
		{
			while (true)
			{
				scanner.next();
				tokens.add(scanner.symstring + "(" + scanner.sym + ")");
			}
		} 
		catch (Exception e)
		{	
		}
		
		// Format and display the tokens
		StringBuilder builder = new StringBuilder("[");
		for (String t : tokens)
		{
			builder.append("'" + t + "', ");
		}
		builder.deleteCharAt(builder.toString().length() - 1).append("]");
		System.out.println(builder.toString());
		
		//// RUN THE PARSER
		scanner = new PLScanner(args[0]);
		PLParser parser = new PLParser();
		parser.parse(scanner);
		
//		PLParser parser = new PLParser();
//		parser.parse(tokenizer);
	}
}
