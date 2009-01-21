/**
 * Group Name: The Little Caesars
 * Members: Cory Stevens, Sean Maloy, James Crosetto, Madison Mosley, Seth Schwiethale
 * Project Part: Simplechat Final
 * Title: EchoServer
 * CS320 Spring 2008
 * May 19, 2008
 * Java
 * Simplechat program
 * ChatClient now implements the Observable Layer. It is an Observer of ObservableCleint
 * EchoServer has an Instance of ObservableClient and through AdaptableClient has
 * access to the functionality of AbstractClient. ChatClient handles messages from the server and the 
 * ClientUI, it has various methods to delegate execution of commands sent through these messages
 */
package client;
import com.lloseng.ocsf.client.*;
//import ocsf.client.*;
import common.*;
import java.io.*;
import java.util.Observable;
import java.util.Observer;
import java.util.StringTokenizer;

/**
* This class overrides some of the methods defined in the abstract
* superclass in order to give more functionality to the client.
*
* @version March 2008
*/
public class ChatClient implements Observer
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
	
	/**
	 * The ChatClient is Observable
	 */
	private ObservableClient obsClient;
	
	//Constructors ****************************************************
	
	/**
	* Constructs an instance of the chat client.
	*
	* @param username The clients loginID
	* @param host The server to connect to.
	* @param port The port number to connect on.
	* @param clientUI The interface type variable.
	*/
	public ChatClient(String username, String password, ChatIF clientUI, ObservableClient newObservable )
	throws IOException
	{
		
		this.obsClient = newObservable;
		this.obsClient.addObserver(this);
		this.clientUI = clientUI;
		this.username = username;
		this.password = password;
		try{
			obsClient.openConnection();
			//Immediately send the login information to the server
			obsClient.sendToServer("#login " + username + " " + password);
		}
		catch(IOException e){
			clientUI.display("Cannot open connection. Awaiting command.");
		}
	}
	
	
	//Instance methods ************************************************
	
	/**
	* This method handles all data that comes in from the server.
	*
	* Observable Layer implemented by Seth Schwiethale on 5/7/08
	*
	* @param o is Observable from Observer
	* @param msg The message from the server.
	*/
	public void update(Observable o, Object msg)
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
		else if(obsClient.isConnected()){
			try
			{
				obsClient.sendToServer(message);
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
			obsClient.closeConnection();
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
		
		//trim off leading and following spaces
		command = command.trim();
		//temp variable for comparison while ignoring case
		String commandtemp;
		int index = command.indexOf(" ");
		if (index != -1)
			commandtemp = command.substring(0, index).toLowerCase();
		else
			commandtemp = command.toLowerCase();
		
		//the quit command
		if(commandtemp.equals("#quit")){
			quit();
		}
		//the logoff command
		else if(commandtemp.equals("#logoff")){
			try
			{
				obsClient.closeConnection();
			}
			catch(IOException e) {}
		}
		//the login command
		else if(commandtemp.equals("#login")){
			//cannot login if you are already logged on
			String[] info = command.split(" ");
			try{
				if(obsClient.isConnected()){
					clientUI.display("Already logged in");
				}
				//#login
				else if (info.length == 3){
					clientUI.display("logging in");
					obsClient.openConnection();
					username = info[1];
					password = info[2];
					System.out.println("#login " + username + " " + password);
					//resend the login information
					obsClient.sendToServer("#login " + username + " " + password);
				} 
				else
					clientUI.display("You must specify a username and password." +
							"Make sure only one space is between words.");
			}
			catch(IOException e) {
				clientUI.display("Unable to establish a" +
				" connection to the host " +
				obsClient.getHost() + " on port " + obsClient.getPort());
			}
			
		}
		
		//setpassword command
		else if(commandtemp.equals("#setpassword"))
		{
			if (command.length() > 13)
			{
				try{
					obsClient.sendToServer(command);
					password = command.substring(command.indexOf(" "), command.length());
					clientUI.display("Password changed");
				}
				catch(Exception e){}
			}
			else
				clientUI.display("A new password must be specified");
		}
		
		//gethost command
		else if(commandtemp.equals("#gethost"))
			clientUI.display("" + obsClient.getHost());
		//getport command
		else if(commandtemp.equals("#getport"))
			clientUI.display("" + obsClient.getPort());
		//sethost command
		//note the space after sethost, this ensures that a host is
		//specified
		else if(commandtemp.equals("#sethost")){
			//cannot sethost if already logged on
			if(obsClient.isConnected()){
				clientUI.display("You must be logged off to do that");
			}
			else if (command.length() > 9){
				String tempHost = command.substring(9, command.length());
				obsClient.setHost(tempHost);
				clientUI.display("Host set to " + tempHost);
			}
			else
				clientUI.display("A host must be specified!");
		}
		//setport commmand note space after command
		else if(commandtemp.equals("#setport")){
			//cannot set port if connected
			if(obsClient.isConnected()){
				clientUI.display("You must be logged off to do that");
			}
			else if (command.length() > 9){
				String tempPort = command.substring(9, command.length());
				//Try/catch to ensure that only numbers are set as ports
				try
				{
					obsClient.setPort(Integer.parseInt(tempPort));
					clientUI.display("Port set to " + tempPort);
				}
				catch(NumberFormatException e){
					clientUI.display("Port must be a number");
				}
			}
			else
				clientUI.display("A port must be specified!");
		}

		//check for private message command
		//first implementation on 4/15 by seth schwiethale
		else if(commandtemp.equals("#private")){
			if(!obsClient.isConnected()){
				clientUI.display("You must be logged on to do that");
			}
			else{
				doPM(command);
			}
		}
		
		//easier command view
		//added 5/1/08 by James Crosetto
		else if (commandtemp.equals("#whoblocksme") || commandtemp.equals("#channellist")
				|| commandtemp.equals("#whoiblock") || commandtemp.equals("#unblock")
				|| commandtemp.equals("#block") || commandtemp.equals("#unforward")
				|| commandtemp.equals("#forward") || commandtemp.equals("#channel")
				|| commandtemp.equals("#createchannel") || commandtemp.equals("#joinchannel"))
		{
			if(!obsClient.isConnected()){
				clientUI.display("You must be logged on to do that");
			}
			else{
				try
				{
					obsClient.sendToServer(command);
				}
				catch(IOException e){
					clientUI.display("Unable to send message to server.");
				}
			}
		}
		
		//give the user help with the commands
		//Added on 4/20 by Cory
		else if(commandtemp.equals("#help")){
			commandHelp(commandtemp);
		}

		//catch anything that starts with #, but isn't a command
		else{
			clientUI.display("Invalid command");
			clientUI.display("Please use #help [command] for command help");
		}
		
	}
	/**
	 * Method that helps the User with the various commands.
	 * Added 4/20 by Cory Stevens tediously filled out by Seth Schwiethale
	 * Modified 5/1 by James Crosetto (added channel stuff)
	 * @param command The command that contains the requested help
	 */
	private void commandHelp(String command){
		command = command.toLowerCase();
		if (command.equals("#help")){
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
			clientUI.display("#channellist\tdisplay all of the available channels");
			clientUI.display("#channel\tdisplay chat channel you are connected to");
			clientUI.display("#forward\tforward message you recieve to another user");
			clientUI.display("#block\tblock messages sent from specified user");
			clientUI.display("#setpassword\tchanges password");
			clientUI.display("#help\t\tthis menu");
		}
		else if(command.equals("#help #quit")){
			clientUI.display("Usage:\t#quit");
		}
		else if(command.equals("#help #logoff")){
			clientUI.display("Usage:\t#logoff");
		}
		else if(command.equals("#help #login")){
			clientUI.display("Usage:\t#login");
		}
		else if(command.equals("#help #gethost")){
			clientUI.display("Usage:\t#gethost");
		}
		else if(command.equals("#help #getport")){
			clientUI.display("Usage:\t#getport");
		}
		else if(command.equals("#help #sethost")){
			clientUI.display("Usage:\t#sethost <hostname>");
		}
		else if(command.equals("#help #setport")){
			clientUI.display("Usage:\t#setport <portname>");
		}
		else if(command.equals("#help #private")){
			clientUI.display("Usage:\t#private <to username> <message>");
		}
		else if(command.equals("#help #joinchannel")){
			clientUI.display("Usage:\t#joinchannel <channelName> [password]");
		}
		else if(command.equals("#help #createchannel")){
			clientUI.display("Usage:\t#createchannel <channelName> [password]");
		}
		else if(command.equals("#help #channel")){
			clientUI.display("Usage:\t#channel");
		}
		else if(command.equals("#help #channellist")){
			clientUI.display("Usage:\t#channellist");
		}
		else if(command.equals("#help #forward")){
			clientUI.display("Usage:\t#forward <to username>");
		}
		else if(command.equals("#help #block")){
			clientUI.display("Usage:\t#block <username>");
		}
		else if(command.equals("#help #setpassword")){
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
				obsClient.sendToServer(command);
			}
			catch(IOException e){
				clientUI.display("Unable to establish a" +
					" connection to the host " +
					obsClient.getHost() + " on port " + obsClient.getPort());	
			}
		}
		else clientUI.display("usage: #private <to user id> <message>");
	}


	/**
	* This method is called after the connection has been closed.
	*/
	public void connectionClosed() {
		clientUI.display("Connection to " + obsClient.getHost() + " closed.");
	}
	/**
	* This method is called each time an exception is thrown by the client's
	* thread that is waiting for messages from the server.
	*
	* @param exception the exception raised
	*/
	public void connectionException(Exception exception) {
		clientUI.display("Connection to " + obsClient.getHost() + " lost.");
	}
	/**
	* This method is called after a connection has been established.
	*/
	public void connectionEstablished() {
		clientUI.display("Welcome! You are connected to "+ obsClient.getHost());
	}

}
//End of ChatClient class