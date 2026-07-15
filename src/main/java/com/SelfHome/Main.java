package com.SelfHome;

import com.GUI.CheckGui;
import com.GUI.CreateGui;
import com.GUI.DenyGui;
import com.GUI.InviteGui;
import com.GUI.MainGui;
import com.GUI.ManageGui;
import com.GUI.ManageGui2;
import com.GUI.TrustGui;
import com.GUI.VisitGui;
import com.Listeners.BlockBreakListener;
import com.Listeners.BlockPlaceListener;
import com.Listeners.CreatureSpawnListener;
import com.Listeners.EntityInteractByEntityListener;
import com.Listeners.FarmProtectListener;
import com.Listeners.FrameProtectListener;
import com.Listeners.HomeProtectInteractListener;
import com.Listeners.HomeProtectPlaceListener;
import com.Listeners.InteractBlackListener;
import com.Listeners.InteractMenuListener;
import com.Listeners.InventoryClickListener;
import com.Listeners.InventoryDragListener;
import com.Listeners.InventoryMoveItemListener;
import com.Listeners.InventoryOpenListener;
import com.Listeners.InventoryPickupItemListener;
import com.Listeners.LivingEntityProtectInHomeListener;
import com.Listeners.MaxHeightPlaceListener;
import com.Listeners.PlayerChatListener;
import com.Listeners.PlayerDamageInHomeListener;
import com.Listeners.PlayerDeathListener;
import com.Listeners.PlayerDropListener;
import com.Listeners.PlayerJoinListener;
import com.Listeners.PlayerMoveListener;
import com.Listeners.PlayerPickupListener;
import com.Listeners.PlayerQuitListener;
import com.Listeners.PlayerRespawnListener;
import com.Listeners.PlayerTeleportListener;
import com.Listeners.PortalCreateListener;
import com.Listeners.TeleportHomeProtectListener;
import com.Listeners.WeatherChangeListener;
import com.Listeners.WorldBlockPlaceListener;
import com.Listeners.WorldInitListener;
import com.Listeners.WorldLoadListener;
import com.PlaceHolder.API;
import com.Util.ConfigUpdate;
import com.Util.MySQL;
import com.Util.Util;
import com.Util.WaitToLoad;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

public class Main extends JavaPlugin implements PluginMessageListener {
    public static JavaPlugin JavaPlugin;
    public static String type = "SelfHomeMain";
    public static String code = "dsfsdfdsfdsf";
    public static Socket socket;
    public static String version = "1.0";
    public static boolean first_success = false;
    public static boolean check_active = false;

    public static boolean isOSLinux() {
        Properties prop = System.getProperties();
        String os = prop.getProperty("os.name");
        return os != null && os.toLowerCase().indexOf("linux") > -1;
    }

    public void onLoad() {
        JavaPlugin = this;
        init();
    }

