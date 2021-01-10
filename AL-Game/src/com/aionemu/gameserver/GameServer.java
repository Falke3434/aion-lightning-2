/*
 * This file is part of aion-emu <aion-emu.com>.
 *
 * aion-emu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * aion-emu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with aion-emu.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.log4j.exceptions.Log4jInitializationError;
import com.aionemu.commons.network.NioServer;
import com.aionemu.commons.network.ServerCfg;
import com.aionemu.commons.utils.AEInfos;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.configs.main.TaskManagerConfig;
import com.aionemu.gameserver.configs.main.ThreadConfig;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.network.aion.GameConnectionFactoryImpl;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.services.AllianceService;
import com.aionemu.gameserver.services.AnnouncementService;
import com.aionemu.gameserver.services.BrokerService;
import com.aionemu.gameserver.services.DebugService;
import com.aionemu.gameserver.services.DropService;
import com.aionemu.gameserver.services.DuelService;
import com.aionemu.gameserver.services.ExchangeService;
import com.aionemu.gameserver.services.GameTimeService;
import com.aionemu.gameserver.services.GroupService;
import com.aionemu.gameserver.services.MailService;
import com.aionemu.gameserver.services.PeriodicSaveService;
import com.aionemu.gameserver.services.PetitionService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.SystemMailService;
import com.aionemu.gameserver.services.ToyPetService;
import com.aionemu.gameserver.services.WeatherService;
import com.aionemu.gameserver.services.ZoneService;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.taskmanager.TaskManagerFromDB;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import com.aionemu.gameserver.utils.AEVersions;
import com.aionemu.gameserver.utils.DeadlockDetector;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.ThreadUncaughtExceptionHandler;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.ChatHandlers;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException; 

/**
 * <tt>GameServer</tt> is the main class of the application and represents the whole game server.<br>
 * This class is also an entry point with main() method.
 * 
 * @author -Nemesiss-
 * @author SoulKeeper
 */
public class GameServer
{
	/** Logger for gameserver */
	private static final Logger	log	= LoggerFactory.getLogger(GameServer.class);

