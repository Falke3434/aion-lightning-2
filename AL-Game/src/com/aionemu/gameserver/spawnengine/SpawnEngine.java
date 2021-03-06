/*
 * This file is part of aion-emu <aion-emu.com>.
 *
 *  aion-emu is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  aion-emu is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with aion-emu.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.spawnengine;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.controllers.ActionitemController;
import com.aionemu.gameserver.controllers.BindpointController;
import com.aionemu.gameserver.controllers.ChestController;
import com.aionemu.gameserver.controllers.GatherableController;
import com.aionemu.gameserver.controllers.GroupGateController;
import com.aionemu.gameserver.controllers.KiskController;
import com.aionemu.gameserver.controllers.MonsterController;
import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.controllers.PortalController;
import com.aionemu.gameserver.controllers.PostboxController;
import com.aionemu.gameserver.controllers.PostmanController;
import com.aionemu.gameserver.controllers.ServantController;
import com.aionemu.gameserver.controllers.SummonController;
import com.aionemu.gameserver.controllers.effect.EffectController;
import com.aionemu.gameserver.controllers.siege.ArtifactController;
import com.aionemu.gameserver.controllers.siege.SiegeGeneralController;
import com.aionemu.gameserver.controllers.siege.SiegeGuardController;
import com.aionemu.gameserver.controllers.siege.SiegeMineController;
import com.aionemu.gameserver.controllers.siege.SiegeNpcController;
import com.aionemu.gameserver.dao.SiegeDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.GroupGate;
import com.aionemu.gameserver.model.gameobjects.Kisk;
import com.aionemu.gameserver.model.gameobjects.Monster;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.Servant;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.Trap;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.siege.Artifact;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeGeneral;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeGuard;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeMine;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.model.templates.GatherableTemplate;
import com.aionemu.gameserver.model.templates.NpcTemplate;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.WorldMapTemplate;
import com.aionemu.gameserver.model.templates.spawn.SpawnGroup;
import com.aionemu.gameserver.model.templates.spawn.SpawnTemplate;
import com.aionemu.gameserver.model.templates.stats.SummonStatsTemplate;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.KnownList;
import com.aionemu.gameserver.world.StaticObjectKnownList;
import com.aionemu.gameserver.world.World;

import javolution.util.FastMap;

/**
 * 
 * This class is responsible for NPCs spawn management. Current implementation is temporal and will be replaced in the
 * future.
 * 
 * @author Luno
 * 
 *         modified by ATracer
 * 
 */
public class SpawnEngine
{
	private static Logger				log					= LoggerFactory.getLogger(SpawnEngine.class);

	/** Counter counting number of npc spawns */
	private int							npcCounter			= 0;
	/** Counter counting number of gatherable spawns */
	private int							gatherableCounter	= 0;

	
	public static final SpawnEngine getInstance()
	{
		return SingletonHolder.instance;
	}

	private SpawnEngine()
	{
		this.spawnAll();
	}

