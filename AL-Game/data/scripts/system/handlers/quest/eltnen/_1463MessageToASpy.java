 /*
 * This file is part of aion-unique <aion-unique.org>.
 *
 * aion-unique is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * aion-unique is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with aion-unique.  If not, see <http://www.gnu.org/licenses/>.
 */
package quest.eltnen;

import java.util.Collections;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.questEngine.handlers.QuestHandler;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * @author Balthazar
 */
 
public class _1463MessageToASpy extends QuestHandler
{
	private final static int	questId	= 1463;
	
	public _1463MessageToASpy()
	{
		super(questId);
	}

	@Override
	public void register()
	{
		qe.setNpcQuestData(203940).addOnQuestStart(questId);
		qe.setNpcQuestData(203940).addOnTalkEvent(questId);
		qe.setNpcQuestData(203903).addOnTalkEvent(questId);
		qe.setNpcQuestData(204424).addOnTalkEvent(questId);
	}

	@Override
	public boolean onDialogEvent(QuestEnv env)
	{
		final Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(questId);
		
		int targetId = 0;
		if(env.getVisibleObject() instanceof Npc)
			targetId = ((Npc) env.getVisibleObject()).getNpcId();
		
		if(qs == null || qs.getStatus() == QuestStatus.NONE) 
		{
			if(targetId == 203940)
			{
				if(env.getDialogId() == 25)
				{
					return sendQuestDialog(player, env.getVisibleObject().getObjectId(), 1011);
				}
				else
					return defaultQuestStartDialog(env);
			}
		}
		if(qs == null)
			return false;
			
		if(qs.getStatus() == QuestStatus.START)
		{
			switch(targetId)
			{
				case 203903:
				{
					switch(env.getDialogId())
					{
						case 25:
						{
							if(qs.getQuestVarById(0) == 0)
							{
								return sendQuestDialog(player, env.getVisibleObject().getObjectId(), 1352);
							}
							else if(qs.getQuestVarById(0) == 2)
							{
								return sendQuestDialog(player, env.getVisibleObject().getObjectId(), 2375);
							}
						}
						case 10000:
						{
							if(player.getInventory().getItemCountByItemId(182201382) == 0)
								if (!ItemService.addItems(player, Collections.singletonList(new QuestItems(182201382, 1))))
									return true;
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							updateQuestStatus(player, qs);
							PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(env.getVisibleObject().getObjectId(), 10));
							return true;
						}
						case 1009:
						{
							qs.setQuestVar(3);
							player.getInventory().removeFromBagByItemId(182201383, 1);
							qs.setStatus(QuestStatus.REWARD);
							updateQuestStatus(player, qs);
							return defaultQuestEndDialog(env);
						}
					}
				}
				case 204424:
				{
					switch(env.getDialogId())
					{
						case 25:
						{
							if(qs.getQuestVarById(0) == 1)
							{
								return sendQuestDialog(player, env.getVisibleObject().getObjectId(), 1693);
							}
						}
						case 10001:
						{
							qs.setQuestVarById(0, qs.getQuestVarById(0) + 1);
							player.getInventory().removeFromBagByItemId(182201382, 1);
							if(player.getInventory().getItemCountByItemId(182201383) == 0)
								if (!ItemService.addItems(player, Collections.singletonList(new QuestItems(182201383, 1))))
									return true;
							updateQuestStatus(player, qs);
							PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(env.getVisibleObject().getObjectId(), 10));
							return true;
						}
						default: return defaultQuestEndDialog(env);
					}
				}
			}
		}
		else if (qs.getStatus() == QuestStatus.REWARD)
		{
			if (targetId == 203903)
			{
				if(env.getDialogId() == 1009)
					return sendQuestDialog(player, env.getVisibleObject().getObjectId(), 5);
				else return defaultQuestEndDialog(env);
			}
		}		
		return false;
	}
}