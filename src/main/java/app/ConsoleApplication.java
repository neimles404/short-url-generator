package app;

import exception.*;
import model.ShortLink;
import model.UserProfile;
import repository.FileJsonShortLinkRepository;
import repository.FileJsonUserRepository;
import repository.ShortLinkRepository;
import repository.UserRepository;
import service.ExpirationCleanupService;
import service.UrlShortenerService;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import config.AppConfig;
import exception.ConfigException;
import service.UserService;

public class ConsoleApplication {

    private final UrlShortenerService service;
    private UUID currentUserId;
    private final AppConfig config;
    private final UserService userService;

    public ConsoleApplication(UrlShortenerService service, UserService userService, AppConfig config) {
        this.service = service;
        this.userService = userService;
        this.config = config;
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Сервис сокращения ссылок ===");
        System.out.println("1) Ввести существующий UUID пользователя");
        System.out.println("2) Создать нового пользователя");
        System.out.println("0) Выход из приложения");

        UserProfile user = null;
        while (user == null) {
            System.out.print("Выберите пункт (0, 1 или 2): ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.print("Введите UUID: ");
                    String uuidStr = scanner.nextLine().trim();
                    try {
                        UUID enteredId = UUID.fromString(uuidStr);
                        var optUser = userService.findUser(enteredId);
                        if (optUser.isPresent()) {
                            user = optUser.get();
                            System.out.println("✅ Пользователь найден. UUID: " + user.getId());
                            System.out.println("   Лимит переходов по умолчанию: " + user.getDefaultMaxClicks());
                            System.out.println("   TTL ссылок (часов): " + user.getTtlHours());
                        } else {
                            System.out.println("⚠️ Пользователь с таким UUID не найден в базе.");
                            System.out.println("   Вы можете выбрать пункт 2, чтобы создать нового пользователя.");
                        }
                    } catch (IllegalArgumentException e) {
                        System.out.println("❌ Некорректный формат UUID. Попробуйте снова или выберите пункт 2.");
                    } catch (DataAccessException e) {
                        System.out.println("❌ Ошибка доступа к базе пользователей: " + e.getMessage());
                    }
                }
                case "2" -> {
                    try {
                        user = userService.createNewUser();
                        System.out.println("✅ Создан новый пользователь. Ваш UUID: " + user.getId());
                        System.out.println("💾 Сохраните его, чтобы в следующий раз работать со своими ссылками.");
                        System.out.println("   Лимит переходов по умолчанию: " + user.getDefaultMaxClicks());
                        System.out.println("   TTL ссылок (часов): " + user.getTtlHours());
                    } catch (DataAccessException e) {
                        System.out.println("❌ Не удалось создать пользователя: " + e.getMessage());
                    }
                }
                case "0" -> {
                    System.out.println("👋 Выход из приложения...");
                    return;
                }
                case "" -> System.out.println("⚠️ Пустой ввод. Введите 0, 1 или 2.");
                default -> System.out.println("❗ Неизвестная команда. Введите 0, 1 или 2.");
            }
        }
        currentUserId = user.getId();

