/*
 * This file is part of aion-unique <aionu-unique.org>.
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
package com.aionemu.gameserver.network.aion.serverpackets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * @author MrPoke
 * 
 */
public class SM_QUEST_LIST extends AionServerPacket
{

	private SortedMap<Integer, QuestState>	completeQuestList	= new TreeMap<Integer, QuestState>();
	private List<QuestState>				startedQuestList	= new ArrayList<QuestState>();
    private List<Integer>                   questList           = new ArrayList<Integer>();
	
	public SM_QUEST_LIST(Player player)
	{
		for(QuestState qs : player.getQuestStateList().getAllQuestState())
		{
			if(qs.getStatus() == QuestStatus.COMPLETE)
				completeQuestList.put(qs.getQuestId(), qs);
			else if(qs.getStatus() != QuestStatus.NONE)
				startedQuestList.add(qs);
		}
		
		//temp solution to enable teleports
		// TODO remove this as this quests are implemented
		if (player.getCommonData().getRace() == Race.ELYOS)
		{
			completeQuestList.put(1130, new QuestState(1130, QuestStatus.COMPLETE, 0, 1));
			completeQuestList.put(1300, new QuestState(1300, QuestStatus.COMPLETE, 0, 1));
			completeQuestList.put(1006, new QuestState(1006, QuestStatus.COMPLETE, 0, 1));
			completeQuestList.put(1007, new QuestState(1007, QuestStatus.COMPLETE, 0, 1));
		}
		else
		{
			completeQuestList.put(2200, new QuestState(2200, QuestStatus.COMPLETE, 0, 1));
			completeQuestList.put(2300, new QuestState(2300, QuestStatus.COMPLETE, 0, 1));
			completeQuestList.put(2008, new QuestState(2008, QuestStatus.COMPLETE, 0, 1));
			completeQuestList.put(2009, new QuestState(2009, QuestStatus.COMPLETE, 0, 1));
		}
    }
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con, ByteBuffer buf)
	{
		writeH(buf, completeQuestList.size());
		for(QuestState qs : completeQuestList.values())
		{
			writeH(buf, qs.getQuestId());
			writeH(buf, 0x00);
			writeC(buf, qs.getCompliteCount());
		}
		writeC(buf, startedQuestList.size());
		for(QuestState qs : startedQuestList) // quest list size ( max is 25 )
		{
			writeH(buf, qs.getQuestId());
			writeH(buf, 0);
		}
		for(QuestState qs : startedQuestList)
		{
			writeC(buf, qs.getStatus().value());
			writeD(buf, qs.getQuestVars().getQuestVars());
			writeC(buf, 0);
		}
	}

}