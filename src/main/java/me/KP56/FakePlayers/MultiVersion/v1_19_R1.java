package me.KP56.FakePlayers.MultiVersion;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import me.KP56.FakePlayers.FakePlayer;
import me.KP56.FakePlayers.Main;
import me.KP56.FakePlayers.Spigot.SpigotUtils;
import net.minecraft.EnumChatFormat;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.EnumProtocolDirection;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutNamedEntitySpawn;
import net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.PlayerInteractManager;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserCache;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EnumGamemode;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class v1_19_R1 {
    public static EntityPlayer spawn(FakePlayer fakePlayer) {

        WorldServer worldServer = ((CraftWorld) Bukkit.getWorlds().get(0)).getHandle();

        MinecraftServer mcServer = ((CraftServer) Bukkit.getServer()).getServer();

        EntityPlayer entityPlayer = createEntityPlayer(fakePlayer.getUUID(), fakePlayer.getName(), worldServer);

        CraftPlayer bukkitPlayer = entityPlayer.getBukkitEntity();

        // was entityPlayer.playerConnection
        entityPlayer.b = new PlayerConnection(mcServer, new NetworkManager(EnumProtocolDirection.a), entityPlayer); // should be EnumProtocolDirection.CLIENTBOUND

        //entityPlayer.playerConnection.networkManager.channel
        entityPlayer.b.b.m = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        entityPlayer.b.b.m.close();

        try {
            PlayerPreLoginEvent preLoginEvent = new PlayerPreLoginEvent(fakePlayer.getName(), InetAddress.getByName("127.0.0.1"), fakePlayer.getUUID());
            AsyncPlayerPreLoginEvent asyncPreLoginEvent = new AsyncPlayerPreLoginEvent(fakePlayer.getName(), InetAddress.getByName("127.0.0.1"), fakePlayer.getUUID());

            new Thread(() -> Bukkit.getPluginManager().callEvent(asyncPreLoginEvent)).start();
            Bukkit.getPluginManager().callEvent(preLoginEvent);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        mcServer.ac().a(entityPlayer); // mcServer.getPlayerList().a(entityPlayer);

        Location loc = bukkitPlayer.getLocation();

        //entityPlayer.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        entityPlayer.forceSetPositionRotation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

        /*DataWatcher data = entityPlayer.getDataWatcher();
        data.set(DataWatcherRegistry.a.a(16), (byte) 127);*/

        String joinMessage = getJoinMessage(entityPlayer);

        /*if (Main.getPlugin().usesPaper() && Main.getPlugin().isPaperUpdated()) {
            PaperUtils_v1_16_R3.playerInitialSpawnEvent(bukkitPlayer);
        }*/

        entityPlayer.spawnIn(worldServer);
        //entityPlayer.playerInteractManager.a((WorldServer) entityPlayer.world);
        entityPlayer.d.a((WorldServer) entityPlayer.s);
        GameMode gamemode = Bukkit.getServer().getDefaultGameMode();
        if (gamemode == GameMode.SURVIVAL) {
            entityPlayer.d.a(EnumGamemode.a);
        } else if (gamemode == GameMode.CREATIVE) {
            entityPlayer.d.a(EnumGamemode.b);
        } else if (gamemode == GameMode.ADVENTURE) {
            entityPlayer.d.a(EnumGamemode.c);
        } else if (gamemode == GameMode.SPECTATOR) {
            entityPlayer.d.a(EnumGamemode.d);
        }

        worldServer.a(entityPlayer); //worldServer.addPlayerJoin(entityPlayer);
        mcServer.ac().k.add(entityPlayer); //mcServer.getPlayerList().players.add(entityPlayer);
        try {
            Field j = PlayerList.class.getDeclaredField("j");
            j.setAccessible(true);
            Object valJ = j.get(mcServer.ac()); //Object valJ = j.get(mcServer.getPlayerList());

            Method jPut = valJ.getClass().getDeclaredMethod("put", Object.class, Object.class);
            jPut.invoke(valJ, bukkitPlayer.getUniqueId(), entityPlayer);

            if (!Main.getPlugin().usesCraftBukkit()) {
                Field playersByName = PlayerList.class.getDeclaredField("playersByName");
                playersByName.setAccessible(true);
                Object valPlayersByName = playersByName.get(mcServer.ac()); //Object valPlayersByName = playersByName.get(mcServer.getPlayerList());

                Method playersByNamePut = valPlayersByName.getClass().getDeclaredMethod("put", Object.class, Object.class);
                playersByNamePut.invoke(valPlayersByName, entityPlayer.displayName.toLowerCase(Locale.ROOT), entityPlayer);
            }
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }

        PlayerJoinEvent playerJoinEvent;
        /*if (Main.getPlugin().usesPaper() && Main.getPlugin().isPaperUpdated()) {
            playerJoinEvent = PaperUtils_v1_16_R3.paperJoinMessageFormat(entityPlayer, joinMessage);
        } else {
            playerJoinEvent = new PlayerJoinEvent(((CraftServer) Bukkit.getServer()).getPlayer(entityPlayer), CraftChatMessage.fromComponent(joinMessage));
        }*/
        playerJoinEvent = new PlayerJoinEvent((Bukkit.getServer()).getPlayer(entityPlayer.x().uuid), CraftChatMessage.fromComponent(IChatBaseComponent.a(joinMessage)));

        Bukkit.getPluginManager().callEvent(playerJoinEvent);

        try {
            Field didPlayerJoinEvent = entityPlayer.getClass().getDeclaredField("didPlayerJoinEvent");
            didPlayerJoinEvent.set(entityPlayer, true);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {

        }

        String finalJoinMessage = playerJoinEvent.getJoinMessage();

        if (finalJoinMessage != null && !finalJoinMessage.equals("")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(finalJoinMessage);
            }
        }

        /*PlayerResourcePackStatusEvent resourcePackStatusEventAccepted = new PlayerResourcePackStatusEvent(bukkitPlayer, PlayerResourcePackStatusEvent.Status.ACCEPTED);
        PlayerResourcePackStatusEvent resourcePackStatusEventSuccessfullyLoaded = new PlayerResourcePackStatusEvent(bukkitPlayer, PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED);

        Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), () -> {
            if (Main.getPlugin().usesPaper()) {
                SpigotUtils.setResourcePackStatus(bukkitPlayer, PlayerResourcePackStatusEvent.Status.ACCEPTED);
            }
            Bukkit.getPluginManager().callEvent(resourcePackStatusEventAccepted);
        }, 20);
        Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), () -> {
            if (Main.getPlugin().usesPaper()) {
                SpigotUtils.setResourcePackStatus(bukkitPlayer, PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED);
            }
            Bukkit.getPluginManager().callEvent(resourcePackStatusEventSuccessfullyLoaded);
        }, 40);*/

        for (Player player : Bukkit.getOnlinePlayers()) {
            //PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
            PlayerConnection connection = ((CraftPlayer) player).getHandle().b;
            //connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entityPlayer));
            connection.a(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.a, entityPlayer));
            connection.a(new PacketPlayOutNamedEntitySpawn(entityPlayer));
        }

        //Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getPlugin(), entityPlayer::playerTick, 1, 1);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getPlugin(), entityPlayer::k, 1, 1);

        return entityPlayer;
    }

    private static EntityPlayer createEntityPlayer(UUID uuid, String name, WorldServer worldServer) {
        MinecraftServer mcServer = ((CraftServer) Bukkit.getServer()).getServer();
        GameProfile gameProfile = new GameProfile(uuid, name);

        //return new EntityPlayer(mcServer, worldServer, gameProfile, new PlayerInteractManager(worldServer));
        return new EntityPlayer(mcServer, worldServer, gameProfile, null);
    }

    public static void removePlayer(FakePlayer player) {
        MinecraftServer mcServer = ((CraftServer) Bukkit.getServer()).getServer();
        CraftServer craftServer = (CraftServer) Bukkit.getServer();

        EntityPlayer entityPlayer = (EntityPlayer) player.getEntityPlayer();

        WorldServer worldServer = entityPlayer.x(); //entityPlayer.getWorld().getWorld().getHandle();

        entityPlayer.a(StatisticList.j); //entityPlayer.a(StatisticList.LEAVE_GAME);

        /*if (entityPlayer.activeContainer != entityPlayer.defaultContainer) {
            entityPlayer.closeInventory();
        }*/

        PlayerQuitEvent playerQuitEvent;
        /*if (Main.getPlugin().usesPaper() && Main.getPlugin().isPaperUpdated()) {
            playerQuitEvent = PaperUtils_v1_16_R3.paperQuitMessageFormat(entityPlayer, craftServer.getPlayer(entityPlayer.displayName));
        } else {
            playerQuitEvent = new PlayerQuitEvent(craftServer.getPlayer(entityPlayer), "§e" + entityPlayer.getName() + " left the game");
        }*/
        playerQuitEvent = new PlayerQuitEvent(craftServer.getPlayer(entityPlayer.x().uuid), "§e" + entityPlayer.displayName + " left the game");


        Bukkit.getPluginManager().callEvent(playerQuitEvent);

        entityPlayer.getBukkitEntity().disconnect(playerQuitEvent.getQuitMessage());

        /*if (mcServer.isMainThread()) {
            entityPlayer.playerTick();
        }*/

        //if (!entityPlayer.inventory.getCarried().isEmpty()) {
        if (!entityPlayer.fA().getContents().isEmpty()) {
            List<ItemStack> carried = entityPlayer.fA().getContents(); //entityPlayer.inventory.getCarried();
            for (ItemStack stack : carried) {
                entityPlayer.drop(stack, false, false, false);
            }
        }

        //entityPlayer.decouple();
        worldServer.a(entityPlayer, Entity.RemovalReason.a); //worldServer.removePlayer(entityPlayer);
        entityPlayer.M().a(); //entityPlayer.getAdvancementData().a();
        mcServer.ac().k.remove(entityPlayer); //mcServer.getPlayerList().players.remove(entityPlayer);

        try {
            Field j = PlayerList.class.getDeclaredField("j");
            j.setAccessible(true);
            /*Object valJ = j.get(mcServer.ac()); //.getPlayerList());

            Method jRemove = valJ.getClass().getDeclaredMethod("remove", Object.class);
            jRemove.invoke(valJ, entityPlayer.getUniqueID());*/

            Field playersByName = PlayerList.class.getDeclaredField("playersByName");
            playersByName.setAccessible(true);
            Object valPlayersByName = playersByName.get(mcServer.ac()); //.getPlayerList());

            Method playersByNameRemove = valPlayersByName.getClass().getDeclaredMethod("remove", Object.class);
            playersByNameRemove.invoke(valPlayersByName, entityPlayer.displayName.toLowerCase(Locale.ROOT));
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }

        FakePlayer.getFakePlayers().remove(player);

        String finalQuitMessage = playerQuitEvent.getQuitMessage();

        if (finalQuitMessage != null && !finalQuitMessage.equals("")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                PlayerConnection connection = ((CraftPlayer) p).getHandle().b; //.playerConnection;
                /*connection.sendPacket(new PacketPlayOutEntityDestroy(entityPlayer.getId()));
                connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, entityPlayer));*/
                connection.a(new PacketPlayOutEntityDestroy(entityPlayer.x().uuid.hashCode()));
                connection.a(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.c, entityPlayer));

                p.sendMessage(playerQuitEvent.getQuitMessage());
            }
        }

        /*try {
            Method savePlayerFile = PlayerList.class.getDeclaredMethod("savePlayerFile", EntityPlayer.class);
            savePlayerFile.setAccessible(true);
            savePlayerFile.invoke(mcServer.ac(), entityPlayer); // mcServer.getPlayerList()
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }*/
    }

    private static String getJoinMessage(EntityPlayer entityPlayer) {
        //GameProfile gameProfile = entityPlayer.getProfile();
        GameProfile gameProfile = entityPlayer.fy();
        /*UserCache userCache = ((CraftServer) Bukkit.getServer()).getServer().ap(); //.getUserCache();
        Optional<GameProfile> gameprofile2 = userCache.a(entityPlayer.displayName); //userCache.getProfile(entityPlayer.getName());

        String s = gameprofile2.isEmpty() ? gameProfile.getName() : gameprofile2.();*/
        String s = gameProfile.getName();

        String joinMessage;
        if (entityPlayer.fy().getName().equalsIgnoreCase(s)) {
            joinMessage = "§e" + entityPlayer.displayName + " joined the game"; //LocaleI18n.a("multiplayer.player.joined", entityPlayer.getName());
        } else {
            joinMessage = "§e" + entityPlayer.displayName + " joined the game (previously " + s + ")"; //LocaleI18n.a("multiplayer.player.joined.renamed", entityPlayer.getName(), s);
        }

        return joinMessage;
    }
}
