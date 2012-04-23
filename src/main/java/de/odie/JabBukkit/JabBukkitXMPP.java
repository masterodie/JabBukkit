package de.odie.JabBukkit;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.Roster.SubscriptionMode;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

public class JabBukkitXMPP extends Handler implements MessageListener, ChatManagerListener {

	protected JabBukkit plugin;
	protected XMPPConnection conn;
	protected ChatManager chatmanager;
	private final transient Map<String, Chat> chats = Collections.synchronizedMap(new HashMap<String, Chat>());
	protected JabBukkitXMPPInterface iface;
	
	public JabBukkitXMPP(JabBukkit jabbukkit) {
		this.plugin = jabbukkit;
		doConnect();
	}

	public void doConnect() {
		ConnectionConfiguration xmppConfig = new ConnectionConfiguration(plugin.getConfig().getString("xmpp.server"), plugin.getConfig().getInt("xmpp.port"));
		xmppConfig.setSASLAuthenticationEnabled(plugin.getConfig().getBoolean("xmpp.sasl"));
		SASLAuthentication.supportSASLMechanism("PLAIN", 0);
		xmppConfig.setSendPresence(true);
		xmppConfig.setReconnectionAllowed(true);
		conn = new XMPPConnection(xmppConfig);
		try {
			conn.connect();
			conn.login(plugin.getConfig().getString("xmpp.user"), plugin.getConfig().getString("xmpp.password"));
			conn.getRoster();
			Roster.setDefaultSubscriptionMode(SubscriptionMode.accept_all);
			Presence available = new Presence(Presence.Type.available);
			available.setStatus(plugin.getConfig().getString("xmpp.status"));
			conn.sendPacket(available);
			for(String users : plugin.getConfig().getStringList("users")) {
				if ((conn != null) && (conn.isAuthenticated())) {
					// check if the user is already on the roster
					if (conn.getRoster().contains(users.substring(0, users.indexOf("|")))) { 
						conn.getRoster().createEntry(users.substring(0, users.indexOf("|")), users.substring(0, users.indexOf("|")), new String[] {"default"});
					}
				}
			}
			chatmanager = conn.getChatManager();
			chatmanager.addChatListener(this);
			plugin.log.info("["+plugin.getPluginName()+"] XMPP Login Successful");		
		} catch(XMPPException e) {
			plugin.log.severe("["+plugin.getPluginName()+"] XMPP Login Failed");
			e.printStackTrace();
		}
	}
	
	public void doDisconnect() {
		if(true)
			plugin.log.info("[" + plugin.getPluginName() + "] XMPP Disconnected");
		conn.disconnect();
	}

	@Override
	public void chatCreated(Chat chat, boolean createdLocally) {
		if(!createdLocally) {
			chat.addMessageListener(this);
			final Chat old = chats.put(StringUtils.parseBareAddress(chat.getParticipant()), chat);
			if (old != null)
			{
				old.removeMessageListener(this);
			}
		}
		
	}

	@Override	
	public void processMessage(Chat chat, Message msg) {
		if(msg.getType() != Message.Type.error && msg.getBody().length()>0) {
			String message = msg.getBody();
			String sender = StringUtils.parseBareAddress(chat.getParticipant());
			String nickname = "";
			List<String> users = plugin.getConfig().getStringList("users");
			for(String address : users) {
				if(address.substring(0, address.toString().indexOf('|')).equals(sender)) {
					nickname = address.substring(address.indexOf("|")+1);
				} 
			}
			for(String address : users) {
				if(!address.substring(0, address.toString().indexOf('|')).equals(sender)) {
				  	sendMessage(address.substring(0, address.indexOf("|")), "[J]<" + nickname + "> " + message);
				}
			}
			plugin.getServer().broadcastMessage("[J]<" + nickname + "> " + message);
		}
	}

	private void startChat(final String address) throws XMPPException
	{
		if (chatmanager == null)
		{
			return;
		}
		synchronized (chats)
		{
			if (!chats.containsKey(address))
			{
				final Chat chat = chatmanager.createChat(address, this);
				if (chat == null)
				{
					throw new XMPPException("Could not start Chat with " + address);
				}
				chats.put(address, chat);
			}
		}
	}

	public boolean sendMessage(final String address, final String message)
	{
		if (address != null && !address.isEmpty())
		{
			try
			{
				startChat(address);
				final Chat chat;
				synchronized (chats)
				{
					chat = chats.get(address);
				}
				if (chat != null)
				{
					if (!conn.isConnected())
					{
						doDisconnect();
						doConnect();
					}
					String tmpmessage = message.toString().replaceAll("§[0-9a-f]", "");
				    chat.sendMessage(tmpmessage);
					return true;
				}
			}
			catch (XMPPException ex)
			{
				disableChat(address);
			}
		}
		return false;
	}

	private void disableChat(final String address)
	{
		final Chat chat = chats.get(address);
		if (chat != null)
		{
			chat.removeMessageListener(this);
			chats.remove(address);
		}
	}
	
	@Override
	public void close() throws SecurityException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void publish(LogRecord arg0) {
		// TODO Auto-generated method stub
		
	}
	

	
}
