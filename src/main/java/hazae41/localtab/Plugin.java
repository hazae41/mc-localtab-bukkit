package hazae41.localtab;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.comphenix.protocol.PacketType.Play;
import static com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;

public class Plugin extends JavaPlugin implements Listener {
  Set<Object> mypackets = new HashSet<>();
  Map<UUID, WrappedGameProfile> profiles = new HashMap<>();
  Map<Player, Set<OfflinePlayer>> lists = new HashMap<>();

  boolean headerEnabled;
  int maxDistance;

  @Override
  public void onEnable() {
    super.onEnable();

    saveDefaultConfig();

    headerEnabled = getConfig().getBoolean("header");
    maxDistance = getConfig().getInt("distance");

    getServer().getPluginManager().registerEvents(this, this);

    getServer().getScheduler().runTaskTimer(this, task -> {
      getServer().getOnlinePlayers().forEach(this::tickPlayer);
    }, 20L, 20L);

    getProtocol().addPacketListener(new PlayerInfoAdapter());
  }

  private void registerLocaleChangeEvent(Player player, Consumer<PlayerLocaleChangeEvent> listener) {
    getServer().getPluginManager().registerEvents(new Listener() {
      @EventHandler(priority = EventPriority.MONITOR)
      public void onLocaleChange(PlayerLocaleChangeEvent e) {
        if (e.getPlayer() != player) return;
        HandlerList.unregisterAll(this);
        listener.accept(e);
      }
    }, this);
  }

  private ProtocolManager getProtocol() {
    return ProtocolLibrary.getProtocolManager();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(PlayerJoinEvent e) {
    lists.put(e.getPlayer(), new HashSet<>());
    tickWorld(e.getPlayer().getWorld());

    if (!headerEnabled) return;

    registerLocaleChangeEvent(e.getPlayer(), localeEvent -> {
      Lang lang = Langs.getLang(localeEvent.getLocale());
      e.getPlayer().setPlayerListHeader(lang.getHeader());
    });
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(PlayerQuitEvent e) {
    lists.remove(e.getPlayer());
    tickWorld(e.getPlayer().getWorld());

    getServer().getScheduler().runTask(this, task -> {
      profiles.remove(e.getPlayer().getUniqueId());
    });
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onTeleport(PlayerTeleportEvent e) {
    tickWorld(e.getPlayer().getWorld());
  }

  private void tickWorld(World world) {
    getServer().getScheduler().runTask(this, task -> {
      world.getPlayers().forEach(this::tickPlayer);
    });
  }

  private void tickPlayer(Player player) {
    Set<OfflinePlayer> old = lists.get(player);
    Set<OfflinePlayer> removed = new HashSet<>(old);

    Map<Player, Integer> added = new HashMap<>();
    Map<Player, Integer> updated = new HashMap<>();

    for (Player target : player.getWorld().getPlayers()) {
      if (!profiles.containsKey(target.getUniqueId())) continue;

      Location pl = player.getLocation();
      Location tl = target.getLocation();

      int distance = (int) Math.round(pl.distance(tl));
      if (distance > maxDistance) continue;

      if (removed.contains(target)) {
        removed.remove(target);
        updated.put(target, distance);
      } else {
        added.put(target, distance);
      }
    }

    old.removeAll(removed);
    old.addAll(added.keySet());

    if (!added.isEmpty())
      sendAddedPlayers(player, onlineInfoDatas(added));
    if (!updated.isEmpty())
      sendUpdatedPlayers(player, onlineInfoDatas(updated));
    if (!removed.isEmpty())
      sendRemovedPlayers(player, offlineInfoDatas(removed));

    if (added.isEmpty()) return;

    getServer().getScheduler().runTask(this, task -> {
      for (Player target : added.keySet()) {
        player.hidePlayer(this, target);
        player.showPlayer(this, target);
      }
    });
  }

  private PlayerInfoData onlineInfoData(Player player, int latency) {
    WrappedGameProfile profile = profiles.get(player.getUniqueId());
    NativeGameMode gameMode = NativeGameMode.fromBukkit(player.getGameMode());
    WrappedChatComponent displayName = WrappedChatComponent.fromText(player.getDisplayName());
    return new PlayerInfoData(profile, latency, gameMode, displayName);
  }

  private PlayerInfoData offlineInfoData(OfflinePlayer player) {
    WrappedGameProfile profile = profiles.get(player.getUniqueId());
    WrappedChatComponent displayName = WrappedChatComponent.fromText("");
    return new PlayerInfoData(profile, 0, NativeGameMode.NOT_SET, displayName);
  }

  private List<PlayerInfoData> onlineInfoDatas(Map<Player, Integer> players) {
    return players.entrySet().stream().map((entry) -> {
      int latency = entry.getValue() * 2000 / maxDistance;
      return onlineInfoData(entry.getKey(), latency);
    }).collect(Collectors.toList());
  }

  private List<PlayerInfoData> offlineInfoDatas(Set<OfflinePlayer> players) {
    return players.stream()
            .map(this::offlineInfoData)
            .collect(Collectors.toList());
  }

  private void sendAddedPlayers(Player player, List<PlayerInfoData> players) {
    PacketContainer packet = new PacketContainer(Play.Server.PLAYER_INFO);
    packet.getPlayerInfoAction().write(0, PlayerInfoAction.ADD_PLAYER);
    packet.getPlayerInfoDataLists().write(0, players);

    try {
      mypackets.add(packet.getHandle());
      getProtocol().sendServerPacket(player, packet);
    } catch (InvocationTargetException ignored) {
    }
  }

  private void sendUpdatedPlayers(Player player, List<PlayerInfoData> players) {
    PacketContainer packet = new PacketContainer(Play.Server.PLAYER_INFO);
    packet.getPlayerInfoAction().write(0, PlayerInfoAction.UPDATE_LATENCY);
    packet.getPlayerInfoDataLists().write(0, players);

    try {
      mypackets.add(packet.getHandle());
      getProtocol().sendServerPacket(player, packet);
    } catch (InvocationTargetException ignored) {
    }
  }

  private void sendRemovedPlayers(Player player, List<PlayerInfoData> players) {
    PacketContainer packet = new PacketContainer(Play.Server.PLAYER_INFO);
    packet.getPlayerInfoAction().write(0, PlayerInfoAction.REMOVE_PLAYER);
    packet.getPlayerInfoDataLists().write(0, players);

    try {
      mypackets.add(packet.getHandle());
      getProtocol().sendServerPacket(player, packet);
    } catch (InvocationTargetException ignored) {
    }
  }

  private class PlayerInfoAdapter extends PacketAdapter {
    private PlayerInfoAdapter() {
      super(Plugin.this, ListenerPriority.NORMAL, Play.Server.PLAYER_INFO);
    }

    @Override
    public void onPacketSending(PacketEvent e) {
      PacketContainer packet = e.getPacket();

      if (mypackets.contains(packet.getHandle())) {
        mypackets.remove(packet.getHandle());
        return;
      }

      e.setCancelled(true);

      PlayerInfoAction action = packet.getPlayerInfoAction().read(0);
      List<PlayerInfoData> infos = packet.getPlayerInfoDataLists().read(0);

      if (action == PlayerInfoAction.ADD_PLAYER) {
        for (PlayerInfoData info : infos) {
          UUID uuid = info.getProfile().getUUID();
          if (profiles.containsKey(uuid)) continue;
          profiles.put(uuid, info.getProfile());
        }
      }
    }
  }
}
