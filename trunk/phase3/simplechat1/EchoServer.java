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
import java.util.ArrayList;
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
		inputUserInfo();
		addNewUser("server", "server" );
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
		//added 4/15 by seth
		else if(tempMsg.startsWith("#private ")){
			sendPrvtMsg(tempMsg, client);
		}
		//change channel
		//first implementation 4/16 by james crosetto
		else if(tempMsg.startsWith("#channel ")){
			changeChannel(tempMsg, client);

		}
		//forward messages
		//added 4/16 by Cory Stevens
		else if(tempMsg.startsWith("#forward ")){
			forwardingSetup(tempMsg, client);

		}
		//block users
		//added 4/19 by Cory Stevens
		else if(tempMsg.startsWith("#block ")){
			blockUser(tempMsg, client);

		}
		//unblock users
		//added 4/19 by Cory Stevens
		else if(tempMsg.startsWith("#unblock")){
			unblockUser(tempMsg, client);

		}
		//unblock users
		//added 4/19 by Cory Stevens
		else if(tempMsg.startsWith("#whoiblock")){
			whoIBlock(client);

		}
		//unblock users
		//added 4/19 by Cory Stevens
		else if(tempMsg.startsWith("#whoblocksme")){
			whoBlocksMe(client);

		}
		//remove forwarding to users
		//added 4/20 by James Crosetto
		else if(tempMsg.startsWith("#unforward")){
			unforwardUser(tempMsg, client);

		}
		//regular messages
		else{
			serverUI.display("Message received: " + msg + " from " 
					+ client.getInfo("username"));
			this.sendToAllClients(client.getInfo("username")+": "+msg, client);
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
				sendToChannel(client.getInfo("username") + 
						" has connected to channel: default", "default");
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
		outputUserInfo();

	}

	/**
	 * Method to send private message to specified user
	 * Added 4/15 
	 * @param tempMsg
	 * @param client
	 * @author seth schwiethale
	 */
	@SuppressWarnings("unchecked")
	private void sendPrvtMsg(String tempMsg, ConnectionToClient client) {
		StringTokenizer msgData = new StringTokenizer(tempMsg);
		Thread[] clientThreadList = getClientConnections();
		ConnectionToClient clientTo;
		ArrayList<String> blockedUsers;

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
				//serverUI.display("PRIVATE from "+client.getInfo("username")+": "+toSend);
				return;
			}

			//try to find recipient in clients and then send message
			for(int i = 0; i<clientThreadList.length;i++){
				clientTo = (ConnectionToClient) clientThreadList[i];
				if(((clientTo.getInfo("username")).equals(recipient))){
					if(clientTo.getInfo("blocking")!=null){
						blockedUsers = new ArrayList((ArrayList<String>)clientTo.getInfo("blocking"));
						//make sure recipient isn't blocking the sender
						if(blockedUsers.contains(client.getInfo("username"))){
							client.sendToClient("Message could not be sent, "+clientTo.getInfo("username")+" is blocking you!");
							return;
						}
					}
					clientTo.sendToClient("PM from " + client.getInfo("username")+": "+toSend);
					//serverUI.display(client.getInfo("username")+" said,'"+toSend+"' to "+clientTo.getInfo("username"));
					ArrayList<String> sent = new ArrayList<String>();
					sent.add((String)client.getInfo("username"));
					forwardMessage(clientTo, toSend, sent);
					return;
				}
			}
			//recipient was not found in connected clients
			client.sendToClient("the user you specified is not connected");
		}
		catch(Exception e){}
	}
	/**
	 * Method blocks a user from sending messages to another user
	 * @param msg The message containing the blocked user
	 * @param client The client that is initiating the block
	 * @author Cory Stevens
	 */
	private void blockUser(String msg, ConnectionToClient client){
		String recipient = "";

		String[] parsedString = msg.split(" ");
		recipient = parsedString[1];

		try {
			//do not allow user to forward to themselves
			if(client.getInfo("username").equals(recipient)){
				client.sendToClient("You may not block yourself.");
				return;
			}
			//check if the user exists
			if(userInfo.containsKey(recipient)){
				client.sendToClient("Messages from " + recipient + " will be blocked");
				storeBlockingInfo(client, recipient);
				processForwardBlock(client, recipient);
			}
			//recipient was not found in connected clients
			else{
				client.sendToClient("The user you specified does not exist");
			}
		} catch (IOException e) {}
	}
	
	/**
	 * Method that cancels any blocking that is established between the two users
	 * @param blockingClient The client that is initiating the block
	 * @param blockedClient The client that is being blocked
	 */
	@SuppressWarnings("unchecked")
	private void processForwardBlock(ConnectionToClient blockingClient, String blockedClient){
		
		Thread[] clientThreadList = getClientConnections();
		ConnectionToClient clientTo = null;
		
		for(int i = 0; i<clientThreadList.length;i++){
			clientTo = (ConnectionToClient) clientThreadList[i];
			if(((clientTo.getInfo("username")).equals(blockedClient))){
				break;
			}
		}
		//if blockedClient doesn't forward to anyone return
		if(clientTo.getInfo("forwardTo") == null || ((ArrayList<String>)clientTo.getInfo("forwardTo")).isEmpty() ){
			return;
		}
		//ArrayList of the blockedClient's forwarding
		ArrayList<String> blockedForward = (ArrayList<String>) clientTo.getInfo("forwardTo");
		//check if blockedClient is forwarding to blockingClient
		if(blockedForward.contains(blockingClient.getInfo("username"))){
			try {
				blockingClient.sendToClient("Forwarding of messages from " + blockedClient + " to you has been terminated");
				//remove blockingClient from blockedClient's block list
				blockedForward.remove(blockingClient.getInfo("username"));
				clientTo.sendToClient("Forwarding to " + blockingClient.getInfo("username") + " has been canceled because "+
						blockingClient.getInfo("username")+ " is blocking messages from you");
			} 
			catch (IOException e) {}
			
		}
		
	}
	
	/**
	 * Method to store the blocking info for each user involved in the blocking.
	 * Added 4/19
	 * @param fromClient The client that is initiating the blocking
	 * @param toClient The client that is receiving the blocking
	 * @author Cory Stevens
	 * 
	 */
	@SuppressWarnings("unchecked")
	private void storeBlockingInfo(ConnectionToClient fromClient, String toClient){
		ArrayList<String> forwardTo;

		if(fromClient.getInfo("blocking") == null){
			forwardTo = new ArrayList<String>();
			forwardTo.add(toClient);
		}
		else{
			ArrayList<String> tempArrayList = (ArrayList<String>) fromClient.getInfo("blocking");
			tempArrayList.add(toClient);
			forwardTo = new ArrayList<String>(tempArrayList);
		}


		fromClient.setInfo("blocking", forwardTo);
	}

	/**
	 * Method that unblocks a user
	 * @param msg The message containing the user to unblock
	 * @param client The client that is unblocking the user
	 * @author cory stevens
	 * 
	 */
	@SuppressWarnings("unchecked")
	private void unblockUser(String msg, ConnectionToClient client){
		
		ArrayList<String> blockedUsers;
		String[] parsedString = msg.split(" ");
		
		if(client.getInfo("blocking") == null || ((ArrayList<String>)client.getInfo("blocking")).isEmpty() ){
			try {
				client.sendToClient("No blocking is in effect.");
			} 
			catch (IOException e) {}
			return;
		}

		blockedUsers = new ArrayList((ArrayList<String>)client.getInfo("blocking")); 
		//if the unblock has a user specified only removed one user
		if(msg.startsWith("#unblock ")){
			if(!blockedUsers.contains(parsedString[1])){
				try {
					client.sendToClient("Messages from " + parsedString[1] + " were not blocked.");
				} 
				catch (IOException e) {}
			}
			else{
				blockedUsers.remove(parsedString[1]);
				try {
					client.sendToClient("Messages from " + parsedString[1] + " will now be displayed.");
				} 
				catch (IOException e) {}
			}			
		}
		//if the unblock command doesnt specify a user, remove all blocked users
		else{
			try {
				Iterator it = blockedUsers.iterator();
				while(it.hasNext()) {
					client.sendToClient("Messages from " + it.next() + " will now be displayed.");
				}
				blockedUsers.clear();
			} 
			catch (IOException e) {}
		}

		client.setInfo("blocking", blockedUsers);
	}
	
	/**
	 * Method that displays to the user the users that they are blocking.
	 * Added 4/20
	 * @param client The client that wants to know who they block.
	 * @author cory stevens
	 * 
	 */
	@SuppressWarnings("unchecked")
	private void whoIBlock(ConnectionToClient client){
		ArrayList<String> blockedUsers;
		
		if(client.getInfo("blocking") == null || ((ArrayList<String>)client.getInfo("blocking")).isEmpty() ){
			try {
				client.sendToClient("No blocking is in effect.");
			} 
			catch (IOException e) {}
			return;
		}
		blockedUsers = new ArrayList((ArrayList<String>)client.getInfo("blocking")); 
		try {
			Iterator it = blockedUsers.iterator();
			while(it.hasNext()) {
				client.sendToClient("Messages from " + it.next() + " are blocked.");
			}
		} 
		catch (IOException e) {}
	}
	/**
	 * Method that displays to the user who is blocking them.
	 * Added 4/20
	 * @param client The client that wishes to know who is blocking him
	 * @author cory stevens
	 */
	@SuppressWarnings("unchecked")
	private void whoBlocksMe(ConnectionToClient client){
		Thread[] clientThreadList = getClientConnections();
		ConnectionToClient tempClient;
		ArrayList<String> blockedUsers = new ArrayList<String>();
		String user = (String)client.getInfo("username");

		for(int i = 0; i<clientThreadList.length;i++){
			tempClient = (ConnectionToClient) clientThreadList[i];
			if((tempClient.getInfo("blocking")) != null)
				if(((ArrayList<String>)(tempClient.getInfo("blocking"))).contains(user)){
					blockedUsers.add((String)tempClient.getInfo("username"));
				}
		}
		try {
			Iterator it = blockedUsers.iterator();
			while(it.hasNext()) {
				client.sendToClient("Messages to " + it.next() + " are blocked.");
			}
		}
		catch (IOException e) {}
	}

	/**
	 * Separate method for Server sent Private Messages.
	 * added because of difference with SendPrivMsg parameters
	 * @author Seth Schwiethale
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
						sendToChannel(client.getInfo("username") + 
								" has connected to channel: " +
								channel, channel);
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
	 * Method that sets up forwarding to a specific user.
	 * Added 4/19
	 * @param msg The msg containing the user to forward to
	 * @param client The Client that is setting up the forwarding
	 * @author cory stevens
	 */
	@SuppressWarnings("unchecked")
	private void forwardingSetup(String msg, ConnectionToClient client){
		String recipient = "";
		Thread[] clientThreadList = getClientConnections();
		ConnectionToClient clientTo;
		ArrayList<String> blockedUsers;

		String[] parsedString = msg.split(" ");
		recipient = parsedString[1];

		try {
			//do not allow user to forward to themselves
			if(client.getInfo("username").equals(recipient)){
				client.sendToClient("You may not forward messages to yourself.");
				return;
			}
			//try to find recipient in clients and then send message
			for(int i = 0; i<clientThreadList.length;i++){
				clientTo = (ConnectionToClient) clientThreadList[i];
				if(((clientTo.getInfo("username")).equals(recipient))){
					if(clientTo.getInfo("blocking")!=null){
						blockedUsers = new ArrayList((ArrayList<String>)clientTo.getInfo("blocking"));
						//make sure recipient isn't blocking the sender
						if(blockedUsers.contains(client.getInfo("username"))){
							client.sendToClient("Cannot forward. "+clientTo.getInfo("username")+" is blocking you!");
							return;
						}
					}
					clientTo.sendToClient("User " + client.getInfo("username") + 
					" is now forwarding messages to you.");
					client.sendToClient("You are now forwarding messages to " + recipient);
					storeForwardingInfo(client, clientTo);
					return;
				}
			}
			//recipient was not found in connected clients
			client.sendToClient("The user you specified is not connected");
		} catch (IOException e) {}


	}
	/**
	 * Method to store the forwarding info for each user involved in the forwarding.
	 * Added 4/19
	 * @param fromClient The client that is initiating the forwarding
	 * @param toClient The client that is receiving the forward
	 * @author Cory Stevens
	 * 
	 */
	@SuppressWarnings("unchecked")
	private void storeForwardingInfo(ConnectionToClient fromClient, ConnectionToClient toClient){
		ArrayList<String> forwardTo;

		if(fromClient.getInfo("forwardTo") == null){
			forwardTo = new ArrayList<String>();
			forwardTo.add((String)toClient.getInfo("username"));
		}
		else{
			ArrayList<String> tempArrayList = (ArrayList<String>) fromClient.getInfo("forwardTo");
			tempArrayList.add((String)toClient.getInfo("username"));
			forwardTo = new ArrayList<String>(tempArrayList);
		}

		fromClient.setInfo("forwardTo", forwardTo);
	}
	/**
	 * Method that will forward messages to their recipients 
	 * @param fromClient The client that is forwarding the message
	 * @param msg The message that is being forwarded
	 * @param sent The list of users the message has been forwarded to
	 * @author cory stevens
	 */
	@SuppressWarnings("unchecked")
	private void forwardMessage(ConnectionToClient fromClient, String msg, ArrayList<String> sent){
		Thread[] clientThreadList = getClientConnections();
		ConnectionToClient clientTo;
		//if the fromClient does not have anyone to forward to return
		if(fromClient.getInfo("forwardTo") == null){
			return;
		}
		ArrayList<String> recipients;
		recipients = new ArrayList<String>((ArrayList<String>)fromClient.getInfo("forwardTo"));

		sent.add((String)fromClient.getInfo("username"));
		
		int n = recipients.size();
		for(int i = 0; i < n ; i++){
			String recipient = recipients.get(i);
			//try to find recipient in clients and then send message
			for(int j = 0; j<clientThreadList.length;j++){
				clientTo = (ConnectionToClient) clientThreadList[j];
				if(!sent.contains(recipient) && ((clientTo.getInfo("username")).equals(recipient))){
					try {
						clientTo.sendToClient("Forward from " + fromClient.getInfo("username")+": "+msg);
					} catch (IOException e) {}
					forwardMessage(clientTo, msg, sent);
					return;
				}
			}
		}
	}
	
	/**
	 * Method that removes forwarding to a user
	 * @param msg The message containing the user to unforward
	 * @param client The client that is unforwarding another client
	 * @author james crosetto
	 * 
	 */
	@SuppressWarnings("unchecked")
	private void unforwardUser(String msg, ConnectionToClient client){
		
		ArrayList<String> forwardedUsers;
		String[] parsedString = msg.split(" ");
		
		if(client.getInfo("forwardTo") == null || ((ArrayList<String>)client.getInfo("forwardTo")).isEmpty() ){
			try {
				client.sendToClient("No forwarding is in effect.");
			} 
			catch (IOException e) {}
			return;
		}

		forwardedUsers = new ArrayList((ArrayList<String>)client.getInfo("forwardTo")); 
		//if the unforward has a user specified only remove one user
		if(msg.startsWith("#unforward ")){
			if(!forwardedUsers.contains(parsedString[1])){
				try {
					client.sendToClient("Messages have not been forwarded to " + parsedString[1]);
				} 
				catch (IOException e) {}
			}
			else{
				forwardedUsers.remove(parsedString[1]);
				try {
					client.sendToClient("Messages are no longer forwarded to " + parsedString[1]);
				} 
				catch (IOException e) {}
			}			
		}
		//if the unforward command doesnt specify a user, remove all forwarded users
		else{
			try {
				Iterator it = forwardedUsers.iterator();
				while(it.hasNext()) {
					client.sendToClient("Messages are no longer forwarded to " + it.next());
				}
				forwardedUsers.clear();
			} 
			catch (IOException e) {}
		}

		client.setInfo("forwardTo", forwardedUsers);
	}

	/**
	 * Method that that will take the userInfo HashMap and output it into a text file.
	 * Added on 4/18
	 * @author Cory Stevens
	 */
	private void outputUserInfo(){
		Iterator<Map.Entry<String, String>> it = userInfo.entrySet().iterator();

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("userInfo.txt"));

			while (it.hasNext()) {
				Map.Entry<String,String> entry = (Map.Entry<String,String>) it.next();
				String key = (String) entry.getKey();
				String value = (String) entry.getValue();
				writer.write(key + "\t" + value);
				writer.newLine();
			}
			writer.close();
		} 
		catch (IOException e) {}

	}

	/**
	 * Method that will take in a text file and store it in the userInfo HashMap.
	 * Added on 4/18
	 * @author Cory Stevens
	 */
	private void inputUserInfo(){

		try {
			BufferedReader reader = new BufferedReader(new FileReader("userInfo.txt"));

			//Loop as long as there are input lines.
			String line = null;
			while ((line=reader.readLine()) != null) {
				String[] parsedLine = line.split("\t");
				userInfo.put(parsedLine[0], parsedLine[1]);
			}
			reader.close();
		} 
		catch (FileNotFoundException e) {} 
		catch (IOException e) {}
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
	 * Method that helps the User with the various commands.
	 * Added 4/20 by Cory Stevens
	 * @param command The command that contains the requested help
	 */
	private void commandHelp(String command){

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
	 * This method is called each time a new client connection is accepted.
	 * On 4/18 Cory added a check for null username for users that didn't 
	 * fully connect.
	 * @param client the connection connected to the client.
	 */
	public void clientConnected(ConnectionToClient client) {
		//added 4/18
		if(client.getInfo("username") != null){
			serverUI.display("" + client + " has connected");
		}
	}

	/**
	 * This method is called each time a client disconnects.
	 * On 4/18 Cory added a check for null username for users that didn't
	 * fully connect.  For example if a user connects but uses the wrong password.
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
	 * fully connect.  For example if a user connects but doesn't use a password.
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
		else if(command.startsWith("#outputUsers")){
			outputUserInfo();
		}
		else if(command.startsWith("#inputUsers")){
			inputUserInfo();
		}
		//give the user help with the commands
		//Added on 4/20 by Cory
		else if(command.startsWith("#help ")){
			commandHelp(command);
		}
		else{
			serverUI.display("Invalid command");
			serverUI.display("Please use #help for command help");
		}
	}

	/**
	 * Overloaded method to send a message to all clients on the same channel as the 
	 * client sending the message. 
	 * Added 4/20 
	 * @param msg The message to be sent
	 * @param client The client sending the message
	 * @author James Crosetto
	 */
	@SuppressWarnings("unchecked")
	public void sendToAllClients(Object msg, ConnectionToClient client){

		Thread[] clientThreadList = getClientConnections();

		for (int i=0; i<clientThreadList.length; i++)
		{
			try
			{
				ArrayList<String> blockedUsers;
				ConnectionToClient clientTemp = (ConnectionToClient)clientThreadList[i];
				if(client.getInfo("channel").equals(clientTemp.getInfo("channel"))){
					if(clientTemp.getInfo("blocking")!=null){
						blockedUsers = new ArrayList((ArrayList<String>)clientTemp.getInfo("blocking"));
						//make sure recipient isn't blocking the sender
						if(blockedUsers.contains(client.getInfo("username"))){
							return;
						}//if user on channel is blocking sender
					}//if blocking list is not null
					
					clientTemp.sendToClient(msg);
				}//if client is on senders channel

			}
			catch (Exception ex) {}
		}
	}

}
//End of EchoServer class