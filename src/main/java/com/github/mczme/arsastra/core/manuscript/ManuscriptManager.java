package com.github.mczme.arsastra.core.manuscript;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理客户端本地手稿文件的加载与保存。
 */
@OnlyIn(Dist.CLIENT)
public class ManuscriptManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManuscriptManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ManuscriptManager instance;

    private String currentSeedHash = "default";
    private final List<ClientManuscript> loadedManuscripts = new ArrayList<>();

    private ManuscriptManager() {}

    public static ManuscriptManager getInstance() {
        if (instance == null) {
            instance = new ManuscriptManager();
        }
        return instance;
    }

    public void setCurrentSeedHash(String hash) {
        if (!this.currentSeedHash.equals(hash)) {
            this.currentSeedHash = hash;
            loadAll();
        }
    }

    public List<ClientManuscript> getManuscripts() {
        return loadedManuscripts;
    }

    /**
     * 加载当前种子目录下的所有手稿。
     */
    public void loadAll() {
        this.loadedManuscripts.clear();
        Path basePath = getStoragePath();
        File dir = basePath.toFile();

        if (!dir.exists()) {
            dir.mkdirs();
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                JsonElement json = GSON.fromJson(reader, JsonElement.class);
                ClientManuscript.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(LOGGER::error)
                        .ifPresent(loadedManuscripts::add);
            } catch (IOException e) {
                LOGGER.error("Failed to load manuscript: " + file.getName(), e);
            }
        }
        LOGGER.info("Loaded {} manuscripts for seed {}", loadedManuscripts.size(), currentSeedHash);
    }

    private String sanitize(String name) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    /**
     * 保存一份手稿到本地。
     */
    public void saveManuscript(ClientManuscript manuscript) {
        Path basePath = getStoragePath();
        File dir = basePath.toFile();
        if (!dir.exists()) dir.mkdirs();

        String fileName = sanitize(manuscript.name()) + ".json";
        File file = new File(dir, fileName);

        JsonElement json = ClientManuscript.CODEC.encodeStart(JsonOps.INSTANCE, manuscript)
                .getOrThrow();

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(json, writer);
            // 更新内存列表
            loadedManuscripts.removeIf(m -> m.name().equals(manuscript.name()));
            loadedManuscripts.add(manuscript);
        } catch (IOException e) {
            LOGGER.error("Failed to save manuscript: " + fileName, e);
        }
    }

    public void deleteManuscript(String name) {
        Path basePath = getStoragePath();
        String fileName = sanitize(name) + ".json";
        File file = basePath.resolve(fileName).toFile();

        if (file.exists() && file.delete()) {
            loadedManuscripts.removeIf(m -> m.name().equals(name));
        }
    }

    private Path getStoragePath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("aamanuscripts")
                .resolve(currentSeedHash);
    }
}