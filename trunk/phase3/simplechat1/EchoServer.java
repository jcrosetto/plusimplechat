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
import java.util.*;

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
	
	/**
	 * Hashmap to represent user information that is stored server side.
	 */
	private HashMap<String,String> userInfo; 
	
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
		userInfo = new HashMap<String, String>();
		userInfo.put("server", "server" );
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
		//if the username is null and the message receiving is not a login message
		if(!tempMsg.startsWith("#login ") && client.getInfo("username")== null){
			try{
				client.sendToClient("Username must be specified");
				client.close();
			}
			catch(IOException e){}
		}
		else if(tempMsg.startsWith("#login ")){
			handleLogin(tempMsg, client);
			
		}
		//check for private message not to be sent to all clients
		else if(tempMsg.startsWith("#private ")){
			SendPrvtMsg(tempMsg, client);
		}
		//change channel
		//first implementation 4/16 by james crosetto
		else if(tempMsg.startsWith("#channel ")){
			changeChannel(tempMsg, client);
			
		}
		//regular messages
		else{
			serverUI.display("Message received: " + msg + " from " 
			+ client.getInfo("username"));
			this.sendToAllClients(client.getInfo("username")+": "+msg);
		}
		
	}
	
	/**
	 * Method that handles all login information from users
	 * Added 4/16
	 * @param msg The entire message sent by the client attempting to login
	 * @param client The client that is connecting to the server.
	 * @author cory stevens
	 */
	private void handleLogin(String msg, ConnectionToClient client){
		
		String password = "";
		String username = "";
		String[] parsedString = msg.split(" ");
		password = parsedString[2];
		username = parsedString[1];
		//if the username is null and the message receiving is a login message
		if(client.getInfo("username")== null){
			//check if client is a new user
			if(!userInfo.containsKey(username)){
				setClientUsername(username, client);
				setClientPassword(password, client);
				addNewUser(username,password);
				try{
					client.sendToClient("You are a new user");
				}
				catch(Exception e){}
			}
			//client is not a new user
			else{
				String tempPassword = userInfo.get(username);
				System.out.println(tempPassword);
				//compare stored password to connecting users password
				if(tempPassword.equals(password)){
					try{
						client.sendToClient("Welcome back " + username);
						setClientUsername(username, client);
						setClientPassword(password, client);
					}
					catch(Exception e){}
					
				}
				//if passwords are not equal inform the user and exit them out.
				else{
					try{
						client.sendToClient("I'm sorry, but your password is incorrect for that username.");
						client.close();
					}
					catch(Exception e){}
					return;
				}
				
			}
			serverUI.display(client.getInfo("username")+" has logged in");
			sendToAllClients(client.getInfo("username") + " has logged in.");
			//following added 4/16 by james crosetto
			client.setInfo("channel", "default");
			try{
				client.sendToClient("You are now connected to channel: default");
			}
			catch(Exception e){}
		}
		//if another login message is received after client is already logged on
		else if(client.getInfo("username") != null){
			try{
				client.sendToClient("You are already logged on");
			}
			catch(IOException e){}
		}
		
	}
	
	/**
	 * Method to add a new user into the hashmap
	 * Added 4/15
	 * @param username The new users username
	 * @param passowrd The new users password
	 * @author cory stevens
	 */
	private void addNewUser(String username, String password){
		
		userInfo.put(username, password);
		
	}
	
	/**
	 * Method to send private message to specified user
	 * Added 4/15 
	 * @param tempMsg
	 * @param client
	 * @author seth schwiethale
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
			if(client.getInfo("username").equals(recipient)){
				client.sendToClient("You may not send private messages to yourself.");
				return;
			}
			
			//user sends a private message to the server
			else if(recipient.equalsIgnoreCase("SERVER")){
				serverUI.display("PRIVATE from "+client.getInfo("username")+": "+toSend);
				return;
			}
			
			//try to find recipient in clients and then send message
			for(int i = 0; i<clientThreadList.length;i++){
				clientTo = (ConnectionToClient) clientThreadList[i];
				if(((clientTo.getInfo("username")).equals(recipient))){
					clientTo.sendToClient("PM from " + client.getInfo("username")+": "+toSend);
					serverUI.display(client.getInfo("username")+" said,'"+toSend+"' to "+clientTo.getInfo("username"));
					return;
				}
			}
			//recipient was not found in connected clients
			client.sendToClient("the user you specified is not connected");
		}
		catch(Exception e){}
}
		

	/**
	 * Separate method for Server sent Private Messages.
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
				if(((clientTo.getInfo("username")).equals(recipient))){
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
	 * Used when a client sends a #channel command. Changes the clients channel or
	 * returns the channel they are currently on.
	 * @param tempMsg The message with the #channel command and the new channel
	 * 					to connect to.
	 * @param client The client changing channels.
	 * @date created 4/16/08
	 * @author James Crosetto
	 */
	private void changeChannel(String tempMsg, ConnectionToClient client)
	{
		//channel is specified if msg length is greater than 9
		try{
			if(tempMsg.length() > 9){
				//checks to see if channel contains space
				int space = tempMsg.indexOf(" ", 9);
				//get channel
				//no space - valid channel name
				if(space == -1){
					String channel = tempMsg.substring(9, tempMsg.length());
					if(client.getInfo("channel").equals(channel)){
						client.sendToClient("You are already on channel: " +
								client.getInfo("channel"));
					}
					else{
						client.setInfo("channel", channel);
						client.sendToClient("You are now connected to channel: " +
								channel);
					}
				}
				else{
					client.sendToClient("Invalid channel name. Channels cannot contain spaces.");
				}
				
					
			}
			//display current channel as private message if new channel isn't specified
			else{
				client.sendToClient("You are currently connected to channel: " + 
						client.getInfo("channel"));
				
			}
		}
		catch(Exception e){}
	}
	
	/**
	 * Sends a message to a specified channel.
	 * @param msg The message to be sent.
	 * @param channel The channel to send the message to.
	 * @date created 4/17/08
	 * @author James Crosetto
	 */
	private void sendToChannel(String msg, String channel)
	{
		Thread[] clientThreadList = getClientConnections();
		ConnectionToClient clientTo;
		boolean sent = false; //true if message sent to a client
		
		try{
			
			//try to find recipient in clients and then send message
			for(int i = 0; i < clientThreadList.length; i++){
				clientTo = (ConnectionToClient) clientThreadList[i];
				if(((clientTo.getInfo("channel")).equals(channel))){
					clientTo.sendToClient("SERVER MESSAGE: " + msg);
					sent = true;
				}
			}
			
			if(!sent)
				serverUI.display("The specified channel was not found.");
		}
		catch(Exception e){
			
		}
	}
	/**
	 * Method to store the clients username
	 * @param username The username of the client being stored.
	 * @param client The client that the username is being stored for.
	 * @date created 4/18/08
	 * @author Cory Stevens
	 */
	private void setClientUsername(String username, ConnectionToClient client){
		client.setInfo("username", username);
		//userInfo.put("username", username);
		
	}
	/**
	 * Method to store the clients password
	 * @param username The password of the client being stored.
	 * @param client The client that the password is being stored for.
	 * @date created 4/18/08
	 * @author Cory Stevens
	 */
	private void setClientPassword(String password, ConnectionToClient client){
		client.setInfo("password", password);
		//userInfo.put("password", password);
		
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
	* On 4/18 Cory added a check for null username for users that didn't
	* full connect.  For example if a user connects but uses the wrong password.
	* @param client the connection with the client.
	*/
	public void clientDisconnected(ConnectionToClient client) {
		//added 4/18
		if(client.getInfo("username") != null){
		serverUI.display(client.getInfo("username")+" has logged out");
		sendToAllClients(client.getInfo("username")+" has logged out");
		}
	}
	/**
	* This method is called each time an exception is thrown in a ConnectionToClient
	* thread,
	*
	* On 4/18 Cory added a check for null username for users that didn't
	* full connect.  For example if a user connects but doesn't use a password.
	* @param client the client that raised the exception.
	* @param Throwable the exception thrown.
	*/
	public void clientException(ConnectionToClient client, Throwable exception) {
		//added 4/18
		if(client.getInfo("username") != null){
		serverUI.display(client.getInfo("username") + " has disconnected.");
		sendToAllClients(client.getInfo("username") + " has disconnected.");
		}
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
		//send message to specified channel
		//first implementation 4/16/08 by james crosetto
		else if(command.startsWith("#channel ")){
			try{
				//find end of channel name
				int space = command.indexOf(" ", 9);
				//get channel name
				String channel = command.substring(9, space);
				sendToChannel(command.substring(space+1, command.length()), channel);
			}
			catch(IndexOutOfBoundsException e){
				serverUI.display("Usage: #channel <channel> <message>");
			}
			
		}
		else{
			serverUI.display("Invalid command");
		}
		
	}

}
//End of EchoServer class