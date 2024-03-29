/**
* Group Name: The Little Caesars
* Members: Cory Stevens, Sean Maloy, James Crosetto, Madison Mosley, Seth Schwiethale
* Project Part: Simplechat Phase 2
* Title: ClientConsole
* CS320 Spring 2008
* March 17, 2008
* Java
* Phase 2 of the Simplechat program
* The console for ChatClient
*/

import java.io.*;
import client.*;
import common.*;

/**
* This class constructs the UI for a chat client.  It implements the
* chat interface in order to activate the display() method.
* Warning: Some of the code here is cloned in ServerConsole 
*
* @version March 2008
*/
public class ClientConsole implements ChatIF 
{
	//Class variables *************************************************
	
	/**
	* The default port to connect on.
	*/
	final public static int DEFAULT_PORT = 5432;
	
	//Instance variables **********************************************
	
	/**
	* The instance of the client that created this ConsoleChat.
	*/
	ChatClient client;
	
	
	//Constructors ****************************************************
	
	/**
	* Constructs an instance of the ClientConsole UI.
	*
	* @param loginID The loginID used by the client
	* @param host The host to connect to.
	* @param port The port to connect on.
	*/
	public ClientConsole(String loginID, String host, int port) 
	{
		try 
		{
			client= new ChatClient(loginID, host, port, this);
		} 
		catch(IOException exception) 
		{
			System.out.println("Error: Can't setup connection!"
				+ " Awaiting command.");
		}
	}
	
	
	//Instance methods ************************************************
	
	/**
	* This method waits for input from the console.  Once it is 
	* received, it sends it to the client's message handler.
	*/
	public void accept() 
	{
		try
		{
			BufferedReader fromConsole = 
			new BufferedReader(new InputStreamReader(System.in));
			String message;
			
			while (true) 
			{
				message = fromConsole.readLine();
				client.handleMessageFromClientUI(message);
			}
		} 
		catch (Exception ex) 
		{
			System.out.println
			("Unexpected error while reading from console!");
		}
	}
	
	/**
	* This method overrides the method in the ChatIF interface.  It
	* displays a message onto the screen.
	*
	* @param message The string to be displayed.
	*/
	public void display(String message) 
	{
		System.out.println("> " + message);
	}
	
	
	//Class methods ***************************************************
	
	/**
	* This method is responsible for the creation of the Client UI.
	*
	* @param args[0] The host to connect to.
	*/
	public static void main(String[] args) 
	{
		String loginID = "";
		String host = "";
		int port = 0;  //The port number
		
		//require there to be a login id
		if(args.length == 0){
			System.out.println("Usage:");
			System.out.println("ClientConsole loginId [host port]");
			System.exit(1);
		}
		//set the loginID
		loginID = args[0];
		//try/catch block to set defaults
		try
		{
			host = args[1];
			port = Integer.parseInt(args[2]);
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			host = "localhost";
			port = DEFAULT_PORT;
		}
		//catch nonnumbers in port
		catch(NumberFormatException e){
			System.out.println("Port must be a number");
			System.exit(1);
		}
		ClientConsole chat= new ClientConsole(loginID, host, port);
		chat.accept();  //Wait for console data
	}
}
//End of ConsoleChat class
