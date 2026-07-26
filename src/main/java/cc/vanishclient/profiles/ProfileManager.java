package cc.vanishclient.profiles;

import cc.vanishclient.VanishClient;
import cc.vanishclient.module.Module;
import cc.vanishclient.module.ModuleManager;
import cc.vanishclient.module.setting.BooleanSetting;
import cc.vanishclient.module.setting.ColorSetting;
import cc.vanishclient.module.setting.KeybindSetting;
import cc.vanishclient.module.setting.ModeSetting;
import cc.vanishclient.module.setting.NumberSetting;
import cc.vanishclient.module.setting.RangeSetting;
import cc.vanishclient.module.setting.Setting;
import cc.vanishclient.module.setting.StringSetting;
import cc.vanishclient.utils.mc.ChatUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ProfileManager {
    private static final String DEFAULT_PROFILE = "Default";
    private static final String FORMAT = "vanishclient-profile";
    private static final String LEGACY_FORMAT = "motionblurr-profile";
    private static final String LEGACY_CONFIG_DIR = "motionblurr";

    private final ModuleManager moduleManager = VanishClient.INSTANCE != null ? VanishClient.INSTANCE.getModuleManager() : null;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve(VanishClient.MOD_ID).normalize();
    private final Path profilePath = configPath.resolve("profiles").normalize();
    @Getter
    private String activeProfile = DEFAULT_PROFILE;
    @Getter
    private boolean dirty;

    public ProfileManager() {
        migrateLegacyProfilesIfNeeded();
        createProfileDirectoryIfNeeded();
        ensureDefaultProfile();
    }

    public File getProfileDir() {
        return profilePath.toFile();
    }

    public Path getProfilePath() {
        return profilePath;
    }

    private void createProfileDirectoryIfNeeded() {
        try {
            Files.createDirectories(profilePath);
        } catch (IOException e) {
            ChatUtil.addChatMessage("Failed to create profile directory: " + profilePath);
        }
    }

    private void migrateLegacyProfilesIfNeeded() {
        Path legacyProfilePath = FabricLoader.getInstance().getConfigDir().resolve(LEGACY_CONFIG_DIR).resolve("profiles").normalize();
        if (!Files.isDirectory(legacyProfilePath)) return;

        int copied = 0;
        int skipped = 0;
        try (var paths = Files.walk(legacyProfilePath)) {
            for (Path source : paths.filter(Files::isRegularFile).toList()) {
                Path relative = legacyProfilePath.relativize(source);
                Path destination = profilePath.resolve(relative).normalize();
                if (!destination.startsWith(profilePath)) {
                    skipped++;
                    continue;
                }
                if (Files.exists(destination)) {
                    skipped++;
                    continue;
                }
                Files.createDirectories(destination.getParent());
                Files.copy(source, destination);
                copied++;
            }
            if (copied > 0) {
                VanishClient.INSTANCE.getLogger().info("Migrated {} legacy MotionBlurr profile file(s) to {}.", copied, profilePath);
            } else if (skipped > 0) {
                VanishClient.INSTANCE.getLogger().info("Legacy MotionBlurr profiles already exist in VanishClient config; skipped {} file(s).", skipped);
            }
        } catch (IOException | RuntimeException e) {
            VanishClient.INSTANCE.getLogger().warn("Failed to migrate legacy MotionBlurr profiles from {} to {}.", legacyProfilePath, profilePath, e);
        }
    }

    private void ensureDefaultProfile() {
        if (moduleManager == null) return;
        try {
            Files.createDirectories(profilePath);
            Path defaultFile = resolveProfileFile(DEFAULT_PROFILE);
            if (!Files.exists(defaultFile)) {
                writeProfileToFile(defaultFile);
            }
        } catch (IOException e) {
            ChatUtil.addChatMessage("Failed to create Default profile: " + e.getMessage());
        }
    }

    public List<String> listProfiles() {
        createProfileDirectoryIfNeeded();
        ensureDefaultProfile();
        List<String> profiles = new ArrayList<>();
        try (var stream = Files.list(profilePath)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .map(path -> path.getFileName().toString())
                    .map(name -> name.substring(0, name.length() - 5))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(profiles::add);
        } catch (IOException e) {
            ChatUtil.addChatMessage("Failed to list profiles: " + e.getMessage());
        }
        if (profiles.stream().noneMatch(DEFAULT_PROFILE::equalsIgnoreCase)) {
            profiles.add(DEFAULT_PROFILE);
            profiles.sort(String.CASE_INSENSITIVE_ORDER);
        }
        return profiles;
    }

    public boolean isValidProfileName(String profileName) {
        String sanitized = sanitizeProfileName(profileName);
        return sanitized != null && sanitized.equals(profileName.trim());
    }

    public String sanitizeProfileName(String profileName) {
        if (profileName == null) return null;
        String trimmed = profileName.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.contains("..")) return null;
        for (char c : trimmed.toCharArray()) {
            if ("\\/:*?\"<>|".indexOf(c) >= 0 || Character.isISOControl(c)) {
                return null;
            }
        }
        return trimmed;
    }

    public boolean profileExists(String profileName) {
        String safe = sanitizeProfileName(profileName);
        return safe != null && Files.exists(resolveProfileFile(safe));
    }

    public boolean createProfile(String profileName) {
        String safe = sanitizeProfileName(profileName);
        if (safe == null) {
            ChatUtil.addChatMessage("Invalid profile name.");
            return false;
        }
        Path file = resolveProfileFile(safe);
        if (Files.exists(file)) {
            ChatUtil.addChatMessage("Profile already exists.");
            return false;
        }
        try {
            Files.createDirectories(profilePath);
            writeProfileToFile(file);
            activeProfile = safe;
            dirty = false;
            ChatUtil.addChatMessage("Profile created: " + safe);
            return true;
        } catch (IOException e) {
            ChatUtil.addChatMessage("Failed to create profile: " + e.getMessage());
            return false;
        }
    }

    public boolean saveActiveProfile() {
        return saveProfile(activeProfile, true);
    }

    public void saveProfile(final String profileName) {
        saveProfile(profileName, false);
    }

    public boolean saveProfile(final String profileName, final boolean forceOverride) {
        String safe = sanitizeProfileName(profileName);
        if (safe == null) {
            ChatUtil.addChatMessage("Invalid profile name.");
            return false;
        }
        Path file = resolveProfileFile(safe);
        if (Files.exists(file) && !forceOverride) {
            ChatUtil.addChatMessage("Profile already exists.");
            return false;
        }
        try {
            Files.createDirectories(profilePath);
            writeProfileToFile(file);
            activeProfile = safe;
            dirty = false;
            ChatUtil.addChatMessage("Profile saved: " + safe);
            return true;
        } catch (IOException e) {
            ChatUtil.addChatMessage("Failed to save profile: " + e.getMessage());
            return false;
        }
    }

    public boolean loadProfile(final String profileName) {
        String safe = sanitizeProfileName(profileName);
        if (safe == null) {
            ChatUtil.addChatMessage("Invalid profile name.");
            return false;
        }
        Path file = resolveProfileFile(safe);
        if (!Files.exists(file)) {
            ChatUtil.addChatMessage("Profile not found: " + safe);
            return false;
        }
        try {
            JsonObject root = readProfileRoot(file);
            JsonObject modules = getModulesObject(root);
            if (modules == null) {
                ChatUtil.addChatMessage("Invalid VanishClient profile: " + safe);
                return false;
            }
            applyProfile(modules);
            activeProfile = safe;
            dirty = false;
            ChatUtil.addChatMessage("Profile loaded: " + safe);
            return true;
        } catch (IOException | RuntimeException e) {
            ChatUtil.addChatMessage("Failed to load profile: " + e.getMessage());
            return false;
        }
    }

    public boolean importProfile(String sourcePathText) {
        if (sourcePathText == null || sourcePathText.isBlank()) {
            ChatUtil.addChatMessage("Enter a JSON path to import.");
            return false;
        }
        Path source = Path.of(sourcePathText.trim().replace("\"", ""));
        if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
            ChatUtil.addChatMessage("Profile import file is not readable.");
            return false;
        }
        try {
            JsonObject root = readProfileRoot(source);
            if (getModulesObject(root) == null) {
                ChatUtil.addChatMessage("Invalid VanishClient profile.");
                return false;
            }
            String baseName = source.getFileName().toString();
            if (baseName.toLowerCase(Locale.ROOT).endsWith(".json")) {
                baseName = baseName.substring(0, baseName.length() - 5);
            }
            String safeName = sanitizeProfileName(baseName);
            if (safeName == null) safeName = "Imported";
            safeName = duplicateSafeName(safeName);
            Path destination = resolveProfileFile(safeName);
            Files.createDirectories(profilePath);
            writeImportedProfile(destination, root);
            activeProfile = safeName;
            dirty = false;
            ChatUtil.addChatMessage("Imported profile: " + safeName);
            return true;
        } catch (IOException | RuntimeException e) {
            ChatUtil.addChatMessage("Failed to import profile: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteProfile(String profileName) {
        String safe = sanitizeProfileName(profileName);
        if (safe == null) return false;
        Path file = resolveProfileFile(safe);
        if (!file.startsWith(profilePath) || !Files.exists(file)) {
            return false;
        }
        try {
            Files.delete(file);
            List<String> remaining = listProfiles();
            if (remaining.isEmpty()) {
                ensureDefaultProfile();
                remaining = listProfiles();
            }
            if (safe.equalsIgnoreCase(activeProfile)) {
                String next = remaining.stream().filter(Objects::nonNull).min(String.CASE_INSENSITIVE_ORDER).orElse(DEFAULT_PROFILE);
                if (!Files.exists(resolveProfileFile(next))) {
                    saveProfile(DEFAULT_PROFILE, true);
                } else {
                    loadProfile(next);
                }
            }
            ensureDefaultProfile();
            dirty = false;
            ChatUtil.addChatMessage("Deleted profile: " + safe);
            return true;
        } catch (IOException e) {
            ChatUtil.addChatMessage("Failed to delete profile: " + e.getMessage());
            return false;
        }
    }

    public void markDirty() {
        dirty = true;
    }

    public void clearDirty() {
        dirty = false;
    }

    private String duplicateSafeName(String baseName) {
        String name = baseName;
        int index = 2;
        while (Files.exists(resolveProfileFile(name))) {
            name = baseName + " " + index++;
        }
        return name;
    }

    private Path resolveProfileFile(String profileName) {
        String safe = sanitizeProfileName(profileName);
        if (safe == null) safe = DEFAULT_PROFILE;
        return profilePath.resolve(safe + ".json").normalize();
    }

    private JsonObject readProfileRoot(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                throw new IOException("JSON root must be an object.");
            }
            return parsed.getAsJsonObject();
        }
    }

    private JsonObject getModulesObject(JsonObject root) {
        if (root == null) return null;
        String format = getString(root, "format");
        if ((FORMAT.equals(format) || LEGACY_FORMAT.equals(format)) && root.has("modules") && root.get("modules").isJsonObject()) {
            return root.getAsJsonObject("modules");
        }
        if (root.has("modules") && root.get("modules").isJsonObject()) {
            return root.getAsJsonObject("modules");
        }
        return root;
    }

    private void writeProfileToFile(Path profileFile) throws IOException {
        Path normalized = profileFile.normalize();
        if (!normalized.startsWith(profilePath)) {
            throw new IOException("Refusing to write outside profile directory.");
        }
        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        root.addProperty("version", 1);
        root.addProperty("activeProfile", normalized.getFileName().toString().replaceFirst("(?i)\\.json$", ""));
        root.add("modules", captureModules());
        writeJsonAtomically(normalized, root);
    }

    private void writeImportedProfile(Path destination, JsonObject root) throws IOException {
        Path normalized = destination.normalize();
        if (!normalized.startsWith(profilePath)) {
            throw new IOException("Refusing to import outside profile directory.");
        }
        writeJsonAtomically(normalized, root);
    }

    private void writeJsonAtomically(Path destination, JsonObject root) throws IOException {
        Files.createDirectories(profilePath);
        Path temp = Files.createTempFile(profilePath, destination.getFileName().toString(), ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            gson.toJson(root, writer);
        }
        try {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailure) {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private JsonObject captureModules() {
        JsonObject modules = new JsonObject();
        if (moduleManager == null || moduleManager.getModules() == null) return modules;
        for (Module module : moduleManager.getModules()) {
            if (module == null || module.getName() == null) continue;
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("enabled", module.isEnabled());
            int bind = module.getKeybindSetting() != null ? module.getKeybindSetting().getKeyCode() : module.getKey();
            moduleJson.addProperty("bind", bind);
            JsonObject settings = new JsonObject();
            for (Setting setting : module.getSettings()) {
                saveSettingValue(setting, settings);
            }
            moduleJson.add("settings", settings);
            modules.add(module.getName(), moduleJson);
        }
        return modules;
    }

    private void applyProfile(JsonObject modules) {
        if (moduleManager == null || moduleManager.getModules() == null) return;
        for (Module module : moduleManager.getModules()) {
            if (module == null || module.getName() == null || !modules.has(module.getName())) continue;
            JsonElement moduleElement = modules.get(module.getName());
            if (moduleElement == null || !moduleElement.isJsonObject()) continue;
            loadModuleSettings(module, moduleElement.getAsJsonObject());
        }
    }

    private void loadModuleSettings(final Module module, final JsonObject moduleJson) {
        if (moduleJson.has("enabled")) {
            module.setEnabled(moduleJson.get("enabled").getAsBoolean());
        }
        int bind = moduleJson.has("bind") ? moduleJson.get("bind").getAsInt() : module.getKey();
        module.setKey(bind);
        if (module.getKeybindSetting() != null) {
            module.getKeybindSetting().setKeyCode(bind);
        }

        JsonObject settingsJson = moduleJson.has("settings") && moduleJson.get("settings").isJsonObject()
                ? moduleJson.getAsJsonObject("settings")
                : moduleJson;
        for (Setting setting : module.getSettings()) {
            loadSettingValue(setting, settingsJson);
        }
    }

    private void loadSettingValue(final Setting setting, final JsonObject settingsJson) {
        if (setting == null || settingsJson == null || setting.getName() == null || !settingsJson.has(setting.getName())) return;
        JsonElement element = settingsJson.get(setting.getName());
        if (element == null || element.isJsonNull()) return;

        try {
            switch (setting) {
                case BooleanSetting booleanSetting -> booleanSetting.setValue(element.getAsBoolean());
                case NumberSetting numberSetting -> numberSetting.setValue(element.getAsDouble());
                case RangeSetting rangeSetting -> {
                    if (element.isJsonObject()) {
                        JsonObject rangeObj = element.getAsJsonObject();
                        double minValue = rangeObj.has("min") ? rangeObj.get("min").getAsDouble() : rangeSetting.getMinValue();
                        double maxValue = rangeObj.has("max") ? rangeObj.get("max").getAsDouble() : rangeSetting.getMaxValue();
                        rangeSetting.setRange(minValue, maxValue);
                    }
                }
                case ModeSetting modeSetting -> modeSetting.setMode(element.getAsString());
                case KeybindSetting keybindSetting -> keybindSetting.setKeyCode(element.getAsInt());
                case StringSetting stringSetting -> stringSetting.setValue(element.getAsString());
                case ColorSetting colorSetting -> loadColor(colorSetting, element);
                default -> {
                }
            }
        } catch (RuntimeException ex) {
            ChatUtil.addChatMessage("Skipped setting '" + setting.getName() + "' while loading profile.");
        }
    }

    private void saveSettingValue(final Setting setting, final JsonObject settingsJson) {
        if (setting == null || settingsJson == null || setting.getName() == null) return;
        switch (setting) {
            case BooleanSetting booleanSetting -> settingsJson.addProperty(setting.getName(), booleanSetting.getValue());
            case NumberSetting numberSetting -> settingsJson.addProperty(setting.getName(), numberSetting.getValue());
            case RangeSetting rangeSetting -> {
                JsonObject rangeObj = new JsonObject();
                rangeObj.addProperty("min", rangeSetting.getMinValue());
                rangeObj.addProperty("max", rangeSetting.getMaxValue());
                settingsJson.add(setting.getName(), rangeObj);
            }
            case ModeSetting modeSetting -> settingsJson.addProperty(setting.getName(), modeSetting.getMode());
            case KeybindSetting keybindSetting -> settingsJson.addProperty(setting.getName(), keybindSetting.getKeyCode());
            case StringSetting stringSetting -> settingsJson.addProperty(setting.getName(), stringSetting.getValue());
            case ColorSetting colorSetting -> {
                String hex = String.format("#%02X%02X%02X", colorSetting.getRed(), colorSetting.getGreen(), colorSetting.getBlue());
                if (colorSetting.isHasAlpha()) hex += String.format("%02X", colorSetting.getAlpha());
                settingsJson.addProperty(setting.getName(), hex);
            }
            default -> {
            }
        }
    }

    private void loadColor(ColorSetting colorSetting, JsonElement element) {
        if (!element.isJsonPrimitive()) return;
        if (element.getAsJsonPrimitive().isNumber()) {
            int argb = element.getAsInt();
            int a = (argb >> 24) & 0xFF;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            if (colorSetting.isHasAlpha()) colorSetting.setValue(r, g, b, a);
            else colorSetting.setValue(r, g, b);
            return;
        }
        String hex = element.getAsString().trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() < 6) return;
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        if (colorSetting.isHasAlpha() && hex.length() >= 8) {
            int a = Integer.parseInt(hex.substring(6, 8), 16);
            colorSetting.setValue(r, g, b, a);
        } else {
            colorSetting.setValue(r, g, b);
        }
    }

    private String getString(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }

    public void resetProfile() {
        if (moduleManager == null || moduleManager.getModules() == null) return;
        moduleManager.getModules().stream()
                .sorted(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(module -> module.setEnabled(false));
    }
}
