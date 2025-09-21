package TòAnteprima.BowBoost;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;

public final class BowBoost extends JavaPlugin {
    private double vertical;
    private double horizontal;
    private double web;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupMessages();
        reloadConfigValues();

        getCommand("bowboost").setExecutor(new BowBoostCommand(this));
        getServer().getPluginManager().registerEvents(new PlayerListeners(this), this);
    }

    private void setupMessages() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reloadConfigValues() {
        reloadConfig();
        this.horizontal = getConfig().getDouble("boost.horizontal");
        this.vertical = getConfig().getDouble("boost.vertical");
        this.web = getConfig().getDouble("boost.web");

        // Ricarica anche i messaggi
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMessage(String path) {
        return ChatColor.translateAlternateColorCodes('&', messagesConfig.getString(path, "&cMessage not found: " + path));
    }

    public double vertical() {
        return this.vertical;
    }

    public double horizontal() {
        return this.horizontal;
    }

    public double web() {
        return this.web;
    }

    public class BowBoostCommand implements CommandExecutor {
        private final BowBoost plugin;

        public BowBoostCommand(BowBoost plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
            if (!sender.hasPermission("bowboost.command")) {
                sender.sendMessage(plugin.getMessage("no-permission"));
                return true;
            }

            if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
                sender.sendMessage(plugin.getMessage("invalid-command"));
                return true;
            }

            plugin.reloadConfigValues();
            sender.sendMessage(plugin.getMessage("reload-success"));
            return true;
        }
    }

    public class PlayerListeners implements Listener {
        private final BowBoost plugin;
        private final Map<Arrow, Vector> vectors = new HashMap<>();
        private Class<?> craftPlayerClass;
        private Method getHandleMethod;
        private Class<?> packetClass;
        private Constructor<?> packetConstructor;
        private Field playerConnectionField;
        private Method sendPacketMethod;

        public PlayerListeners(BowBoost plugin) {
            this.plugin = plugin;
            try {
                String version = Bukkit.getServer().getClass().getPackage().getName().substring(23);
                this.craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
                this.getHandleMethod = this.craftPlayerClass.getMethod("getHandle");
                this.packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutEntityVelocity");
                this.packetConstructor = this.packetClass.getConstructor(int.class, double.class, double.class, double.class);
                Class<?> entityPlayerClass = Class.forName("net.minecraft.server." + version + ".EntityPlayer");
                this.playerConnectionField = entityPlayerClass.getField("playerConnection");
                Class<?> playerConnectionClass = this.playerConnectionField.getType();
                Class<?> packetSuperClass = Class.forName("net.minecraft.server." + version + ".Packet");
                this.sendPacketMethod = playerConnectionClass.getMethod("sendPacket", packetSuperClass);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> vectors.entrySet().removeIf(entry -> entry.getKey().isDead() || !entry.getKey().isValid()), 20L, 20L);
        }

        @EventHandler
        public void playerVelocity(PlayerVelocityEvent event) {
            Player player = event.getPlayer();
            EntityDamageEvent lastDamage = player.getLastDamageCause();
            if (!(lastDamage instanceof EntityDamageByEntityEvent))
                return;
            EntityDamageByEntityEvent lastDamageByEntity = (EntityDamageByEntityEvent) lastDamage;
            Entity damager = lastDamageByEntity.getDamager();
            if (!(damager instanceof Arrow) || !this.vectors.containsKey(damager))
                return;
            event.setCancelled(true);
            this.vectors.remove(damager);
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void entityDamageByEntity(EntityDamageByEntityEvent event) {
            if (event.isCancelled())
                return;
            if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Arrow))
                return;
            Arrow arrow = (Arrow) event.getDamager();
            Player damaged = (Player) event.getEntity();
            Vector vector = this.vectors.get(arrow);
            if (vector == null)
                return;
            Location location = damaged.getLocation();
            if (location.getBlock().getType() == Material.WEB || location.clone().add(0.0D, 1.0D, 0.0D).getBlock().getType() == Material.WEB) {
                double y = vector.getY();
                vector = vector.clone().multiply(plugin.web()).setY(y);
            }
            sendVelocityPacket(damaged, vector);
        }

        @EventHandler
        public void projectileShoot(ProjectileLaunchEvent event) {
            if (!(event.getEntity() instanceof Arrow))
                return;
            Arrow arrow = (Arrow) event.getEntity();
            if (!(arrow.getShooter() instanceof Player))
                return;
            Player shooter = (Player) arrow.getShooter();
            ItemStack itemStack = shooter.getItemInHand();
            if (itemStack == null || itemStack.getType() != Material.BOW)
                return;
            if (!itemStack.containsEnchantment(Enchantment.ARROW_KNOCKBACK))
                return;
            float yaw = shooter.getEyeLocation().getYaw();
            Vector sight = (new Vector(-Math.sin(Math.toRadians(yaw)), 0.0D, Math.cos(Math.toRadians(yaw)))).multiply(Math.max(1.0D, itemStack.getEnchantmentLevel(Enchantment.ARROW_KNOCKBACK) * plugin.horizontal())).setY(plugin.vertical());
            this.vectors.put(arrow, sight);
        }

        private void sendVelocityPacket(Player player, Vector vector) {
            try {
                Object craftPlayer = this.craftPlayerClass.cast(player);
                Object entityPlayer = this.getHandleMethod.invoke(craftPlayer);
                Object playerConnection = this.playerConnectionField.get(entityPlayer);
                Object packet = this.packetConstructor.newInstance(player.getEntityId(), vector.getX(), vector.getY(), vector.getZ());
                this.sendPacketMethod.invoke(playerConnection, packet);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
    }
}