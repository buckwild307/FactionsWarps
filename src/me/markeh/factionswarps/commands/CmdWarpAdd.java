package me.markeh.factionswarps.commands;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import me.markeh.factionsframework.command.FactionsCommand;
import me.markeh.factionsframework.command.requirements.ReqInFaction;
import me.markeh.factionsframework.command.requirements.ReqRankAtLeast;
import me.markeh.factionsframework.entities.Faction;
import me.markeh.factionsframework.entities.Factions;
import me.markeh.factionswarps.Config;
import me.markeh.factionswarps.event.EventFactionsWarpsCreate;
import me.markeh.factionswarps.store.WarpData;

public class CmdWarpAdd extends FactionsCommand {
	
	// -------------------------------------------------- //
	// COMMAND CONSTRUCT  
	// -------------------------------------------------- //
	
	private static CmdWarpAdd instance = new CmdWarpAdd();
	public static CmdWarpAdd get() { return instance; }
	
	public CmdWarpAdd() {
		this.addAlias("addwarp", "setwarp", "createwarp");
		this.setDescription("add a faction warp");
		
		this.setPermission("factions.warp.manage");
		
		this.addRequiredArgument("name");
		this.addOptionalArgument("password", "none");
		
		this.addRequirement(ReqInFaction.get(this));
		this.addRequirement(ReqRankAtLeast.get(this, Config.get().minimumManage));
	}
	
	// -------------------------------------------------- //
	// METHODS  
	// -------------------------------------------------- //
	
	public void run() throws Exception {
		if ( ! this.getFPlayer().getRole().isAtLeast(Config.get().minimumManage)) {
			msg("<red>You must be at least {required-rank} to manage warps.",
					"required-rank", Config.get().minimumManage.getDescPlayerOne());
			
			return;
		}
		
		// Fetch our warp data
		WarpData warpData = WarpData.get(this.getFPlayer().getFaction());
		
		Player leader = this.getFPlayer().getFaction().getLeader().asBukkitPlayer();
		for (PermissionAttachmentInfo perm : leader.getEffectivePermissions()) {
			if (perm.getPermission().startsWith("factionswarps.warplimit.")) {
				Integer max = Integer.valueOf(perm.getPermission().split(".")[2]);
				
				if (warpData.warpLocations.size()+1 > max) {
					msg("<red>You have reached the maximum amount of warps (<gold>{max}<red>) for this faction!",
							"max", max.toString());
					
					return;
				}
				
				break;
			}
		}
		
		String name = this.getArg(0);
		
		// Ensure the warp exists 
		if (warpData.warpExists(name)) {
			msg("<red>The warp <aqua>{name} <red>already exists",
					"name", name);
			return;
		}
		
		Location location = this.getFPlayer().asBukkitPlayer().getLocation();
		
		// If configured, check the warp is in their territory 
		if (Config.get().mustBeInTerritory) {
			Faction factionAtLocation = Factions.getFactionAt(location);
			
			if (factionAtLocation.getId() != this.getFPlayer().getFaction().getId()) {
				msg("<red>The warp must reside in your faction territory.");
				return;
			}
		}
		
		for (FactionsCommand command : CmdWarp.get().getSubCommands()) {
			if (command.getAliases().contains(name)) {
				msg("<red>You can't use this name as it is a subcommand.");
				return;
			}
		}
		
		EventFactionsWarpsCreate event = new EventFactionsWarpsCreate(this.getFPlayer().getFaction(), this.getFPlayer(), location, name, this.getArg(1));
		event.call();
		if (event.isCancelled()) return;
		
		String password = event.getPassword();
		name = event.getName();
		location = event.getLocation();
		
		if (password != null) {
			warpData.addWarp(name, password, location);
		} else {
			warpData.addWarp(name, location);
		}
		
		msg("<green>The warp <aqua>{name} <green>has been set at your location!", "name", name);
	}
	
}