	/**
	 * Creates VisibleObject instance and spawns it using given {@link SpawnTemplate} instance.
	 * 
	 * @param spawn
	 * @return created and spawned VisibleObject
	 */
	public VisibleObject spawnObject(SpawnTemplate spawn, int instanceIndex)
	{
		
		VisibleObjectTemplate template = null;
		int objectId = spawn.getSpawnGroup().getNpcid();
		NpcData	npcData = DataManager.NPC_DATA;
		if(objectId > 400000 && objectId < 499999)// gatherable
		{
			template = DataManager.GATHERABLE_DATA.getGatherableTemplate(objectId);
			if(template == null)
				return null;
			gatherableCounter++;
		}
		else
		// npc
		{
			template = npcData.getNpcTemplate(objectId);
			if(template == null)
				return null;
			npcCounter++;
		}
		IDFactory iDFactory = IDFactory.getInstance();
		if(template instanceof NpcTemplate)
		{
			NpcType npcType = ((NpcTemplate) template).getNpcType();
			Npc npc = null;

		/*-------Spawning Npcs,Monsters and other simple Npcs---------*/
			if (spawn.getSpawnGroup().getSiegeId()==0) 
			{
				switch(npcType)
				{
					case AGGRESSIVE:
					case ATTACKABLE:
						npc = new Monster(iDFactory.nextId(), new MonsterController(),
							spawn, template);
						npc.setKnownlist(new KnownList(npc));
						break;
					case POSTBOX:
						npc = new Npc(iDFactory.nextId(), new PostboxController(), spawn,
							template);
						npc.setKnownlist(new StaticObjectKnownList(npc));
						break;
					case RESURRECT:
						BindpointController bindPointController = new BindpointController();
						bindPointController.setBindPointTemplate(DataManager.BIND_POINT_DATA.getBindPointTemplate(objectId));
						npc = new Npc(iDFactory.nextId(), bindPointController, spawn, template);
						npc.setKnownlist(new StaticObjectKnownList(npc));
						break;
					case USEITEM:
						npc = new Npc(iDFactory.nextId(), new ActionitemController(),
							spawn, template);
						npc.setKnownlist(new StaticObjectKnownList(npc));
						break;
					case PORTAL:
						npc = new Npc(iDFactory.nextId(), new PortalController(), spawn,
							template);
						npc.setKnownlist(new StaticObjectKnownList(npc));
						break;
					case CHEST:
						npc = new Npc(iDFactory.nextId(), new ChestController(), spawn,
							template);
						npc.setKnownlist(new StaticObjectKnownList(npc));
						break;
					default: // NON_ATTACKABLE
						npc = new Npc(iDFactory.nextId(), new NpcController(), spawn,
							template);
						npc.setKnownlist(new KnownList(npc));
				}
			}
						/*----------Spawn siege npcs like Siege Guardians,Protectors,Vendors------------*/
			else
			{
				FastMap<Integer, SiegeLocation> locations = DataManager.SIEGE_LOCATION_DATA.getSiegeLocations();
				DAOManager.getDAO(SiegeDAO.class).loadSiegeLocations(locations);
				int spawnSiegeId = spawn.getSpawnGroup().getSiegeId();
				SiegeRace spawnRace = spawn.getSpawnGroup().getRace();
				SiegeLocation siegeLocation = locations.get(spawnSiegeId);
				SiegeRace siegeRace = siegeLocation.getRace();
				if ( (spawnSiegeId==siegeLocation.getLocationId()) && (spawnRace==siegeRace))
				{
					if(spawn.getSpawnGroup().getSiegeSpawnType()!=null)
						switch(spawn.getSpawnGroup().getSiegeSpawnType())
						{
							case PEACE:
								SiegeNpcController controller = new SiegeNpcController();
								npc = new SiegeNpc(iDFactory.nextId(), controller, spawn, template);
								npc.setKnownlist(new KnownList(npc));
								break;
							case ARTIFACT:
								ArtifactController artifactController= new ArtifactController();
								npc = new Artifact(iDFactory.nextId(), artifactController, spawn, template);
								npc.setKnownlist(new KnownList(npc));
								break;
							case PROTECTOR:
								SiegeGeneralController generalController = new SiegeGeneralController();
								npc = new SiegeGeneral(iDFactory.nextId(), generalController, spawn, template);
								npc.setKnownlist(new KnownList(npc));
								break;
							case MINE:
								SiegeMineController mineController = new SiegeMineController();
								npc = new SiegeMine(iDFactory.nextId(), mineController, spawn, template);
								npc.setKnownlist(new KnownList(npc));
								break;
							default:
								break;
						}
					else	//default: GUARD
					{
						SiegeGuardController guardController= new SiegeGuardController();
						npc = new SiegeGuard(iDFactory.nextId(), guardController, spawn, template);
						npc.setKnownlist(new KnownList(npc));
					}
				}
				else
					return null;
			}

			npc.setNpcSkillList(DataManager.NPC_SKILL_DATA.getNpcSkillList(template.getTemplateId()));
			npc.setEffectController(new EffectController(npc));
			npc.getController().onRespawn();
			bringIntoWorld(npc, spawn, instanceIndex);
			return npc;
		}
		
		else if(template instanceof GatherableTemplate)
		{
			Gatherable gatherable = new Gatherable(spawn, template, iDFactory.nextId(), new GatherableController());
			gatherable.setKnownlist(new StaticObjectKnownList(gatherable));
			bringIntoWorld(gatherable, spawn, instanceIndex);
			return gatherable;
		}
		return null;
	}

	/**
	 * 
	 * @param spawn
	 * @param instanceIndex
	 * @param creator
	 * @return
	 */
	public Trap spawnTrap(SpawnTemplate spawn, int instanceIndex, Creature creator, int skillId)
	{
		int objectId = spawn.getSpawnGroup().getNpcid();
		NpcTemplate npcTemplate = DataManager.NPC_DATA.getNpcTemplate(objectId);
		Trap trap = new Trap(IDFactory.getInstance().nextId(), new NpcController(), spawn,
			npcTemplate);
		trap.setKnownlist(new KnownList(trap));
		trap.setEffectController(new EffectController(trap));
		trap.setCreator(creator);
		trap.setSkillId(skillId);
		trap.getController().onRespawn();
		bringIntoWorld(trap, spawn, instanceIndex);
		return trap;
	}
	
