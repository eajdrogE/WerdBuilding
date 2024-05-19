package egordjae.werdbuilding;
import egordjae.werdbuilding.commands.regbuild;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

public class InventoryCreators implements InventoryHolder {
    private final regbuild regBuild; // Добавляем поле для RegBuild
    private FileConfiguration config;
    private final HashMap<Integer, Inventory> pages = new HashMap<>();
    private int currentPage = 0;

    public InventoryCreators(Plugin plugin, regbuild regBuild) { // Добавляем RegBuild в конструктор
        this.regBuild = regBuild;
        File configFile = new File(plugin.getDataFolder(), "buildings.yml");
        if (!configFile.exists()) {
            plugin.saveResource("buildings.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        InventoryClickListener inventoryClickListener = new InventoryClickListener(this);
        Bukkit.getPluginManager().registerEvents(inventoryClickListener, plugin);
    }

    public void MultiPageInventory() {
        // Создайте страницы инвентаря и добавьте их в HashMap
        for (String key : config.getKeys(false)) {
            String title = key;
            Inventory inventory = Bukkit.createInventory(null, 54, title);

            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) {
                // Обрабатываем ситуацию, когда раздел конфигурации не найден или не является ConfigurationSection
            } else {
                Map<String, Object> buildings = section.getValues(false);
                // ...

                for (Map.Entry<String, Object> entry : buildings.entrySet()) {
                    String buildingName = entry.getKey();
                    Map<String, Object> buildingData = ((ConfigurationSection) entry.getValue()).getValues(false);

                    // Получаем материал из конфигурации
                    String materialName = (String) buildingData.get("Материал");
                    Material material = Material.getMaterial(materialName.toUpperCase());

                    // Создаем предмет с информацией о здании
                    ItemStack item = new ItemStack(material != null ? material : Material.DIRT);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(buildingName);

                    List<String> loreList = new ArrayList<>();
                    if ((int) buildingData.get("Благосостояние") != 0) {
                        loreList.add("Благосостояние: " + buildingData.get("Благосостояние"));
                    }
                    if ((int) buildingData.get("Очки пополнения") != 0) {
                        loreList.add("Очки пополнения: " + buildingData.get("Очки пополнения"));
                    }
                    if ((int) buildingData.get("Доход") != 0) {
                        loreList.add("Доход: " + buildingData.get("Доход"));
                    }
                    if ((int) buildingData.get("Стабильность") != 0) {
                        loreList.add("Стабильность: " + buildingData.get("Стабильность"));
                    }
                    if ((int) buildingData.get("Лимит армии") != 0) {
                        loreList.add("Лимит армии: " + buildingData.get("Лимит армии"));
                    }
                    if ((int) buildingData.get("Престиж") != 0) {
                        loreList.add("Престиж: " + buildingData.get("Престиж"));
                    }

                    meta.setLore(loreList);
                    item.setItemMeta(meta);

                    // Добавляем предмет в инвентарь
                    inventory.addItem(item);
                }

            }

            ItemStack nextPageItem = new ItemStack(Material.PAPER);
            ItemMeta nextPageMeta = nextPageItem.getItemMeta();
            nextPageMeta.setDisplayName("Следующая страница");
            nextPageItem.setItemMeta(nextPageMeta);

            ItemStack previousPageItem = new ItemStack(Material.PAPER);
            ItemMeta previousPageMeta = previousPageItem.getItemMeta();
            previousPageMeta.setDisplayName("Предыдущая страница");
            previousPageItem.setItemMeta(previousPageMeta);

            // Добавляем предметы в левый и правый нижний углы инвентаря
            inventory.setItem(45, previousPageItem);
            inventory.setItem(53, nextPageItem);

            pages.put(currentPage, inventory);
            currentPage++;
        }
    }


    @Override
    public Inventory getInventory() {
        return pages.get(currentPage);
    }

    public boolean isCustomInventory(Inventory inventory) {
        return inventory.getHolder() instanceof InventoryCreators;
    }

    public void openInventory(Player player) {
        // Проверяем, существует ли страница
if (currentPage>4)
{
    currentPage=0;
}
        if (!pages.containsKey(currentPage)) {
            currentPage =0;
            // Если страницы не существует, создаем её
            MultiPageInventory();
        }

        // Открываем инвентарь
        player.openInventory(pages.get(currentPage));
    }


    public void nextPage(Player player) {
        if (currentPage < pages.size() - 1) {
            currentPage++;
            openInventory(player);
        }
    }

    public void previousPage(Player player) {
        if (currentPage > 0) {
            currentPage--;
            openInventory(player);
        }
    }

    public class InventoryClickListener implements Listener {
        private final InventoryCreators InventoryCreators;

        public InventoryClickListener(InventoryCreators InventoryCreators) {
            this.InventoryCreators = InventoryCreators;
        }

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }
            Player player = (Player) event.getWhoClicked(); // Определяем игрока здесь
            String inventoryName = player.getOpenInventory().getTitle();
            List<String> validNames = Arrays.asList("Деревенские здания", "Деревенские регионы", "Городские здания", "Городские регионы", "Замки");

            if (!validNames.contains(inventoryName)) {
                return;
            }
            Inventory clickedInventory = event.getClickedInventory();
            // Создаем список допустимых названий
            ItemStack clickedItem = event.getCurrentItem();
            // Проверяем, является ли инвентарь одним из наших кастомных инвентарей
            String displayName = clickedItem.getItemMeta().getDisplayName();
            if (displayName.equals("Следующая страница")) {
                InventoryCreators.nextPage(player);
                event.setCancelled(true);
            } else if (displayName.equals("Предыдущая страница")) {
                InventoryCreators.previousPage(player);
                event.setCancelled(true);
            } else {
                // Если нажатый предмет не является кнопкой переключения страниц, мы считаем его зданием
                // и вызываем функцию processBuildRequest с именем здания в качестве аргумента
                String[] args = {displayName};
                regBuild.processBuildRequest(player.getName(), args, regBuild.getRegConfig(), player);
                event.setCancelled(true);
                player.closeInventory();
            }
        }
    }
}
