package pl.mythoscore;

/**
 * Główna klasa startowa silnika MythosCore.
 * Czyści konsolę i wyświetla kolorowy baner ASCII.
 */
public final class MythosLauncher {

    // Kolory ANSI
    private static final String ANSI_RESET   = "\u001B[0m";
    private static final String ANSI_PURPLE  = "\u001B[35m"; // fiolet
    private static final String ANSI_CYAN    = "\u001B[36m"; // cyjan
    private static final String ANSI_BOLD    = "\u001B[1m";

    private MythosLauncher() {
        // klasa narzędziowa – nie tworzymy instancji
    }

    public static void main(String[] args) {
        clearConsole();
        printBanner();

        // Tutaj w przyszłości uruchomisz hybrydowy loader:
        // FabricLoader + mostek Bukkit/Spigot
        System.out.println(ANSI_CYAN + "  [+] Inicjalizacja środowiska Fabric..." + ANSI_RESET);
        System.out.println(ANSI_CYAN + "  [+] Ładowanie warstwy Bukkit/Spigot API..." + ANSI_RESET);
        System.out.println(ANSI_PURPLE + "  [+] MythosCore gotowy do startu." + ANSI_RESET);
    }

    /**
     * Czyści konsolę (działa na większości terminali Linux/Unix oraz w Windows 10+).
     */
    private static void clearConsole() {
        System.out.print("\u001B[H\u001B[2J");
        System.out.flush();
    }

    /**
     * Wyświetla duży, kolorowy napis ASCII "MythosCore" oraz informacje o wersji.
     */
    public static void printBanner() {
        String banner = """
                ███╗   ███╗██╗   ██╗████████╗██╗  ██╗ ██████╗ ███████╗ ██████╗ ██████╗ ██████╗ ███████╗
                ████╗ ████║╚██╗ ██╔╝╚══██╔══╝██║  ██║██╔═══██╗██╔════╝██╔════╝██╔═══██╗██╔══██╗██╔════╝
                ██╔████╔██║ ╚████╔╝    ██║   ███████║██║   ██║███████╗██║     ██║   ██║██████╔╝█████╗
                ██║╚██╔╝██║  ╚██╔╝     ██║   ██╔══██║██║   ██║╚════██║██║     ██║   ██║██╔══██╗██╔══╝
                ██║ ╚═╝ ██║   ██║      ██║   ██║  ██║╚██████╔╝███████║╚██████╗╚██████╔╝██║  ██║███████╗
                ╚═╝     ╚═╝   ╚═╝      ╚═╝   ╚═╝  ╚═╝ ╚═════╝ ╚══════╝ ╚═════╝ ╚═════╝ ╚═╝  ╚═╝╚══════╝
                """;

        // Baner w fiolecie
        System.out.println(ANSI_PURPLE + ANSI_BOLD + banner + ANSI_RESET);

        // Linia informacyjna w cyjanie
        System.out.println(ANSI_CYAN + ANSI_BOLD
                + "                     MythosCore  •  Minecraft 1.20.1  •  Hybrid Engine (Fabric + Spigot/Paper)"
                + ANSI_RESET);
        System.out.println(ANSI_CYAN
                + "                                  Uruchamianie silnika..."
                + ANSI_RESET);
        System.out.println();
    }
}