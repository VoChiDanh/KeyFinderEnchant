package gc.tokens.enchants.keyfinder;

import com.solodevelopment.tokens.Tokens;
import com.solodevelopment.tokens.enchant.Enchantment;
import com.solodevelopment.tokens.enchant.events.SystemEnchantEvent;
import com.solodevelopment.tokens.enchant.events.SystemEnchantExplodeEvent;
import com.solodevelopment.tokens.nbt.NBT;
import com.solodevelopment.tokens.utils.EventType;
import com.solodevelopment.tokens.utils.FileSystem;
import com.solodevelopment.tokens.utils.Occurrence;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class KeyFinderEnchant extends Enchantment {

    private final ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

    private Map<String, LevelReward> levels;

    private boolean blocksExplodeEnabled;

    public String getName() {
        return "KeyFinder";
    }

    private int getRandomInt() {
        Random r = new Random();
        return r.nextInt(100);
    }

    public void onActivation(SystemEnchantEvent e) {
        if (e.getEventType() == EventType.PLAYER_BREAK) {
            TFile fileSystem = new TFile("keyfinder");
            Player player = e.getPlayer();
            ItemStack hand = player.getItemInHand();
            NBT itemNBT = NBT.get(hand);
            if (itemNBT.hasKey("Tokens" + getName())) {
                long currentLevel = itemNBT.getLong("Tokens" + getName());
                if (!this.levels.containsKey(String.valueOf(currentLevel))) {
                    return;
                }
                int chance = fileSystem.getConfig().getInt("enchant.levels.order." + (int) currentLevel + ".rewards.block." + (int) currentLevel + ".chance");
                int chance2 = getRandomInt();
                if (chance > chance2) {
                    executeLevel(e.getPlayer(), currentLevel);
                }
            }
        }
    }

    private void executeLevel(Player player, long level) {
        executeLevel(player, level, 1, false);
    }

    private void executeLevel(Player player, long level, int blocks, boolean explosive) {
        String playerName = player.getName();
        List<String> rewards = explosive ? this.levels.get(String.valueOf(level)).getExplosiveRewards() : this.levels.get(String.valueOf(level)).getRewards();
        for (String reward : rewards) {
            reward = parseMath(reward.replace("%player%", playerName).replace("%blocks%", "" + blocks).replace("%level%", "" + level));
            if (reward.startsWith("[MESSAGE]")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', reward.replace("[MESSAGE] ", "")));
                continue;
            }
            if (reward.startsWith("[COMMAND]"))
                Bukkit.dispatchCommand(this.console, reward.replace("[COMMAND] ", ""));
        }
    }

    @EventHandler
    public void onExplode(SystemEnchantExplodeEvent e) {
        if (this.blocksExplodeEnabled) {
            if (e.getBlockList() == null || e.getBlockList().isEmpty())
                return;
            TFile fileSystem = new TFile("keyfinder");
            Player player = e.getPlayer();
            ItemStack hand = player.getItemInHand();
            NBT itemNBT = NBT.get(hand);
            if (itemNBT.hasKey("Tokens" + getName())) {
                long currentLevel = itemNBT.getLong("Tokens" + getName());
                if (!this.levels.containsKey(String.valueOf(currentLevel)))
                    return;
                int chance = fileSystem.getConfig().getInt("enchant.levels.order." + (int) currentLevel + ".rewards.explosive." + (int) currentLevel + ".chance");
                int chance2 = getRandomInt();
                if (chance > chance2) {
                    executeLevel(e.getPlayer(), currentLevel, e.getBlockList().size(), true);
                }
            }
        }
    }

    public void create(FileSystem localFile) {
        localFile.set("enchant.configuration.custom-name", getName());
        localFile.set("enchant.configuration.display", "&c" + getName() + " %level%");
        localFile.set("enchant.configuration.proc", 0.6D);
        localFile.set("enchant.configuration.allowed-items", Collections.singletonList("DIAMOND_PICKAXE"));
        localFile.set("enchant.configuration.refundable", Boolean.FALSE);
        localFile.set("enchant.configuration.permission.enabled", Boolean.FALSE);
        localFile.set("enchant.configuration.permission.value", "tokens.enchant." + getName());
        localFile.set("enchant.configuration.blocks-explode.enabled", Boolean.TRUE);
        localFile.set("enchant.levels.configuration.max-level", 2);
        localFile.set("enchant.levels.configuration.starting-price", 0);
        localFile.set("enchant.levels.configuration.cost-algorithm", "(levels * 500) + startingPrice");
        localFile.set("enchant.levels.order.1.rewards.block.1.chance", 50);
        localFile.set("enchant.levels.order.1.rewards.block.1.commands", Arrays.asList("[MESSAGE] &dYou have won 1 dirt as a reward", "[COMMAND] give %player% dirt 1"));
        localFile.set("enchant.levels.order.1.rewards.explosive.1.chance", 5);
        localFile.set("enchant.levels.order.1.rewards.explosive.1.commands", Arrays.asList("[MESSAGE] &dYou have won <math>%blocks%/2</math> dirt as a reward", "[COMMAND] give %player% dirt <math>%blocks%/2</math>"));
        localFile.set("enchant.levels.order.1.rewards.block.2.chance", 20);
        localFile.set("enchant.levels.order.1.rewards.block.2.commands", Arrays.asList("[MESSAGE] &aYou have won 1 emerald as a reward", "[COMMAND] give %player% emerald 1"));
        localFile.set("enchant.levels.order.1.rewards.explosive.2.chance", 2);
        localFile.set("enchant.levels.order.1.rewards.explosive.2.commands", Arrays.asList("[MESSAGE] &aYou have won <math>%blocks%/2</math> emerald as a reward", "[COMMAND] give %player% emerald <math>%blocks%/2</math>"));
    }

    public void load(FileSystem localFile) {
        setEnchantName(localFile.getString("enchant.configuration.custom-name"));
        setOccurrence(Occurrence.RANDOM);
        setEventTypes(Collections.singletonList(EventType.PLAYER_BREAK));
        setDisplay(localFile.getString("enchant.configuration.display"));
        setProc(localFile.getDouble("enchant.configuration.proc"));
        setAllowedItems(localFile.getStringList("enchant.configuration.allowed-items"));
        setRefundable(localFile.getBoolean("enchant.configuration.refundable"));
        if (localFile.getBoolean("enchant.configuration.permission.enabled")) {
            togglePermission();
            setPermission(localFile.getString("enchant.configuration.permission.value"));
        }
        setMaxLevel(localFile.getLong("enchant.levels.configuration.max-level"));
        setCostAlgorithm(localFile.getString("enchant.levels.configuration.cost-algorithm"));
        if (localFile.getString("enchant.levels.configuration.starting-price") != null)
            setStartingPrice(localFile.getLong("enchant.levels.configuration.starting-price"));
        this.blocksExplodeEnabled = localFile.getBoolean("enchant.configuration.blocks-explode.enabled");
        this.levels = new HashMap<>();
        loadLevelsConfiguration(localFile);
    }

    public void save(FileSystem localFile) {
    }

    private void loadLevelsConfiguration(FileSystem localFile) {
        Set<String> levels = localFile.getFC().getConfigurationSection("enchant.levels.order").getKeys(false);
        for (String level : levels) {
            LevelReward levelReward = new LevelReward();
            Set<String> levelRewards = localFile.getFC().getConfigurationSection(String.format("enchant.levels.order.%s.rewards.block", level)).getKeys(false);
            for (String blockRewardLevel : levelRewards)
                levelReward.addReward(new Reward(localFile.getStringList("enchant.levels.order." + level + ".rewards.block." + blockRewardLevel + ".commands")), localFile.getInt("enchant.levels.order." + level + ".rewards.block." + blockRewardLevel + ".chance"));
            Set<String> explosiveLevelRewards = localFile.getFC().getConfigurationSection(String.format("enchant.levels.order.%s.rewards.explosive", level)).getKeys(false);
            for (String explosiveRewardLevel : explosiveLevelRewards)
                levelReward.addExplosiveReward(new Reward(localFile.getStringList("enchant.levels.order." + level + ".rewards.explosive." + explosiveRewardLevel + ".commands")), localFile.getInt("enchant.levels.order." + level + ".rewards.explosive." + explosiveRewardLevel + ".chance"));
            this.levels.put(level, levelReward);
        }
    }

    static class Reward {
        private final List<String> commands;

        public Reward(List<String> commands) {
            this.commands = commands;
        }

        public List<String> getCommands() {
            return this.commands;
        }
    }

    static class LevelReward {
        private final ProbabilityCollection<KeyFinderEnchant.Reward> rewards = new ProbabilityCollection<>();

        private final ProbabilityCollection<KeyFinderEnchant.Reward> explosiveRewards = new ProbabilityCollection<>();

        public void addReward(KeyFinderEnchant.Reward reward, int probability) {
            this.rewards.add(reward, probability);
        }

        public void addExplosiveReward(KeyFinderEnchant.Reward reward, int probability) {
            this.explosiveRewards.add(reward, probability);
        }

        public List<String> getRewards() {
            return this.rewards.get().getCommands();
        }

        public List<String> getExplosiveRewards() {
            return this.explosiveRewards.get().getCommands();
        }
    }

    private static String parseMath(String reward) {
        String formula = StringUtils.substringBetween(reward, "<math>", "</math>");
        if (formula != null && !formula.isEmpty())
            reward = reward.replace(formula, String.valueOf(Tokens.getAPI().evaluate(formula))).replace("<math>", "").replace("</math>", "");
        return reward;
    }

    static class TFile {

        private final String name;
        private File file;
        private FileConfiguration config;

        public TFile(String name) {
            this.name = name;
            this.file = new File(Tokens.getInstance().getDataFolder() + File.separator + "Enchantments", name + ".yml");
            this.config = YamlConfiguration.loadConfiguration(this.file);
        }

        public void load() {
            File folder = new File(Tokens.getInstance().getDataFolder(), "Enchantments");

            if (!folder.exists()) {
                folder.mkdirs();
            }

            this.file = new File(folder, this.name + ".yml");
            if (!this.file.exists()) {
                try {
                    this.file.createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            this.config = YamlConfiguration.loadConfiguration(this.file);
        }

        public String getName() {
            return name;
        }

        public File getFile() {
            return this.file;
        }

        public FileConfiguration getConfig() {
            return this.config;
        }

        public void save() {
            try {
                this.config.save(this.file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
