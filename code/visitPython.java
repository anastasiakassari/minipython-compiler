import mygram.analysis.*;
import mygram.node.*;
import java.util.*;

public class visitPython extends DepthFirstAdapter 
{
	public static int errors = 0;
	private static Hashtable globalSymbols;
	private static Hashtable funcParSymbols;
	private static Hashtable funcVarSymbols;
	
	private String funcName = "";
	private boolean inAFuncDef = false;
	private boolean errorInFunc = false;
	private boolean errorInEqualValue = false;
	private boolean foundEqualValue = false;
	
	
	public visitPython(Hashtable t)
	{
		globalSymbols = t;
		funcVarSymbols = new Hashtable();
		funcParSymbols = new Hashtable();
	}
	
	
	//Find out if there's a function with the same name that's been defined
	public void inADefineFunction(ADefineFunction func) 
	{
		String name = func.getId().toString();
		if (!globalSymbols.containsKey(name))
		{
			funcName = name;
			inAFuncDef = true;
		}
		
	}
	
	
	public void outADefineFunction(ADefineFunction func)
	{
		String name = func.getId().toString();
		int line = ((TId) func.getId()).getLine();
		if (putInGlobalHashtable(name, line, "Function"))
		{
			if (!errorInFunc)
				globalSymbols.put(name, func);
		}
		funcName = "";
		inAFuncDef = false;
		errorInFunc = false;
		errorInEqualValue = false;
		foundEqualValue = false;
	}
	
	
	//Insert the argument in the funcSymbols of the current function
	public void inASingleArgument(ASingleArgument arg)
	{
		String name = arg.getId().toString();
		Hashtable temp = putFuncInParHashtable();
		temp.put(name, arg);
		funcParSymbols.put(funcName, temp);
	}
	
	
	//Insert all the arguments in the funcSymbols of the current function
	// and check if there's an argument which has been already defined
	public void caseAListofArgument(AListofArgument list)
	{
		
		int line = ((TId) list.getId()).getLine();
		
		Hashtable table = putFuncInParHashtable();
		ArrayList<String> str = getArguments(list, new ArrayList<String>(), 0, table);	
		HashSet<String> hm = new HashSet<String>();
		
		for (String s: str)
		{
			hm.add(s);
			//System.out.print(s);
		}
		//System.out.println("");
		
		if (hm.size() != str.size())
		{
			errors++;
			errorInFunc = true;
			System.out.println("Line " + (line+1)/2 + ": function argument in function "+funcName+"is already defined.");
		}
			
		funcParSymbols.put(funcName, table);
		
		//System.out.println("\nFunction "+funcName+" size: "+table.size()+" and lowerlimit: "+countLowerLimit());
		
	}
	
	
	public void outAAssignStatement(AAssignStatement st)
	{
		String name = st.getId().toString();
		int line = ((TId) st.getId()).getLine();
		if (putInGlobalHashtable(name, line, "Variable"))
		{
			if (inAFuncDef)
			{
				Hashtable temp = putFuncInVarHashtable();
				temp.put(name, st);
				funcVarSymbols.put(funcName, temp);
				
			}
			else
			{
				globalSymbols.put(name, st);
			}
		}
		
	}
	
	
	//Find out if this function we're calling exists
	public void inACallingFunctioncall(ACallingFunctioncall fc)
	{
		String name = fc.getId().toString();
		int line = ((TId) fc.getId()).getLine();
		
		boolean exists = false;
		if (inAFuncDef)
		{
			//Either in the variables or in the parameters
			if (funcParSymbols.containsKey(funcName))
			{
				Hashtable symbols = (Hashtable)funcParSymbols.get(funcName);
				if (symbols.containsKey(name))
					exists = true;
			}
			if (funcVarSymbols.containsKey(funcName))
			{
				Hashtable symbols = (Hashtable)funcVarSymbols.get(funcName);
				if (symbols.containsKey(name))
					exists = true;
			}
		}
		if (globalSymbols.containsKey(name))
		{
			try
			{
				ADefineFunction adf = (ADefineFunction)globalSymbols.get(name);
				exists = true;
			}
			catch (ClassCastException e)
			{ }
		}
		
		if (!exists)
		{
			errors++;
			System.out.println("Line " + (line+1)/2 + ": method " + name +"is NOT defined.");
			return;
		}


		int args = 0;
		if (fc.getArglist() != null)
		{
			try
			{
				ASingleArgArglist a = (ASingleArgArglist)fc.getArglist();
				args = 1;
			}
			catch (ClassCastException e)
			{
				args = getArgs((AMultiArgArglist) fc.getArglist(), args);
			}
		}
		
		Hashtable ht = (Hashtable) funcParSymbols.get(name);
		//System.out.println("Line "+ (line+1)/2 + ": method has "+args+" args");
		
		if (args > ht.size())
		{
			errors++;
			System.out.println("Line " + (line+1)/2 + ": function "+name+"has MORE parameters.");
			return;
		}
		if (args < countLowerLimit(name))
		{
			errors++;
			System.out.println("Line " + (line+1)/2 + ": function "+name+"has LESS parameters.");
			return;
		}
		 
	}
	
	
	//Find out if this variable (identifier) has been initialized (exists)
	public void inAIdentifierExpression(AIdentifierExpression exp)
	{
		//System.out.println("~~~"+exp+"~~~");
		String name = exp.getId().toString();
		int line = ((TId) exp.getId()).getLine();
		
		boolean exists = false;
		if (inAFuncDef)
		{
			//Either in the variables or in the parameters
			if (funcParSymbols.containsKey(funcName))
			{
				Hashtable symbols = (Hashtable)funcParSymbols.get(funcName);
				if (symbols.containsKey(name))
					exists = true;
			}
			if (funcVarSymbols.containsKey(funcName))
			{
				Hashtable symbols = (Hashtable)funcVarSymbols.get(funcName);
				if (symbols.containsKey(name))
					exists = true;
			}
		}
		if (exists)
			return;
		if (globalSymbols.containsKey(name))
		{
			try
			{
				AAssignStatement aas = (AAssignStatement)globalSymbols.get(name);
				return;
			}
			catch (ClassCastException e)
			{ }
		}
		errors++;
		if (inAFuncDef)
		{
			errorInFunc = true;
			System.out.println("Line " + (line+1)/2 + ": variable " + name +"is NOT defined in function "+funcName);
		}
		else
		{
			System.out.println("Line " + (line+1)/2 + ": variable " + name +"is NOT defined.");
		}
	
	}
	
	
	
