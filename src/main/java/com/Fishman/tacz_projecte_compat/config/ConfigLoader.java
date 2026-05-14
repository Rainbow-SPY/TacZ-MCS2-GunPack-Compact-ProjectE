package com.Fishman.tacz_projecte_compat.config;

import com.Fishman.tacz_projecte_compat.TaczProjectECompat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigLoader {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final String CONFIG_FILE_NAME = "guns.json";

    // This file must exist in:
    // src/main/resources/assets/tacz_projecte_compat/default_guns.json
    private static final String DEFAULT_RESOURCE_PATH =
            "assets/" + TaczProjectECompat.MODID + "/default_guns.json";

    // Safety fallback. If you forget to embed default_guns.json, the mod still creates a small config.
    private static final String FALLBACK_DEFAULT_JSON = """
            {
              "guns": [
                {
                  "itemId": "tacz:modern_kinetic_gun",
                  "gunId": "mcs2:cs_usp",
                  "basePrice": 200,
                  "weaponScale": 1000,
                  "ammoEmc": 50,
                  "maxAmmo": 12,
                  "countBarrelBullet": false,
                  "attachmentsPrice": 0,
                  "fireMode": "SEMI",
                  "attachments": [
                    {
                      "slotName": "AttachmentMUZZLE",
                      "attachmentItemId": "tacz:attachment",
                      "attachmentId": "mcs2:usp_silencer"
                    }
                  ]
                }
              ]
            }
            """;

    private ConfigLoader() {
    }

    public static GunRuleConfig loadOrCreate() {
        Path configPath = FMLPaths.CONFIGDIR.get()
                .resolve(TaczProjectECompat.MODID)
                .resolve(CONFIG_FILE_NAME);

        try {
            Files.createDirectories(configPath.getParent());

            if (!Files.exists(configPath)) {
                createDefaultConfig(configPath);
            }

            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                GunRuleConfig config = GSON.fromJson(reader, GunRuleConfig.class);

                if (config == null) {
                    LOGGER.warn("TacZ ProjectE Compat: config file is empty: {}", configPath);
                    return new GunRuleConfig();
                }

                return config;
            }
        } catch (Exception e) {
            LOGGER.error("TacZ ProjectE Compat: failed to load config {}", configPath, e);
            return new GunRuleConfig();
        }
    }

    private static void createDefaultConfig(Path targetPath) {
        ClassLoader classLoader = ConfigLoader.class.getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
            if (inputStream != null) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("TacZ ProjectE Compat: created default config from embedded resource: {}", targetPath);
                return;
            }

            Files.writeString(
                    targetPath,
                    FALLBACK_DEFAULT_JSON,
                    StandardCharsets.UTF_8
            );

            LOGGER.warn(
                    "TacZ ProjectE Compat: embedded default config not found at {}, wrote fallback config instead: {}",
                    DEFAULT_RESOURCE_PATH,
                    targetPath
            );
        } catch (Exception e) {
            LOGGER.error("TacZ ProjectE Compat: failed to create default config {}", targetPath, e);
        }
    }
}
