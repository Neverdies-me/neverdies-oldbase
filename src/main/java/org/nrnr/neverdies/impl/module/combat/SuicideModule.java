package org.nrnr.neverdies.impl.module.combat;


import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.EnumConfig;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.ToggleModule;

import static org.nrnr.neverdies.util.Globals.mc;

public class SuicideModule extends ToggleModule {
    public Config<Mode> modeConfig = new EnumConfig<>("Mode", "Modes for suicide method", Mode.AutoCrystal, Mode.values());
    //    Config<Boolean> armor = new BooleanConfig("ArmorDrop","",false);
//    Config<Boolean> totems = new BooleanConfig("TotemDrop","",false);
    public SuicideModule() {
        super("Suicide", "Automatically traps you in webs", ModuleCategory.Combat);
    }

    @Override
    public void onEnable() {
        if (modeConfig.getValue() == Mode.Command) {
            mc.getNetworkHandler().sendChatCommand("kill");
            disable();
        }
    }
    //    @EventListener
//    public void onTick(PlayerTickEvent event) {
//    }
    public enum Mode {
        Command,
        AutoCrystal
    }
}
