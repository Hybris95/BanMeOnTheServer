package com.hybris.bukkit.banMeOnTheServer;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import org.bukkit.plugin.PluginManager;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent;

public class BanMeOnTheServer extends JavaPlugin implements Listener
{
    // Fichier où les joueurs bannis seront enregistrés
	private static final String DIR = "plugins"+File.separator+"BMOTS";
	private static final String DATFILE = "BannedPlayers.dat";
	private static final String BAKFILE = "BannedPlayers.dat.bak";
	private static final String DATPATH = DIR+File.separator+DATFILE;
	private static final String BAKPATH = DIR+File.separator+BAKFILE;
	
	// Liste chargée depuis le fichier BannedPlayers.dat
	private HashMap<String, Long> currentTime;
	
	// Liste remise à zero à chaque Activation
	private HashMap<String, Long> onlinePlayers;
	
	// Statut d'écoute du plugin
	private boolean listening;
	
	// Cet objet permet d'empêcher les joueurs bannis de se connecter lorsque le plugin est désactivé
	private DisallowJoining disallow;
	
	public void onLoad()
	{
		// Disallow Banned players from joining
		this.disallow = new DisallowJoining(this);
		
		// Chargement ne veux pas dire Activation
		this.listening = false;
		
		// Créer la liste des bannis
		this.currentTime = null;
		
		// Charger la liste des bannis depuis le fichier BannedPlayers.dat
		try{
			FileInputStream fis = new FileInputStream(DATPATH);
			ObjectInputStream ois = new ObjectInputStream(fis);
			try{
				Object myHashMap = ois.readObject();
				this.currentTime = (HashMap<String, Long>)myHashMap;// TODO Check du cast
			}
			catch(IOException exc){}// Fin du fichier (ne devrait arriver que si le fichier n'a jamais été rempli)
			catch(ClassNotFoundException exc){}// Ne devrait pas arriver si le fichier n'a pas été modifié manuellement
		}
		catch(IOException e)
		{
			new File(DIR).mkdir();
			File createFile = new File(DATPATH);
			try{
				createFile.createNewFile();
			}
			catch(IOException exc){}// Ne devrait pas arriver étant donné que le fichier n'existe pas
		}
		
		// Créer une HashMap si le fichier n'a rien donné
		if(this.currentTime == null)
		{
			this.currentTime = new HashMap<String, Long>();
		}
		
		// Créer la liste des joueurs bannis connectés
		this.onlinePlayers = new HashMap<String, Long>(this.currentTime.size());
	}
	
	public void onEnable()
	{
		// Enregistrer les evenements
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvents(this, this);
		pm.registerEvents(this.disallow, this);
		
		// Lancer le processus d'écoute des commandes des bannis
		// Lancer le processus d'écoute des logins/déconnexion
		// Lancer le processus d'écoute des commandes des administrateurs
		this.listening = true;
		
		// Autoriser les joueurs bannis à se reconnecter
		this.disallow.stop();
	}
	
	private class DisallowJoining implements Listener
	{
		private boolean running = false;
		private BanMeOnTheServer plugin;
		
		public DisallowJoining(BanMeOnTheServer plugin)
		{
			this.plugin = plugin;
		}
		
		@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
		public void disallowJoiningBannedPlayers(PlayerLoginEvent event)
		{
			if(this.running)
			{
				if(this.plugin.isBanned(event.getPlayer().getName()))
				{
					event.getPlayer().kickPlayer("[BanMeOnTheServer] Is Disabled!");
				}
			}
		}
		
		void start()
		{
			this.running = true;
		}
		
		void stop()
		{
			this.running = false;
		}
	}
	
