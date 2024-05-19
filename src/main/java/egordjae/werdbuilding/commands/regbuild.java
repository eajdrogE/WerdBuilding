package egordjae.werdbuilding.commands;

import egordjae.werdbuilding.InventoryCreators;
import egordjae.werdbuilding.commands.verdbuild;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
public class regbuild implements CommandExecutor{
    private JavaPlugin plugin;
    private FileConfiguration regConfig,buildingConfig;
    private File regFile,buildingFile;
    public regbuild(JavaPlugin plugin) {
        this.plugin = plugin;
        this.regFile = new File(plugin.getDataFolder(), "buildRequests.yml");
        this.regConfig = YamlConfiguration.loadConfiguration(regFile);
        this.buildingFile = new File(plugin.getDataFolder(), "building.yml");
        this.buildingConfig = YamlConfiguration.loadConfiguration(buildingFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("regbuild")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String playerName = player.getName();
                LuckPerms api = LuckPermsProvider.get();
                User user = api.getUserManager().getUser(player.getUniqueId());
                if (!user.getCachedData().getPermissionData().checkPermission("reg.building").asBoolean()) {
                    player.sendMessage(ChatColor.RED + "У вас нет прав на использование данной команды"); // Красный текст

                } else {
                    reloadConfig();
                    // Если название здания не указано, выводим все заявки этого игрока
                    if (args.length == 0) {
                        boolean hasRequests = false;
                        for (String key : regConfig.getKeys(false)) {
                            if (key.startsWith(playerName + ", ")) {
                                hasRequests = true;
                                String status = regConfig.getString(key + ".status");
                                String colorCode = "";

                                switch (status) {
                                    case "Принято":
                                        colorCode = "§a"; // Зеленый
                                        break;
                                    case "Отклонено":
                                        colorCode = "§c"; // Красный
                                        break;
                                    case "На рассмотрении":
                                        colorCode = "§e"; // Желтый
                                        break;
                                    case "Ждем принятие запроса":
                                        colorCode = "§9"; // Синий
                                        break;
                                }
                                String coordinates = regConfig.getString(key + ".coordinates");
                                player.sendMessage("Здание: " + regConfig.getString(key + ".buildingName") + ", Координаты: " + coordinates + ", Статус: " + colorCode + status);
                            }
                        }
                        if (!hasRequests) {
                            player.sendMessage("У вас пока нет действующих заявок. Если хотите её оставить, встаньте на место регистрации и пропишите /regbuild <название здания>");
                        }
                        removeFinalRequests(playerName);
                        return true;
                    }

                    switch (args[0].toLowerCase()) {
                        case "help":
                            player.sendMessage("§c/regbuild <название постройки> - подать заявку. Заявое не больше 5, подавайте внимательней - удалить заявку может только проверяющий. Нужно ОБЯЗАТЕЛЬНО стоять на месте постройки");
                            player.sendMessage("§c/regbuild - просмотреть свои заявки");
                            break;
                        case "menu":
                            // Создаем экземпляр InventoryCreators, создаем инвентарь и открываем его
                            if (sender instanceof Player) {
                                InventoryCreators inventoryCreators = new InventoryCreators(plugin, this);
                                inventoryCreators.MultiPageInventory(); // Создаем инвентарь
                                inventoryCreators.openInventory(player);
                            }
                            break;
                        default:
                            return true;
                    }
                }
            }
        }
        return false;
    }



    public void saveBuildRequest(String playerName, String buildingName, Location location, String townName) {
        int x = (int) Math.round(location.getX());
        int y = (int) Math.round(location.getY());
        int z = (int) Math.round(location.getZ());
        String worldName = location.getWorld().getName();
        worldName = worldName+", "+ x + ", "+ y +", " + z;
        // Добавляем имя игрока и название постройки в ключ
        String key = playerName + ", " + buildingName + ", " + worldName;
        reloadConfig();
        regConfig.set(key + ".playerName", playerName);
        regConfig.set(key + ".buildingName", buildingName);
        regConfig.set(key + ".coordinates", worldName);
        regConfig.set(key + ".town", townName);
        regConfig.set(key + ".status", "Ждем принятие запроса");

        try {
            regConfig.save(regFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void removeFinalRequests(String playerName) {
        reloadConfig();
        for (String key : regConfig.getKeys(false)) {
            // Проверяем, что заявка принадлежит данному игроку
            if (key.startsWith(playerName + ",")) {
                // Получаем статус заявки
                String status = regConfig.getString(key + ".status");
                // Проверяем, что статус заявки равен "Принято" или "Отвергнуто"
                if ("Принято".equals(status) || "Отклонено".equals(status)) {
                    // Удаляем заявку
                    regConfig.set(key, null);
                }
            }
        }

        // Сохраняем изменения в файл
        try {
            regConfig.save(regFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void reloadConfig() {
        // Очищаем текущую конфигурацию
        regConfig = new YamlConfiguration();

        // Загружаем данные из файла в конфигурацию
        try {
            regConfig.load(regFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }
    // Функция для подсчета заявок игрока
    public int getPlayerRequests(String playerName, ConfigurationSection regConfig) {
        int playerRequests = 0;
        for (String key : regConfig.getKeys(false)) {
            if (key.startsWith(playerName + ", ")) {
                playerRequests++;
            }
        }
        return playerRequests;
    }

    // Функция для получения имени города игрока
    public String getPlayerTownName(String playerName) {
        String townName = "";
        try {
            Resident resident = TownyUniverse.getInstance().getResident(playerName);
            if (resident.hasTown()) {
                townName = resident.getTown().getName();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return townName;
    }

    // Основная функция
    public boolean processBuildRequest(String playerName, String[] args, ConfigurationSection regConfig, Player player) {
        // Проверяем количество заявок от этого игрока
        int playerRequests = getPlayerRequests(playerName, regConfig);

        // Если у игрока уже есть 5 заявок, не позволяем создать новую
        if (playerRequests >= 5) {
            player.sendMessage("§cВы не можете создать больше 5 заявок.");
            return true;
        }

        String buildingName = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
        Location location = player.getLocation();

        // Получаем город игрока с помощью Towny API
        String townName = getPlayerTownName(playerName);

        saveBuildRequest(playerName, buildingName, location, townName);
        player.sendMessage("Ваш запрос на строительство " + buildingName + " был зарегистрирован!");
        return true;
    }
    public FileConfiguration getRegConfig() {
        return this.regConfig;
    }
}
