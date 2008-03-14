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
* @author Dr Timothy C. Lethbridge
* @author Dr Robert Lagani&egrave;
* @author Fran&ccedil;ois B&eacute;langer
* @version July 2000
*/
public class ChatClient extends AbstractClient
{
	//Instance variables **********************************************
	
	/**
	* The interface type variable.  It allows the implementation of 
	* the display method in the client.
	*/
	ChatIF clientUI; 
	
	String loginID;
	
	
	//Constructors ****************************************************
	
	/**
	* Constructs an instance of the chat client.
	*
	* @param host The server to connect to.
	* @param port The port number to connect on.
	* @param clientUI The interface type variable.
	*/
	////////////////////////////////
	////////////////////////////////
	//////loginid
	public ChatClient(String loginID, String host, int port, ChatIF clientUI) 
	throws IOException 
	{
		super(host, port); //Call the superclass constructor
		this.clientUI = clientUI;
		openConnection();
		//System.out.println("test " + getInetAddress());
		sendToServer("#login " + loginID);
		this.loginID = loginID;
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
		//if first char of message is # then call method
		//else do this crap
		if(message.charAt(0) == '#')
			clientCommand(message);
		else if(isConnected()){
			try
			{
				sendToServer(message);
			}
			catch(IOException e)
			{
				clientUI.display
				("Could not send message to server.  Terminating client.");
				quit();
			}
		}
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
	public void clientCommand(String command){
		if(command.equalsIgnoreCase("#quit")){
			quit();
		}
		else if(command.equalsIgnoreCase("#logoff")){
			try
			{
				closeConnection();
			}
			catch(IOException e) {}
		}
		else if(command.equalsIgnoreCase("#login")){
			if(isConnected()){
				clientUI.display("Already logged in");
			}
			else{
				clientUI.display("logging in");
				try
				{
					openConnection();
					sendToServer("#login " + loginID);
				}
				catch(IOException e) {}
			}
		}
		else if(command.equalsIgnoreCase("#gethost"))
			clientUI.display("" + getHost());
		else if(command.equalsIgnoreCase("#getport"))
			clientUI.display("" + getPort());
		
		else if(command.startsWith("#sethost ")){
			if(isConnected()){
				clientUI.display("You must be logged off to do that");
			}
			else{
				String tempHost = command.substring(9, command.length());
				setHost(tempHost);
				clientUI.display("Host set to " + tempHost);
			}
		}
		else if(command.startsWith("#setport ")){
			if(isConnected()){
				clientUI.display("You must be logged off to do that");
			}
			else{
				//should check if port is a number
				String tempPort = command.substring(9, command.length());
				setPort(Integer.parseInt(tempPort));
				clientUI.display("Port set to " + tempPort);
			}
		}
		else{
			clientUI.display("Invalid command");
		}
		
	}
	
	
	
	
	public void connectionClosed() {
		clientUI.display("Connection to " + getHost() + " closed.");
	}
	public void connectionException(Exception exception) {
		clientUI.display("Connection to " + getHost() + " lost.");
		quit();
	}
	public void connectionEstablished() {
		clientUI.display("Welcome! You are connected to "+ getHost());
	}
}
//End of ChatClient class