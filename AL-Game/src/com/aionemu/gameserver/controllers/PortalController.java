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
package com.aionemu.gameserver.controllers;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.group.PlayerGroup;
import com.aionemu.gameserver.model.templates.portal.ExitPoint;
import com.aionemu.gameserver.model.templates.portal.PortalTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_USE_OBJECT;
import com.aionemu.gameserver.services.InstanceService;
import com.aionemu.gameserver.services.TeleportService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMap;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * @author ATracer
 * 
 */
public class PortalController extends NpcController
{
	private static final Logger	log						= LoggerFactory.getLogger(PortalController.class);

	PortalTemplate portalTemplate;

	@Override
	public void setOwner(Creature owner)
	{
		super.setOwner(owner);
		portalTemplate = DataManager.PORTAL_DATA.getPortalTemplate(owner.getObjectTemplate().getTemplateId());
	}

	@Override
	public void onDialogRequest(final Player player)
	{
		if(portalTemplate == null)
			return;
			
		if(!CustomConfig.ENABLE_INSTANCES)
			return;

		final int defaultUseTime = 3000;
		PacketSendUtility.sendPacket(player, new SM_USE_OBJECT(player.getObjectId(), getOwner().getObjectId(),
			defaultUseTime, 1));
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.START_QUESTLOOT, 0, getOwner().getObjectId()), true);

		ThreadPoolManager.getInstance().schedule(new Runnable(){
			@Override
			public void run()
			{
				PacketSendUtility.sendPacket(player, new SM_USE_OBJECT(player.getObjectId(), getOwner().getObjectId(),
					defaultUseTime, 0));
				PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.END_QUESTLOOT, 0, getOwner().getObjectId()), true);
				
				analyzePortation(player);
			}
			
			/**
			 * @param player
			 */
			private void analyzePortation(final Player player)
			{
				if(portalTemplate.getIdTitle() !=0 && player.getCommonData().getTitleId() != portalTemplate.getIdTitle() && CustomConfig.INSTANCES_TITLE_REQ)
					return;

				if(portalTemplate.getRace() != null && !portalTemplate.getRace().equals(player.getCommonData().getRace()) && CustomConfig.INSTANCES_RACE_REQ)
				{
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MOVE_PORTAL_ERROR_INVALID_RACE);
					return;
				}

				if(((portalTemplate.getMaxLevel() != 0 && player.getLevel() > portalTemplate.getMaxLevel()) || player.getLevel() < portalTemplate.getMinLevel()) && CustomConfig.INSTANCES_LEVEL_REQ)
				{
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANT_INSTANCE_ENTER_LEVEL);
					return;
				}

				PlayerGroup group = player.getPlayerGroup();
				if(portalTemplate.isGroup() && group == null && CustomConfig.INSTANCES_GROUP_REQ)
				{
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ENTER_ONLY_PARTY_DON);
					return;
				}

				int useDelay;
				try
				{
					useDelay = portalTemplate.getUseDelay()/CustomConfig.INSTANCES_COOLDOWN;
				}
				catch (ArithmeticException e)
				{
					useDelay = 0;
				}

				if(player.isPortalUseDisabled(portalTemplate.getExitPoint().getMapId()) && useDelay > 0)
				{
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANNOT_MAKE_INSTANCE_COOL_TIME);
					return;
				}
         
				if((portalTemplate.isGroup() && group != null) || (portalTemplate.isGroup() && !CustomConfig.INSTANCES_GROUP_REQ))
				{
					WorldMapInstance instance;

					// If there is a group (whatever group requirement exists or not)...
					if(group != null)
					{
						instance = InstanceService.getRegisteredInstance(portalTemplate.getExitPoint().getMapId(), group.getGroupId());
					}
					// But if there is no group (and solo is enabled, of course)
					else
					{
						instance = InstanceService.getRegisteredInstance(portalTemplate.getExitPoint().getMapId(), player.getObjectId());
					}

					// No instance (for group), group on and default requirement off
					if(instance == null && group != null && !CustomConfig.INSTANCES_GROUP_REQ)
					{					
						// For each player from group
						for(Player member : group.getMembers())
						{
							// Get his instance
							instance = InstanceService.getRegisteredInstance(portalTemplate.getExitPoint().getMapId(), member.getObjectId());

							// If some player is soloing and I found no one else yet, I get his instance
							if(instance != null)
							{
								break;
							}
						}

						// No solo instance found
						if(instance == null)
							instance = registerGroup(group);
					}

					// No instance and default requirement on = Group on
					else if(instance == null && CustomConfig.INSTANCES_GROUP_REQ)
					{
						instance = registerGroup(group);
					}
					// No instance, default requirement off, no group = Register new instance with player ID
					else if(instance == null && !CustomConfig.INSTANCES_GROUP_REQ && group == null)
					{
						instance = InstanceService.getNextAvailableInstance(portalTemplate.getExitPoint().getMapId());
						InstanceService.registerPlayerWithInstance(instance, player);
					}
				
					transfer(player, instance);
				}
				else if(!portalTemplate.isGroup())
				{
					WorldMapInstance instance;

					// If there is a group (whatever group requirement exists or not)...
					if(group != null && !CustomConfig.INSTANCES_GROUP_REQ)
					{
						instance = InstanceService.getRegisteredInstance(portalTemplate.getExitPoint().getMapId(), group.getGroupId());
					}
					// But if there is no group, go to solo
					else
					{
						instance = InstanceService.getRegisteredInstance(portalTemplate.getExitPoint().getMapId(), player.getObjectId());
					}

					// No group instance, group on and default requirement off
					if(instance == null && group != null && !CustomConfig.INSTANCES_GROUP_REQ)
					{
						// For each player from group
						for(Player member : group.getMembers())
						{
							// Get his instance
							instance = InstanceService.getRegisteredInstance(portalTemplate.getExitPoint().getMapId(), member.getObjectId());

							// If some player is soloing and I found no one else yet, I get his instance
							if(instance != null)
							{
								break;
							}
						}

						// No solo instance found
						if(instance == null)
							instance = registerGroup(group);
					}

					// if already registered - just teleport
					if(instance != null)
					{
						transfer(player, instance);
						return;
					}
					port(player);
				}
			}
		}, defaultUseTime);

	}

	/**
	 * @param player
	 */
	private void port(Player requester)
	{
		WorldMapInstance instance = null;
		int worldId = portalTemplate.getExitPoint().getMapId();
		if(portalTemplate.isInstance())
		{
			instance = InstanceService.getNextAvailableInstance(worldId);
			InstanceService.registerPlayerWithInstance(instance, requester);
			
		}
		else
		{
			WorldMap worldMap = World.getInstance().getWorldMap(worldId);
			if(worldMap == null)
			{
				log.warn("There is no registered map with id " + worldId);
				return;
			}
			instance = worldMap.getWorldMapInstance();
		}
		
		transfer(requester, instance);
	}

	/**
	 * @param player
	 */
	private WorldMapInstance registerGroup(PlayerGroup group)
	{
		WorldMapInstance instance = InstanceService.getNextAvailableInstance(portalTemplate.getExitPoint().getMapId());
		InstanceService.registerGroupWithInstance(instance, group);
		return instance;
	}

	/**
	 * @param players
	 */
	private void transfer(Player player, WorldMapInstance instance)
	{
		ExitPoint exitPoint = portalTemplate.getExitPoint();
		TeleportService.teleportTo(player, exitPoint.getMapId(), instance.getInstanceId(), exitPoint.getX(), exitPoint.getY(), exitPoint.getZ(), 0);
			int useDelay = portalTemplate.getUseDelay();
			if (useDelay > 0)
			player.addPortalCoolDown(exitPoint.getMapId(), System.currentTimeMillis() + (useDelay * 1000), useDelay);
	}

}
