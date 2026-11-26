package pl.mythoscore.server;

import org.yaml.snakeyaml.Yaml;
import pl.mythoscore.MythosCoreMod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MythosConfig {

    private final File file;
    private Map<String, Object> data = new HashMap<>();
    private final Yaml yaml = new Yaml();

    public MythosConfig(File file) {
        this.file = file;
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null) file.getParentFile().mkdirs();
                file.createNewFile();
                save();
            } catch (Exception e) {
                MythosCoreMod.LOGGER.error("Nie udało się utworzyć pliku: {}", file.getName(), e);
            }
            return;
        }

        try (InputStream is = new FileInputStream(file)) {
            Map<String, Object> loaded = yaml.load(is);
            if (loaded != null) {
                data = loaded;
            }
        } catch (Exception e) {
            MythosCoreMod.LOGGER.error("Błąd odczytu pliku: {}", file.getName(), e);
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(file)) {
            yaml.dump(data, writer);
        } catch (Exception e) {
            MythosCoreMod.LOGGER.error("Błąd zapisu pliku: {}", file.getName(), e);
        }
    }

    public void set(String path, Object value) {
        data.put(path, value);
        save();
    }

    public Object get(String path) {
        return data.get(path);
    }
}