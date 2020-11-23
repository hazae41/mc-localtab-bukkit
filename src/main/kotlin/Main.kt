package hazae41.localtab

import com.comphenix.protocol.PacketType.Play
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority.NORMAL
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction
import com.comphenix.protocol.wrappers.PlayerInfoData
import com.comphenix.protocol.wrappers.WrappedChatComponent
import com.comphenix.protocol.wrappers.WrappedGameProfile
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class Main : JavaPlugin(), Listener {
  var maxDistance = 1000

  val protocol get() = ProtocolLibrary.getProtocolManager()

  val mypackets = mutableSetOf<Any>()

  val profiles = mutableMapOf<UUID, WrappedGameProfile>()

  override fun onEnable() {
    super.onEnable()

    saveDefaultConfig()

    maxDistance = config.getInt("distance")

    server.pluginManager.registerEvents(this, this)

    server.scheduler.runTaskTimer(this, { _ -> tick() }, 20L, 20L)

    protocol.addPacketListener(adapter)
  }

  fun Player.distance(other: Player) =
    location.distance(other.location)

  val adapter = object : PacketAdapter(this, NORMAL, Play.Server.PLAYER_INFO) {
    override fun onPacketSending(e: PacketEvent) {
      val packet = e.packet

      if (mypackets.contains(packet.handle)) {
        mypackets.remove(packet.handle)
      } else {
        e.isCancelled = true

        val action = packet.playerInfoAction.read(0)
        val infos = packet.playerInfoDataLists.read(0)

        if (action == PlayerInfoAction.ADD_PLAYER) {
          for (info in infos)
            profiles[info.profile.uuid] = info.profile
        }

        if (action == PlayerInfoAction.REMOVE_PLAYER) {
          for (info in infos)
            profiles.remove(info.profile.uuid)
        }
      }
    }
  }

  val lists = mutableMapOf<Player, MutableSet<Player>>()

  @EventHandler(priority = EventPriority.MONITOR)
  fun onjoin(e: PlayerJoinEvent) {
    lists[e.player] = mutableSetOf()

    server.scheduler.runTask(this) { _ ->
      e.player.world.players.forEach { it.tick() }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  fun onquit(e: PlayerQuitEvent) {
    lists.remove(e.player)

    server.scheduler.runTask(this) { _ ->
      e.player.world.players.forEach { it.tick() }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  fun onteleport(e: PlayerTeleportEvent) {
    server.scheduler.runTask(this) { _ ->
      e.player.world.players.forEach { it.tick() }
    }
  }

  fun tick() = server.onlinePlayers.forEach { it.tick() }

  fun Player.tick() {
    val old = lists[this]!!

    val distances = mutableMapOf<Player, Double>()

    val removed = HashSet(old)
    val added = HashSet<Player>()
    val updated = HashSet<Player>()

    for (target in world.players) {
      val distance = distance(target)
      if (distance > maxDistance) continue
      distances[target] = distance

      if (target in old) {
        removed.remove(target)
        updated.add(target)
      } else {
        added.add(target)
      }
    }

    sendAdded(added, distances)
    sendUpdated(updated, distances)
    sendRemoved(removed)

    lists[this] = distances.keys

    if (added.isEmpty()) return

    server.scheduler.runTask(this@Main) { _ ->
      for (target in added) {
        hidePlayer(this@Main, target)
        showPlayer(this@Main, target)
      }
    }
  }

  fun Player.toInfoData(latency: Int): PlayerInfoData? {
    val profile = profiles[uniqueId] ?: return null
    val gameMode = NativeGameMode.fromBukkit(gameMode)
    val displayName = WrappedChatComponent.fromText(displayName)
    return PlayerInfoData(profile, latency, gameMode, displayName)
  }

  fun Player.sendAdded(players: Set<Player>, distances: Map<Player, Double>) {
    val list = players.mapNotNull {
      val coeff = 2000 / maxDistance
      val latency = distances[it]!! * coeff
      it.toInfoData(latency.toInt())
    }

    val packet = PacketContainer(Play.Server.PLAYER_INFO)
    packet.playerInfoAction.write(0, PlayerInfoAction.ADD_PLAYER)
    packet.playerInfoDataLists.write(0, list)

    mypackets.add(packet.handle)
    protocol.sendServerPacket(this, packet)
  }

  fun Player.sendUpdated(players: Set<Player>, distances: Map<Player, Double>) {
    val list = players.mapNotNull {
      val coeff = 2000 / maxDistance
      val latency = distances[it]!! * coeff
      it.toInfoData(latency.toInt())
    }

    val packet = PacketContainer(Play.Server.PLAYER_INFO)
    packet.playerInfoAction.write(0, PlayerInfoAction.UPDATE_LATENCY)
    packet.playerInfoDataLists.write(0, list)

    mypackets.add(packet.handle)
    protocol.sendServerPacket(this, packet)
  }

  fun Player.sendRemoved(players: Set<Player>) {
    val list = players.mapNotNull { it.toInfoData(0) }

    val packet = PacketContainer(Play.Server.PLAYER_INFO)
    packet.playerInfoAction.write(0, PlayerInfoAction.REMOVE_PLAYER)
    packet.playerInfoDataLists.write(0, list)

    mypackets.add(packet.handle)
    protocol.sendServerPacket(this, packet)
  }
}