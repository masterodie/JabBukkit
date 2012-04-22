package de.odie.JabBukkit;

import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;


public class JabBukkit extends JavaPlugin {

	public Logger log = Logger.getLogger("Minecraft");
	static private String PluginName = "JabBukkit";
	private JabBukkitXMPP xmpp;
	public void onEnable() {		
		Plugin jabbukkit = this.getServer().getPluginManager().getPlugin(PluginName);
		getServer().getPluginManager().registerEvents(new JabBukkitPlayerListener(this), this);
		xmpp = new JabBukkitXMPP(this);
		if (jabbukkit != null) {
		    if (!jabbukkit.isEnabled()) {
		        getServer().getPluginManager().enablePlugin(jabbukkit);
		    }
		}
		
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