        while (true) {
            printMenu();
            System.out.print("Введите номер команды: ");
            String cmd = scanner.nextLine().trim();

            switch (cmd) {
                case "1" -> handleCreateLink(scanner);
                case "2" -> handleOpenLink(scanner);
                case "3" -> handleListLinks();
                case "4" -> handleDeleteLink(scanner);
                case "5" -> handleEditUserSettings(scanner);
                case "0" -> {
                    System.out.println("👋 До встречи!");
                    return;
                }
                case "" -> System.out.println("⚠️ Пустой ввод. Пожалуйста, выберите пункт меню.");
                default -> System.out.println("❗ Неизвестная команда. Введите число от 0 до 5.");
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("Меню:");
        System.out.println("1) Создать короткую ссылку");
        System.out.println("2) Перейти по короткой ссылке");
        System.out.println("3) Показать мои ссылки");
        System.out.println("4) Удалить мою ссылку");
        System.out.println("5) Изменить мои настройки (лимит и TTL)");
        System.out.println("0) Выход");
    }

    private void handleCreateLink(Scanner scanner) {
        try {
            System.out.print("Введите длинный URL: ");
            String url = scanner.nextLine().trim();

            ShortLink link = service.createShortLink(currentUserId, url);
            System.out.println("Короткая ссылка создана!");
            System.out.println("Код: " + link.getShortCode());
            System.out.println("Полная короткая ссылка: " +
                    config.baseShortUrl() + "/" + link.getShortCode());
            System.out.println("Лимит переходов по этой ссылке: " + link.getMaxClicks());
            System.out.println("Истекает: " + link.getExpiresAt());
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        } catch (DataAccessException e) {
            System.out.println("Ошибка работы с базой данных: " + e.getMessage());
        }
    }


    private void handleOpenLink(Scanner scanner) {
        System.out.print("Введите короткий код: ");
        String code = scanner.nextLine().trim();
        try {
            String url = service.resolveShortLink(code);
            System.out.println("Переход по ссылке: " + url);
            openInBrowser(url);
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void handleListLinks() {
        try {
            List<ShortLink> links = service.getUserLinks(currentUserId);
            if (links.isEmpty()) {
                System.out.println("У вас пока нет ссылок.");
                return;
            }
            for (ShortLink l : links) {
                System.out.printf("Код: %s | URL: %s | %d/%d | Активна: %s | Истекает: %s%n",
                        l.getShortCode(), l.getOriginalUrl(),
                        l.getClickCount(), l.getMaxClicks(),
                        l.isActive(), l.getExpiresAt());
            }
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void handleDeleteLink(Scanner scanner) {
        System.out.print("Введите короткий код: ");
        String code = scanner.nextLine().trim();
        try {
            service.deleteUserLink(currentUserId, code);
            System.out.println("Ссылка удалена.");
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void handleEditUserSettings(Scanner scanner) {
        try {
            UserProfile user = userService.getRequiredUser(currentUserId);
            System.out.println("Текущие настройки:");
            System.out.println("  Лимит переходов по умолчанию: " + user.getDefaultMaxClicks());
            System.out.println("  TTL ссылок (часов): " + user.getTtlHours());

            System.out.println("Введите новый лимит переходов по умолчанию " +
                    "(или пусто, чтобы оставить " + user.getDefaultMaxClicks() + "): ");
            String limitStr = scanner.nextLine().trim();

            int newLimit = user.getDefaultMaxClicks();
            if (!limitStr.isEmpty()) {
                newLimit = Integer.parseInt(limitStr);
            }

            System.out.println("Введите новый TTL ссылок в часах " +
                    "(или пусто, чтобы оставить " + user.getTtlHours() + "): ");
            String ttlStr = scanner.nextLine().trim();

            long newTtl = user.getTtlHours();
            if (!ttlStr.isEmpty()) {
                newTtl = Long.parseLong(ttlStr);
            }

            userService.updateUserSettings(currentUserId, newLimit, newTtl);
            System.out.println("✅ Настройки обновлены.");

        } catch (NumberFormatException e) {
            System.out.println("Ошибка: ожидалось целое число. Настройки не изменены.");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        } catch (DataAccessException e) {
            System.out.println("Ошибка работы с базой пользователей: " + e.getMessage());
        }
    }

    private void openInBrowser(String url) throws IOException, URISyntaxException {
        if (!Desktop.isDesktopSupported()) {
            System.out.println("Откройте URL вручную: " + url);
            return;
        }
        Desktop.getDesktop().browse(new URI(url));
    }

    public static void main(String[] args) {
        ExpirationCleanupService cleanupService = null;
        try {
            AppConfig config = AppConfig.loadDefault();

            ShortLinkRepository linkRepository = new FileJsonShortLinkRepository(config.dbFilePath());
            UserRepository userRepository = new FileJsonUserRepository(config.usersDbFilePath());

            UserService userService = new UserService(userRepository, config);
            UrlShortenerService urlService = new UrlShortenerService(linkRepository, config, userRepository);

            cleanupService = new ExpirationCleanupService(urlService);
            cleanupService.start();

            ConsoleApplication app = new ConsoleApplication(urlService, userService, config);
            app.run();

        } catch (ConfigException e) {
            System.out.println("Ошибка конфигурации: " + e.getMessage());
        } catch (DataAccessException e) {
            System.out.println("Ошибка работы с базой данных: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Неожиданная ошибка: " + e.getMessage());
        } finally {
            if (cleanupService != null) {
                cleanupService.stop();
            }
        }
    }

}
