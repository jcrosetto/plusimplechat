/**
* Group Name: The Little Caesars
* Members: Cory Stevens, Sean Maloy, James Crosetto, Madison Mosley, Seth Schwiethale
* Project Part: Simplechat Phase 2
* Title: ChatClient
* CS320 Spring 2008
* April 21, 2008
* Java
* Phase 3 of the Simplechat program
*/
package client;

import ocsf.client.*;
import common.*;
import java.io.*;
import java.util.StringTokenizer;

/**
* This class overrides some of the methods defined in the abstract
* superclass in order to give more functionality to the client.
*
* @version March 2008
*/
public class ChatClient extends AbstractClient
{
	//Instance variables **********************************************
	
	/**
	* The interface type variable.  It allows the implementation of
	* the display method in the client.
	*/
	private ChatIF clientUI;
	/**
	* The username of the client
	*/
	String username;
	
	/**
	 * The password of the client
	 */
	String password;
	
	//Constructors ****************************************************
	
	/**
	* Constructs an instance of the chat client.
	*
	* @param username The clients loginID
	* @param host The server to connect to.
	* @param port The port number to connect on.
	* @param clientUI The interface type variable.
	*/
	public ChatClient(String username, String password, String host, int port, ChatIF clientUI)
	throws IOException
	{
		super(host, port); //Call the superclass constructor
		this.clientUI = clientUI;
		this.username = username;
		this.password = password;
		try{
			openConnection();
			//Immediately send the login information to the server
			sendToServer("#login " + username + " " + password);
		}
		catch(IOException e){
			clientUI.display("Cannot open connection. Awaiting command.");
		}
	}
	
	
	//Instance methods ************************************************
	
	/**
	* This method handles all data that comes in from the server.
	*
	* @param msg The message from the server.
	*/
	public void handleMessageFromServer(Object msg)
	{
		clientUI.display(msg.toString());
	}
	
	/**
	* This method handles all data coming from the UI
	*
	* @param message The message from the UI.
	*/
	public void handleMessageFromClientUI(String message)
	{
		//check the first character of the message for the # sign
		if(message.length() > 0 && message.charAt(0) == '#'){
			clientCommand(message);
		}
		//check if connected, if not do not send message to server
		else if(isConnected()){
			try
			{
				sendToServer(message);
			}
			catch(IOException e)
			{
				clientUI.display
				("Could not send message to server.");
			}
		}
		//if not connected display error message
		else{
			clientUI.display("Not connected to server");
		}
	}
	
