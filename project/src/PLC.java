import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

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
		while (true)
		{
			System.out.println(scanner.next());
		}
		
//		PLParser parser = new PLParser();
//		parser.parse(tokenizer);
	}
}
