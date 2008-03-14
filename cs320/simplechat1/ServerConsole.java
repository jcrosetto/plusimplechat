import java.io.*;
import client.*;
import common.*;

/**
 * This class constructs the UI for a server client.  It implements the
 * chat interface in order to activate the display() method.
 * Uses code from ClientConsole.java
 *
 * @author James Crosetto
 */
public class ServerConsole implements ChatIF 
{
  //Class variables *************************************************
  
  /**
  * The default port to listen on.
  */
  final public static int DEFAULT_PORT = 5432;
  
  //Instance variables **********************************************
  
  /**
   * The instance of the server that created this ServerConsole.
   */
  EchoServer server;

  
  //Constructors ****************************************************

  /**
   * Constructs an instance of the ServerConsole UI.
   *
   * @param host The host to connect to.
   * @param port The port to connect on.
   */
  public ServerConsole(int port) 
  {
    server= new EchoServer(port);
    try 
    {
      server.listen();
    } 
    catch(IOException exception) 
    {
      System.out.println("ERROR - Could not listen for clients!");
    }
  }

  
  //Instance methods ************************************************
  
  /**
   * This method waits for input from the console.  Once it is 
   * received, it sends it to the server's message handler.
   */
  public void accept() 
  {
    try
    {
      BufferedReader fromConsole = 
        new BufferedReader(new InputStreamReader(System.in));
      String message;

      while (true) 
      {
        message = fromConsole.readLine();
        //server.handleMessageFromClientUI(message);
	display(message);
	server.sendToAllClients("SERVER MSG> " + message);
      }
    } 
    catch (Exception ex) 
    {
      System.out.println
        ("Unexpected error while reading from console!");
    }
  }

  /**
   * This method overrides the method in the ChatIF interface.  It
   * displays a message onto the screen.
   *
   * @param message The string to be displayed.
   */
  public void display(String message) 
  {
    System.out.println("SERVER MSG> " + message);
  }
  
  //Class methods ***************************************************
	
	/**
	* This method is responsible for the creation of 
	* the server instance (there is no UI in this phase).
	*
	* @param args[0] The port number to listen on.  Defaults to 5555 
	*          if no argument is entered.
	*/
	public static void main(String[] args) 
	{
		int port = 0; //Port to listen on
		
		try
		{
			port = Integer.parseInt(args[0]); //Get port from command line
		}
		catch(Throwable t)
		{
			port = DEFAULT_PORT; //Set port to 5432
		}
		
		ServerConsole sv = new ServerConsole(port);
		sv.accept();  //Wait for console data
	}
}
//End of ServerConsole class
