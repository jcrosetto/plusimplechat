
import java.util.ArrayList;
//import ocsf.server.ConnectionToClient;
import com.lloseng.ocsf.server.ConnectionToClient;

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
	private ArrayList<ConnectionToClient> clientList;
	
	/**
	 * The password associated with the channel
	 */
	private String password;
	
	/**
	 * The name associated with the channel
	 */
	private String name;
	
	/**
	 * Constructs an instance of Channel
	 * @param name The name of the channel
	 * @param client The client creating the channel
	 */
	public ChannelInfo(String name) {
		clientList = new ArrayList<ConnectionToClient>();
		password = "";
		this.name = name;
	}
	
	/**
	 * Add a client to this channel
	 * @param client The client to be added to the channel
	 */
	public void addClient(ConnectionToClient client) {
		clientList.add(client);
	}
	
	/**
	 * Remove a client from this channel
	 * @param client The client to be removed
	 */
	public void removeClient(ConnectionToClient client) {
		clientList.remove(client);
	}

	/**
	 * Get the number of clients connected to this channel
	 * @return The number of clients connected to this channel
	 */
	public int getSize(){
		return clientList.size();
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
	public ArrayList<ConnectionToClient> getClients() {
		return clientList;
	}
	
	/**
	 * Get the name associated with this channel
	 * @return The name associated with this channel
	 */
	public String getName() {
		return name;
	}
}
