
import java.util.ArrayList;
import ocsf.server.ConnectionToClient;

/**
 * This class holds the information for a channel, including the clients connected
 * to it and the name of the channel.
 * 
 * @author James 5/1/08
 *
 */
public class ChannelInfo {

	/**
	 * The list of clients on this channel
	 */
	private ArrayList<Thread> clientThreadList;
	
	/**
	 * The password associated with the channel
	 */
	private String password;
	
	/**
	 * Constructs an instance of Channel
	 * @param name The name of the channel
	 * @param client The client creating the channel
	 */
	public ChannelInfo(ConnectionToClient client) {
		clientThreadList = new ArrayList<Thread>();
		clientThreadList.add(client);
	}
	
	/**
	 * Add a client to this channel
	 * @param client The client to be added to the channel
	 */
	public void addClient(ConnectionToClient client) {
		clientThreadList.add(client);
	}
	
	/**
	 * Remove a client from this channel
	 * @param client The client to be removed
	 */
	public void removeClient(ConnectionToClient client) {
		clientThreadList.remove(client);
	}

	/**
	 * Get the number of clients connected to this channel
	 * @return The number of clients connected to this channel
	 */
	public int getSize(){
		return clientThreadList.size();
	}
	
	/**
	 * Set the password associated with this channel
	 * @param pass The new password for joining this channel
	 */
	public void setPassword(String pass) {
		password = pass;
	}
	
	/**
	 * Get the password associated with this channel
	 * @return The password associated with this channel
	 */
	public String getPassword() {
		return password;
	}
	
	/**
	 * Get the list of clients connected to this channel
	 * @return The list of clients connected to this channel
	 */
	public ArrayList<Thread> getClients() {
		return clientThreadList;
	}
}