	/**
	* This method terminates the client.
	*/
	public void quit()
	{
		try
		{
			closeConnection();
		}
		catch(IOException e) {}
		System.exit(0);
	}
	/**
	* This method processes the message when there is a # as the first
	* character.
	*
	* @param command The command to process
	*/
	public void clientCommand(String command){
		//the quit command
		if(command.equalsIgnoreCase("#quit")){
			quit();
		}
		//the logoff command
		else if(command.equalsIgnoreCase("#logoff")){
			try
			{
				closeConnection();
			}
			catch(IOException e) {}
		}
		//the login command
		else if(command.equalsIgnoreCase("#login")){
			//cannot login if you are already logged on
			if(isConnected()){
				clientUI.display("Already logged in");
			}
			else{
				clientUI.display("logging in");
				try
				{
					openConnection();
					//resend the login information
					sendToServer("#login " + username + " " + password);
				}
				catch(IOException e) {
					clientUI.display("Unable to establish a" +
					" connection to the host " +
					getHost() + " on port " + getPort());
				}
			}
		}
		//the login command with username
		else if(command.startsWith("#login ")){
			//cannot login if already logged on
			if(isConnected()){
				clientUI.display("You must be logged off to do that");
			}
			else{
				try{
				String newUsername = command.substring(7, command.length());
				username = newUsername;
				openConnection();
				sendToServer("#login " + username + " " + password);
				}
				catch(IOException e){
					clientUI.display("Unable to establish a" +
						" connection to the host " +
						getHost() + " on port " + getPort());	
				}
			}
		}
		
		//gethost command
		else if(command.equalsIgnoreCase("#gethost"))
			clientUI.display("" + getHost());
		//getport command
		else if(command.equalsIgnoreCase("#getport"))
			clientUI.display("" + getPort());
		//sethost command
		//note the space after sethost, this ensures that a host is
		//specified
		else if(command.startsWith("#sethost ")){
			//cannot sethost if already logged on
			if(isConnected()){
				clientUI.display("You must be logged off to do that");
			}
			else{
				String tempHost = command.substring(9, command.length());
				setHost(tempHost);
				clientUI.display("Host set to " + tempHost);
			}
		}
		//setport commmand note space after command
		else if(command.startsWith("#setport ")){
			//cannot set port if connected
			if(isConnected()){
				clientUI.display("You must be logged off to do that");
			}
			else{
				String tempPort = command.substring(9, command.length());
				//Try/catch to ensure that only numbers are set as ports
				try
				{
					setPort(Integer.parseInt(tempPort));
					clientUI.display("Port set to " + tempPort);
				}
				catch(NumberFormatException e){
					clientUI.display("Port must be a number");
				}
			}
		}

		//check for private message command
		//first implementation on 4/15 by seth schwiethale
		else if(command.startsWith("#private ")){
			if(!isConnected()){
				clientUI.display("You must be logged on to do that");
			}
			else{
				doPM(command);
			}
		}

		//join channel command
		//added 5/1/08 by James Crosetto
		else if (command.startsWith("#joinchannel ")){
			if(!isConnected()){
				clientUI.display("You must be logged on to do that");
			}
			else{
				try{
					sendToServer(command);
				}
				catch(IOException e){
					clientUI.display("Unable to send message to server.");
				}
			}
		}
		
		//create channel command
		//added 5/1/08 by James Crosetto
		else if (command.startsWith("#createchannel ")){
			if(!isConnected()){
				clientUI.display("You must be logged on to do that");
			}
			else{
				try{
					sendToServer(command);
				}
				catch(IOException e){
					clientUI.display("Unable to send message to server.");
				}
			}
		}
		
		//channel command
		//added 5/1/08 by James Crosetto
		else if (command.startsWith("#channel")){
			if(!isConnected()){
				clientUI.display("You must be logged on to do that");
			}
			else{
				try{
					sendToServer(command);
				}
				catch(IOException e){
					clientUI.display("Unable to send message to server.");
				}
			}
		}
		
		//forward command
		//first implementation on 4/18 by cory
		else if (command.startsWith("#forward ")){
			if(!isConnected()){
				clientUI.display("You must be logged on to do that");
			}
			else{
				try{
					sendToServer(command);
				}
				catch(IOException e){
					clientUI.display("Unable to send message to server.");
				}
			}

		}
		//unforward command
		//first implementation on 4/19 by cory
		else if (command.startsWith("#unforward")){
			if(!isConnected()){
				clientUI.display("You must be logged on to do that");
			}
			else{
				try{
					sendToServer(command);
				}
				catch(IOException e){
					clientUI.display("Unable to send message to server.");
				}
			}

		}
		
		//block command
		//first implementation on 4/19 by cory
		else if (command.startsWith("#block ")){
			if(!isConnected()){
				clientUI.display("You must be logged on to do that");
			}
			else{
				try{
					sendToServer(command);
				}
				catch(IOException e){
					clientUI.display("Unable to send message to server.");
				}
			}

		}
		//unblock command
		//first implementation on 4/19 by cory
		else if (command.startsWith("#unblock")){
			if(!isConnected()){
				clientUI.display("You must be logged on to do that");
			}
			else{
				try{
					sendToServer(command);
				}
				catch(IOException e){
					clientUI.display("Unable to send message to server.");
				}
			}

		}
		//whoiblock command
		//first implementation on 4/20 by cory
		else if (command.startsWith("#whoiblock")){
			if(!isConnected()){
				clientUI.display("You must be logged on to do that");
			}
			else{
				try{
					sendToServer(command);
				}
				catch(IOException e){
					clientUI.display("Unable to send message to server.");
				}
			}

		}
		//unblock command
		//first implementation on 4/20 by cory
		else if (command.startsWith("#whoblocksme")){
			if(!isConnected()){
				clientUI.display("You must be logged on to do that");
			}
			else{
				try{
					sendToServer(command);
				}
				catch(IOException e){
					clientUI.display("Unable to send message to server.");
				}
			}

		}
		//change password on the server
		//first implementation on 4/19 by Cory
		else if(command.startsWith("#setpassword ")){
			//cannot set password if not connected
			if(!isConnected()){
				clientUI.display("You must be logged on to do that");
			}
			else{
				try
				{
					sendToServer(command);
				}
				catch(IOException e){
					clientUI.display("Unable to send message to server.");
				}
			}
		}
		//give the user help with the commands
		//Added on 4/20 by Cory
		else if(command.startsWith("#help")){
			commandHelp(command);
		}

		//catch anything that starts with #, but isn't a command
		else{
			clientUI.display("Invalid command");
			clientUI.display("Please use #help for command help");
		}
		
	}
	/**
	 * Method that helps the User with the various commands.
	 * Added 4/20 by Cory Stevens tediously filled out by Seth Schwiethale
	 * Modified 5/1 by James Crosetto (added channel stuff)
	 * @param command The command that contains the requested help
	 */
	private void commandHelp(String command){
		if (command.equalsIgnoreCase("#help")){
			clientUI.display("Commands\tdescription");
			clientUI.display("#quit\t\texit client program");
			clientUI.display("#logoff\tdisconnect from server");
			clientUI.display("#login\tused to connect to server");
			clientUI.display("#gethost\treturns hostname of host you are connected to");
			clientUI.display("#getport\treturns port number you are connected to");
			clientUI.display("#sethost\tif you are not logged in you may specify the host to log into");
			clientUI.display("#setport\tif you are not logged in you may specify the port number to connect to");
			clientUI.display("#private\tsend private message to a specified user");
			clientUI.display("#joinchannel\tconnect to a specified chat channel with optional password");
			clientUI.display("#createchannel\tcreate specified chat channel with optional password");
			clientUI.display("#channel\tdisplay chat channel you are connected to");
			clientUI.display("#forward\tforward message you recieve to another user");
			clientUI.display("#block\tblock messages sent from specified user");
			clientUI.display("#setpassword\tchanges password");
			clientUI.display("#help\t\tthis menu");
		}
		else if(command.equalsIgnoreCase("#help #quit")){
			clientUI.display("Usage:\t#quit");
		}
		else if(command.equalsIgnoreCase("#help #logoff")){
			clientUI.display("Usage:\t#logoff");
		}
		else if(command.equalsIgnoreCase("#help #login")){
			clientUI.display("Usage:\t#login");
		}
		else if(command.equalsIgnoreCase("#help #gethost")){
			clientUI.display("Usage:\t#gethost");
		}
		else if(command.equalsIgnoreCase("#help #getport")){
			clientUI.display("Usage:\t#getport");
		}
		else if(command.equalsIgnoreCase("#help #sethost")){
			clientUI.display("Usage:\t#sethost <hostname>");
		}
		else if(command.equalsIgnoreCase("#help #setport")){
			clientUI.display("Usage:\t#setport <portname>");
		}
		else if(command.equalsIgnoreCase("#help #private")){
			clientUI.display("Usage:\t#private <to username> <message>");
		}
		else if(command.equalsIgnoreCase("#help #joinchannel")){
			clientUI.display("Usage:\t#joinchannel <channelName> [password]");
		}
		else if(command.equalsIgnoreCase("#help #createchannel")){
			clientUI.display("Usage:\t#createchannel <channelName> [password]");
		}
		else if(command.equalsIgnoreCase("#help #channel")){
			clientUI.display("Usage:\t#channel");
		}
		else if(command.equalsIgnoreCase("#help #forward")){
			clientUI.display("Usage:\t#forward <to username>");
		}
		else if(command.equalsIgnoreCase("#help #block")){
			clientUI.display("Usage:\t#block <username>");
		}
		else if(command.equalsIgnoreCase("#help #setpassword")){
			clientUI.display("Usage:\t#setpassword <password>");
		}
	
		else
			clientUI.display("invalid help topic (make sure there are no characters or spaces at the tail)");
		
	}
	
	/**
	 * This method does the client side verification 
	 * of a private message
	 * @param command
	 */
	private void doPM(String command) {
		//check that private message command is in valid form
		StringTokenizer verify = new StringTokenizer(command);
		if (verify.countTokens()>=3){
			try{
				sendToServer(command);
			}
			catch(IOException e){
				clientUI.display("Unable to establish a" +
					" connection to the host " +
					getHost() + " on port " + getPort());	
			}
		}
		else clientUI.display("usage: #private <to user id> <message>");
	}


	/**
	* This method is called after the connection has been closed.
	*/
	public void connectionClosed() {
		clientUI.display("Connection to " + getHost() + " closed.");
	}
	/**
	* This method is called each time an exception is thrown by the client's
	* thread that is waiting for messages from the server.
	*
	* @param exception the exception raised
	*/
	public void connectionException(Exception exception) {
		clientUI.display("Connection to " + getHost() + " lost.");
	}
	/**
	* This method is called after a connection has been established.
	*/
	public void connectionEstablished() {
		clientUI.display("Welcome! You are connected to "+ getHost());
	}
}
//End of ChatClient class