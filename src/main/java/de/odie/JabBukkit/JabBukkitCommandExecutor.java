package de.odie.JabBukkit;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JabBukkitCommandExecutor implements CommandExecutor {
	
	private JabBukkit plugin;
	
	public JabBukkitCommandExecutor(JabBukkit plugin) {
		this.plugin = plugin;
	}

	public JabBukkitCommandExecutor getCommandListener() {
		return this;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		if(cmd.getName().equalsIgnoreCase("jb")){
			return true;
		}
		if(cmd.getName().equalsIgnoreCase("jbadmin")){
			Player player = null;
			if(sender instanceof Player) {
				player = (Player)sender;
			}
			if(player != null) {
				if (!player.hasPermission("jabbukkit.admin")) {
				return false;
				}
			}
			
			switch(args[0]) {
			case "reload":
				plugin.log.info("[" + plugin.getName() + "] reloading config now");
				plugin.reloadConfig();
				plugin.getXMPP().doDisconnect();
				plugin.getXMPP().doConnect();
				return true;
			case "adduser":
				plugin.log.info("Adding user now!");
				if(args.length == 3) {
					List<String> users = plugin.getConfig().getStringList("users");
					users.add(args[2] + "|" + args[1]);
					plugin.getConfig().set("users", users);
					plugin.saveConfig();
					plugin.getXMPP().addUsersToRoster();
					return true;
				} else {
					return false;
				}
			case "setstatus":
				if(args.length > 2) {
					String temp = "";
					for(int i = 0; i < args.length; i++) {
						if(i != 0) {
							temp = temp + args[i] + " ";
						}
					}
					plugin.getConfig().set("xmpp.status", temp);
					plugin.saveConfig();
					plugin.getXMPP().updateStatusMessage();
					plugin.log.info("[" + plugin.getName() + "] XMPP status message set to: " + temp);
					return true;
				}
			}
				
		}//If this has happened the function will break and return true. if this hasn't happened the a value of false will be returned.
		return false; 
	}
}
