/*
 * BuildBattle 4 - Ultimate building competition minigame
 * Copyright (C) 2018  Plajer's Lair - maintained by Plajer and Tigerpanzer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.plajer.buildbattle.handlers.setup;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import pl.plajer.buildbattle.Main;
import pl.plajer.buildbattle.arena.Arena;
import pl.plajer.buildbattle.arena.ArenaRegistry;
import pl.plajer.buildbattle.arena.plots.ArenaPlot;
import pl.plajer.buildbattle.handlers.ChatManager;
import pl.plajer.buildbattle.handlers.PermissionManager;
import pl.plajer.buildbattle.utils.Cuboid;
import pl.plajer.buildbattle.utils.Utils;
import pl.plajerlair.core.services.exception.ReportedException;
import pl.plajerlair.core.utils.ConfigUtils;
import pl.plajerlair.core.utils.ItemBuilder;
import pl.plajerlair.core.utils.LocationUtils;

/**
 * Created by Tom on 15/06/2015.
 */
//todo recodeee
public class SetupInventoryEvents implements Listener {

  private Main plugin;

  public SetupInventoryEvents(Main plugin) {
    this.plugin = plugin;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler
  public void onGameTypeSetClick(InventoryClickEvent e) {
    try {
      if (!(e.getWhoClicked() instanceof Player || e.getWhoClicked().hasPermission(PermissionManager.getEditGames()))) {
        return;
      }
      if (e.getInventory() == null || !e.getInventory().getName().equals("Game type:") || !Utils.isNamed(e.getCurrentItem())) {
        return;
      }
      Player player = (Player) e.getWhoClicked();
      String name = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
      Arena arena = ArenaRegistry.getArena(e.getInventory().getName().replace("Game type: ", ""));
      if (arena == null) {
        return;
      }
      e.setCancelled(true);
      player.closeInventory();
      FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");
      if (name.contains("Solo")) {
        arena.setArenaType(Arena.ArenaType.SOLO);
        config.set("instances." + arena.getID() + ".gametype", "SOLO");
      } else if (name.contains("Team")) {
        arena.setArenaType(Arena.ArenaType.TEAM);
        config.set("instances." + arena.getID() + ".gametype", "TEAM");
      }
      player.sendMessage(ChatColor.GREEN + "Game type of arena set to " + ChatColor.GRAY + name);
      ConfigUtils.saveConfig(plugin, config, "arenas");
    } catch (Exception ex) {
      new ReportedException(plugin, ex);
    }
  }

  @EventHandler
  public void onClick(InventoryClickEvent e) {
    try {
      if (e.getWhoClicked().getType() != EntityType.PLAYER) {
        return;
      }
      Player player = (Player) e.getWhoClicked();
      if (!(player.hasPermission("buildbattle.admin.create") && e.getInventory().getName().contains("BB Arena:") && Utils.isNamed(e.getCurrentItem()))) {
        return;
      }

      //do not close inventory nor cancel event when setting arena name via name tag
      if (e.getCurrentItem().getType() != Material.NAME_TAG) {
        player.closeInventory();
        e.setCancelled(true);
      }

      Arena arena = ArenaRegistry.getArena(e.getInventory().getName().replace("BB Arena: ", ""));
      if (arena == null) {
        return;
      }
      ClickType clickType = e.getClick();
      String locationString = player.getLocation().getWorld().getName() + "," + player.getLocation().getX() + "," + player.getLocation().getY() + "," +
          player.getLocation().getZ() + "," + player.getLocation().getYaw() + ",0.0";
      FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");
      switch (SetupInventory.ClickPosition.getByPosition(e.getRawSlot())) {
        case SET_ENDING:
          config.set("instances." + arena.getID() + ".Endlocation", locationString);
          player.sendMessage(ChatManager.colorRawMessage("&e✔ Completed | &aEnding location for arena " + arena.getID() + " set at your location!"));
          break;
        case SET_LOBBY:
          config.set("instances." + arena.getID() + ".lobbylocation", locationString);
          player.sendMessage(ChatManager.colorRawMessage("&e✔ Completed | &aLobby location for arena " + arena.getID() + " set at your location!"));
          break;
        case SET_MINIMUM_PLAYERS:
          if (clickType.isRightClick()) {
            e.getCurrentItem().setAmount(e.getCurrentItem().getAmount() + 1);
          }
          if (clickType.isLeftClick()) {
            e.getCurrentItem().setAmount(e.getCurrentItem().getAmount() - 1);
          }
          config.set("instances." + arena.getID() + ".minimumplayers", e.getCurrentItem().getAmount());
          player.openInventory(new SetupInventory(arena).getInventory());
          break;
        case SET_MAXIMUM_PLAYERS:
          if (clickType.isRightClick()) {
            e.getCurrentItem().setAmount(e.getCurrentItem().getAmount() + 1);
          }
          if (clickType.isLeftClick()) {
            e.getCurrentItem().setAmount(e.getCurrentItem().getAmount() - 1);
          }
          config.set("instances." + arena.getID() + ".maximumplayers", e.getCurrentItem().getAmount());
          player.openInventory(new SetupInventory(arena).getInventory());
          break;
        case ADD_SIGN:
          Location location = player.getTargetBlock(null, 10).getLocation();
          if (!(location.getBlock().getState() instanceof Sign)) {
            player.sendMessage(ChatManager.colorMessage("Commands.Look-Sign"));
            break;
          }
          plugin.getSignManager().getLoadedSigns().put((Sign) location.getBlock().getState(), arena);
          player.sendMessage(ChatManager.PLUGIN_PREFIX + ChatManager.colorMessage("Signs.Sign-Created"));
          String loc = location.getBlock().getWorld().getName() + "," + location.getBlock().getX() + "," + location.getBlock().getY() + "," + location.getBlock().getZ() + ",0.0,0.0";
          List<String> locs = config.getStringList("instances." + arena + ".signs");
          locs.add(loc);
          config.set("instances." + arena + ".signs", locs);
          break;
        case SET_GAME_TYPE:
          Inventory inv = Bukkit.createInventory(null, 9, "Game type: " + arena.getID());
          inv.addItem(new ItemBuilder(new ItemStack(Material.NAME_TAG))
              .name(ChatColor.GREEN + "Solo game mode")
              .lore(ChatColor.GRAY + "1 player per plot")
              .build());
          inv.addItem(new ItemBuilder(new ItemStack(Material.NAME_TAG))
              .name(ChatColor.GREEN + "Team game mode")
              .lore(ChatColor.GRAY + "2 players per plot")
              .build());
          player.openInventory(inv);
          break;
        case SET_MAP_NAME:
          if (e.getCurrentItem().getType() == Material.NAME_TAG && e.getCursor().getType() == Material.NAME_TAG) {
            if (!Utils.isNamed(e.getCursor())) {
              player.sendMessage(ChatColor.RED + "This item doesn't has a name!");
              return;
            }
            String newName = e.getCursor().getItemMeta().getDisplayName();
            config.set("instances." + arena.getID() + ".mapname", newName);
            player.sendMessage(ChatManager.colorRawMessage("&e✔ Completed | &aName of arena " + arena.getID() + " set to " + newName));
            e.getCurrentItem().getItemMeta().setDisplayName(ChatColor.GOLD + "Set a mapname (currently: " + newName);
          }
          break;
        case ADD_GAME_PLOT:
          player.performCommand("bba addplot " + arena.getID());
          break;
        case ADD_FLOOR_CHANGER_NPC:
          player.performCommand("bba addnpc");
          break;
        case REGISTER_ARENA:
          if (arena.isReady()) {
            e.getWhoClicked().sendMessage(ChatColor.GREEN + "This arena was already validated and is ready to use!");
            return;
          }
          String[] locations = new String[] {"lobbylocation", "Endlocation"};
          for (String s : locations) {
            if (!config.isSet("instances." + arena.getID() + "." + s) || config.getString("instances." + arena.getID() + "." + s).equals(LocationUtils.locationToString(Bukkit.getWorlds().get(0).getSpawnLocation()))) {
              e.getWhoClicked().sendMessage(ChatColor.RED + "Arena validation failed! Please configure following spawn properly: " + s + " (cannot be world spawn location)");
              return;
            }
          }
          if (config.getConfigurationSection("instances." + arena.getID() + ".plots") == null) {
            e.getWhoClicked().sendMessage(ChatColor.RED + "Arena validation failed! Please configure plots properly");
            return;
          }
          for (String plotName : config.getConfigurationSection("instances." + arena.getID() + ".plots").getKeys(false)) {
            if (!config.isSet("instances." + arena.getID() + ".plots." + plotName + ".maxpoint") ||
                !config.isSet("instances." + arena.getID() + ".plots." + plotName + ".minpoint")) {
              e.getWhoClicked().sendMessage(ChatColor.RED + "Arena validation failed! Plots are not configured properly! (missing selection values)");
              return;
            }
            Location minPoint = LocationUtils.getLocation(config.getString("instances." + arena.getID() + ".plots." + plotName + ".minpoint"));
            ArenaPlot buildPlot = new ArenaPlot(minPoint.getWorld().getBiome(minPoint.getBlockX(), minPoint.getBlockZ()));
            buildPlot.setCuboid(new Cuboid(minPoint, LocationUtils.getLocation(config.getString("instances." + arena.getID() + ".plots." + plotName + ".maxpoint"))));
            buildPlot.fullyResetPlot();
            arena.getPlotManager().addBuildPlot(buildPlot);
          }
          e.getWhoClicked().sendMessage(ChatColor.GREEN + "Validation succeeded! Registering new arena instance: " + arena.getID());
          config.set("instances." + arena.getID() + ".isdone", true);
          ConfigUtils.saveConfig(plugin, config, "arenas");
          List<Sign> signsToUpdate = new ArrayList<>();
          ArenaRegistry.unregisterArena(arena);
          if (plugin.getSignManager().getLoadedSigns().containsValue(arena)) {
            for (Sign s : plugin.getSignManager().getLoadedSigns().keySet()) {
              if (plugin.getSignManager().getLoadedSigns().get(s).equals(arena)) {
                signsToUpdate.add(s);
              }
            }
          }
          arena = new Arena(arena.getID(), plugin);
          arena.setReady(true);
          arena.setMinimumPlayers(config.getInt("instances." + arena.getID() + ".minimumplayers"));
          arena.setMaximumPlayers(config.getInt("instances." + arena.getID() + ".maximumplayers"));
          arena.setMapName(config.getString("instances." + arena.getID() + ".mapname"));
          arena.setLobbyLocation(LocationUtils.getLocation(config.getString("instances." + arena.getID() + ".lobbylocation")));
          arena.setEndLocation(LocationUtils.getLocation(config.getString("instances." + arena.getID() + ".Endlocation")));
          arena.setArenaType(Arena.ArenaType.valueOf(config.getString("instances." + arena.getID() + ".gametype").toUpperCase()));

          for (String plotName : config.getConfigurationSection("instances." + arena.getID() + ".plots").getKeys(false)) {
            Location minPoint = LocationUtils.getLocation(config.getString("instances." + arena.getID() + ".plots." + plotName + ".minpoint"));
            ArenaPlot buildPlot = new ArenaPlot(minPoint.getWorld().getBiome(minPoint.getBlockX(), minPoint.getBlockZ()));
            buildPlot.setCuboid(new Cuboid(minPoint, LocationUtils.getLocation(config.getString("instances." + arena.getID() + ".plots." + plotName + ".maxpoint"))));
            buildPlot.fullyResetPlot();
            arena.getPlotManager().addBuildPlot(buildPlot);
          }
          arena.initPoll();
          ArenaRegistry.registerArena(arena);
          arena.start();
          for (Sign s : signsToUpdate) {
            plugin.getSignManager().getLoadedSigns().put(s, arena);
          }
          break;
        case VIEW_SETUP_VIDEO:
          player.sendMessage(ChatManager.PLUGIN_PREFIX + ChatManager.colorRawMessage("&6Check out this video: " + SetupInventory.VIDEO_LINK));
          break;
      }
      ConfigUtils.saveConfig(plugin, config, "arenas");
    } catch (Exception ex) {
      new ReportedException(plugin, ex);
    }
  }

}