	public void onDisable()
	{
		// Empecher les joueurs bannis de se reconnecter
		this.disallow.start();
		
		// Kicker tous les joueurs bannis connectés
		Server server = this.getServer();
		Object[] onlinePlayers = this.onlinePlayers.keySet().toArray();
		for(int i = 0; i < onlinePlayers.length; i++)
		{
			String onlinePlayerName = (String)onlinePlayers[i];
			try{
				server.getPlayer(onlinePlayerName).kickPlayer("[BanMeOnTheServer] Is Disabled!");
			}
			catch(NullPointerException e){continue;}
		}
		
		// Stopper le processus d'écoute des commandes des administrateurs
		// Stopper le processus d'écoute des logins/déconnexion
		// Stopper le processus d'écoute des commandes des bannis
		this.listening = false;
		
		// Backup du fichier BannedPlayers.dat
		File bakFile = new File(BAKPATH);
		try{
			bakFile.createNewFile();
			// TODO Backup du fichier BannedPlayers.dat
		}
		catch(IOException e){}
		
		// Detruire le fichier BannedPlayers.dat
		File datFile = new File(DATPATH);
		datFile.delete();
		try{
			datFile.createNewFile();
		}
		catch(IOException e){}// Ne devrait pas arriver puisqu'on vient de détruire le fichier
		
		// Réécrire le fichier BannedPlayers.dat
		try{
			FileOutputStream fos = new FileOutputStream(datFile);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this.currentTime);
		}
		catch(IOException e)
		{
			// Ne devrait pas arriver puisqu'on viens de créer le fichier
		}
		
