/**
 * Group Name: The Little Caesars
 * Members: Cory Stevens, Sean Maloy, James Crosetto, Madison Mosley, Seth Schwiethale
 * Project Part: Simplechat Phase 2
 * Title: EchoServer
 * CS320 Spring 2008
 * April 21, 2008
 * Java
 * Phase 3 of the Simplechat program
 */

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.StringTokenizer;

//import ocsf.server.*;
import com.lloseng.ocsf.server.*;
import common.*;
import java.util.*;

/**
 * This class overrides some of the methods in the abstract 
 * superclass in order to give more functionality to the server.
 *
 * @version March 2008
 */
public class EchoServer implements Observer
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
	
	/**
	 * Hashmap to represent channels available.
	 */
	private HashMap<String, ChannelInfo> channels; 
	
	/**
	 * EchoServer is Observable in the Observer Layer
	 */
	private ObservableOriginatorServer obsOrigServ;

	//Constructors ****************************************************

	/**
	 * Constructs an instance of the echo server.
	 *
	 * Observer Layer added 5/9/08
	 * 
	 * @param serverUI The interface type variable
	 * @param newOoS The ObservableOriginatorServer instantiated in ServerConsole
	 */
	public EchoServer(ChatIF serverUI, ObservableOriginatorServer newOoS) 
	{
		this.obsOrigServ = newOoS;
		this.obsOrigServ.addObserver(this); //Adds this as an observer to the set of observers for this object
		this.serverUI = serverUI; 
		userInfo = new HashMap<String, String>();
		channels = new HashMap<String, ChannelInfo>();
		channels.put("default", new ChannelInfo("default"));
		inputUserInfo();
		addNewUser("server", "server" );
	}


	//Instance methods ************************************************

	/**
	 * This method handles any messages received from the ObservableServer.
	 *
	 * Observer Layer implemented 5/9/08
	 * 
	 * @param o is Observable
	 * @param msg The message received from the client.
	 */
	public void update(Observable o,Object msg)
	{
		//convert object to an OriginatorMessage
		OriginatorMessage origMsg = (OriginatorMessage)msg;
		//get the string message from the OriginatorMessage
		String tempMsg = (String)origMsg.getMessage();
		
		//Check for Server Message
		if(tempMsg.startsWith("#OS:")){
			serverUI.display(tempMsg);
			return;
		}
		
		//otherwise msg was sent from a client has a ConnectionToClient associated
		ConnectionToClient client = origMsg.getOriginator();
		
		//command will determine which method will handle msg
		String command;
		int index = tempMsg.indexOf(" ");
		if (index != -1)
			command = tempMsg.substring(0, index).toLowerCase();
		else
			command = tempMsg.toLowerCase();

		//if the username is null and the message receiving is not a login message
		if(!command.equals("#login") && client.getInfo("username")== null){
			try{
				client.sendToClient("Username must be specified");
				client.close();
			}
			catch(IOException e){}
		}
		else if(command.equals("#login")){
			handleLogin(tempMsg, client);

		}
		//check for private message not to be sent to all clients
		//added 4/15 by seth
		else if(command.equals("#private")){
			sendPrvtMsg(tempMsg, client);
		}
		//join channel
		//added 5/1/08 by James Crosetto
		else if(command.equals("#joinchannel")){
			try{
				if (tempMsg.length() > 13)
					joinChannel(tempMsg.substring(13,tempMsg.length()), client);
				else
					client.sendToClient("You must specify a channel name. Usage: " + 
							"#joinchannel <channel> [password]");
			}	
			catch(IOException e){}

		}
		//create channel
		//added 5/1/08 by James Crosetto
		else if(command.equals("#createchannel")){
			try{
				if (tempMsg.length() > 15)
					createChannel(tempMsg.substring(15,tempMsg.length()), client);
				else
					client.sendToClient("You must specify a channel name. Usage: " + 
							"#createchannel <channel> [password]");
			}	
			catch(IOException e){}

		}
		//change channel
		//first implementation 4/16 by james crosetto
		//modified 5/1/08 by James Crosetto
		else if(command.equals("#channel")){
			try{
				client.sendToClient("You are currently connected to channel: " + 
						client.getInfo("channel"));
			}
			catch(IOException e){}

		}
		//display a sorted list of channels to the user
		//added 5/8/08 by James Crosetto
		else if(command.equals("#channellist")){
			try{
				String[] chanlist = getChannels(client);
				Arrays.sort(chanlist);
				
				client.sendToClient("List of all channels:");
				for (int i = 0; i < chanlist.length; i++)
				{
					client.sendToClient(chanlist[i]);
				}
				
			}
			catch(IOException e){}

		}
		else if(command.equals("#setpassword"))
		{
			String pass = tempMsg.substring(tempMsg.indexOf(" ")+1, tempMsg.length());
			setPassword(client, pass.trim());
		}
		//forward messages
		//added 4/16 by Cory Stevens
		else if(command.equals("#forward")){
			forwardingSetup(tempMsg, client);

		}
		//block users
		//added 4/19 by Cory Stevens
		else if(command.equals("#block")){
			blockUser(tempMsg, client);

		}
		//unblock users
		//added 4/19 by Cory Stevens
		else if(command.equals("#unblock")){
			unblockUser(tempMsg, client);

		}
		//unblock users
		//added 4/19 by Cory Stevens
		else if(command.equals("#whoiblock")){
			whoIBlock(client);

		}
		//unblock users
		//added 4/19 by Cory Stevens
		else if(command.equals("#whoblocksme")){
			whoBlocksMe(client);

		}
		//remove forwarding to users
		//added 4/20 by James Crosetto
		else if(command.equals("#unforward")){
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
		username = parsedString[1].toLowerCase();	//all user names are stored in lowercase
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
				String tempPassword = (String)userInfo.get(username);
				//compare stored password to connecting users password
				if(password.equals(tempPassword)){
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
			serverUI.display(client.getInfo("username")+ " has logged in.");
			obsOrigServ.sendToAllClients(client.getInfo("username") + " has logged in.");
			
			//following added 4/16 by james crosetto
			//modified 5/1 for improved channels
			//add client to default channel
			channels.get("default").addClient(client);
			client.setInfo("channel", "default");
			try{
				sendToChannel(client.getInfo("username") + 
						" has connected to channel: default", "default");
			}
			catch(Exception e){}
		}
		//if another login message is received after client is already logged on
		else{
			try{
				client.sendToClient("You are already logged on");
			}
			catch(IOException e){}
		}

	}
	
	/**
	 * Method to change a users password
	 * Added 5/8
	 * @param client The client requesting the new password
	 * @param password The new users password
	 * @author james crosetto
	 */
	private void setPassword(ConnectionToClient client, String password){

		client.setInfo("password", password);
		userInfo.put((String)client.getInfo("username"), password);
		outputUserInfo();

	}

	/**
	 * Method to add a new user into the hashmap
	 * Added 4/15
	 * @param username The new users username
	 * @param password The new users password
	 * @author cory stevens
	 */
	private void addNewUser(String username, String password){

		userInfo.put(username, password);
		outputUserInfo();

	}

	/**
	 * Method to send private message to specified user
	 * Added 4/15 
	 * 
	 * @param tempMsg Contains the message and recipient's username
	 * @param client The client sending the message
	 * @author seth schwiethale
	 * Modified 5/1 by James Crosetto
	 */
	@SuppressWarnings("unchecked")
	private void sendPrvtMsg(String tempMsg, ConnectionToClient client) {
		StringTokenizer msgData = new StringTokenizer(tempMsg);
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
				serverUI.display("PRIVATE from "+client.getInfo("username")+": "+toSend);
				return;
			}
			
			clientTo = findClient(recipient);
			
			//recipient was not found in connected clients
			if (clientTo == null){
				client.sendToClient("the user you specified is not connected");
				return;
			}
			//client found - check blocking
			else if(clientTo.getInfo("blocking")!=null){
				blockedUsers = (ArrayList<String>)clientTo.getInfo("blocking");
				//make sure recipient isn't blocking the sender
				if(blockedUsers.contains(client.getInfo("username"))){
					client.sendToClient("Message could not be sent, "+clientTo.getInfo("username")+" is blocking you!");
					return;
				}
			}
			clientTo.sendToClient("PM from " + client.getInfo("username")+": "+toSend);
			//serverUI.display(client.getInfo("username")+" said,'"+toSend+"' to "+clientTo.getInfo("username"));
			
			//forward the message to people in clientTo's forwarding list
			ArrayList<String> sent = new ArrayList<String>();
			sent.add((String)client.getInfo("username"));
			forwardMessage(clientTo, toSend, sent);			
		}
		catch(Exception e){}
	}
	
	/**
	 * Finds a client with a specified username
	 * @param clientName The username of the client
	 * @return The ConnectionToClient object with username of clientName. If none exists
	 * returns null.
	 * @author James Crosetto 5/1/08
	 */
	private ConnectionToClient findClient(String clientName) {
		Thread[] clientThreadList = obsOrigServ.getClientConnections();
		
		ConnectionToClient client; 
		
		for(int i = 0; i<clientThreadList.length;i++){
			client = (ConnectionToClient) clientThreadList[i];
			if(((client.getInfo("username")).equals(clientName))){
				return client;
			}
		}
		
		//not found
		return null;
	}
	
	/**
	 * Method blocks a user from sending messages to another user
	 * @param msg The message containing the blocked user
	 * @param client The client that is initiating the block
	 * @author Cory Stevens
	 * Modified 5/1 by James Crosetto
	 */
	private void blockUser(String msg, ConnectionToClient client){
		String recipient = "";

		String[] parsedString = msg.split(" ");

		try {
			
			if(parsedString.length <= 1){
				client.sendToClient("You must specify a user to block.");
				return;
			}
				
				
			recipient = parsedString[1];
			
			//do not allow user to block himself/herself
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
	 * Method that cancels any forwarding that is established between the two users
	 * @param blockingClient The client that is initiating the block
	 * @param blockedClient The client that is being blocked
	 * Modified 5/1 by James Crosetto
	 */
	@SuppressWarnings("unchecked")
	private void processForwardBlock(ConnectionToClient blockingClient, String blockedClient){
		
		ConnectionToClient blocked = findClient(blockedClient);
		
		//if blockedClient doesn't forward to anyone return
		if(blocked.getInfo("forwardTo") == null || ((ArrayList<String>)blocked.getInfo("forwardTo")).isEmpty() ){
			return;
		}
		//ArrayList of the blockedClient's forwarding
		ArrayList<String> blockedForward = (ArrayList<String>)blocked.getInfo("forwardTo");
		//check if blockedClient is forwarding to blockingClient
		if(blockedForward.contains(blockingClient.getInfo("username"))){
			try {
				blockingClient.sendToClient("Forwarding of messages from " + blocked.getInfo("username") + " to you has been terminated");
				//remove blockingClient from blockedClient's block list
				blockedForward.remove(blockingClient.getInfo("username"));
				blocked.sendToClient("Forwarding to " + blockingClient.getInfo("username") + " has been canceled because "+
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
	 * Modified 5/1 by James Crosetto
	 */
	@SuppressWarnings("unchecked")
	private void storeBlockingInfo(ConnectionToClient fromClient, String toClient){
		ArrayList<String> blocking;

		//add the client that gets blocked to the blocking list of fromClient
		if(fromClient.getInfo("blocking") == null){
			blocking = new ArrayList<String>();
			blocking.add(toClient);
			fromClient.setInfo("blocking", blocking);
		}
		else{
			blocking = (ArrayList<String>) fromClient.getInfo("blocking");
			blocking.add(toClient);
		}
	}

	/**
	 * Method that unblocks a user
	 * @param msg The message containing the user to unblock
	 * @param client The client that is unblocking the user
	 * @author cory stevens
	 * Modified 5/1 by James Crosetto
	 */
	@SuppressWarnings("unchecked")
	private void unblockUser(String msg, ConnectionToClient client){
		
		ArrayList<String> blockedUsers;
		String[] parsedString = msg.split(" ");
		ConnectionToClient blocked;
		
		if(client.getInfo("blocking") == null || ((ArrayList<String>)client.getInfo("blocking")).isEmpty() ){
			try {
				client.sendToClient("No blocking is in effect.");
			} 
			catch (IOException e) {}
			return;
		}
		
		blockedUsers = (ArrayList<String>)client.getInfo("blocking"); 
		//if the unblock has a user specified only removed one user
		if(parsedString.length > 1){
			if(!blockedUsers.contains(parsedString[1])){
				try {
					client.sendToClient("Messages from " + parsedString[1] + " were not blocked.");
				} 
				catch (IOException e) {}
			}
			else{
				blocked = findClient(parsedString[1]);
				blockedUsers.remove(parsedString[1]);
				
				try {
					client.sendToClient("Messages from " + parsedString[1] + " will now be displayed.");
					if(blocked != null)
						blocked.sendToClient("You are no longer blocked by " + client.getInfo("username"));
				} 
				catch (IOException e) {}
			}			
		}
		//if the unblock command doesn't specify a user, remove all blocked users
		else{
			try {
				Iterator it = blockedUsers.iterator();
				while(it.hasNext()) {
					blocked = findClient((String)it.next());
					client.sendToClient("Messages from " + blocked.getInfo("username") + " will now be displayed.");
					if(blocked != null)
						blocked.sendToClient("You are no longer blocked by " + client.getInfo("username"));
				}
				blockedUsers.clear();
			} 
			catch (IOException e) {}
		}
	}
	
	/**
	 * Method that displays to the user the users that they are blocking.
	 * Added 4/20
	 * @param client The client that wants to know who they block.
	 * @author cory stevens
	 * Modified 5/1 by James Crosetto
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
		blockedUsers = (ArrayList<String>)client.getInfo("blocking"); 
		try {
			Iterator it = blockedUsers.iterator();
			while(it.hasNext()) {
				client.sendToClient("Messages from " + it.next()
									+ " are blocked.");
			}
		} 
		catch (IOException e) {}
	}
	
	/**
	 * Method that displays to the user who is blocking them.
	 * Added 4/20
	 * @param client The client that wishes to know who is blocking him
	 * @author cory stevens
	 * Rewritten 5/1 by James Crosetto
	 */
	@SuppressWarnings("unchecked")
	private void whoBlocksMe(ConnectionToClient client){
		Thread[] threadList = obsOrigServ.getClientConnections();
		ConnectionToClient clientTemp;
		boolean blocked = false;
		
		try{
			//find all of the clients blocking this client
			for(int i = 0; i < threadList.length; i++)
			{
				clientTemp = (ConnectionToClient)threadList[i];
				
				ArrayList<String> blocks = (ArrayList<String>)clientTemp.getInfo("blocking");
				
				if(blocks != null && blocks.contains(client.getInfo("username"))){
					client.sendToClient("Messages to " + clientTemp.getInfo("username")
							+ " are blocked.");
					blocked = true;
				}
			}
			
			if(!blocked)
				client.sendToClient("No one is blocking you.");
		}
		catch (IOException e) {}
	}

	/**
	 * Separate method for Server sent Private Messages.
	 * added because of difference with SendPrivMsg parameters
	 * @author Seth Schwiethale
	 * @version 3 04/16/08
	 * @param tempMsg
	 * Modified 5/1 by James Crosetto
	 */
	private void serverPM(String tempMsg) {
		StringTokenizer msgData = new StringTokenizer(tempMsg);
		ConnectionToClient clientTo;

		//get desired recipient and message from command
		try{
			msgData.nextToken();
			String recipient = msgData.nextToken();
			String shave = ("#private  "+recipient);
			String toSend = tempMsg.substring(shave.length());

			//try to find recipient in clients and then send message
			clientTo = findClient(recipient);
			
			//recipient was not found in connected clients
			if(clientTo == null)
				serverUI.display("the user you specified is not connected");
			
			else {
				clientTo.sendToClient("SERVER MESSAGE: "+toSend);
				return;
			}
		}
		catch(Exception e){}
	}

	/**
	 * Used when a client sends a #joinchannel command. Changes the client's channel if
	 * the channel specified with the command is different from the client's current channel.
	 * Channels are stored in lower case. Password must be supplied with channel name if
	 * the channel is protected by a password.
	 * @param channel The channel the client is trying to join
	 * @param client The client joining channel.
	 * @throws IOException
	 * @author James Crosetto 5/1/08
	 */
	private void joinChannel(String channel, ConnectionToClient client) throws IOException
	{
		
		//remove leading and trailing white space
		channel = channel.trim();
		
		//split channel into channel and password
		String[] info = channel.split(" ");
		
		//holds a password supplied by user
		String password = "";
		
		//password specified
		if(info.length > 1) 
			password = info[1];
		
		channel = info[0];
		
		//convert channel to lowercase
		channel = channel.toLowerCase();
		
		//check to see if client is currently on channel
		if(((String)client.getInfo("channel")).equals(channel)){
			client.sendToClient("You are already on channel: " +
					client.getInfo("channel"));
		}
		//not on channel
		else{
			//check to see if channel exists
			if (channels.containsKey(channel))
			{
				//get new channel info
				ChannelInfo newChannel = channels.get(channel);
				
				//get old channel info
				ChannelInfo oldChannel = channels.get(client.getInfo("channel"));
				
				//check to see if channel has password
				//no password
				if(newChannel.getPassword().length() == 0)
				{
					//remove client from previous channel
					oldChannel.removeClient(client);
					
					//add client to new channel
					client.setInfo("channel", channel);
					
					newChannel.addClient(client);
					
					sendToChannel(client.getInfo("username") + 
							" has connected to channel: " +
							channel, channel);
				}
				//password
				else
				{
					//correct password entered
					if(password.equals(newChannel.getPassword()))
					{
						//remove client from previous channel
						oldChannel.removeClient(client);
						
						//add client to new channel
						client.setInfo("channel", channel);
						
						newChannel.addClient(client);
						
						sendToChannel(client.getInfo("username") + 
								" has connected to channel: " +
								channel, channel);
					}
					//incorrect password entered
					else
					{
						client.sendToClient("Unable to join channel. Incorrect password entered.");
					}
				}
				
				//close channel if it isn't the default channel
				//and there are no more clients in it
				if(!oldChannel.getName().equals("default") && 
						oldChannel.getSize() == 0)
				{
					channels.remove(oldChannel.getName());
				}
				
				
			}
			//channel doesn't exist
			else
				client.sendToClient("Unable to join. Channel " + channel + " does not exist");
		}

	}
	
	/**
	 * Creates a channel specified by a client.
	 * @param channel The name of the channel
	 * @param client The client creating the channel
	 * @throws IOException
	 * @author James Crosetto 5/1/08
	 */
	private void createChannel(String channel, ConnectionToClient client) throws IOException
	{
		//remove leading and trailing white space
		channel = channel.trim();
		
		//split channel into channel and password
		String[] info = channel.split(" ");
		
		//holds a password supplied by user
		String password = "";
		
		//password specified
		if(info.length > 1)
			password = info[1];
		
		channel = info[0];
		
		//convert to lowercase for comparison
		channel = channel.toLowerCase();
		
		//check to see if channel already exists
		if(channels.containsKey(channel)){
			client.sendToClient("The specified channel already exists.");
		}
		//doesn't exist
		else{
			//create new ChannelInfo object and store info
			ChannelInfo c = new ChannelInfo(channel);
			c.addClient(client);
			c.setPassword(password);
			channels.put(channel, c);
			
			ChannelInfo oldChannel = channels.get(client.getInfo("channel"));
			
			//remove client from previous channel
			oldChannel.removeClient(client);
			
			//close channel if it isn't the default channel
			//and there are no more clients in it
			if(!oldChannel.getName().equals("default") && 
					oldChannel.getSize() == 0)
			{
				channels.remove(oldChannel.getName());
			}
			
			//add client to new channel
			client.setInfo("channel", channel);
			
			client.sendToClient("You have created channel " + channel);
		}

	}

	
	/**
	 * Returns an array containing the names of the channels currently available.
	 * The elements in the array are in no particular order.
	 * @author James Crosetto 5/8/08
	 *
	 * @return The array containing the names of the currently available channels.
	 */
	private String[] getChannels(ConnectionToClient c)
	{
		String[] chan = new String[1];
		chan = channels.keySet().toArray(chan);
		return chan;
	}
	
	
	/**
	 * Sends a message to a specified channel as a server message.
	 * 
	 * Modified 5/1/08 by James Crosetto
	 * @param msg The message to be sent.
	 * @param channel The channel to send the message to.
	 * @author James Crosetto 4/17/08
	 */
	private void sendToChannel(String msg, String channel)
	{

		try{

			//valid channel
			if(channels.containsKey(channel))
			{
				//get list of clients on channel
				ArrayList<ConnectionToClient> clients = channels.get(channel).getClients();
				
				for(int i = 0; i < clients.size(); i++)
				{
					clients.get(i).sendToClient("SERVER MSG: " + msg);
				}
			}
			else
			{
				serverUI.display("Specified channel not found");
			}
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
	 * Modified 5/2 by James Crosetto
	 */
	@SuppressWarnings("unchecked")
	private void forwardingSetup(String msg, ConnectionToClient client){
		String recipient = "";
		ConnectionToClient clientTo;
		ArrayList<String> blockedUsers;

		String[] parsedString = msg.split(" ");
		

		try {
			if(parsedString.length <= 1){
				client.sendToClient("You must specify a user to forward to.");
				return;
			}
			
			recipient = parsedString[1];
			
			//do not allow user to forward to themselves
			if(client.getInfo("username").equals(recipient)){
				client.sendToClient("You may not forward messages to yourself.");
				return;
			}
			
			//get clientTo connection
			clientTo = findClient(recipient);
			
			//recipient was not found in connected clients
			if (clientTo == null){
				client.sendToClient("The user you specified is not connected");
				return;
			}
			
			if(clientTo.getInfo("blocking") != null){
				blockedUsers = (ArrayList<String>)clientTo.getInfo("blocking");
				//make sure recipient isn't blocking the sender
				if(blockedUsers.contains(client.getInfo("username"))){
					client.sendToClient("Cannot forward. "+clientTo.getInfo("username")+" is blocking you!");
					return;
				}
			}

			clientTo.sendToClient("User " + client.getInfo("username") + 
			" is now forwarding messages to you.");
			client.sendToClient("You are now forwarding messages to " + recipient);
			storeForwardingInfo(client, recipient);
			return;
			
		} catch (IOException e) {}


	}
	/**
	 * Method to store the forwarding info for each user involved in the forwarding.
	 * Added 4/19
	 * @param fromClient The client that is initiating the forwarding
	 * @param toClient The client username that is receiving the forward
	 * @author Cory Stevens
	 * Modified 5/2 by James Crosetto
	 */
	@SuppressWarnings("unchecked")
	private void storeForwardingInfo(ConnectionToClient fromClient, String toClient){
		ArrayList<String> forwardTo;

		if(fromClient.getInfo("forwardTo") == null){
			forwardTo = new ArrayList<String>();
			forwardTo.add(toClient);
			fromClient.setInfo("forwardTo", forwardTo);
		}
		else{
			forwardTo = (ArrayList<String>) fromClient.getInfo("forwardTo");
			forwardTo.add(toClient);
		}
	}
	/**
	 * Method that will forward messages to their recipients 
	 * @param fromClient The client that is forwarding the message
	 * @param msg The message that is being forwarded
	 * @param sent The list of users the message has been forwarded to
	 * @author cory stevens
	 * Modified 5/2 by James Crosetto
	 */
	@SuppressWarnings("unchecked")
	private void forwardMessage(ConnectionToClient fromClient, String msg, ArrayList<String> sent){
		//if the fromClient does not have anyone to forward to return
		if(fromClient.getInfo("forwardTo") == null){
			return;
		}
		ArrayList<String> recipients;
		recipients = (ArrayList<String>)fromClient.getInfo("forwardTo");

		//add the client sending the message to the list sent
		sent.add((String)fromClient.getInfo("username"));

		ConnectionToClient recipient;
		
		String recipientTemp;
		
		for(int i = 0; i < recipients.size(); i++){
			recipientTemp = recipients.get(i);
			
			recipient = findClient(recipientTemp);
			
			//send the message if they haven't already received the message
			if(!sent.contains(recipientTemp) && recipient != null){
				try {
					recipient.sendToClient("Forward from " + fromClient.getInfo("username")+": "+msg);
				} catch (IOException e) {}
				forwardMessage(recipient, msg, sent);
				return;
			}
		}
	}

	/**
	 * Method that removes forwarding to a user
	 * @param msg The message containing the user to unforward
	 * @param client The client that is unforwarding another client
	 * @author james crosetto 4/20
	 * Modified 5/2 by James Crosetto
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

		forwardedUsers = (ArrayList<String>)client.getInfo("forwardTo"); 
		//if the unforward has a user specified only remove one user
		if(parsedString.length > 1){
			
			//no forwarding to client
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
				String forwarded;
				while(it.hasNext()) {
					forwarded = (String)it.next();
					client.sendToClient("Messages are no longer forwarded to " + forwarded);
				}
				forwardedUsers.clear();
			} 
			catch (IOException e) {}
		}
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

		command = command.toLowerCase();
		if (command.equals("#help")){
			serverUI.display("Commands\tdescription");
			serverUI.display("#quit\t\texit server program");
			serverUI.display("#stop\t\tserver stops listening for connections");
			serverUI.display("#close\t\tserver stops listening and closes socket");
			serverUI.display("#setport\tsets port number to listen on");
			serverUI.display("#start\t\tstarts listening");
			serverUI.display("#getport\treturns the current port server is listening to");
			serverUI.display("#private\tsend private message to a specified user");
			serverUI.display("#channel\tsend a message to all clients connected to a channel");
			serverUI.display("#outputusers\twrites userInfo hashmap to textfile");
			serverUI.display("#inputUsers\treads in userInfo text file into userInfo hashmap");
			serverUI.display("#help\t\tthis menu");
		}
		else if(command.equals("#help #quit")){
			serverUI.display("Usage:\t#quit");
		}
		else if(command.equals("#help #stop")){
			serverUI.display("Usage:\t#stop");
		}
		else if(command.equals("#help #close")){
			serverUI.display("Usage:\t#close");
		}
		else if(command.equals("#help #setport")){
			serverUI.display("Usage:\t#setport <portname>");
		}
		else if(command.equals("#help #start")){
			serverUI.display("Usage:\t#start");
		}
		else if(command.equals("#help #getport")){
			serverUI.display("Usage:\t#getport");
		}
		else if(command.equals("#help #private")){
			serverUI.display("Usage:\t#private <to username> <message>");
		}
		else if(command.equals("#help #channel")){
			serverUI.display("Usage:\t#channel <channelname> <message>");
		}
		else if(command.equals("#help #outputusers")){
			serverUI.display("Usage:\t#outputusers");
		}
		else if(command.equals("#help #inputusers")){
			serverUI.display("Usage:\t#inputusers");
		}
		else
			serverUI.display("invalid help topic (make sure there are no characters or spaces at the tail)");
	}

	/**
	 * This method is called when the server starts listening for connections
	 */
	protected void serverStarted()
	{
		serverUI.display("Server listening for connections on port " + obsOrigServ.getPort());
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
	 * Modified 5/2 by James Crosetto
	 */
	public void clientDisconnected(ConnectionToClient client) {
		//added 4/18
		if(client.getInfo("username") != null){
			serverUI.display(client.getInfo("username")+" has logged out");
			obsOrigServ.sendToAllClients(client.getInfo("username")+" has logged out");
		}
		
		//remove client from channel
		ChannelInfo oldChannel = channels.get(client.getInfo("channel"));
		oldChannel.removeClient(client);
		
		//close channel if it isn't the default channel
		//and there are no more clients in it
		if(!oldChannel.getName().equals("default") && 
				oldChannel.getSize() == 0)
		{
			channels.remove(oldChannel.getName());
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
			obsOrigServ.sendToAllClients(client.getInfo("username") + " has disconnected.");
		}
		
		//remove client from channel
		ChannelInfo oldChannel = channels.get(client.getInfo("channel"));
		oldChannel.removeClient(client);
		
		//close channel if it isn't the default channel
		//and there are no more clients in it
		if(!oldChannel.getName().equals("default") && 
				oldChannel.getSize() == 0)
		{
			channels.remove(oldChannel.getName());
		}
	}

	/**
	 * This method terminates the server.
	 */
	public void quit()
	{
		try
		{
			obsOrigServ.close();
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
			obsOrigServ.sendToAllClients("SERVER MSG> " + message);
		}
	}

	/**
	 * This method processes the messages from the serverUI           
	 *
	 * @param message The command that will be processed.    
	 */
	public void serverCommand(String command){
		//Quit command
		
		//trim off leading and following spaces
		command = command.trim();
		//temp variable for comparison while ignoring case
		String commandtemp;
		int index = command.indexOf(" ");
		if (index != -1)
			commandtemp = command.substring(0, index).toLowerCase();
		else
			commandtemp = command.toLowerCase();
		
		if(commandtemp.equals("#quit")){
			serverUI.display("Server is quitting");
			obsOrigServ.sendToAllClients("Server is quitting");
			quit();
		}
		//Stop command
		else if(commandtemp.equals("#stop")){
			if(!obsOrigServ.isListening())
				serverUI.display("Server is already stopped");
			else{
				obsOrigServ.stopListening();
				obsOrigServ.sendToAllClients("Server has stopped listening for connections.");
			}
		}
		//close command
		else if(commandtemp.equals("#close")){
			try
			{
				serverUI.display("Server is closing");
				obsOrigServ.sendToAllClients("Server is closing");
				obsOrigServ.close();
				isClosed = true;
			}
			catch(IOException e) {}
		}
		//setport command
		else if(commandtemp.equals("#setport")){
			if(!isClosed){
				serverUI.display("The server must be closed" +
				" to change the port");
			}
			else{
				String tempPort = command.substring(9, command.length());
				obsOrigServ.setPort(Integer.parseInt(tempPort));
				serverUI.display("Port set to " + tempPort);
			}
		}
		//start command
		else if(commandtemp.equals("#start")){
			if(obsOrigServ.isListening())
				serverUI.display("Server is already running");
			else{
				try
				{
					obsOrigServ.listen();
				}
				catch(IOException e) {}
				isClosed = false;
			}
		}
		//getport command
		else if(commandtemp.equals("#getport")){
			serverUI.display("The current port is " + obsOrigServ.getPort());
		}
		//catch all other commands
		//check for private message command
		//first implementation on 4/15 by seth schwiethale
		else if(commandtemp.equals("#private")){
			serverPM(command);
		}
		//send message to specified channel
		//first implementation 4/16/08 by james crosetto
		else if(commandtemp.equals("#channel")){
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
		else if(commandtemp.equals("#outputusers")){
			outputUserInfo();
		}
		else if(commandtemp.equals("#inputusers")){
			inputUserInfo();
		}
		//give the user help with the commands
		//Added on 4/20 by Cory
		else if(commandtemp.equals("#help")){
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
	 * Modified 5/2 by James Crosetto
	 */
	@SuppressWarnings("unchecked")
	public void sendToAllClients(Object msg, ConnectionToClient client){

		//get list of users on same channel as client
		ArrayList<ConnectionToClient> clientList = channels.get(client.getInfo("channel")).getClients();

		//current client that sending client is sending a message to
		ConnectionToClient clientTemp;
		
		

		for (int i=0; i < clientList.size(); i++)
		{
			
			clientTemp = clientList.get(i);
			
			//holds clients blocking the sending client
			ArrayList<String> blockedUsers = new ArrayList<String>();
			
			if(clientTemp.getInfo("blocking")!=null){
				blockedUsers = (ArrayList<String>)clientTemp.getInfo("blocking");
			}//if blocking list is not null
			
			try
			{

				//make sure recipient isn't blocking the sender
				if(!blockedUsers.contains(client.getInfo("username")) ){
					clientTemp.sendToClient(msg);
				}//if user on channel is blocking sender
			}

			catch (Exception ex) {}
		}
	}




}
//End of EchoServer class