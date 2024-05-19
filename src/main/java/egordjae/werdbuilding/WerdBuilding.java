package egordjae.werdbuilding;


import egordjae.werdbuilding.commands.regbuild;
import egordjae.werdbuilding.commands.verdbuild;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class WerdBuilding extends JavaPlugin implements Listener {

    private JavaPlugin plugin;
    private FileConfiguration regConfig, logConfig, buildingConfig;
    private File regFile, logFile,buildingFile;
    public static final Logger LOGGER = Logger.getLogger("verdbuilding");
    @Override
    public void onEnable() {
        this.getCommand("verdbuild").setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("regbuild").setExecutor(new regbuild(this));
        getCommand("verdbuild").setExecutor(new verdbuild(this));
        regFile = new File(getDataFolder(), "buildRequests.yml");
        if (!regFile.exists()) {
            regFile.getParentFile().mkdirs();
            try {
                regFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            saveResource("buildRequests.yml", false);
        }
        regConfig = new YamlConfiguration();
        try {
            regConfig.load(regFile);
        } catch (IOException | InvalidConfigurationException e) {
            LOGGER.warning(e.getMessage());
        }

        logFile = new File(getDataFolder(), "plugLogs.yml");
        if (!logFile.exists()) {
            logFile.getParentFile().mkdirs();
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            saveResource("plugLogs.yml", false);
        }
        logConfig = new YamlConfiguration();
        try {
            logConfig.load(logFile);
        } catch (IOException | InvalidConfigurationException e) {
            LOGGER.warning(e.getMessage());
        }

        buildingFile = new File(getDataFolder(), "buildings.yml");
        if (!buildingFile.exists()) {
            buildingFile.getParentFile().mkdirs();
            try {
                regFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            saveResource("building.yml", false);
        }
        buildingConfig = new YamlConfiguration();
        try {
            buildingConfig.load(buildingFile);
        } catch (IOException | InvalidConfigurationException e) {
            LOGGER.warning(e.getMessage());
        }
   }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("verdbuild")) {
            if (args.length == 1) {
                return Arrays.asList("accept", "denied","showmystat","showourstat");
            }
        }
        return null;
    }
}