	private static void initalizeLoggger() {
		new File("./log/backup/").mkdirs();
		File[] files = new File("log").listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".log");
			}
		});

		if (files != null && files.length > 0) {
			byte[] buf = new byte[1024];
			try {
				String outFilename = "./log/backup/" + new SimpleDateFormat("yyyy-MM-dd HHmmss").format(new Date()) + ".zip";
				ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));
				out.setMethod(ZipOutputStream.DEFLATED);
				out.setLevel(Deflater.BEST_COMPRESSION);

				for (File logFile : files) {
					FileInputStream in = new FileInputStream(logFile);
					out.putNextEntry(new ZipEntry(logFile.getName()));
					int len;
					while ((len = in.read(buf)) > 0) {
						out.write(buf, 0, len);
					}
					out.closeEntry();
					in.close();
					logFile.delete();
				}
				out.close();
			} catch (IOException e) {
                e.printStackTrace();
			}
		}
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(lc);
			lc.reset();
			configurator.doConfigure("config/slf4j-logback.xml");
		} catch (JoranException je) {
			throw new RuntimeException("Failed to configure loggers, shutting down...", je);
		}
	}

	/**
	 * Launching method for GameServer
	 * 
	 * @param args
	 *            arguments, not used
	 */
	public static void main(String[] args)
	{
		long start = System.currentTimeMillis();

		initalizeLoggger();
		initUtilityServicesAndConfig();
		
		Util.printSection("World");
		DataManager.getInstance();
		IDFactory.getInstance();
		World.getInstance();

		GameServer gs = new GameServer();
		// Set all players is offline
		DAOManager.getDAO(PlayerDAO.class).setPlayersOffline(false);

		Util.printSection("Spawns");
		SpawnEngine.getInstance();

		Util.printSection("Quests");
		QuestEngine.getInstance();
		QuestEngine.getInstance().load();

		Util.printSection("TaskManagers");
		PacketBroadcaster.getInstance();
	
		GameTimeService.getInstance();

		AnnouncementService.getInstance();

		DebugService.getInstance();

		ZoneService.getInstance();
		
		WeatherService.getInstance();

		DuelService.getInstance();

		MailService.getInstance();
		
		SystemMailService.getInstance();

		GroupService.getInstance();
	
		SiegeService.getInstance();
	
		AllianceService.getInstance();
		
		BrokerService.getInstance();

		SiegeService.getInstance();
		
		Influence.getInstance();
		
		DropService.getInstance();

		ExchangeService.getInstance();

		PeriodicSaveService.getInstance(); 		
		
		ToyPetService.getInstance();
		
		PetitionService.getInstance();

		ChatHandlers.getInstance();
		TaskManagerFromDB.getInstance();
		Util.printSection("System");
		AEVersions.printFullVersionInfo();
		System.gc();
		AEInfos.printAllInfos();

		Util.printSection("GameServerLog");
		log.info("AL Game Server started in " + (System.currentTimeMillis() - start) / 1000 + " seconds.");
		log.info("============================================");
		log.info("===========>Aion Lightning Core<============");
		log.info("============================================");
		log.info("Thanks to all who helped this project!");
		log.info("Core Version: 2.0");
		log.info("Client Version: 2.0");
		log.info("Copyright 2010-2021");
		log.info("============================================");

		gs.startServers();
		GameTimeManager.startClock();

		if(TaskManagerConfig.DEADLOCK_DETECTOR_ENABLED)
		{
			log.info("Starting deadlock detector");
			new Thread(new DeadlockDetector(TaskManagerConfig.DEADLOCK_DETECTOR_INTERVAL)).start();
		}

		Runtime.getRuntime().addShutdownHook(ShutdownHook.getInstance());

		// gs.injector.getInstance(com.aionemu.gameserver.utils.chathandlers.ChatHandlers.class);
		onStartup();
	}

	/**
	 * Starts servers for connection with aion client and login server.
	 */
	private void startServers()
	{
		Util.printSection("Starting Network");
		ServerCfg aion = new ServerCfg(NetworkConfig.GAME_BIND_ADDRESS, NetworkConfig.GAME_PORT, "Game Connections", new GameConnectionFactoryImpl());
		//ServerCfg login = new ServerCfg(NetworkConfig.LOGIN_ADDRESS.getHostName(), NetworkConfig.LOGIN_ADDRESS.getPort(), "Login Connections", new LoginConnectionFactoryImpl());
		NioServer nioServer = new NioServer(1, ThreadPoolManager.getInstance(), aion);

		LoginServer loginServer = LoginServer.getInstance();
		ChatServer chatServer = ChatServer.getInstance();
		loginServer.setNioServer(nioServer);
		chatServer.setNioServer(nioServer);
		
		// Nio must go first
		nioServer.connect();
		loginServer.connect();
		
		if(!GSConfig.DISABLE_CHAT_SERVER)
			chatServer.connect();
	}

	/**
	 * Initialize all helper services, that are not directly related to aion gs, which includes:
	 * <ul>
	 * <li>Logging</li>
	 * <li>Database factory</li>
	 * <li>Thread pool</li>
	 * </ul>
	 * 
	 * This method also initializes {@link Config}
	 * 
	 * @throws Log4jInitializationError
	 */
	private static void initUtilityServicesAndConfig() throws Log4jInitializationError
	{
		// Set default uncaught exception handler
		Thread.setDefaultUncaughtExceptionHandler(new ThreadUncaughtExceptionHandler());
		// init config
		Util.printSection("Config");
		Config.load();
		// Second should be database factory
		Util.printSection("DataBase");
		DatabaseFactory.init();
		// Initialize DAOs
		DAOManager.init();
		// Initialize thread pools
		Util.printSection("Threads");
		ThreadConfig.load();
		ThreadPoolManager.getInstance();
	}

	private static Set<StartupHook>	startUpHooks	= new HashSet<StartupHook>();

	public synchronized static void addStartupHook(StartupHook hook)
	{
		if(startUpHooks != null)
			startUpHooks.add(hook);
		else
			hook.onStartup();
	}

	private synchronized static void onStartup()
	{
		final Set<StartupHook> startupHooks = startUpHooks;

		startUpHooks = null;

		for(StartupHook hook : startupHooks)
			hook.onStartup();
	}

	public interface StartupHook
	{
		public void onStartup();
	}
}
