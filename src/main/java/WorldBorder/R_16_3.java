package WorldBorder;

import net.minecraft.server.v1_16_R3.PacketPlayOutWorldBorder;
import net.minecraft.server.v1_16_R3.WorldBorder;
import net.minecraft.server.v1_16_R3.PacketPlayOutWorldBorder.EnumWorldBorderAction;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class R_16_3 {
    public static void show(Player player, World world, double x, double z, double size) {
        WorldBorder worldBorder = new WorldBorder();
        worldBorder.world = ((CraftWorld)world).getHandle();
        worldBorder.setCenter(x, z);
        worldBorder.setSize(size);
        PacketPlayOutWorldBorder packetPlayOutWorldBorder = new PacketPlayOutWorldBorder(worldBorder, EnumWorldBorderAction.INITIALIZE);
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetPlayOutWorldBorder);
    }

    public static void hide(Player player, World world) {
        WorldBorder worldBorder = new WorldBorder();
        worldBorder.world = ((CraftWorld)world).getHandle();
        PacketPlayOutWorldBorder packetPlayOutWorldBorder = new PacketPlayOutWorldBorder(worldBorder, EnumWorldBorderAction.LERP_SIZE);
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(packetPlayOutWorldBorder);
    }
}
