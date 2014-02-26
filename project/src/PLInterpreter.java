import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.uci.cs241.pl241.frontend.PLEndOfFileException;
import com.uci.cs241.pl241.frontend.PLSyntaxErrorException;
//import com.uci.cs241.pl241.frontend.PLTokenizer;

public class PLInterpreter
{
	public static void main(String[] args) throws IOException
	{
		if (args.length != 1)
		{
			System.err.println("usage: PLInterpreter <program.txt>");
			System.exit(-1);
		}

		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
//		PLScanner tokenizer = new PLScanner(reader);
//		
//		try
//		{
//			while (true)
//			{
//				String token = tokenizer.next();
//				System.out.print(token);
//			}
//		}
//		catch (PLEndOfFileException e)
//		{
//			System.err.println(e);
//		} 
//		catch (PLSyntaxErrorException e)
//		{
//			System.err.println(e);
//			e.printStackTrace();
//		}
	}

}
