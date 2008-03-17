// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com

package client;

import ocsf.client.*;
import common.*;
import java.io.*;

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
	* the loginID of the client
	*/
	String loginID;
	
	
	//Constructors ****************************************************
	
	/**
	* Constructs an instance of the chat client.
	*
	* @param loginID The clients loginID
	* @param host The server to connect to.
	* @param port The port number to connect on.
	* @param clientUI The interface type variable.
	*/
	public ChatClient(String loginID, String host, int port, ChatIF clientUI)
	throws IOException
	{
		super(host, port); //Call the superclass constructor
		this.clientUI = clientUI;
		this.loginID = loginID;
		try{
			openConnection();
			//Immediately send the login information to the server
			sendToServer("#login " + loginID);
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
					sendToServer("#login " + loginID);
				}
				catch(IOException e) {
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
		//catch anything that starts with #, but isnt a command
		else{
			clientUI.display("Invalid command");
		}
		
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