package seml.randomStartTP

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import org.bukkit.Location
import org.bukkit.event.Listener
import kotlin.random.Random

class RandomStartTP : JavaPlugin(), Listener {

    private val joinedPlayers = mutableSetOf<UUID>()
    private val dataFile = dataFolder.resolve("joined.dat")

    override fun onEnable() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()  // 데이터 폴더 생성 보장
        }
        saveDefaultConfig()
        Bukkit.getPluginManager().registerEvents(this, this)
        loadJoinedPlayers()
    }


    override fun onDisable() {
        saveJoinedPlayers()
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (joinedPlayers.contains(player.uniqueId)) return  // 이미 접속한 플레이어 무시

        // 처음 접속: 랜덤 위치 생성
        val world = player.world
        val minX = config.getInt("range.minX", -1000)
        val maxX = config.getInt("range.maxX", 1000)
        val minZ = config.getInt("range.minZ", -1000)
        val maxZ = config.getInt("range.maxZ", 1000)

        var attempts = 0
        while (attempts < 50) {  // 최대 50회 시도
            val x = Random.nextInt(minX, maxX + 1)
            val z = Random.nextInt(minZ, maxZ + 1)
            val y = world.getHighestBlockYAt(x, z) + 1  // 땅 위로

            val location = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
            if (isSafeLocation(location)) {
                player.teleport(location)
                joinedPlayers.add(player.uniqueId)
                return
            }
            attempts++
        }
        player.sendMessage("안전한 위치를 찾지 못했습니다. 수동으로 이동하세요.")
    }

    private fun isSafeLocation(loc: Location): Boolean {
        val below = loc.clone().subtract(0.0, 1.0, 0.0).block.type
        val feet = loc.block.type
        val head = loc.clone().add(0.0, 1.0, 0.0).block.type

        return below.isSolid && feet == Material.AIR && head == Material.AIR &&
                below != Material.LAVA && below != Material.WATER
    }

    private fun loadJoinedPlayers() {
        if (dataFile.exists()) {
            dataFile.readLines().forEach { joinedPlayers.add(UUID.fromString(it)) }
        }
    }

    private fun saveJoinedPlayers() {
        if (!dataFile.parentFile.exists()) {
            dataFile.parentFile.mkdirs()
        }
        dataFile.writeText(joinedPlayers.joinToString("\n") { it.toString() })
    }

}
