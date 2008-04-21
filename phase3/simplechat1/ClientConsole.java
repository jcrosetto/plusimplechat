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
	* @param username The username used by the client
	* @param password The password used by the client
	* @param host The host to connect to.
	* @param port The port to connect on.
	*/
	public ClientConsole(String username,String password, String host, int port) 
	{
		try 
		{
			client= new ChatClient(username, password, host, port, this);
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
		String username = "";
		String password = "";
		String host = "";
		int port = 0;  //The port number
		
		//require there to be a login id
		if(args.length == 0){
			System.out.println("Usage:");
			System.out.println("ClientConsole username password [host port]");
			System.exit(1);
		}
		//set the username
		username = args[0];
		//set the password
		//Added 4/18 by Cory Stevens
		try {
			password = args[1];
		} 
		catch (ArrayIndexOutOfBoundsException e1) {
			System.out.println("You must specify a password!");
			System.out.println("Usage:");
			System.out.println("ClientConsole username password [host port]");
			System.exit(1);
		}
		//try/catch block to set defaults
		try
		{
			host = args[2];
			port = Integer.parseInt(args[3]);
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			host = "localhost";
			port = DEFAULT_PORT;
			//Check where the exception is and output a message
			//Added 4/18 by Cory
			if(e.equals(2))
				System.out.println("Using default host of 'localhost' ");
			else
				System.out.println("Using default port of '5432' ");
		}
		//catch nonnumbers in port
		catch(NumberFormatException e){
			System.out.println("Port must be a number");
			System.exit(1);
		}
		ClientConsole chat= new ClientConsole(username, password ,host, port);
		chat.accept();  //Wait for console data
	}
}
//End of ClientConsole class
