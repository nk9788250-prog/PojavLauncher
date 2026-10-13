package net.kdt.pojavlaunch.quartrs;

import android.app.Activity;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.quartrs.QuartrsOptimizerEngine.GraphicsMode;

import java.io.File;
import java.util.Locale;

public class QuartrsUIController {

    private static GraphicsMode currentMode = GraphicsMode.AUTO;

    public static void initQuartrsUI(Activity activity) {
        TextView ramBadge = activity.findViewById(R.id.tv_quartrs_ram_badge);
        RadioGroup graphicsGroup = activity.findViewById(R.id.rg_quartrs_graphics);

        if (ramBadge == null || graphicsGroup == null) return;

        double ramGB = QuartrsOptimizerEngine.getDeviceRamGB(activity);
        File mcDir = new File(Tools.DIR_GAME_NEW);

        // Startup par RAM check karke default button select karein
        GraphicsMode autoDetected = QuartrsOptimizerEngine.applyHardwareOptimization(
                activity, mcDir, GraphicsMode.AUTO
        );
        currentMode = autoDetected;

        ramBadge.setText(String.format(Locale.US, "RAM: %.1f GB (%s)", ramGB, autoDetected.name()));

        switch (autoDetected) {
            case SMOOTH:
                graphicsGroup.check(R.id.rb_mode_smooth);
                break;
            case STANDARD:
                graphicsGroup.check(R.id.rb_mode_standard);
                break;
            case ULTRA:
                graphicsGroup.check(R.id.rb_mode_ultra);
                break;
            case RAY_TRACING:
                graphicsGroup.check(R.id.rb_mode_rtx);
                break;
            default:
                break;
        }

        // User jab button badle toh turant graphics preset update ho
        graphicsGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_mode_smooth) {
                currentMode = GraphicsMode.SMOOTH;
            } else if (checkedId == R.id.rb_mode_standard) {
                currentMode = GraphicsMode.STANDARD;
            } else if (checkedId == R.id.rb_mode_ultra) {
                currentMode = GraphicsMode.ULTRA;
            } else if (checkedId == R.id.rb_mode_rtx) {
                if (ramGB < 3.5) {
                    Toast.makeText(activity, "Warning: Low RAM detected! FPS may drop in Ray Tracing.", Toast.LENGTH_SHORT).show();
                }
                currentMode = GraphicsMode.RAY_TRACING;
            }

            QuartrsOptimizerEngine.applyHardwareOptimization(activity, mcDir, currentMode);
            ramBadge.setText(String.format(Locale.US, "RAM: %.1f GB (%s)", ramGB, currentMode.name()));
        });
    }

    // Play button dabane se theek pehle call karein
    public static void onBeforeGameLaunch(Activity activity) {
        File mcDir = new File(Tools.DIR_GAME_NEW);
        QuartrsOptimizerEngine.applyHardwareOptimization(activity, mcDir, currentMode);
    }
}