		// Destruction du fichier BannedPlayers.bak
		bakFile.delete();
	}
	
	private void newBan(String name, long duration)
	{
		if(!this.isEnabled())
		{
			return;
		}
		
		// Ajouter le joueur ou changer la durée de ban actuelle
		this.currentTime.put(name, duration);
		
		// S'il est en ligne l'ajouter également à la liste des joueurs bannis connectés
		this.onlinePlayers.put(name, System.currentTimeMillis());
	}
	
	private void removeBan(String name)
	{
		if(!this.listening)
		{
			return;
		}
		
		// Mettre à jour les listes
		this.currentTime.remove(name);
		this.onlinePlayers.remove(name);
	}
	
	private void newOnline(String name)
	{
		if(!this.listening)
		{
			return;
		}
		
		// Ajouter à la liste en ligne
		this.onlinePlayers.put(name, System.currentTimeMillis());
	}
	
	private void removeOnline(String name)
	{
		if(!this.listening)
		{
			return;
		}
		
		// Prendre le temps actuel
		long now = System.currentTimeMillis();
		// Comparer avec la liste des sentences, du temps actuel, et du temps restant
		long whenHeConnected = this.onlinePlayers.get(name);
		long howMuchHeHad = this.currentTime.get(name);
		long howMuchHeHas = howMuchHeHad - (now - whenHeConnected);
		this.onlinePlayers.remove(name);
		if(howMuchHeHas < 0){// Deban (ne devrait jamais arriver car arrive après isBanned(name) sauf erreur de synchro)
			this.currentTime.remove(name);
			return;
		}
		this.currentTime.put(name, howMuchHeHas);
	}
	
	boolean isBanned(String name)
	{
		if(this.currentTime.containsKey(name))
		{
			if(this.onlinePlayers.containsKey(name))
			{
				// Vérifier avec le temps actuel
				long now = System.currentTimeMillis();
				long whenHeConnected = this.onlinePlayers.get(name);
				long howMuchHeHad = this.currentTime.get(name);
				long howMuchHeHas = howMuchHeHad - (now - whenHeConnected);
				if(howMuchHeHas < 0)// Déban s'il n'est plus ban et renvoyer false
				{
					this.currentTime.remove(name);
					this.onlinePlayers.remove(name);
					return false;
				}
				else{
					return true;
				}
			}
			else
			{
				return true;
			}
		}
		else
		{
			return false;
		}
	}
	
	private long timeLeft(String name)
	{
		long timeLeft = 0;
		long now = System.currentTimeMillis();
		long whenHeConnected;
		if(this.onlinePlayers.containsKey(name))
		{
			whenHeConnected = this.onlinePlayers.get(name);
		}
		else
		{
			whenHeConnected = now;
		}
		long howMuchHeHad = this.currentTime.get(name);
		timeLeft = howMuchHeHad - (now - whenHeConnected);
		return timeLeft;
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void parseEveryCommand(PlayerCommandPreprocessEvent event)
	{
		// Ne parser que si l'on est lancé
		if(!this.listening)
		{
			return;
		}
		
		// Récupérer le nom du joueur lançant la commande
		String name = event.getPlayer().getName();
		
		// Si le joueur est banni
		if(this.isBanned(name))
		{
			// L'empêcher de lancer la commande
			event.setCancelled(true);
			
			// Lui donner son temps restant
			event.getPlayer().sendMessage("[BanMeOnTheServer] Time Left : " + this.timeLeft(name));
			return;
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void parseStdCmds(PlayerCommandPreprocessEvent event)
	{
		// Ne parser que si l'on est lancé
		if(!this.listening)
		{
			return;
		}
		
		// Ne parser que les commandes de "BanMeOnTheServer"
		String command = event.getMessage();
		if(!command.toLowerCase().startsWith("/bmots "))
		{
			return;
		}
		
		// Recuperation du joueur ayant lancé la commande bmots
		Player sender = event.getPlayer();
		
		// Vérifier si le joueur a les droits d'utiliser cette commande admin (Operateur)
		if(!sender.isOp())
		{
			event.setCancelled(true);
			return;
		}
		
		// Parser la commande
		String[] splittedCmd = command.split(" ", 3);
		String subCommand = splittedCmd[1];
		
		// Ajouter ou retirer le ban
		String bannedPlayerName = null;
		if(subCommand.equalsIgnoreCase("add"))
		{
			// Récupérer le nom du joueur à ban ainsi que la durée de la peine
			if(splittedCmd.length != 3)
			{
				event.setCancelled(true);
				return;
			}
			String[] nameAndDuration = splittedCmd[2].split(" ", 2);
			if(nameAndDuration.length != 2)
			{
				event.setCancelled(true);
				return;
			}
			try{
				long duration = Integer.parseInt(nameAndDuration[1]) * 1000;
				bannedPlayerName = nameAndDuration[0];
				this.newBan(bannedPlayerName, duration);
			}
			catch(NumberFormatException e)
			{
				event.setCancelled(true);
				return;
			}
		}
		else if(subCommand.equalsIgnoreCase("remove"))
		{
			// Récuperer le nom du joueur à deban
			if(splittedCmd.length != 3)
			{
				event.setCancelled(true);
				return;
			}
			bannedPlayerName = splittedCmd[2];
			this.removeBan(bannedPlayerName);
		}
		else if(subCommand.equalsIgnoreCase("list"))
		{
			// Lister au joueur appelant les joueurs bannis, leur durée et leur état de connexion
			sender.sendMessage("[BanMeOnTheServer] Liste des bannis :");
			Object[] bannedPlayers = this.currentTime.entrySet().toArray();
			for(int i = 0; i < bannedPlayers.length; i++)
			{
				sender.sendMessage(bannedPlayers[i].toString() + " reste " + this.timeLeft(bannedPlayers[i].toString()) + "ms");
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void getLogins(PlayerLoginEvent event)
	{
		// Informer le plugin lorsqu'un joueur banni se connecte
		Player connectedPlayer = event.getPlayer();
		String connectedPlayerName = connectedPlayer.getName();
		if(this.isBanned(connectedPlayerName))
		{
			this.newOnline(connectedPlayerName);
			connectedPlayer.sendMessage("[BanMeOnTheServer] Time Left : " + this.timeLeft(connectedPlayerName));
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void getLogouts(PlayerQuitEvent event)
	{
		// Informer le plugin lorsqu'un joueur banni se déconnecte
		Player disconnectedPlayer = event.getPlayer();
		String disconnectedPlayerName = disconnectedPlayer.getName();
		if(this.isBanned(disconnectedPlayerName))
		{
			this.removeOnline(disconnectedPlayerName);
			disconnectedPlayer.sendMessage("[BanMeOnTheServer] Time Left : " + this.timeLeft(disconnectedPlayerName));
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void getBannedDmgs(EntityDamageEvent event)
	{
		if(!this.listening)
		{
			return;
		}
		
		Entity involved = event.getEntity();
		if(!(involved instanceof Player))
		{
			return;
		}
		Player involvedPlayer = (Player)involved;
		if(!this.isBanned(involvedPlayer.getName()))
		{
			return;
		}
		event.setDamage(0);
		event.setCancelled(true);
	}
}
