/*
 * This file is part of aion-unique <aion-unique.org>.
 *
 *  aion-unique is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  aion-unique is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with aion-unique.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.network.aion.clientpackets;

import java.util.List;

import com.aionemu.commons.versionning.Version;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.model.ChatType;
import com.aionemu.gameserver.model.account.Account;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.Storage;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_RANK;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CHANNEL_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ENTER_WORLD_CHECK;
import com.aionemu.gameserver.network.aion.serverpackets.SM_GAME_TIME;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INFLUENCE_RATIO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_COOLDOWN;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_COOLDOWN;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MACRO_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_SPAWN;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PRICES;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_RECIPE_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SIEGE_LOCATION_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_COOLDOWN;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATS_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TITLE_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_UI_SETTINGS;
import com.aionemu.gameserver.services.AllianceService;
import com.aionemu.gameserver.services.BrokerService;
import com.aionemu.gameserver.services.ChatService;
import com.aionemu.gameserver.services.ClassChangeService;
import com.aionemu.gameserver.services.GroupService;
import com.aionemu.gameserver.services.KiskService;
import com.aionemu.gameserver.services.LegionService;
import com.aionemu.gameserver.services.MailService;
import com.aionemu.gameserver.services.PetitionService;
import com.aionemu.gameserver.services.PlayerService;
import com.aionemu.gameserver.services.PunishmentService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.StigmaService;
import com.aionemu.gameserver.services.TeleportService;
import com.aionemu.gameserver.services.ToyPetService;
import com.aionemu.gameserver.utils.rates.Rates;
import com.aionemu.gameserver.world.World;

/**
 * In this packets aion client is asking if given char [by oid] may login into game [ie start playing].
 * 
 * @author -Nemesiss-, Avol
 * 
 */
public class CM_ENTER_WORLD extends AionClientPacket
{
	/**
	 * Object Id of player that is entering world
	 */
	private int					objectId;
	
	private static final String serverMessage;
		
	/**
	 * Constructs new instance of <tt>CM_ENTER_WORLD </tt> packet
	 * 
	 * @param opcode
	 */
	public CM_ENTER_WORLD(int opcode)
	{
		super(opcode);
	}
	
