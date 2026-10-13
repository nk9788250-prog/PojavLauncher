package net.kdt.pojavlaunch.quartrs;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class QuartrsOptimizerEngine {

    private static final String TAG = "QuartrsEngine";
    private static final String PREF_QUARTRS_MODE = "quartrs_graphics_mode";

    public enum GraphicsMode {
        AUTO,
        SMOOTH,
        STANDARD,
        ULTRA,
        RAY_TRACING
    }

    /**
     * Device ki total RAM (GB me) detect karta hai.
     */
    public static double getDeviceRamGB(Context context) {
        ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        return (double) memInfo.totalMem / (1024.0 * 1024.0 * 1024.0);
    }

    /**
     * App start hote hi ya Play dabate hi RAM dekh kar best preset aur settings apply karta hai.
     */
    public static GraphicsMode applyHardwareOptimization(Context context, File minecraftDir, GraphicsMode userSelectedMode) {
        double totalRamGB = getDeviceRamGB(context);
        GraphicsMode finalMode = userSelectedMode;

        int allocatedRamMB;
        int resolutionRatio;
        String rendererName;

        // 1. Auto-RAM Detection (2GB, 3GB, 4GB, 6GB, 8GB, 12GB)
        if (totalRamGB <= 2.3) {
            // 2GB RAM Device -> Force Ultra Low Smooth Mode
            allocatedRamMB = 650;
            resolutionRatio = 55;
            rendererName = "opengles2"; // Holy-GL4ES fast renderer
            if (finalMode == GraphicsMode.AUTO) finalMode = GraphicsMode.SMOOTH;
        } else if (totalRamGB <= 3.3) {
            // 3GB RAM Device
            allocatedRamMB = 1100;
            resolutionRatio = 65;
            rendererName = "opengles2";
            if (finalMode == GraphicsMode.AUTO) finalMode = GraphicsMode.SMOOTH;
        } else if (totalRamGB <= 4.5) {
            // 4GB RAM Device
            allocatedRamMB = 1750;
            resolutionRatio = 75;
            rendererName = "opengles3_mobileglues";
            if (finalMode == GraphicsMode.AUTO) finalMode = GraphicsMode.STANDARD;
        } else if (totalRamGB <= 6.5) {
            // 6GB RAM Device
            allocatedRamMB = 2900;
            resolutionRatio = 85;
            rendererName = "opengles3_mobileglues";
            if (finalMode == GraphicsMode.AUTO) finalMode = GraphicsMode.ULTRA;
        } else if (totalRamGB <= 8.5) {
            // 8GB RAM Device
            allocatedRamMB = 4096;
            resolutionRatio = 100;
            rendererName = "opengles3_mobileglues";
            if (finalMode == GraphicsMode.AUTO) finalMode = GraphicsMode.RAY_TRACING;
        } else {
            // 12GB+ RAM Device
            allocatedRamMB = 6144;
            resolutionRatio = 100;
            rendererName = "opengles3_mobileglues";
            if (finalMode == GraphicsMode.AUTO) finalMode = GraphicsMode.RAY_TRACING;
        }

        // Agar user ne khud Ray Tracing chuna hai, toh MobileGlues renderer force karein
        if (finalMode == GraphicsMode.RAY_TRACING) {
            rendererName = "opengles3_mobileglues";
        }

        // 2. Fast Boot & Zero-Lag JVM Arguments
        String quartrsJvmArgs = "-XX:+UseG1GC -XX:+ParallelRefProcEnabled " +
                "-XX:MaxGCPauseMillis=100 -XX:+UnlockExperimentalVMOptions " +
                "-XX:+DisableExplicitGC -XX:+AlwaysPreTouch " +
                "-XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 " +
                "-XX:G1HeapRegionSize=16M -Dfml.earlyprogresswindow=false";

        // 3. PojavLauncher ke Internal Preferences me save karein
        SharedPreferences.Editor editor = LauncherPreferences.DEFAULT_PREF.edit();
        editor.putInt("allocation", allocatedRamMB);
        editor.putInt("resolutionRatio", resolutionRatio);
        editor.putString("renderer", rendererName);
        editor.putString("javaArgs", quartrsJvmArgs);
        editor.putString(PREF_QUARTRS_MODE, finalMode.name());
        editor.apply();

        // 4. Minecraft ki Graphics aur Shader Config files likhein
        writeMinecraftOptions(minecraftDir, finalMode);
        writeSodiumOptions(minecraftDir, finalMode);
        writeIrisShaderProperties(minecraftDir, finalMode);

        Log.i(TAG, "Quartrs Applied -> RAM: " + allocatedRamMB + "MB | Mode: " + finalMode.name());
        return finalMode;
    }

    /**
     * Minecraft ki main options.txt ko Graphics Mode ke hisab se set karta hai.
     */
    private static void writeMinecraftOptions(File mcDir, GraphicsMode mode) {
        if (!mcDir.exists()) mcDir.mkdirs();
        File optionsFile = new File(mcDir, "options.txt");

        int renderDistance = 4;
        int simDistance = 5;
        String graphicsMode = "0"; // 0 = Fast, 1 = Fancy
        String ao = "false"; // Smooth lighting
        String clouds = "false";
        String particles = "2"; // 2 = Minimal, 1 = Decreased, 0 = All
        String entityShadows = "false";
        int biomeBlend = 0;

        switch (mode) {
            case SMOOTH:
                renderDistance = 4;
                simDistance = 5;
                graphicsMode = "0";
                ao = "false";
                clouds = "false";
                particles = "2";
                entityShadows = "false";
                biomeBlend = 0;
                break;
            case STANDARD:
                renderDistance = 8;
                simDistance = 6;
                graphicsMode = "1";
                ao = "true";
                clouds = "fast";
                particles = "1";
                entityShadows = "true";
                biomeBlend = 2;
                break;
            case ULTRA:
                renderDistance = 10;
                simDistance = 8;
                graphicsMode = "1";
                ao = "true";
                clouds = "true";
                particles = "0";
                entityShadows = "true";
                biomeBlend = 3;
                break;
            case RAY_TRACING:
                renderDistance = 8; // 8 chunks keeps 100+ FPS with RTX Reflections
                simDistance = 6;
                graphicsMode = "1";
                ao = "true";
                clouds = "true";
                particles = "0";
                entityShadows = "true";
                biomeBlend = 4;
                break;
            default:
                break;
        }

        String optionsContent = "version:3700\n" +
                "renderDistance:" + renderDistance + "\n" +
                "simulationDistance:" + simDistance + "\n" +
                "graphicsMode:" + graphicsMode + "\n" +
                "ao:" + ao + "\n" +
                "renderClouds:\"" + clouds + "\"\n" +
                "particles:" + particles + "\n" +
                "entityShadows:" + entityShadows + "\n" +
                "biomeBlendRadius:" + biomeBlend + "\n" +
                "maxFps:260\n" +
                "enableVsync:false\n" +
                "mipmapLevels:0\n" +
                " entityDistanceScaling:0.75\n" +
                "fullscreen:true\n";

        writeFileSafety(optionsFile, optionsContent);
    }

    /**
     * Sodium Mod ki JSON config file (100+ FPS Boost ke liye) generate karta hai.
     */
    private static void writeSodiumOptions(File mcDir, GraphicsMode mode) {
        File configDir = new File(mcDir, "config");
        if (!configDir.exists()) configDir.mkdirs();
        File sodiumFile = new File(configDir, "sodium-options.json");

        boolean weatherQuality = (mode == GraphicsMode.ULTRA || mode == GraphicsMode.RAY_TRACING);
        boolean leavesQuality = (mode != GraphicsMode.SMOOTH);

        String json = "{\n" +
                "  \"quality\": {\n" +
                "    \"weather_quality\": \"" + (weatherQuality ? "FANCY" : "FAST") + "\",\n" +
                "    \"leaves_quality\": \"" + (leavesQuality ? "FANCY" : "FAST") + "\",\n" +
                "    \"enable_vignette\": false\n" +
                "  },\n" +
                "  \"performance\": {\n" +
                "    \"chunk_builder_threads\": 0,\n" +
                "    \"always_defer_chunk_updates\": true,\n" +
                "    \"animate_only_visible_textures\": true,\n" +
                "    \"use_entity_culling\": true,\n" +
                "    \"use_fog_occlusion\": true,\n" +
                "    \"use_block_face_culling\": true\n" +
                "  }\n" +
                "}";

        writeFileSafety(sodiumFile, json);
    }

    /**
     * Iris Shaders ko Ultra ya Ray Tracing mode me auto-enable karta hai.
     */
    private static void writeIrisShaderProperties(File mcDir, GraphicsMode mode) {
        File configDir = new File(mcDir, "config");
        if (!configDir.exists()) configDir.mkdirs();
        File irisFile = new File(configDir, "iris.properties");

        String shaderPack = "";
        boolean enableShaders = false;

        if (mode == GraphicsMode.ULTRA) {
            shaderPack = "MakeUp-UltraFast.zip";
            enableShaders = true;
        } else if (mode == GraphicsMode.RAY_TRACING) {
            shaderPack = "ComplementaryReimagined_QuartrsRTX.zip";
            enableShaders = true;
        }

        String content = "shaderPack=" + shaderPack + "\n" +
                "enableShaders=" + enableShaders + "\n" +
                "maxShadowRenderDistance=6\n";

        writeFileSafety(irisFile, content);

        // Agar Ray Tracing mode hai toh Shader ki RTX PBR reflections auto-enable karein
        if (mode == GraphicsMode.RAY_TRACING) {
            File shaderPacksDir = new File(mcDir, "shaderpacks");
            if (!shaderPacksDir.exists()) shaderPacksDir.mkdirs();
            File rtxSettings = new File(shaderPacksDir, "ComplementaryReimagined_QuartrsRTX.zip.txt");
            String rtxProfile = "SHADER_STYLE=4\n" +
                    "SHADOW_QUALITY=1\n" +
                    "shadowDistance=64.0\n" +
                    "GENERATED_NORMALS=true\n" +
                    "COATED_TEXTURES=true\n" +
                    "WATER_REFLECT_QUALITY=2\n" +
                    "BLOCK_REFLECT_QUALITY=3\n" +
                    "LIGHTSHAFT_QUALI_DEFINE=1\n" +
                    "RAIN_PUDDLES=1\n";
            writeFileSafety(rtxSettings, rtxProfile);
        }
    }

    private static void writeFileSafety(File file, String content) {
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(content);
        } catch (IOException e) {
            Log.e(TAG, "Config write error: " + file.getName(), e);
        }
    }
          }
