import java.io.*;
import mygram.lexer.Lexer;
import mygram.parser.Parser;
import mygram.node.Start;


public class ParserTest2
{
  public static void main(String[] args)
  {
    try
    {
      Parser parser = 
        new Parser(
        new Lexer(
        new PushbackReader(
        new FileReader(args[0].toString()), 1024)));

      Start ast = parser.parse();

	ast.apply(new ASTPrinter());
     System.out.println(ast);
    }
    catch (Exception e)
    {
      System.err.println(e);
    }
  }
}

