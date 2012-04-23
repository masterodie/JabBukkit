package de.odie.JabBukkit;

import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;


public class JabBukkitPlayerListener implements Listener {
	private JabBukkit plugin;
	private List<String> users;
	
	public JabBukkitPlayerListener(JabBukkit plugin) {
		this.plugin = plugin;
		users = plugin.getConfig().getStringList("users");
	}
	
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerChatEvent(PlayerChatEvent event) {
		String message = event.getMessage();
		String fullmessage[] = new String[2];
		fullmessage[0] = event.getPlayer().getDisplayName();
		fullmessage[1] = message;
		if(message.length() >= 3) {
			for(String address : users) {
					String jid = address.substring(0, address.indexOf("|"));
					plugin.getXMPP().sendMessage(jid, fullmessage);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoinEvent(PlayerJoinEvent event) {
		String fullmessage[] = new String[2];
		fullmessage[0] = event.getPlayer().getDisplayName();
		fullmessage[1] = plugin.getConfig().getString("messages.connect");
		for(String address : users) {

			String jid = address.substring(0, address.indexOf("|"));
				plugin.getXMPP().sendMessage(jid, fullmessage);
		}
	}
	

	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuitEvent(PlayerQuitEvent event) {
		String fullmessage[] = new String[2];
		fullmessage[0] = event.getPlayer().getDisplayName();
		fullmessage[1] = plugin.getConfig().getString("messages.disconnect");
		for(String address : users) {

			String jid = address.substring(0, address.indexOf("|"));
			plugin.log.info(jid);
			plugin.getXMPP().sendMessage(jid, fullmessage);
		}
	}
	
}

