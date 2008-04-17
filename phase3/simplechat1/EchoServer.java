/**
* Group Name: The Little Caesars
* Members: Cory Stevens, Sean Maloy, James Crosetto, Madison Mosley, Seth Schwiethale
* Project Part: Simplechat Phase 2
* Title: EchoServer
* CS320 Spring 2008
* March 17, 2008
* Java
* Phase 2 of the Simplechat program
*/

import java.io.*;
import java.util.StringTokenizer;

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
			sendToAllClients(client.getInfo("loginid") + " has logged in.");
		}
		//if another login message is received after client is already logged on
		else if(tempMsg.startsWith("#login ")){
			try{
				client.sendToClient("You are already logged on");
			}
			catch(IOException e){}
		}
		//check for private message not to be sent to all clients
		else if(tempMsg.startsWith("#private ")){
			SendPrvtMsg(tempMsg, client);
		}
		//regular messages
		else{
			serverUI.display("Message received: " + msg + " from " 
			+ client.getInfo("loginid"));
			this.sendToAllClients(client.getInfo("loginid")+": "+msg);
		}
		
	}
	
	/**
	 * Method to send private message to specified user
	 * added 4/15 by seth schwiethale
	 * @param tempMsg
	 * @param client
	 */
	private void SendPrvtMsg(String tempMsg, ConnectionToClient client) {
		StringTokenizer msgData = new StringTokenizer(tempMsg);
		Thread[] clientThreadList = getClientConnections();
		ConnectionToClient clientTo;
		
		//get desired recipient and message from command
		try{
			msgData.nextToken();
			String recipient = msgData.nextToken();
			String shave = ("#private  "+recipient);
			String toSend = tempMsg.substring(shave.length());
			
			//do not allow user to pm themselves
			if(client.getInfo("loginid").equals(recipient)){
				client.sendToClient("You may not send private messages to yourself.");
				return;
			}
			
			//user sends a private message to the server
			else if(recipient.equalsIgnoreCase("SERVER")){
				serverUI.display("PRIVATE from "+client.getInfo("loginid")+": "+toSend);
				return;
			}
			
			//try to find recipient in clients and then send message
			for(int i = 0; i<clientThreadList.length;i++){
				clientTo = (ConnectionToClient) clientThreadList[i];
				if(((clientTo.getInfo("loginid")).equals(recipient))){
					clientTo.sendToClient(client.getInfo("loginid")+": "+toSend);
					serverUI.display(client.getInfo("loginid")+" said,'"+toSend+"' to "+clientTo.getInfo("loginid"));
					return;
				}
			}
			//recipient was not found in connected clients
			client.sendToClient("the user you specified is not connected");
		}
		catch(Exception e){}
}
		

	/**
	 * Seperate method for Server sent Private Messages.
	 * added because of difference with SendPrivMsg parameters
	 * -tag author:a:"Seth Schwiethale"
	 * @version 3 04/16/08
	 * @date 04/
	 * @param tempMsg
	 */
	private void serverPM(String tempMsg) {
		StringTokenizer msgData = new StringTokenizer(tempMsg);
		Thread[] clientThreadList = getClientConnections();
		ConnectionToClient clientTo;
		
		//get desired recipient and message from command
		try{
			msgData.nextToken();
			String recipient = msgData.nextToken();
			String shave = ("#private  "+recipient);
			String toSend = tempMsg.substring(shave.length());
			
			//try to find recipient in clients and then send message
			for(int i = 0; i<clientThreadList.length;i++){
				clientTo = (ConnectionToClient) clientThreadList[i];
				if(((clientTo.getInfo("loginid")).equals(recipient))){
					clientTo.sendToClient("SERVER MESSAGE: "+toSend);
					return;
				}
			}
			//recipient was not found in connected clients
			serverUI.display("the user you specified is not connected");
		}
		catch(Exception e){
			
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
		sendToAllClients(client.getInfo("loginid")+" has logged out");
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
		sendToAllClients(client.getInfo("loginid") + " has disconnected.");
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
		if(message.length() > 0 && message.charAt(0) == '#')
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
			sendToAllClients("Server is quitting");
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
				sendToAllClients("Server is closing");
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
		//check for private message command
		//first implementation on 4/15 by seth schwiethale
		else if(command.startsWith("#private")){
			serverPM(command);
		}
		else{
			serverUI.display("Invalid command");
		}
		
	}

}
//End of EchoServer class