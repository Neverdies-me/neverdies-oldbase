package org.nrnr.neverdies.impl.module.client;

import net.minecraft.sound.SoundCategory;
import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.BooleanConfig;
import org.nrnr.neverdies.api.config.setting.EnumConfig;
import org.nrnr.neverdies.api.config.setting.NumberConfig;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.ToggleModule;
import org.nrnr.neverdies.init.Managers;
import org.nrnr.neverdies.init.Modules;

import static org.nrnr.neverdies.util.SoundRegister.OPEN_SOUNDEVENT;


public class SoundsModule extends ToggleModule {
    Config<Float> volumeConfig = new NumberConfig<>("Volume", "volume for all saundz", 0f, 1f, 1f);
    Config<enableSound> enableConfig = new EnumConfig<>("EnableSound", ":3 :P :D <3 >.<", enableSound.GTASA, enableSound.values());
    Config<disableSound> disableConfig = new EnumConfig<>("DisableSound", ":3 :P :D <3 >.<", disableSound.GTASA, disableSound.values());
    Config<openSound> openModuleConfig = new EnumConfig<>("OpenSound", ":3 :P :D <3 >.<", openSound.NEVERDIES, openSound.values());
    Config<Boolean> scrollConfig = new BooleanConfig("ScrollSound", "module scroll sound", true);

    public SoundsModule() {
        super("Sounds", "adds different sounds", ModuleCategory.CLIENT);
    }

    public void playEnableSound() {
        switch (enableConfig.getValue()) {
            case GTASA ->
                    mc.world.playSound(mc.player, mc.player.getBlockPos(), ENABLE_SOUNDEVENT, SoundCategory.MASTER, Modules.SOUNDS.getVolumeConfig().getValue(), 1f);
            case CUSTOM -> Managers.SOUND.playSound("enable");
        }
    }

    public void playDisableSound() {
        switch (disableConfig.getValue()) {
            case GTASA ->
                    mc.world.playSound(mc.player, mc.player.getBlockPos(), DISABLE_SOUNDEVENT, SoundCategory.MASTER, Modules.SOUNDS.getVolumeConfig().getValue(), 1f);
            case CUSTOM -> Managers.SOUND.playSound("disable");
        }
    }

    public void playOpenSound() {
        switch (openModuleConfig.getValue()) {
            case NEVERDIES ->
                    mc.world.playSound(mc.player, mc.player.getBlockPos(), OPEN_SOUNDEVENT, SoundCategory.MASTER, Modules.SOUNDS.getVolumeConfig().getValue(), 1f);
            case CUSTOM -> Managers.SOUND.playSound("open");
        }
    }

    public Config<Float> getVolumeConfig() {
        return volumeConfig;
    }

    public Config<Boolean> getScrollConfig() {
        return scrollConfig;
    }

    public enum enableSound {
        OFF,
        GTASA,
        CUSTOM
    }

    public enum disableSound {
        OFF,
        GTASA,
        CUSTOM
    }

    public enum openSound {
        OFF,
        NEVERDIES,
        CUSTOM
    }
}
