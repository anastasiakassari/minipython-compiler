import java.io.*;
import mygram.lexer.Lexer;
import mygram.parser.Parser;
import mygram.node.*;
import java.util.*;

public class ParserTest
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

      Hashtable table =  new Hashtable();
      Start ast = parser.parse();
      ast.apply(new visitPython(table));
     
      Hashtable h = visitPython.getGlobal();

      
      System.out.println("\n\n\nHashset size: "+h.size());
      System.out.println("Error(s) found: "+visitPython.errors+"\n");
     
      System.out.println("Global variables and functions...");
      System.out.println(" ");
      for (Object name: h.keySet())
    	  System.out.println(((String) name));

      System.out.println(" ");
      
      System.out.println("Function variables...");
      System.out.println(" ");
      
      Hashtable param = visitPython.getFuncPar();
      Hashtable var = visitPython.getFuncVar();
      for (Object name: h.keySet())
      {
    	  String n = ((String) name);
    	  ADefineFunction adf = null;
    	  try 
    	  {
    		  adf = (ADefineFunction)h.get(n);
    	  }
    	  catch (ClassCastException e)
    	  {}
    	  if (adf == null)
    		  continue;
    	  System.out.println( n + "has the following symbols: ");
    	  Hashtable symbols = ((Hashtable) param.get(n));
    	  if (symbols != null)
    	  {
    		  System.out.println("\t Parameters:");
	    	  for (Object sym: symbols.keySet())
	    	  {
	    		  ASingleArgument asa = null;
	    		  try 
	    		  {
	    			  asa = ((ASingleArgument) symbols.get(((String)sym)));
	    		  }
	    		  catch(ClassCastException e)
					{ }
	    		  if (asa != null)
	    			  System.out.println("\t\t"+asa.toString());
	    		  else
	        		  System.out.println("\t\t"+((String) sym));
	    	  }
    	  }
    	  symbols = ((Hashtable) var.get(n));
    	  if (symbols != null)
    	  {
    		  System.out.println("\t Variables:");
	    	  for (Object sym: symbols.keySet())
	    	  {
	    		  ASingleArgument asa = null;
	    		  try 
	    		  {
	    			  asa = ((ASingleArgument) symbols.get(((String)sym)));
	    		  }
	    		  catch(ClassCastException e)
					{ }
	    		  if (asa != null)
	    			  System.out.println("\t\t"+asa.toString());
	    		  else
	        		  System.out.println("\t\t"+((String) sym));
	    	  }
    	  }
      }
    
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}

