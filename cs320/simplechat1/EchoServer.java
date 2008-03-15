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
* @author Dr Timothy C. Lethbridge
* @author Dr Robert Lagani&egrave;re
* @author Fran&ccedil;ois B&eacute;langer
* @author Paul Holden
* @version July 2000
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
		
		String tempMsg = msg.toString();
		if(tempMsg.startsWith("#login ") && client.getInfo("loginid")== null){
			//System.out.println(client.getInfo("loginid"));
			String username = tempMsg.substring(7);
			client.setInfo("loginid", username);
			System.out.println(client.getInfo("loginid")+" has logged in");
			
			
		}
		else{
			
			//if #login & client.getLogin=null : store  else error
			System.out.println("Message received: " + msg + " from " + client.getInfo("loginid"));
			this.sendToAllClients(client.getInfo("loginid")+": "+msg);
		}
		
	}
	
	/**
	* This method overrides the one in the superclass.  Called
	* when the server starts listening for connections.
	*/
	protected void serverStarted()
	{
		System.out.println
		("Server listening for connections on port " + getPort());
	}
	
	/**
	* This method overrides the one in the superclass.  Called
	* when the server stops listening for connections.
	*/
	protected void serverStopped()
	{
		System.out.println
		("Server has stopped listening for connections.");
	}
	
	/**
	* Hook method called each time a new client connection is
	* accepted. The default implementation does nothing.
	* @param client the connection connected to the client.
	*/
	public void clientConnected(ConnectionToClient client) {
		//Set user ID here 
		System.out.println("" + client + " has connected");
	}
	
	/**
	* Hook method called each time a client disconnects.
	* The default implementation does nothing. The method
	* may be overridden by subclasses but should remains synchronized.
	*
	* @param client the connection with the client.
	*/
	public void clientDisconnected(
		ConnectionToClient client) {
	System.out.println(client.getInfo("loginid")+" has logged out");
		}
		
		/**
		* Hook method called each time an exception is thrown in a
		* ConnectionToClient thread.
		* The method may be overridden by subclasses but should remains
		* synchronized.
		*
		* @param client the client that raised the exception.
		* @param Throwable the exception thrown.
		*/
		public void clientException(
			ConnectionToClient client, Throwable exception) {
		System.out.println(client.getInfo("loginid") + " has disconnected.");
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
				//if first char of message is # then call method
				//else do this crap
				if(message.charAt(0) == '#')
					serverCommand(message);
				else{
					serverUI.display(message);
					sendToAllClients(message);
				}
			}
			
			public void serverCommand(String command){
				if(command.equalsIgnoreCase("#quit")){
					serverUI.display("QUITTING");
					quit();
				}
				else if(command.equalsIgnoreCase("#stop")){
					if(!isListening())
						serverUI.display("Server is already stopped");
					else{
						stopListening();
						sendToAllClients("Server has stopped listening for connections.");
					}
				}
				else if(command.equalsIgnoreCase("#close")){
					try
					{
						serverUI.display("CLOSING");
						close();
						isClosed = true;
					}
					catch(IOException e) {}
				}
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
				else if(command.equalsIgnoreCase("#getport")){
					serverUI.display("The current port is " + getPort());
				}
				else{
					serverUI.display("Invalid command");
				}
				
			}
			
}
//End of EchoServer class