	/**
	 *	
	 * @param spawn
	 * @param instanceIndex
	 * @param creator
	 * @return
	 */
	 public GroupGate spawnGroupGate(SpawnTemplate spawn, int instanceIndex, Creature creator)
    {
        int objectId = spawn.getSpawnGroup().getNpcid();
        NpcTemplate npcTemplate = DataManager.NPC_DATA.getNpcTemplate(objectId);
        GroupGate groupgate = new GroupGate(IDFactory.getInstance().nextId(), new GroupGateController(), spawn,
            npcTemplate); 
        groupgate.setKnownlist(new StaticObjectKnownList(groupgate));
        groupgate.setEffectController(new EffectController(groupgate));
        groupgate.setCreator(creator);
        groupgate.getController().onRespawn();
        bringIntoWorld(groupgate, spawn, instanceIndex);
        return groupgate;
    }
	       		 
 		    /** 
 		     * @param spawn 
 		     * @param instanceIndex 
 		     * @param creator 
 		     * @return 
 		     */ 
	public Kisk spawnKisk(SpawnTemplate spawn, int instanceIndex, Player creator)
	{
		int npcId = spawn.getSpawnGroup().getNpcid();
		NpcTemplate template = DataManager.NPC_DATA.getNpcTemplate(npcId);
		Kisk kisk = new Kisk(IDFactory.getInstance().nextId(), new KiskController(),
			spawn, template, creator);
		kisk.setKnownlist(new StaticObjectKnownList(kisk));
		kisk.setEffectController(new EffectController(kisk));
		kisk.getController().onRespawn();
		bringIntoWorld(kisk, spawn, instanceIndex);
		return kisk;
	}  	
	
		/**
	 * @param recipient
	 * @author ViAl
	 * 
	 * Spawns postman for express mail
	 */
	public void spawnPostman(Player recipient)
	{
		NpcData	npcData = DataManager.NPC_DATA;
		NpcTemplate template = npcData.getNpcTemplate(798044);
		IDFactory iDFactory = IDFactory.getInstance();
		int worldId = recipient.getWorldId();
		int instanceId = recipient.getInstanceId();
		float x = recipient.getX();
		float y = recipient.getY();
		float z = recipient.getZ();
		byte heading = recipient.getHeading();
		SpawnTemplate spawn = addNewSpawn(worldId, instanceId, 798044, x, y, z, heading, 0, 0, false, true);
		Npc postman = new Npc(iDFactory.nextId(), new PostmanController(recipient.getObjectId()), spawn, template);
		postman.setKnownlist(new StaticObjectKnownList(postman));
		bringIntoWorld(postman, spawn, instanceId);
	}   
	
	/**
	 * 
	 * @param spawn
	 * @param instanceIndex
	 * @param creator
	 * @param skillId
	 * @return
	 */
	public Servant spawnServant(SpawnTemplate spawn, int instanceIndex, Creature creator, int skillId, int hpRatio)
	{
		int objectId = spawn.getSpawnGroup().getNpcid();
		NpcTemplate npcTemplate = DataManager.NPC_DATA.getNpcTemplate(objectId);
		Servant servant = new Servant(IDFactory.getInstance().nextId(), new ServantController(), spawn,
			npcTemplate);
		servant.setKnownlist(new KnownList(servant));
		servant.setEffectController(new EffectController(servant));
		servant.setCreator(creator);
		servant.setSkillId(skillId);
		servant.setTarget(creator.getTarget());
		servant.setHpRatio(hpRatio);
		servant.getController().onRespawn();
		bringIntoWorld(servant, spawn, instanceIndex);
		return servant;
	}
	
	/**
	 * 
	 * @param creator
	 * @param npcId
	 * @return
	 */
	public Summon spawnSummon(Player creator, int npcId, int skillLvl)
	{	
		float x = creator.getX();
		float y = creator.getY();
		float z = creator.getZ();
		byte heading = creator.getHeading();
		int worldId = creator.getWorldId();
		int instanceId = creator.getInstanceId();
		
		SpawnTemplate spawn = createSpawnTemplate(worldId, npcId, x, y, z, heading, 0, 0);
		NpcTemplate npcTemplate = DataManager.NPC_DATA.getNpcTemplate(npcId);
		
		byte level = (byte) (npcTemplate.getLevel() + skillLvl - 1);
		SummonStatsTemplate statsTemplate = DataManager.SUMMON_STATS_DATA.getSummonTemplate(npcId, level);
		Summon summon = new Summon(IDFactory.getInstance().nextId(), new SummonController(), spawn,
			npcTemplate, statsTemplate, level);
		summon.setKnownlist(new KnownList(summon));
		summon.setEffectController(new EffectController(summon));
		summon.setMaster(creator);

		bringIntoWorld(summon, spawn, instanceId);
		return summon;
	}