    public static String getMD5() {
        String md5Hash = "null";

        try {
            byte[] programBytes = Files.readAllBytes(Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5Bytes = md.digest(programBytes);
            StringBuilder sb = new StringBuilder();
            byte[] var8 = md5Bytes;
            int var7 = md5Bytes.length;

            for (int var6 = 0; var6 < var7; var6++) {
                byte b = var8[var6];
                sb.append(String.format("%02x", b));
            }

            md5Hash = sb.toString();
        } catch (NoSuchAlgorithmException | IOException | URISyntaxException var9) {
        }

        return md5Hash;
    }

    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory() != null) {
                InventoryHolder inv = p.getOpenInventory().getTopInventory().getHolder();
                if (inv instanceof CheckGui
                    || inv instanceof CreateGui
                    || inv instanceof DenyGui
                    || inv instanceof InviteGui
                    || inv instanceof MainGui
                    || inv instanceof ManageGui
                    || inv instanceof ManageGui2
                    || inv instanceof TrustGui
                    || inv instanceof VisitGui
                    ) {
                    p.sendMessage("搂a搂l搂m--------------" + Variable.Prefix + "搂a搂l搂m--------------");
                    p.sendMessage(Variable.Lang_YML.getString("CloseGuiWhenPluginReload"));
                    p.sendMessage("搂a搂l搂m--------------" + Variable.Prefix + "搂a搂l搂m--------------");
                    p.closeInventory();
                }
            }
        }


        for (World temp : Bukkit.getWorlds()) {
            boolean is_jump = false;

            for (int i = 0; i < JavaPlugin.getConfig().getStringList("UnAutoSaveWorlds").size(); i++) {
                String str = (String)JavaPlugin.getConfig().getStringList("UnAutoSaveWorlds").get(i);
                if (str.equalsIgnoreCase(Variable.prefix_p + temp.getName().replace(Variable.world_prefix, ""))) {
                    is_jump = true;
                    break;
                }
            }

            if (!is_jump) {
                temp.save();
            }
        }

        Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("AutoSaveSuccess"));
        this.getServer().getPluginManager().disablePlugin(this);
        Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("DisablePlugin"));
    }

    private boolean setupEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        Variable.econ = (Economy)rsp.getProvider();
        return Variable.econ != null;
    }

    public static boolean setupPlayerPoints() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        Variable.playerPoints = PlayerPoints.class.cast(plugin);
        return Variable.playerPoints != null;
    }

    public void onEnable() {
        JavaPlugin = this;
        JavaPlugin.saveResource("config.yml", false);
        if (!new File(JavaPlugin.getDataFolder() + Variable.file_loc_prefix + "Language" + Variable.file_loc_prefix + "English.yml").exists()) {
            JavaPlugin.saveResource("Language" + Variable.file_loc_prefix + "English.yml", false);
        }
        reloadConfig();
        Variable.Lang_YML = YamlConfiguration.loadConfiguration(
            new File(
                JavaPlugin.getDataFolder()
                    + Variable.file_loc_prefix
                    + "Language"
                    + Variable.file_loc_prefix
                    + JavaPlugin.getConfig().getString("Language")
                    + ".yml"
            )
        );
        Variable.NMS_Version = Bukkit.getServer()
            .getClass()
            .getPackage()
            .toString()
            .substring(
                Bukkit.getServer().getClass().getPackage().toString().lastIndexOf(".") + 1, Bukkit.getServer().getClass().getPackage().toString().length()
            )
            .replace("V", "v");
        if (!JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport")) {
            Bukkit.getPluginManager().registerEvents(new CreatureSpawnListener(), this);
            Bukkit.getPluginManager().registerEvents(new InteractBlackListener(), this);
            Bukkit.getPluginManager().registerEvents(new BlockBreakListener(), this);
            Bukkit.getPluginManager().registerEvents(new BlockPlaceListener(), this);
            Bukkit.getPluginManager().registerEvents(new HomeProtectInteractListener(), this);
            Bukkit.getPluginManager().registerEvents(new InventoryOpenListener(), this);
            Bukkit.getPluginManager().registerEvents(new LivingEntityProtectInHomeListener(), this);
            Bukkit.getPluginManager().registerEvents(new PlayerDamageInHomeListener(), this);
            Bukkit.getPluginManager().registerEvents(new FrameProtectListener(), this);
            Bukkit.getPluginManager().registerEvents(new PlayerDropListener(), this);
            Bukkit.getPluginManager().registerEvents(new PlayerPickupListener(), this);
            Bukkit.getPluginManager().registerEvents(new PlayerTeleportListener(), this);
            Bukkit.getPluginManager().registerEvents(new WeatherChangeListener(), this);
            Bukkit.getPluginManager().registerEvents(new WorldLoadListener(), this);
            Bukkit.getPluginManager().registerEvents(new BlockPlaceListener(), this);
            Bukkit.getPluginManager().registerEvents(new WorldBlockPlaceListener(), this);
            Bukkit.getPluginManager().registerEvents(new HomeProtectPlaceListener(), this);
            Bukkit.getPluginManager().registerEvents(new InteractMenuListener(), this);
            Bukkit.getPluginManager().registerEvents(new TeleportHomeProtectListener(), this);
            Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(), this);
            Bukkit.getPluginManager().registerEvents(new PlayerRespawnListener(), this);
            Bukkit.getPluginManager().registerEvents(new EntityInteractByEntityListener(), this);
            Bukkit.getPluginManager().registerEvents(new FarmProtectListener(), this);
            Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(), this);
            Bukkit.getPluginManager().registerEvents(new PlayerChatListener(), this);
            Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), this);
            Bukkit.getPluginManager().registerEvents(new PortalCreateListener(), this);
            Bukkit.getPluginManager().registerEvents(new WorldInitListener(), this);
            Bukkit.getPluginManager().registerEvents(new PlayerMoveListener(), this);
            if (JavaPlugin.getConfig().getBoolean("EnableAsnycTime")) {
            }
        }

        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(), this);
        Bukkit.getPluginManager().registerEvents(new InventoryDragListener(), this);
        Bukkit.getPluginManager().registerEvents(new InventoryPickupItemListener(), this);
        Bukkit.getPluginManager().registerEvents(new InventoryMoveItemListener(), this);
        if (JavaPlugin.getConfig().getBoolean("EnableHeightLimit")) {
            Bukkit.getPluginManager().registerEvents(new MaxHeightPlaceListener(), this);
            Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("EnableHeightLimit"));
        }

        if (JavaPlugin.getConfig().getBoolean("BungeeCord")) {
            if (JavaPlugin.getConfig().getBoolean("AutoReCreateInLowerLagHome") && !JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport")) {
                MySQL.autoUpdateServer();
            }

            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
        }

        if (!JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport") && JavaPlugin.getConfig().getBoolean("EnableMoveListener")) {
            Bukkit.getPluginManager().registerEvents(new PlayerMoveListener(), this);
            Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("EnableMoveListener"));
        }

        Bukkit.getPluginCommand("SH").setExecutor(new CommandListener());
        Bukkit.getPluginCommand("SH").setTabCompleter(new CommandListener());
        Variable.Prefix = this.getConfig().getString("Prefix");
        if (!JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport")) {
            if (JavaPlugin.getServer().getPluginManager().getPlugin("Vault") == null) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("NotHookVault"));
                this.getServer().getPluginManager().disablePlugin(this);
                return;
            }

            this.setupEconomy();
            Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("HookVault"));
            if (JavaPlugin.getServer().getPluginManager().getPlugin("PlayerPoints") == null) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("NotHookPlayerPoints"));
            } else {
                setupPlayerPoints();
                Variable.PlyaerPointsModule = true;
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("HookPlayerPoints"));
            }

            if (JavaPlugin.getServer().getPluginManager().getPlugin("NBTAPI") == null) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("NotHookNBTAPI"));
            } else {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("HookNBTAPI"));
            }

            if (JavaPlugin.getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("NotHookProtocolLib"));
            } else {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("HookProtocolLib"));
            }

            if (JavaPlugin.getServer().getPluginManager().getPlugin("HolographicDisplays") == null) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("NotHookHolographicDisplays"));
                Variable.Hologram_switch = false;
            } else {
                Variable.Hologram_switch = true;
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("HookHolographicDisplays"));
            }

            if (JavaPlugin.getServer().getPluginManager().getPlugin("Multiverse-Core") != null
                && JavaPlugin.getConfig().getBoolean("MultiverseCoreCompability")) {
                Variable.hook_multiverseCore = true;
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("MultiverseCoreCompability"));
            }

            if (JavaPlugin.getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") != null && JavaPlugin.getConfig().getBoolean("FaweSwitch")) {
                Variable.hook_FastAsyncWorldEdit = true;
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("FaweAndWorldEditCompability"));
            }
        }

        if (JavaPlugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("NotHookPlaceholderAPI"));
        } else {
            API api = new API();
            api.register();
            Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("HookPlaceholderAPI"));
        }

        init.init();


    }

    public static void init() {
        if (isOSLinux()) {
            Variable.linux_os = true;
            Variable.file_loc_prefix = "/";
        } else {
            Variable.file_loc_prefix = "\\";
        }

        Variable.prefix_p = JavaPlugin.getConfig().getString("WorldPrefix");
        if (Bukkit.getVersion().toString().toUpperCase().contains("THERMOS")) {
            Variable.has_no_click_message = true;
        }

        if (!Bukkit.getVersion().toString().toUpperCase().contains("CATSERVER")
            && !Bukkit.getVersion().toString().toUpperCase().contains("URANIUM")
            && !Bukkit.getVersion().toString().toUpperCase().contains("KCAULDRON")
            && !Bukkit.getVersion().toString().toUpperCase().contains("THERMOS")
            && !Bukkit.getVersion().toString().toUpperCase().contains("MOHIST")) {
            Variable.world_prefix = "SelfHomeWorld/";
        } else {
            Variable.world_prefix = "";
            Variable.Cat_Check = true;
        }

        if (Bukkit.getVersion().toString().contains("1.7.10")) {
            Variable.world_prefix = "";
            Variable.Cat_Check = true;
        }

        if (Bukkit.getVersion().toString().contains("1.16.5")) {
            Variable.world_prefix = "";
            Variable.Cat_Check = true;
        }

        if (Bukkit.getVersion().toString().toUpperCase().contains("ARCLIGHT")) {
            Variable.world_prefix = "";
            Variable.Cat_Check = false;
        }

        if (Bukkit.getVersion().toString().contains("1.20.1") && Bukkit.getVersion().toString().toUpperCase().contains("1.20.1")) {
            Variable.world_prefix = "";
            Variable.Cat_Check = true;
        }

        if (Bukkit.getVersion().toString().contains("1.20.1") && Bukkit.getVersion().toString().toUpperCase().contains("ARCLIGHT")) {
            Variable.world_prefix = "";
            Variable.Cat_Check = true;
        }

        if (Bukkit.getVersion().toString().contains("Banner") && Bukkit.getVersion().toString().toUpperCase().contains("1.20.1")) {
            Variable.world_prefix = "SelfHomeWorld/";
        }

        JavaPlugin.saveDefaultConfig();
        JavaPlugin.reloadConfig();
        if (!new File(JavaPlugin.getDataFolder() + Variable.file_loc_prefix + "GUI.yml").exists()) {
            JavaPlugin.saveResource("GUI.yml", false);
        }

        if (!new File(JavaPlugin.getDataFolder() + Variable.file_loc_prefix + "GUI_en.yml").exists()) {
            JavaPlugin.saveResource("GUI_en.yml", false);
        }

        if (!new File(JavaPlugin.getDataFolder() + Variable.file_loc_prefix + "log.yml").exists()) {
            JavaPlugin.saveResource("log.yml", false);
        }

        if (!new File(JavaPlugin.getDataFolder() + Variable.file_loc_prefix + "Language" + Variable.file_loc_prefix + "Chinese.yml").exists()) {
            JavaPlugin.saveResource("Language" + Variable.file_loc_prefix + "Chinese.yml", false);
        }

        if (!new File(JavaPlugin.getDataFolder() + Variable.file_loc_prefix + "Language" + Variable.file_loc_prefix + "Chinese_TW.yml").exists()) {
            JavaPlugin.saveResource("Language" + Variable.file_loc_prefix + "Chinese_TW.yml", false);
        }

        if (!new File(JavaPlugin.getDataFolder() + Variable.file_loc_prefix + "Language" + Variable.file_loc_prefix + "English.yml").exists()) {
            JavaPlugin.saveResource("Language" + Variable.file_loc_prefix + "English.yml", false);
        }

        Variable.Lang_YML = YamlConfiguration.loadConfiguration(
            new File(
                JavaPlugin.getDataFolder()
                    + Variable.file_loc_prefix
                    + "Language"
                    + Variable.file_loc_prefix
                    + JavaPlugin.getConfig().getString("Language")
                    + ".yml"
            )
        );
        File f = new File("");
        String Tempf0 = null;

        try {
            Tempf0 = f.getCanonicalPath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        Variable.Final = "";
        if (Variable.linux_os) {
            Variable.ab = Tempf0.split(Variable.file_loc_prefix);
        } else {
            Variable.ab = Tempf0.split(String.valueOf(Variable.file_loc_prefix) + Variable.file_loc_prefix);
        }

        if (isOSLinux()) {
            String[] args = Tempf0.split(Variable.file_loc_prefix);

            for (int i = 0; i < args.length - 1; i++) {
                Variable.Final = String.valueOf(Variable.Final) + Variable.file_loc_prefix + args[i];
            }
        } else {
            String[] args = Tempf0.split(String.valueOf(Variable.file_loc_prefix) + Variable.file_loc_prefix);

            for (int i = 0; i < args.length - 1; i++) {
                Variable.Final = String.valueOf(Variable.Final) + Variable.file_loc_prefix + args[i];
            }
        }

        Variable.Final = String.valueOf(Variable.Final) + Variable.file_loc_prefix;
        if (isOSLinux()) {
            Variable.Final = Variable.Final.replaceFirst(Variable.file_loc_prefix, "");
        } else {
            Variable.Final = Variable.Final.replaceFirst(String.valueOf(Variable.file_loc_prefix) + Variable.file_loc_prefix, "");
        }

        if (JavaPlugin.getConfig().getBoolean("BungeeCord")) {
            Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("EnableBungeeCord"));
            Variable.bungee = true;
            /* excluded HikariCPUtils */
            if (JavaPlugin.getConfig().getBoolean("DisableFunctionButTeleport")) {
                Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("DisableFunctionButTeleport"));
            }
        } else {
            Variable.bungee = false;
            Bukkit.getConsoleSender().sendMessage(Variable.Lang_YML.getString("DisableBungeeCord"));
        }

        Variable.custom_playerdata_location = String.valueOf(Variable.Final)
            + Variable.ab[Variable.ab.length - 1]
            + Variable.file_loc_prefix
            + "plugins"
            + Variable.file_loc_prefix
            + "SelfHomeMain"
            + Variable.file_loc_prefix
            + "playerdata";
        Variable.custom_autobackup_location = String.valueOf(Variable.Final)
            + Variable.ab[Variable.ab.length - 1]
            + Variable.file_loc_prefix
            + "plugins"
            + Variable.file_loc_prefix
            + "SelfHomeMain"
            + Variable.file_loc_prefix
            + "backup";
        Variable.server_file_world = Variable.Final;
        Variable.worldFinal = String.valueOf(Tempf0)
            + Variable.file_loc_prefix
            + "plugins"
            + Variable.file_loc_prefix
            + "SelfHomeMain"
            + Variable.file_loc_prefix;
        Variable.Log_All = String.valueOf(Tempf0) + Variable.file_loc_prefix + "plugins" + Variable.file_loc_prefix + "SelfHomeMain" + Variable.file_loc_prefix;
        Variable.single_server_gen = String.valueOf(Variable.Final) + Variable.file_loc_prefix + Variable.ab[Variable.ab.length - 1] + Variable.file_loc_prefix;
        Variable.Final = Variable.custom_playerdata_location;
        File check_file = new File(Variable.Final);
        if (!check_file.isDirectory()) {
            check_file.mkdir();
        }

        File autobackup = new File(Variable.custom_autobackup_location);
        if (!autobackup.isDirectory()) {
            autobackup.mkdir();
        }

        Variable.CheckIsHome = Variable.Final;
        File aaa = new File(Variable.Final);
        if (!aaa.isDirectory()) {
            aaa.mkdir();
        }

        Variable.Tempf = Variable.Final;
        Variable.Tempf2 = Variable.Temp;
        Variable.f_log = new File(Variable.Log_All, "log.yml");
        if (!Variable.f_log.exists()) {
            try {
                Variable.f_log.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            Variable.f_log = new File(Variable.Log_All, "log.yml");
        }

        Variable.Papi_world = JavaPlugin.getConfig().getString("WorldName");
        File f2 = new File(Variable.worldFinal, "config.yml");
        Variable.getName_yml = YamlConfiguration.loadConfiguration(f2);
        Variable.GUI_YML = YamlConfiguration.loadConfiguration(new File(JavaPlugin.getDataFolder() + Variable.file_loc_prefix + "GUI.yml"));
        ConfigUpdate.update();
        Variable.Soil = JavaPlugin.getConfig().getString("SoilType");
        initHome.init();
    }

    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (channel.equals("BungeeCord")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subchannel = in.readUTF();
            if (subchannel.equals("SelfHomeMain")) {
                short len = in.readShort();
                byte[] msgbytes = new byte[len];
                in.readFully(msgbytes);
                DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));

                try {
                    String[] somedata = msgin.readUTF().split(",");
                    if (somedata[0].equalsIgnoreCase("waitDelayToHome")) {
                        if (JavaPlugin.getConfig().getBoolean("Debug")) {
                            JavaPlugin.getLogger().info("[璋冭瘯]:鏀跺埌寤舵椂鍥炲鏁版嵁鍖?" + somedata[1] + "," + somedata[2]);
                        }

                        Variable.wait_to_spawn_home.put(somedata[1], somedata[2]);
                    } else if (somedata[0].equalsIgnoreCase("waitToCommand")) {
                        if (JavaPlugin.getConfig().getBoolean("Debug")) {
                            JavaPlugin.getLogger().info("[璋冭瘯]:鏀跺埌寤舵椂鎵ц鎸囦护鏁版嵁鍖?" + somedata[1] + "," + somedata[2]);
                        }

                        Variable.wait_to_command.put(somedata[1], somedata[2]);
                    } else if (somedata[0].equalsIgnoreCase("waitToLoad")) {
                        final WaitToLoad wt = new WaitToLoad();
                        wt.home_name = somedata[2];
                        wt.file_loc = somedata[3];
                        MySQL.setServer(wt.home_name, JavaPlugin.getConfig().getString("Server"));
                        File new_f = null;
                        if (Variable.world_prefix.equalsIgnoreCase("")) {
                            if (!Bukkit.getVersion().toString().toUpperCase().contains("ARCLIGHT") && !Bukkit.getVersion().toString().contains("1.20.1")) {
                                new_f = new File(String.valueOf(Variable.single_server_gen) + "world" + Variable.file_loc_prefix + wt.home_name);
                            } else {
                                new_f = new File(String.valueOf(Variable.single_server_gen) + Variable.world_prefix + wt.home_name);
                            }
                        } else {
                            new_f = new File(String.valueOf(Variable.single_server_gen) + Variable.world_prefix + wt.home_name);
                        }

                        if (new_f.isDirectory()) {
                            Util.deleteFile(new_f);
                        }

                        Util.copyDir(String.valueOf(wt.file_loc) + Variable.file_loc_prefix + wt.home_name, new_f.getAbsolutePath());
                        Variable.wait_to_command.put(somedata[1], "sh v " + wt.home_name);
                        Variable.has_already_move_world.add(somedata[1]);
                        (new BukkitRunnable() {
                            public void run() {
                                Util.deleteFile(new File(String.valueOf(wt.file_loc) + Variable.file_loc_prefix + wt.home_name));
                            }
                        }).runTaskLater(JavaPlugin, 20L);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
