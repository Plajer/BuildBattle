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

package pl.plajer.buildbattle4;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import pl.plajer.buildbattle4.handlers.ChatManager;
import pl.plajer.buildbattle4.utils.XMaterial;
import pl.plajerlair.core.utils.ConfigUtils;

/**
 * Created by Tom on 17/08/2015.
 */
public class VoteItems {

  private static Map<ItemStack, Integer> voteItems = new HashMap<>();
  private static FileConfiguration config = ConfigUtils.getConfig(JavaPlugin.getPlugin(Main.class), "voteItems");

  public static void giveVoteItems(Player player) {
    for (ItemStack itemStack : voteItems.keySet()) {
      player.getInventory().setItem(voteItems.get(itemStack), itemStack);
    }
    player.updateInventory();
  }


  public static void loadVoteItemsFromConfig() {
    for (String s : config.getKeys(false)) {
      if (config.contains(s + ".displayname") && !config.contains(s + ".report-item-function")) {
        if (!config.isSet(s + ".material-name")) {
          config.set(s + ".material-name", XMaterial.GREEN_TERRACOTTA.name());
          Main.debug("Found outdated item in votingItems.yml! We've converted it to the newest version!", System.currentTimeMillis());
        }
        ConfigUtils.saveConfig(JavaPlugin.getPlugin(Main.class), config, "voteItems");
        ItemStack item = XMaterial.fromString(config.getString(s + ".material-name").toUpperCase()).parseItem();
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatManager.colorRawMessage(config.getString(s + ".displayname")));
        item.setItemMeta(itemMeta);
        voteItems.put(item, Integer.parseInt(s));
      }
    }
  }

  public static int getPoints(ItemStack itemStack) {
    for (ItemStack voteItem : voteItems.keySet()) {
      if (itemStack.getType() == voteItem.getType() && itemStack.getItemMeta().getDisplayName().equalsIgnoreCase(voteItem.getItemMeta().getDisplayName()))
        return voteItems.get(voteItem) + 1;
    }
    return 1;
  }

}