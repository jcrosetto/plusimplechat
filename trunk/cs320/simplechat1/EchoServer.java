// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

import java.io.*;
import ocsf.server.*;
import common.*;

/**
* This class overrides some of the methods in the abstract 
* superclass in order to give more functionality to the server.
*
* @version March 2008
*/
public class EchoServer extends AbstractServer 
{
	//Class variables *************************************************
	
	/**
	* The interface type variable.  It allows the implementation of 
	* the display method in the server.
	*/
	private ChatIF serverUI;
	
	/**
	* True if the server is closed.
	*/
	private boolean isClosed = false; 
	
	//Constructors ****************************************************
	
	/**
	* Constructs an instance of the echo server.
	*
	* @param port The port number to connect on.
	* @param serverUI The interface type variable
	*/
	public EchoServer(int port, ChatIF serverUI) 
	{
		super(port);
		this.serverUI = serverUI;
	}
	
	
	//Instance methods ************************************************
	
	/**
	* This method handles any messages received from the client.
	*
	* @param msg The message received from the client.
	* @param client The connection from which the message originated.
	*/
	public void handleMessageFromClient
	(Object msg, ConnectionToClient client)
	{
		//convert object to string	
		String tempMsg = msg.toString();
		//if the loginid is null and the message receiving is not a login message
		if(!tempMsg.startsWith("#login ") && client.getInfo("loginid")== null){
			try{
				client.sendToClient("LoginID must be specified");
				client.close();
			}
			catch(IOException e){}
		}
		//if the loginid is null and the message receiving is a login message
		else if(tempMsg.startsWith("#login ") && client.getInfo("loginid")== null){
			String username = tempMsg.substring(7);
			client.setInfo("loginid", username);
			serverUI.display(client.getInfo("loginid")+" has logged in");
		}
		//if another login message is recieved after client is already logged on
		else if(tempMsg.startsWith("#login ")){
			try{
			client.sendToClient("You are already logged on");
			}
			catch(IOException e){}
		}
		//regular messages
		else{
			serverUI.display("Message received: " + msg + " from " 
				+ client.getInfo("loginid"));
			this.sendToAllClients(client.getInfo("loginid")+": "+msg);
		}
		
	}
	
	/**
	* This method is called when the server starts listening for connections
	*/
	protected void serverStarted()
	{
		serverUI.display("Server listening for connections on port " + getPort());
	}
	
	/**
	* This method is called when the server stops listening for connections.
	*/
	protected void serverStopped()
	{
		serverUI.display("Server has stopped listening for connections.");
	}
	
	/**
	* This method is called each time a new client connection is accepted
	*
	* @param client the connection connected to the client.
	*/
	public void clientConnected(ConnectionToClient client) {
		serverUI.display("" + client + " has connected");
	}
	
	/**
	* This method is called each time a client disconnects.
	*
	* @param client the connection with the client.
	*/
	public void clientDisconnected(ConnectionToClient client) {
		serverUI.display(client.getInfo("loginid")+" has logged out");
	}
	/**
	* This method is called each time an exception is thrown in a ConnectionToClient
	* thread,
	*
	* @param client the client that raised the exception.
	* @param Throwable the exception thrown.
	*/
	public void clientException(ConnectionToClient client, Throwable exception) {
		serverUI.display(client.getInfo("loginid") + " has disconnected.");
	}

	/**
	* This method terminates the server.
	*/
	public void quit()
	{
		try
		{
			close();
		}
		catch(IOException e) {}
		System.exit(0);
	}
	
	/**
	* This method handles all data coming from the UI            
	*
	* @param message The message from the UI.    
	*/
	public void handleMessageFromServerUI(String message)
	{
		if(message.charAt(0) == '#')
			serverCommand(message);
		else{
			serverUI.display(message);
			sendToAllClients("SERVER MSG> " + message);
		}
	}
		/**
	* This method processes the messages from the serverUI           
	*
	* @param message The command that will be processed.    
	*/
	public void serverCommand(String command){
		//Quit command
		if(command.equalsIgnoreCase("#quit")){
			serverUI.display("Server is quitting");
			quit();
		}
		//Stop command
		else if(command.equalsIgnoreCase("#stop")){
			if(!isListening())
				serverUI.display("Server is already stopped");
			else{
				stopListening();
				sendToAllClients("Server has stopped listening for connections.");
			}
		}
		//close command
		else if(command.equalsIgnoreCase("#close")){
			try
			{
				serverUI.display("Server is closing");
				close();
				isClosed = true;
			}
			catch(IOException e) {}
		}
		//setport command
		else if(command.startsWith("#setport ")){
			if(!isClosed){
				serverUI.display("The server must be closed" +
					" to change the port");
			}
			else{
				String tempPort = command.substring(9, command.length());
				setPort(Integer.parseInt(tempPort));
				serverUI.display("Port set to " + tempPort);
			}
		}
		//start command
		else if(command.startsWith("#start")){
			if(isListening())
				serverUI.display("Server is already running");
			else{
				try
				{
					listen();
				}
				catch(IOException e) {}
				isClosed = false;
			}
		}
		//getport command
		else if(command.equalsIgnoreCase("#getport")){
			serverUI.display("The current port is " + getPort());
		}
		//catch all other commands
		else{
			serverUI.display("Invalid command");
		}
		
	}
	
}
//End of EchoServer class