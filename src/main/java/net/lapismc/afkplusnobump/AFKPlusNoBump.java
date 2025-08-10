package net.lapismc.afkplusnobump;

import net.lapismc.afkplus.AFKPlus;
import net.lapismc.afkplus.api.AFKStartEvent;
import net.lapismc.afkplus.api.AFKStopEvent;
import net.lapismc.afkplus.playerdata.AFKPlusPlayer;
import net.lapismc.afkplus.util.core.utils.LapisTaskHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class AFKPlusNoBump extends JavaPlugin implements Listener {

    List<LivingEntity> bumpRemovedEntities = new ArrayList<>();

    @Override
    public void onEnable() {
        //Start a reaping task that will handle collision settings
        LapisTaskHandler.LapisTask repeatingTask = AFKPlus.getInstance().tasks.runTaskTimer(this::runCollisionTask, 10, 10, false);
        AFKPlus.getInstance().tasks.addTask(repeatingTask);
        //Add a listener for AFK start and stop events
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info(getName() + " v." + getDescription().getVersion() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        //Set these entities as collidable again
        for (LivingEntity e : bumpRemovedEntities) {
            e.setCollidable(true);
        }
        getLogger().info(getName() + " has been disabled!");
    }

    @EventHandler
    public void playerStartAFK(AFKStartEvent e) {
        Player p = Bukkit.getPlayer(e.getPlayer().getUUID());
        if (p == null)
            return;
        p.setCollidable(false);
        bumpRemovedEntities.add(p);
    }

    @EventHandler
    public void playerStopAFK(AFKStopEvent e) {
        Player p = Bukkit.getPlayer(e.getPlayer().getUUID());
        if (p == null)
            return;
        p.setCollidable(true);
        bumpRemovedEntities.remove(p);
    }

    public void runCollisionTask() {
        List<AFKPlusPlayer> afkPlayers = new ArrayList<>();
        //Get the players that are currently AFK
        ((AFKPlus) AFKPlus.getInstance()).getPlayers().values().forEach(player -> {
            if (player.isAFK())
                afkPlayers.add(player);
        });
        //This set will store the living entities near AFK players
        HashSet<LivingEntity> entitiesToProcess = new HashSet<>();
        //Find entities near players
        for (AFKPlusPlayer afkPlayer : afkPlayers) {
            //Get the bukkit player
            Player p = Bukkit.getPlayer(afkPlayer.getUUID());
            //Continue if the player isn't online
            if (p == null)
                continue;
            //Find entities within 2 blocks
            List<Entity> nearbyEntities = p.getNearbyEntities(4, 4, 4);
            for (Entity e : nearbyEntities) {
                if (!(e instanceof LivingEntity))
                    continue;
                //It's a living entity within two blocks
                //Add it to our set
                entitiesToProcess.add((LivingEntity) e);
            }
        }
        //Find the new entities and set then to not collide
        for (LivingEntity e : entitiesToProcess) {
            //If this entity is already bump removed, ignore it
            if (bumpRemovedEntities.contains(e))
                continue;
            //This is a new entity, add it and set collide to false
            bumpRemovedEntities.add(e);
            e.setCollidable(false);
            //TODO: DEBUG
            getLogger().info(e.getName() + " has been set as non collidable");
        }
        //Find entities that are no longer near players and remove them from the list and let them collide
        List<LivingEntity> entitiesToRemove = new ArrayList<>();
        for (LivingEntity e : bumpRemovedEntities) {
            if (!entitiesToProcess.contains(e)) {
                //No longer near players, remove and set collide to true
                entitiesToRemove.add(e);
                e.setCollidable(true);
                //TODO: DEBUG
                getLogger().info(e.getName() + " has been set as collidable");
            }
        }
        //remove the entities we've stopped tracking from the list
        bumpRemovedEntities.removeIf(entitiesToRemove::contains);
    }

}
