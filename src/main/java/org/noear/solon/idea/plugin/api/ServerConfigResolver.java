package org.noear.solon.idea.plugin.api;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 从工程配置文件中解析服务端地址信息（端口、上下文路径）。
 *
 * <p>读取 app.yml / app.yaml / app.properties，并按 solon.env 叠加 app-{env}.yml。
 * 解析不到时回落到 Solon 默认值：端口 8080、无上下文路径。</p>
 */
public final class ServerConfigResolver {

    private static final int DEFAULT_PORT = 8080;

    public record ServerConfig(int port, String contextPath, String source) {
    }

    private ServerConfigResolver() {
    }

    public static @NotNull ServerConfig resolve(@NotNull Project project) {
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);

        VirtualFile base = findFile(project, scope, "app.yml", "app.yaml");
        if (base != null) {
            Map<String, Object> root = loadYaml(base);
            String env = asString(nested(root, "solon", "env"));
            if (env == null) {
                env = asString(nested(root, "solon", "app", "env"));
            }
            if (env != null && !env.isBlank() && !"default".equals(env)) {
                VirtualFile overlay = findFile(project, scope, "app-" + env + ".yml", "app-" + env + ".yaml");
                if (overlay != null) {
                    root = merge(root, loadYaml(overlay));
                }
            }

            int port = DEFAULT_PORT;
            Integer configuredPort = toInteger(nested(root, "server", "port"));
            if (configuredPort != null) {
                port = configuredPort;
            }
            String contextPath = asString(nested(root, "server", "contextPath"));
            if (contextPath == null) {
                contextPath = asString(nested(root, "server", "context-path"));
            }
            return new ServerConfig(port, contextPath == null ? "" : contextPath.trim(), base.getPath());
        }

        VirtualFile propertiesFile = findFile(project, scope, "app.properties");
        if (propertiesFile != null) {
            Map<String, String> properties = loadProperties(propertiesFile);
            int port = DEFAULT_PORT;
            Integer configuredPort = toInteger(properties.get("server.port"));
            if (configuredPort != null) {
                port = configuredPort;
            }
            String contextPath = properties.get("server.contextPath");
            if (contextPath == null) {
                contextPath = properties.get("server.context-path");
            }
            return new ServerConfig(port, contextPath == null ? "" : contextPath.trim(), propertiesFile.getPath());
        }

        return new ServerConfig(DEFAULT_PORT, "", "default");
    }

    private static VirtualFile findFile(@NotNull Project project,
                                        @NotNull GlobalSearchScope scope,
                                        @NotNull String @NotNull ... names) {
        for (String name : names) {
            List<VirtualFile> files = new ArrayList<>();
            for (VirtualFile file : FilenameIndex.getVirtualFilesByName(name, scope)) {
                files.add(file);
            }
            if (files.isEmpty()) {
                continue;
            }
            files.sort(Comparator
                    .comparingInt(ServerConfigResolver::preference)
                    .thenComparingInt(file -> file.getPath().length()));
            return files.get(0);
        }
        return null;
    }

    private static int preference(@NotNull VirtualFile file) {
        String path = file.getPath();
        if (path.contains("src/main/resources")) {
            return 0;
        }
        if (path.contains("/resources/")) {
            return 1;
        }
        return 2;
    }

    private static Map<String, Object> loadYaml(@NotNull VirtualFile file) {
        try {
            String text = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
            Object loaded = new Yaml().load(text);
            if (loaded instanceof Map<?, ?> map) {
                Map<String, Object> result = new LinkedHashMap<>();
                map.forEach((key, value) -> result.put(String.valueOf(key), value));
                return result;
            }
        } catch (IOException | RuntimeException ignored) {
            // 配置文件损坏时回落到默认值
        }
        return new LinkedHashMap<>();
    }

    private static Map<String, String> loadProperties(@NotNull VirtualFile file) {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            Properties properties = new Properties();
            properties.load(new StringReader(new String(file.contentsToByteArray(), StandardCharsets.UTF_8)));
            properties.forEach((key, value) -> result.put(String.valueOf(key), String.valueOf(value)));
        } catch (IOException | RuntimeException ignored) {
            // 配置文件损坏时回落到默认值
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> merge(@NotNull Map<String, Object> base, @NotNull Map<String, Object> overlay) {
        Map<String, Object> result = new LinkedHashMap<>(base);
        overlay.forEach((key, value) -> {
            Object existing = result.get(key);
            if (existing instanceof Map<?, ?> baseChild && value instanceof Map<?, ?> overlayChild) {
                result.put(key, merge((Map<String, Object>) baseChild, (Map<String, Object>) overlayChild));
            } else {
                result.put(key, value);
            }
        });
        return result;
    }

    private static Object nested(@NotNull Map<String, Object> root, @NotNull String @NotNull ... keys) {
        Object current = root;
        for (String key : keys) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(key);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private static Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.valueOf(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
