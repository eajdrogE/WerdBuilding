package egordjae.werdbuilding.commands;


import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class verdbuild implements CommandExecutor , Listener{
    private JavaPlugin plugin;

    private FileConfiguration regConfig, logConfig;
    private File regFile, logFile;
    // private String lastKey="";
    private HashMap<String, String> playerKeys = new HashMap<>();
    public verdbuild(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "plugLogs.yml");
        this.logConfig = YamlConfiguration.loadConfiguration(logFile);
        this.regFile = new File(plugin.getDataFolder(), "buildRequests.yml");
        this.regConfig = YamlConfiguration.loadConfiguration(regFile);
        Bukkit.getPluginManager().registerEvents(this, plugin);

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        LuckPerms api = LuckPermsProvider.get();
        User user = api.getUserManager().getUser(player.getUniqueId());
        // Проверяем, есть ли у игрока активная заявка
        if (user.getCachedData().getPermissionData().checkPermission("werd.building").asBoolean()) {
            if (playerKeys.containsKey(playerName)) {
                String lastKey = playerKeys.get(playerName);
                if (lastKey != null) {
                    reloadConfig();
                    // Изменяем статус на "Ждем принятие запроса"
                    regConfig.set(lastKey + ".status", "Ждем принятие запроса, бо");
                    try {
                        regConfig.save(regFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        String playerName = player.getName(); // Получаем имя игрока
        String lastKey = playerKeys.get(playerName); // Получаем lastKey для данного игрока
        LuckPerms api = LuckPermsProvider.get();
        User user = api.getUserManager().getUser(player.getUniqueId());
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эта команда может быть выполнена только игроком!");
            return false;
        }
        if (!user.getCachedData().getPermissionData().checkPermission("werd.building").asBoolean()) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование данной команды"); // Красный текст
        }
        else {
            if (args.length > 0) {
                switch (args[0].toLowerCase()) {
                    case "accept":
                        if (lastKey == null) {
                            player.sendMessage("Вы не выбрали что оценивать, пропишите /verdbuild");
                            break;
                        }
                        reloadConfig();
                        reloadLogs();
                        // Изменяем статус на "Принято"
                        regConfig.set(lastKey + ".status", "Принято");
                        player.sendMessage(ChatColor.GREEN + "Заявка принята."); // Зеленый текст
                        playerKeys.put(playerName, null); // Обнуляем lastKey после обработки команды
                        String requestPlayerName = regConfig.getString(lastKey + ".playerName");
                        Player requestPlayer = Bukkit.getPlayer(requestPlayerName);
                        if (requestPlayer != null && requestPlayer.isOnline()) {
                            requestPlayer.sendMessage(ChatColor.GREEN + "Ваша заявка была рассмотрена - /regbuild для большей информации");
                            // Воспроизводим звук получения опыта
                            requestPlayer.playSound(requestPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                        }

                        // Добавляем запись в файл pluginLogs.yml
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
                        String logKey = playerName + " " +LocalDateTime.now().format(formatter);
                        ConfigurationSection requestInfo = regConfig.getConfigurationSection(lastKey);
                        logConfig.createSection(logKey);
                        logConfig.set(logKey + ".playerName", requestInfo.get("playerName"));
                        logConfig.set(logKey + ".buildingName", requestInfo.get("buildingName"));
                        for (String infoKey : requestInfo.getKeys(false)) {
                            if (!infoKey.equals("buildingName")) {
                                logConfig.set(logKey + "." + infoKey, requestInfo.get(infoKey));
                            }
                        }
                        try {
                            logConfig.save(logFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "showmystat":
                        // Получаем все ключи из logConfig
                        reloadLogs();
                        Set<String> keys = logConfig.getKeys(false);
                        // Определяем количество заявок на страницу
                        int requestsPerPage = 5;
                        // Определяем номер страницы (по умолчанию 1, если не указан)
                        int pageNumber = 1;
                        if (args.length > 1) {
                            try {
                                pageNumber = Integer.parseInt(args[1]);
                            } catch (NumberFormatException e) {
                                player.sendMessage("Неверный номер страницы. Используйте /showmystat <номер страницы>.");
                                break;
                            }
                        }
                        // Определяем индексы начала и конца для заявок на этой странице
                        int start = (pageNumber - 1) * requestsPerPage;
                        int end = start + requestsPerPage;
                        // Если номер страницы больше, чем количество страниц
                        if (start >= keys.size()) {
                            player.sendMessage("Нет такой страницы. Попробуйте меньший номер страницы.");
                            break;
                        }
                        player.sendMessage("Ваши обработанные заявки (страница " + pageNumber + "):");
                        // Преобразуем Set в List для доступа по индексу
                        List<String> keyList = new ArrayList<>(keys);
                        for (int i = start; i < end && i < keyList.size(); i++) {
                            String key = keyList.get(i);
                            // Если имя игрока совпадает с именем игрока в ключе
                            if (key.startsWith(playerName)) {
                                // Получаем всю информацию о заявке
                                Map<String, Object> requestInfo2 = logConfig.getConfigurationSection(key).getValues(true);
                                // Создаем список только для значений
                                List<Object> values = new ArrayList<>(requestInfo2.values());
                                // Выводим информацию о заявке
                                String output =playerName + " от " + key.split("_")[1] + ": " + values.toString();
                                player.sendMessage(output);
                            }
                        }
                        break;

                    case "showourstat":
                        // Создаем HashMap для хранения статистики игроков
                        reloadLogs();
                        Set<String> keys2 = logConfig.getKeys(false);
                        HashMap<String, Integer> stats = new HashMap<>();
                        for (String key : keys2) {
                            // Получаем имя игрока из ключа
                            String name = key.split(" ")[0];
                            // Если игрок уже есть в статистике, увеличиваем его счетчик на 1
                            if (stats.containsKey(name)) {
                                stats.put(name, stats.get(name) + 1);
                            } else {
                                // Иначе добавляем игрока в статистику с счетчиком 1
                                stats.put(name, 1);
                            }
                        }
                        player.sendMessage("Статистика обработанных заявок:");
                        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                            player.sendMessage(entry.getKey() + ", Обработано заявок: " + entry.getValue());
                        }
                        break;

                    case "denied":
                        if (lastKey == null) {
                            player.sendMessage("Вы не выбрали что оценивать, пропишите /verdbuild");
                            break;
                        }
                        reloadConfig();
                        reloadLogs();
                        // Изменяем статус на "Отклонено"
                        regConfig.set(lastKey + ".status", "Отклонено");
                        String reason;
                        String buildingName;
                        if (args.length > 1) {
                            // Добавляем причину отказа к названию постройки
                            reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                            buildingName = regConfig.getString(lastKey + ".buildingName");

                        }
                        // Получаем имя игрока, подавшего заявку, и координаты постройки
                        requestPlayerName = regConfig.getString(lastKey + ".playerName");
                        String coordinates = regConfig.getString(lastKey + ".coordinates");
                        buildingName = regConfig.getString(lastKey + ".buildingName");
                        reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Не указано";
                        player.sendMessage(ChatColor.RED + "Заявка отклонена. Игрок: " + requestPlayerName + ", Постройка: " + buildingName + ", Координаты: " + coordinates + ", Причина: " + reason); // Красный текст
                        requestPlayer = Bukkit.getPlayer(requestPlayerName);
                        regConfig.set(lastKey + ".buildingName", buildingName + " (" + reason + ")");
                        if (requestPlayer != null && requestPlayer.isOnline()) {
                            requestPlayer.sendMessage(ChatColor.GREEN + "Ваша заявка была рассмотрена - /regbuild для большей информации");
                            // Воспроизводим звук получения опыта
                            requestPlayer.playSound(requestPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                        }
                        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
                        String logKey2 = playerName + " " +LocalDateTime.now().format(formatter2);
                        ConfigurationSection requestInfo2 = regConfig.getConfigurationSection(lastKey);
                        logConfig.createSection(logKey2);
                        logConfig.set(logKey2 + ".playerName", requestInfo2.get("playerName"));
                        logConfig.set(logKey2 + ".buildingName", requestInfo2.get("buildingName"));
                        for (String infoKey : requestInfo2.getKeys(false)) {
                            if (!infoKey.equals("buildingName")) {
                                logConfig.set(logKey2 + "." + infoKey, requestInfo2.get(infoKey));
                            }
                        }
                        try {
                            logConfig.save(logFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        playerKeys.put(playerName, null);
                        break;
                }

                // Сохраняем изменения в файл
                try {
                    regConfig.save(regFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return true;
            }

            // Создаем новый инвентарь размером 9 ячеек
            if (lastKey == null) {
                Inventory inventory = Bukkit.createInventory(null, 9, "Verdbuild Menu");
                reloadConfig();

                // Для каждого ключа в regConfig
                for (String key : regConfig.getKeys(false)) {
                    // Если статус равен "Ждем принятие запроса"
                    if ("Ждем принятие запроса".equals(regConfig.getString(key + ".status"))) {
                        // Получаем никнейм игрока, город и название постройки
                        String requestPlayerName = regConfig.getString(key + ".playerName");
                        String townName = regConfig.getString(key + ".town");
                        String buildingName = regConfig.getString(key + ".buildingName");

                        // Создаем лист бумаги с ключом и городом в качестве отображаемого имени
                        ItemStack paper = new ItemStack(Material.PAPER);
                        ItemMeta meta = paper.getItemMeta();
                        meta.setDisplayName(key + ", " + townName);
                        paper.setItemMeta(meta);

                        // Добавляем лист бумаги в инвентарь
                        if (inventory.firstEmpty() != -1) {
                            // Добавляем лист бумаги в инвентарь
                            inventory.addItem(paper);
                        } else {
                            // Если места нет, прерываем цикл
                            break;
                        }
                        // Если игрок, отправивший запрос, совпадает с текущим игроком, сохраняем ключ
                        if (playerName.equals(requestPlayerName)) {
                            playerKeys.put(playerName, key);
                        }
                    }
                }

                // Открываем инвентарь для игрока
                player.openInventory(inventory);
            }
            else{
                player.sendMessage("Вы уже оцениваете постройку!");
            }
        }
        return true;
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().getViewers().isEmpty() && event.getInventory().getViewers().get(0).getOpenInventory().getTitle().equals("Verdbuild Menu"))
            if(event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PAPER) {
                Player player = (Player) event.getWhoClicked();
                LuckPerms api = LuckPermsProvider.get();
                User user = api.getUserManager().getUser(player.getUniqueId());
                if (user.getCachedData().getPermissionData().checkPermission("werd.building").asBoolean()) {
                    String displayName = event.getCurrentItem().getItemMeta().getDisplayName();
                    event.setCancelled(true);

                    // Разделяем отображаемое имя на части
                    String[] parts = displayName.split(", ");
                    String playerName = parts[0];
                    String buildingName = parts[1];
                    String worldName = parts[2];
                    int x = Integer.parseInt(parts[3]);
                    int y = Integer.parseInt(parts[4]);
                    int z = Integer.parseInt(parts[5]);

                    // Формируем ключ для доступа к regConfig
                    String key = playerName + ", " + buildingName + ", " + worldName + ", " + x + ", " + y + ", " + z;
                    reloadConfig();
                    // Если ключ существует в regConfig
                    if (regConfig.contains(key)) {
                        // Телепортируем игрока на эти координаты
                        player.teleport(new Location(Bukkit.getWorld(worldName), x, y, z));
                        regConfig.set(key + ".status", "На рассмотрении");
                        try {
                            regConfig.save(regFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // Отправляем сообщения "ПРИНЯТЬ" и "ОТКЛОНИТЬ" в чат
                        ShowChatCommandForVerd(player);
                        // Сохраняем lastKey для данного игрока
                        playerKeys.put(player.getName(), key);
                    }
                }
            }
    }
    public void ShowChatCommandForVerd(Player player){
        TextComponent acceptMessage = new TextComponent(ChatColor.GREEN + "ПРИНЯТЬ");
        acceptMessage.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/verdbuild accept"));
        player.spigot().sendMessage(acceptMessage);

        TextComponent denyMessage = new TextComponent(ChatColor.RED + "ОТКЛОНИТЬ (не забудьте причину отказа!)");
        denyMessage.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/verdbuild denied"));
        player.spigot().sendMessage(denyMessage);
    }
    public void reloadLogs(){
        logConfig = new YamlConfiguration();
        try {
            logConfig.load(logFile);
        } catch (IOException | InvalidConfigurationException e){
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

}
