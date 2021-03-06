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
import org.jivesoftware.smackx.XHTMLManager;
import org.jivesoftware.smackx.XHTMLText;

public class JabBukkitXMPP extends Handler implements MessageListener, ChatManagerListener {

	private JabBukkit plugin;
	private XMPPConnection conn;
	private ChatManager chatmanager;
	private final transient Map<String, Chat> chats = Collections.synchronizedMap(new HashMap<String, Chat>());
	
	public JabBukkitXMPP(JabBukkit jabbukkit) {
		this.plugin = jabbukkit;
		doConnect();
	}

	public boolean doConnect() {
		if(plugin.getConfig().getString("xmpp.server").equals("localhost") && plugin.getConfig().getString("xmpp.user").equals("user")) {
			plugin.log.warning("[" + plugin.getPluginName() + "] XMPP not configured");
			return false;
		}
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
			updateStatusMessage();
			addUsersToRoster();
			chatmanager = conn.getChatManager();
			chatmanager.addChatListener(this);
			plugin.log.info("["+plugin.getPluginName()+"] XMPP Login Successful");	
			return true;
		} catch(XMPPException e) {
			plugin.log.severe("["+plugin.getPluginName()+"] XMPP Login Failed");
			e.printStackTrace();
		}
		return false;
	}
	
	public void doDisconnect() {
		if(true)
			plugin.log.info("[" + plugin.getPluginName() + "] XMPP Disconnected");
		conn.disconnect();
	}
	
	public void updateStatusMessage() {
		Presence available = new Presence(Presence.Type.available);
		available.setStatus(plugin.getConfig().getString("xmpp.status"));
		conn.sendPacket(available);
	}
	
	public void addUsersToRoster() {
		for(String users : plugin.getConfig().getStringList("users")) {
			if ((conn != null) && (conn.isAuthenticated())) {
				// check if the user is already on the roster
				if (conn.getRoster().contains(users.substring(0, users.indexOf("|")))) {
					try {
						conn.getRoster().createEntry(users.substring(0, users.indexOf("|")), users.substring(0, users.indexOf("|")), new String[] {"default"});
					} catch (XMPPException e) {
						plugin.log.warning("[" + plugin.getPluginName() + "] User " + users.substring(0, users.indexOf("|")) + " could not be added to Roster!");
					}
				}
			}
		}
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
			if(message.startsWith("\\")) {
				List<String> ops = plugin.getConfig().getStringList("op-users");
				if(ops.contains(sender)) {
					plugin.log.info(message.substring(1));
					plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), message.substring(1));
				}
			} else if(message.startsWith("@")) {
				if(plugin.getServer().getPlayer(message.substring(1, message.indexOf(" "))).isOnline()) {
					plugin.log.info("Player online");
					for(String address : users) {
						if(address.substring(address.toString().indexOf('|') + 1, address.toString().length()).equals(message.substring(1, message.indexOf(" ")))) {
							plugin.getServer().getPlayer(message.substring(1, message.indexOf(" "))).sendMessage("[J]<" + nickname + "> " + message.substring(message.indexOf(" ")));
						}
					}
				}
				if(plugin.getServer().getOfflinePlayer(message.substring(1, message.indexOf(" "))).isOnline() == false) {
					plugin.log.info("Player offline");
					for(String address : users) {
						String offlinemessage[] = new String[2];
						offlinemessage[0] = message.substring(1, message.indexOf(" "));
						offlinemessage[1] = "is offline";
						sendMessage(address.substring(0, address.indexOf("|")), offlinemessage);
					}
				}
			} else {	
			String fullmessage[] = new String[2];
				fullmessage[0] = nickname;
				fullmessage[1] = message;
				for(String address : users) {
					if(!address.substring(0, address.indexOf('|')).equals(sender)) {
						sendMessage(address.substring(0, address.indexOf("|")), fullmessage);
					}
				}
			plugin.getServer().broadcastMessage("[J]<" + nickname + "> " + message);
			}
		}
	}

	private void startChat(final String address) throws XMPPException {
		if (chatmanager == null) {
			return;
		}
		synchronized (chats) {
			if (!chats.containsKey(address)) {
				final Chat chat = chatmanager.createChat(address, this);
				if (chat == null) {
					throw new XMPPException("Could not start Chat with " + address);
				}
				chats.put(address, chat);
			}
		}
	}

	public boolean sendMessage(final String address, final String[] message) {
		if (address != null && !address.isEmpty()) {
			try {
				startChat(address);
				final Chat chat;
				synchronized (chats) {
					chat = chats.get(address);
				}
				if (chat != null) {
					if (!conn.isConnected()) {
						doDisconnect();
						doConnect();
					}
					Message msg = new Message();
					msg.setBody(message[0] + ": " + message[1].replaceAll("§[0-9a-f]", ""));
					XHTMLText fullmessage = new XHTMLText(null, null);
					fullmessage.appendOpenParagraphTag(null);
					fullmessage.appendOpenSpanTag("font-weight:bold;");
					fullmessage.appendOpenSpanTag("text-decoration:underline;");
					fullmessage.append(message[0]);
					fullmessage.appendCloseSpanTag();
					fullmessage.appendCloseSpanTag();
					fullmessage.append(" ");
					fullmessage.append(message[1].replaceAll("§[0-9a-f]", ""));
					fullmessage.appendCloseParagraphTag();
				    XHTMLManager.addBody(msg, fullmessage.toString());
				    chat.sendMessage(msg);
					return true;
				}
			}
			catch (XMPPException ex) {
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
	
	public XMPPConnection getConnection() {
		return conn;
	}
	

	
}
