/*
 * JabBukkit.java
 * Copyright (C) 2012 Patrick Neff <masterodie@gmail.com>
 * 
 * JabBukkit is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * JabBukkit is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.odie.JabBukkit;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;


public class JabBukkit extends JavaPlugin {

	public Logger log = Logger.getLogger("Minecraft");
	static private String PluginName = "JabBukkit";
	private JabBukkitXMPP xmpp;
	private JabBukkitCommandExecutor executor = new JabBukkitCommandExecutor(this);
	
	public void onEnable() {		
		Plugin jabbukkit = this.getServer().getPluginManager().getPlugin(PluginName);
		getServer().getPluginManager().registerEvents(new JabBukkitPlayerListener(this), this);
		boolean file = new File((getDataFolder().toString()) + File.separator + "/config.yml").exists();
		
		if(!file)
			saveDefaultConfig();
		
		xmpp = new JabBukkitXMPP(this);
		if (jabbukkit != null) {
		    if (!jabbukkit.isEnabled()) {
		        getServer().getPluginManager().enablePlugin(jabbukkit);
		    }
		}

		getCommand("jb").setExecutor(executor);
		getCommand("jbadmin").setExecutor(executor);
		
	}
 
	public void onDisable() {
		xmpp.doDisconnect();
	}
	
	public String getPluginName() {
		return PluginName;
	}
	
	public JabBukkitXMPP getXMPP() {
		return xmpp;
	}
}
