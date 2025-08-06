package ga1maz.ru.funConnect;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;

public final class FunConnect extends JavaPlugin implements Listener {

    private final String githubLatestReleaseApi = "https://api.github.com/repos/Ga1maz/funconnect/releases/latest";

    private boolean isLicenseActive;
    private String dbUrl;
    private String latestVersionCache = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dbUrl = getConfig().getString("DB_connection");
        checkLicense();
        checkForUpdates();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("FunConnect загружен!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("funconnect.admin")) {
            String currentVersion = getDescription().getVersion();

            if (latestVersionCache != null && isNewerVersion(latestVersionCache, currentVersion)) {
                String msg1 = "Доступна новая версия FunConnect: " + latestVersionCache + " (у вас " + currentVersion + ")";
                String msg2 = "Скачать: https://github.com/Ga1maz/funconnect/releases/latest";

                player.sendMessage(getGradientText(msg1, "#ff0000", "#800000"));
                player.sendMessage(getGradientText(msg2, "#ff5555", "#aa0000"));
            }
        }
    }

    private void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(githubLatestReleaseApi))
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    getLogger().warning("Не удалось проверить обновления: HTTP " + response.statusCode());
                    return;
                }

                String body = response.body();
                String tagNameMarker = "\"tag_name\":\"";
                int index = body.indexOf(tagNameMarker);
                if (index == -1) {
                    getLogger().warning("Не найден tag_name в ответе GitHub API");
                    return;
                }
                int start = index + tagNameMarker.length();
                int end = body.indexOf("\"", start);
                String latestVersion = body.substring(start, end);

                latestVersionCache = latestVersion;

                String currentVersion = getDescription().getVersion();

                if (isNewerVersion(latestVersion, currentVersion)) {
                    sendRedGradientLog("Доступна новая версия FunConnect: " + latestVersion + " (у вас " + currentVersion + ")");
                    sendRedGradientLog("Скачать: https://github.com/Ga1maz/funconnect/releases/latest");
                } else {
                    getLogger().info("FunConnect обновлён до последней версии: " + currentVersion);
                }
            } catch (Exception e) {
                getLogger().warning("Ошибка при проверке обновлений: " + e.getMessage());
            }
        });
    }

    private boolean isNewerVersion(String latest, String current) {
        latest = latest.startsWith("v") ? latest.substring(1) : latest;
        current = current.startsWith("v") ? current.substring(1) : current;

        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");

        int len = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < len; i++) {
            int l = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            int c = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            if (l > c) return true;
            if (l < c) return false;
        }
        return false;
    }

    @Override
    public void onDisable() {
        getLogger().info("FunConnect выгружен!");
    }

    private void checkLicense() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://funday.ga1maz.ru/"))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body().trim();

            if (body.equals("0")) {
                isLicenseActive = false;
                printExpiredLicense();
            } else {
                isLicenseActive = true;
                printActiveLicense();
            }
        } catch (Exception e) {
            isLicenseActive = true;
            getLogger().warning("Не удалось проверить лицензию. Предполагается, что она активна.");
            printActiveLicense();
        }
    }

    private void printActiveLicense() {
        sendGradient("Создатель: Ga1maz");
        sendGradient("Сайт: https://ga1maz.ru");
        sendGradient("Лицензия: АКТИВНА ✅");
        sendGradient("Telegram: @al_maz_g");
    }

    private void printExpiredLicense() {
        sendGradient("Создатель: Ga1maz");
        sendGradient("Сайт: https://ga1maz.ru");
        sendGradient("Лицензия: ❌ ПРОСРОЧЕНА ❌");
        sendGradient("Telegram: @al_maz_g");
    }

    private void sendRedGradientLog(String text) {
        int r1 = 255, g1 = 50, b1 = 50;
        int r2 = 150, g2 = 0, b2 = 0;

        StringBuilder gradient = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            double ratio = (double) i / (text.length() - 1);
            int r = (int) (r1 + (r2 - r1) * ratio);
            int g = (int) (g1 + (g2 - g1) * ratio);
            int b = (int) (b1 + (b2 - b1) * ratio);

            gradient.append("\u001B[38;2;")
                    .append(r).append(";").append(g).append(";").append(b).append("m")
                    .append(text.charAt(i));
        }
        gradient.append("\u001B[0m");

        getLogger().info(gradient.toString());
    }

    private void sendGradient(String text) {
        int r1 = 255, g1 = 0, b1 = 128;
        int r2 = 0, g2 = 255, b2 = 255;

        StringBuilder gradient = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            double ratio = (double) i / (text.length() - 1);
            int r = (int) (r1 + (r2 - r1) * ratio);
            int g = (int) (g1 + (g2 - g1) * ratio);
            int b = (int) (b1 + (b2 - b1) * ratio);

            gradient.append("\u001B[38;2;")
                    .append(r).append(";").append(g).append(";").append(b).append("m")
                    .append(text.charAt(i));
        }
        gradient.append("\u001B[0m");

        System.out.println(gradient);
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    private String getGradientText(String text, String hexStart, String hexEnd) {
        if (text == null || hexStart == null || hexEnd == null) return text;

        hexStart = hexStart.trim();
        hexEnd = hexEnd.trim();

        if (!hexStart.matches("#[0-9a-fA-F]{6}") || !hexEnd.matches("#[0-9a-fA-F]{6}")) {
            return text;
        }

        int r1 = Integer.parseInt(hexStart.substring(1, 3), 16);
        int g1 = Integer.parseInt(hexStart.substring(3, 5), 16);
        int b1 = Integer.parseInt(hexStart.substring(5, 7), 16);

        int r2 = Integer.parseInt(hexEnd.substring(1, 3), 16);
        int g2 = Integer.parseInt(hexEnd.substring(3, 5), 16);
        int b2 = Integer.parseInt(hexEnd.substring(5, 7), 16);

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            double ratio = (text.length() == 1) ? 0 : (double) i / (text.length() - 1);
            int r = (int) (r1 + (r2 - r1) * ratio);
            int g = (int) (g1 + (g2 - g1) * ratio);
            int b = (int) (b1 + (b2 - b1) * ratio);

            builder.append("§x")
                    .append(toMinecraftColorCode(r))
                    .append(toMinecraftColorCode(g))
                    .append(toMinecraftColorCode(b))
                    .append(text.charAt(i));
        }
        return builder.toString();
    }

    private String toMinecraftColorCode(int colorComponent) {
        String hex = String.format("%02x", colorComponent);
        return "§" + hex.charAt(0) + "§" + hex.charAt(1);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("funconnect")) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage("§eИспользование:");
            sender.sendMessage("§e/funconnect {nick} {number} - установить FunID");
            sender.sendMessage("§e/funconnect info {nick} - инфа об игроке");
            sender.sendMessage("§e/funconnect list - список донатов");
            sender.sendMessage("§e/funconnect reload - перезагрузить конфиг");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload":
                reloadConfig();
                dbUrl = getConfig().getString("DB_connection");
                sender.sendMessage("§aКонфиг перезагружен!");
                return true;

            case "list":
                listDonats(sender);
                return true;

            case "help":
                sendHelp(sender);
                return true;

            case "info":
                if (args.length < 2) {
                    sender.sendMessage("§cИспользование: /funconnect info {nick}");
                    return true;
                }
                showInfo(sender, args[1]);
                return true;

            default:
                if (args.length < 2) {
                    sender.sendMessage("§eИспользование: /funconnect {nick} {number}");
                    return true;
                }
                setFunID(sender, args[0], args[1]);
                return true;
        }
    }

    private void listDonats(CommandSender sender) {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, name, hex1, hex2 FROM Donat");

            sender.sendMessage("§6--- Список Донатов ---");
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String hex1 = rs.getString("hex1");
                String hex2 = rs.getString("hex2");

                String gradientName = getGradientText(name, hex1, hex2);
                sender.sendMessage("§f" + id + " §r" + gradientName);
            }
        } catch (SQLException e) {
            sender.sendMessage("§cОшибка при получении списка донатов!");
            e.printStackTrace();
        }
    }

    private void showInfo(CommandSender sender, String nick) {
        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT u.balance, u.donat_id, u.donat_id as funid, d.name, d.hex1, d.hex2 " +
                            "FROM User u LEFT JOIN Donat d ON u.donat_id = d.id WHERE u.nick_minecraft = ?");
            ps.setString(1, nick);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                double balance = rs.getDouble("balance");
                int donatId = rs.getInt("donat_id");
                String donatName = rs.getString("name");
                String hex1 = rs.getString("hex1");
                String hex2 = rs.getString("hex2");
                int funid = rs.getInt("funid");

                sender.sendMessage("§6--- Инфа игрока §e" + nick + " §6---");
                sender.sendMessage("§fБаланс: §a" + balance);

                String donatDisplay = donatName != null ?
                        getGradientText(donatName, hex1, hex2) + " §7(ID: " + donatId + ")" :
                        "§7Нет";

                sender.sendMessage("§fДонат: §b" + donatDisplay);
                sender.sendMessage("§fFunID: §d" + funid);
            } else {
                sender.sendMessage("§cИгрок с ником " + nick + " не найден в базе!");
            }
        } catch (SQLException e) {
            sender.sendMessage("§cОшибка при получении информации об игроке!");
            e.printStackTrace();
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6--- Помощь FunConnect ---");
        sender.sendMessage("§e/funconnect {nick} {number} §7- установить FunID");
        sender.sendMessage("§e/funconnect info {nick} §7- информация об игроке");
        sender.sendMessage("§e/funconnect list §7- список донатов");
        sender.sendMessage("§e/funconnect reload §7- перезагрузить конфиг");
        sender.sendMessage("§e/funconnect help §7- показать эту помощь");
    }

    private void setFunID(CommandSender sender, String nick, String number) {
        try (Connection conn = getConnection()) {
            PreparedStatement check = conn.prepareStatement("SELECT id FROM User WHERE nick_minecraft = ?");
            check.setString(1, nick);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                PreparedStatement ps = conn.prepareStatement("UPDATE User SET donat_id = ? WHERE nick_minecraft = ?");
                ps.setString(1, number);
                ps.setString(2, nick);
                int updated = ps.executeUpdate();

                if (updated > 0) {
                    sender.sendMessage("§a✅ Установлен FunID §e" + number + " §aдля ника §f" + nick);
                } else {
                    sender.sendMessage("§c❌ Не удалось обновить FunID!");
                }
            } else {
                sender.sendMessage("§cИгрок с ником " + nick + " не найден в БД!");
            }
        } catch (SQLException e) {
            sender.sendMessage("§cОшибка подключения к БД! Смотри консоль.");
            e.printStackTrace();
        }
    }
}