	/**
	 * 
	 * @param worldId
	 * @param objectId
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param walkerid
	 * @param randomwalk
	 * @return
	 */
	private SpawnTemplate createSpawnTemplate(int worldId, int objectId, float x, float y, float z, byte heading,
		int walkerid, int randomwalk)
	{
		SpawnTemplate spawnTemplate = new SpawnTemplate(x, y, z, heading, walkerid, randomwalk, 0);

		SpawnGroup spawnGroup = new SpawnGroup(worldId, objectId, 295, 1);
		spawnTemplate.setSpawnGroup(spawnGroup);
		spawnGroup.getObjects().add(spawnTemplate);

		return spawnTemplate;
	}

	/**
	 * Should be used when need to define whether spawn will be deleted after death Using this method spawns will not be
	 * saved with //save_spawn command
	 * 
	 * @param worldId
	 * @param objectId
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param walkerid
	 * @param randomwalk
	 * @param noRespawn
	 * @return SpawnTemplate
	 */
	public SpawnTemplate addNewSpawn(int worldId, int instanceId, int objectId, float x, float y, float z,
		byte heading, int walkerid, int randomwalk, boolean noRespawn)
	{
		return this
			.addNewSpawn(worldId, instanceId, objectId, x, y, z, heading, walkerid, randomwalk, noRespawn, false);
	}

	/**
	 * 
	 * @param worldId
	 * @param instanceId
	 * @param objectId
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param walkerid
	 * @param randomwalk
	 * @param noRespawn
	 * @param isNewSpawn
	 * @return SpawnTemplate
	 */
	public SpawnTemplate addNewSpawn(int worldId, int instanceId, int objectId, float x, float y, float z,
		byte heading, int walkerid, int randomwalk, boolean noRespawn, boolean isNewSpawn)
	{
		SpawnTemplate spawnTemplate = createSpawnTemplate(worldId, objectId, x, y, z, heading, walkerid, randomwalk);

		if(spawnTemplate == null)
		{
			log.warn("Object couldn't be spawned");
			return null;
		}

		if(!noRespawn)
		{
			DataManager.SPAWNS_DATA.addNewSpawnGroup(spawnTemplate.getSpawnGroup(), worldId, objectId, isNewSpawn);
		}

		spawnTemplate.setNoRespawn(noRespawn, instanceId);

		return spawnTemplate;
	}

	private void bringIntoWorld(VisibleObject visibleObject, SpawnTemplate spawn, int instanceIndex)
	{
		World world = World.getInstance();
		world.storeObject(visibleObject);
		world.setPosition(visibleObject, spawn.getWorldId(), instanceIndex, spawn.getX(), spawn.getY(), spawn.getZ(),
			spawn.getHeading());
		world.spawn(visibleObject);
	}

	/**
	 * Spawn all NPC's from templates
	 */
	public void spawnAll()
	{
		this.npcCounter = 0;
		this.gatherableCounter = 0;
		
		for(WorldMapTemplate worldMapTemplate : DataManager.WORLD_MAPS_DATA)
		{
			if(worldMapTemplate.isInstance())
				continue;
			int maxTwin = worldMapTemplate.getTwinCount();
			final int mapId = worldMapTemplate.getMapId();
			int numberToSpawn = maxTwin > 0 ? maxTwin : 1;

			for(int i = 1; i <= numberToSpawn; i++)
			{
				spawnInstance(mapId, i);
			}
		}

		log.info("Loaded " + npcCounter + " npc spawns");
		log.info("Loaded " + gatherableCounter + " gatherable spawns");

		RiftSpawnManager.startRiftPool();
	}

	/**
	 * 
	 * @param worldId
	 * @param instanceIndex
	 */
	public void spawnInstance(int worldId, int instanceIndex)
	{

		List<SpawnGroup> worldSpawns = DataManager.SPAWNS_DATA.getSpawnsForWorld(worldId);

		if(worldSpawns == null || worldSpawns.size() == 0)
			return;

		int instanceSpawnCounter = 0;
		for(SpawnGroup spawnGroup : worldSpawns)
		{
			spawnGroup.resetLastSpawnCounter(instanceIndex);
			if(spawnGroup.getHandler() == null)
			{
				int pool = spawnGroup.getPool();
				for(int i = 0; i < pool; i++)
				{
					spawnObject(spawnGroup.getNextAvailableTemplate(instanceIndex), instanceIndex);

					instanceSpawnCounter++;
				}
			}
			else
			{
				switch(spawnGroup.getHandler())
				{
					case RIFT:
						RiftSpawnManager.addRiftSpawnGroup(spawnGroup);
						break;
					case STATIC:
						StaticObjectSpawnManager.spawnGroup(spawnGroup, instanceIndex);
					default:
						break;
				}
			}
		}
		log.info("Spawned " + worldId + " [" + instanceIndex + "] : " + instanceSpawnCounter);
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final SpawnEngine instance = new SpawnEngine();
	}
}