	//Private methods
	
	
	//Returns true when you can add the function/variable in the globalSymbols table
	//Returns true if it's not in the global hash table
	private boolean putInGlobalHashtable(String name, int line, String op)
	{
		//ERROR : Same name of function
		if (globalSymbols.containsKey(name))
		{
			errors++;
			System.out.println("Line " + (line+1)/2 + ": "+op+" " + name +"is already defined.");
			return false;
		}
		return true;
	}
	
	
	//Returns a new hash table or the existing one for the parameters of
	// the current function we're parsing 
	private Hashtable putFuncInParHashtable()
	{
		Hashtable temp = new Hashtable();
		if (funcParSymbols.containsKey(funcName))
			temp = ((Hashtable) funcParSymbols.get(funcName));
		return temp;
	}
	
	
	//Returns a new hash table or the existing one for the variables of
	// the current function we're parsing
	private Hashtable putFuncInVarHashtable()
	{
		Hashtable temp = new Hashtable();
		if (funcVarSymbols.containsKey(funcName))
			temp = ((Hashtable) funcVarSymbols.get(funcName));
		return temp;
	}
	
	
	//Returns an arraylist of all the names of the arguments/parameters in a define function & inserts these arguments in the table
	//Checks if there's a default value out of place in the arguments of the function
	private ArrayList<String> getArguments(AListofArgument list, ArrayList<String> set, int count, Hashtable table)
	{
		if (errorInEqualValue)
			return set;
		
		set.add(list.getId().toString());
		PEqualValue pev = list.getEqualValue();
		if (pev != null) // equal value
		{
			if (count == 0)
				foundEqualValue = true;
			else
				if (!foundEqualValue) // exists = false
				{
					errorInEqualValue = true;
					errors++;
					errorInFunc = true;
					System.out.println("Line "+(((TId) list.getId()).getLine()+1)/2+": found a default value "
							+ "in argument list of function "+funcName);
					
				}	
		}
		else
		{
			if (count != 0 && foundEqualValue)
					foundEqualValue = false;
		}
		count++;
		
		if (!errorInEqualValue)
			table.put(list.getId().toString(), new ASingleArgument(list.getId(), list.getEqualValue()));
		
		PArgument arg = list.getArgument();
		
		try
		{
			set.add(((ASingleArgument)arg).getId().toString());
			pev = ((ASingleArgument)arg).getEqualValue();
			if (pev != null) // equal value
			{
				if (count == 0)
					foundEqualValue = true;
				else
					if (!foundEqualValue) // exists = false
					{
						errorInEqualValue = true;
						errors++;
						errorInFunc = true;
						System.out.println("Line "+(((TId) ((ASingleArgument)arg).getId()).getLine()+1)/2+": found a default value "
								+ "in argument list of function "+funcName);
						
					}	
			}
			else
			{
				if (count != 0 && foundEqualValue)
						foundEqualValue = false;
			}
			count++;
			if (!errorInEqualValue)
				table.put(((ASingleArgument)arg).getId().toString(), 
						new ASingleArgument(((ASingleArgument)arg).getId(), ((ASingleArgument)arg).getEqualValue()));
			
		}
		catch(ClassCastException e)
		{
			set = getArguments(((AListofArgument)arg),set, count, table);
		}
		
		return set;
	}
	
	//Returns the number of args it has
	private int getArgs(AMultiArgArglist amaa, int count)
	{
		count++;
		PArglist arg = amaa.getArglist();
		ASingleArgArglist sarg = null;
		try
		{
			sarg = (ASingleArgArglist)arg;
			count++;
		}
		catch (ClassCastException e)
		{
			count = getArgs((AMultiArgArglist)arg, count);
		}
		return count;
	}
	
	
	//Returns the lower limit of parameters that a function call can have
	private int countLowerLimit(String name)
	{
		int count = 0;
		if (!funcParSymbols.containsKey(name))
			return 0;
		Hashtable temp = (Hashtable)funcParSymbols.get(name);
		for (Object o: temp.keySet())
		{
			ASingleArgument asa = null;
			try
			{
				asa = (ASingleArgument)temp.get((String)o);
			}
			catch(ClassCastException e)
			{}
			if (asa != null && asa.getEqualValue() != null)
				count++;
			
		}
		return (temp.size() - count);
	}
	
	
	//Getters
	
	
	//Return globalSymbols
	public static Hashtable getGlobal()
	{
		return globalSymbols;
	}
	
	
	//Return funcParSymbols
	public static Hashtable getFuncPar()
	{
		return funcParSymbols;
	}
	
	
	//Return funcVarSymbols
	public static Hashtable getFuncVar()
	{
		return funcVarSymbols;
	}


}
