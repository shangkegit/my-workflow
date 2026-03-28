package com.ruoyi.web.plugin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class PluginStorageService {

    @Value("${plugin.storage.path:./plugins}")
    private String storagePath;

    public PluginContext extractPlugin(File zipFile) throws IOException {
        PluginContext context = new PluginContext();

        String fileName = zipFile.getName().replace(".zip", "");
        Path pluginDir = Paths.get(storagePath, fileName);
        Files.createDirectories(pluginDir);
        context.setPluginDir(pluginDir.toFile());
        context.setPluginId(fileName.replace("-plugin", ""));

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = pluginDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zis, filePath);

                    String name = entry.getName();
                    if (name.startsWith("backend/") && name.endsWith(".jar")) {
                        context.setJarFile(filePath.toFile());
                    } else if (name.startsWith("frontend/") && name.endsWith(".js")) {
                        context.setFrontendFile(filePath.toFile());
                    } else if (name.startsWith("bpmn/") && name.endsWith(".xml")) {
                        context.setBpmnFile(filePath.toFile());
                    } else if (name.startsWith("database/") && name.endsWith(".sql")) {
                        context.getSqlScripts().add(new String(Files.readAllBytes(filePath)));
                    }
                }
            }
        }
        return context;
    }

    public void deletePluginDir(String pluginId) throws IOException {
        Path pluginDir = Paths.get(storagePath, pluginId + "-plugin");
        if (Files.exists(pluginDir)) {
            Files.walk(pluginDir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(path -> {
                    try { Files.delete(path); } catch (IOException e) { }
                });
        }
    }

    public String getFrontendUrl(String pluginId) {
        return "/plugins/" + pluginId + "-plugin/frontend/" + pluginId + "Form.umd.js";
    }
}