	static
	{
		String buffer = "Welcome to " + GSConfig.SERVER_NAME + " Server.\nPowered By Aion-Lightning Software Developed By http://aion-lightning.org/ Team.\n";
		buffer += "Copyright 2010. ";
		if(GSConfig.SERVER_MOTD_DISPLAYREV)
			buffer += "Server Revision: " + String.format("%-6s", new Version(GameServer.class).getRevision());
		
		serverMessage = buffer;
		buffer = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl()
	{
		objectId = readD();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl()
	{
		AionConnection client = getConnection();
		enterWorld(client, objectId);
	}
	
	public static void enterWorld(AionConnection client, int objectId)
	{
		Account account = client.getAccount();
		PlayerAccountData playerAccData = client.getAccount().getPlayerAccountData(objectId);

		if(playerAccData == null)
		{
			// Somebody wanted to login on character that is not at his account
			return;
		}

		Player player = PlayerService.getPlayer(objectId, account);

		if(player != null && client.setActivePlayer(player))
		{
			player.setClientConnection(client);
			/*
			 * Store player into World.
			 */
			Player player2 = World.getInstance().findPlayer(player.getObjectId());
			if (player2 != null)
				World.getInstance().removeObject(player2);
			World.getInstance().storeObject(player);

			StigmaService.onPlayerLogin(player);
			client.sendPacket(new SM_SKILL_LIST(player));
			
			if(player.getSkillCoolDowns() != null)
				client.sendPacket(new SM_SKILL_COOLDOWN(player.getSkillCoolDowns()));
			
			if(player.getItemCoolDowns() != null)
				client.sendPacket(new SM_ITEM_COOLDOWN(player.getItemCoolDowns()));

			client.sendPacket(new SM_QUEST_LIST(player));
			client.sendPacket(new SM_RECIPE_LIST(player.getRecipeList().getRecipeList()));

			/*
			 * Needed
			 */
			client.sendPacket(new SM_ENTER_WORLD_CHECK());

			byte[] uiSettings = player.getPlayerSettings().getUiSettings();
			byte[] shortcuts = player.getPlayerSettings().getShortcuts();

			if(uiSettings != null)
				client.sendPacket(new SM_UI_SETTINGS(uiSettings, 0));

			if(shortcuts != null)
				client.sendPacket(new SM_UI_SETTINGS(shortcuts, 1));

			// Cubesize limit set in inventory.
			int cubeSize = player.getCubeSize();
			player.getInventory().setLimit(27 + cubeSize * 9);

			// items
			Storage inventory = player.getInventory();
			List<Item> equipedItems = player.getEquipment().getEquippedItems();
			if(equipedItems.size() != 0)
			{
				client.sendPacket(new SM_INVENTORY_INFO(player.getEquipment().getEquippedItems(), cubeSize));
			}

			List<Item> unequipedItems = inventory.getAllItems();
			int itemsSize = unequipedItems.size();

			if(itemsSize != 0)
			{
				int index = 0;
				while(index + 10 < itemsSize)
				{
					client.sendPacket(new SM_INVENTORY_INFO(unequipedItems.subList(index, index + 10), cubeSize));
					index += 10;
				}
				client.sendPacket(new SM_INVENTORY_INFO(unequipedItems.subList(index, itemsSize), cubeSize));
			}

			client.sendPacket(new SM_INVENTORY_INFO());

			PlayerService.playerLoggedIn(player);
			
			client.sendPacket(new SM_STATS_INFO(player));
			
			client.sendPacket(new SM_CUBE_UPDATE(player, 6, player.getCommonData().getAdvencedStigmaSlotSize()));
			
			KiskService.onLogin(player);
			TeleportService.sendSetBindPoint(player);
			
			// Alliance Packet after SetBindPoint
			if(player.isInAlliance())
				AllianceService.getInstance().onLogin(player);


			client.sendPacket(new SM_INSTANCE_COOLDOWN(player));
			
			client.sendPacket(new SM_MACRO_LIST(player));
			client.sendPacket(new SM_GAME_TIME());
			player.getController().updateNearbyQuests();

			client.sendPacket(new SM_TITLE_LIST(player));		
			client.sendPacket(new SM_CHANNEL_INFO(player.getPosition()));
			client.sendPacket(new SM_PLAYER_SPAWN(player));
			client.sendPacket(new SM_EMOTION_LIST());
			client.sendPacket(new SM_INFLUENCE_RATIO());
			client.sendPacket(new SM_SIEGE_LOCATION_INFO());
			// TODO: Send Rift Announce Here
			client.sendPacket(new SM_PRICES(player.getPrices()));
			client.sendPacket(new SM_ABYSS_RANK(player.getAbyssRank()));

			client.sendPacket(new SM_MESSAGE(0, null, serverMessage, ChatType.ANNOUNCEMENTS));

			if(player.isInPrison())
				PunishmentService.updatePrisonStatus(player);

			if(player.isLegionMember())
				LegionService.getInstance().onLogin(player);

			if(player.isInGroup())
				GroupService.getInstance().onLogin(player);

			player.setRates(Rates.getRatesFor(client.getAccount().getMembership()));
			
			ToyPetService.getInstance().onPlayerLogin(player);

			ClassChangeService.showClassChangeDialog(player);
			
			/**
			 * Notify mail service to load all mails
			 */
			MailService.getInstance().onPlayerLogin(player);

			/**
			 * Notify player if have broker settled items
			 */
			BrokerService.getInstance().onPlayerLogin(player);
			/**
			 * Start initializing chat connection(/1, /2, /3, /4 channels)
			 */
			if(!GSConfig.DISABLE_CHAT_SERVER)
				ChatService.onPlayerLogin(player);
			
			/**
			 * Send petition data if player has one
			 */
			PetitionService.getInstance().onPlayerLogin(player);

			/**
			 * Alert player about currently vulnerable fortresses
			 */
			SiegeService.getInstance().onPlayerLogin(player);

			/**
			 * Trigger restore services on login.
			 */
			player.getLifeStats().updateCurrentStats();
		}
		else
		{
			// TODO this is an client error - inform client.
		}
	}

}