package org.nrnr.neverdies.util;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class SoundRegister implements Globals {
    public static final Identifier SKEET_SOUND = new Identifier("neverdies:neverdies");
    public static final Identifier NL_SOUND = new Identifier("neverdies:nl");
    public static final Identifier ENABLE_SOUND = new Identifier("neverdies:enable");
    public static final Identifier DISABLE_SOUND = new Identifier("neverdies:disable");
    public static final Identifier OPEN_SOUND = new Identifier("neverdies:open");
    public static final Identifier FIRSTOPEN_SOUND = new Identifier("neverdies:firstopen");
    public static final Identifier SCROLL_SOUND = new Identifier("neverdies:scroll");
    public static SoundEvent SKEET_SOUNDEVENT = SoundEvent.of(SKEET_SOUND);
    public static SoundEvent NL_SOUNDEVENT = SoundEvent.of(NL_SOUND);
    public static SoundEvent ENABLE_SOUNDEVENT = SoundEvent.of(ENABLE_SOUND);
    public static SoundEvent DISABLE_SOUNDEVENT = SoundEvent.of(DISABLE_SOUND);
    public static SoundEvent OPEN_SOUNDEVENT = SoundEvent.of(OPEN_SOUND);
    public static SoundEvent FIRSTOPEN_SOUNDEVENT = SoundEvent.of(FIRSTOPEN_SOUND);
    public static SoundEvent SCROLL_SOUNDEVENT = SoundEvent.of(SCROLL_SOUND);
    public final Identifier PRIMO_SOUND = new Identifier("neverdies:primordial");
    public SoundEvent PRIMO_SOUNDEVENT = SoundEvent.of(PRIMO_SOUND);

    public static void registerSounds() {
        Registry.register(Registries.SOUND_EVENT, SKEET_SOUND, SKEET_SOUNDEVENT);
        Registry.register(Registries.SOUND_EVENT, NL_SOUND, NL_SOUNDEVENT);
        Registry.register(Registries.SOUND_EVENT, ENABLE_SOUND, ENABLE_SOUNDEVENT);
        Registry.register(Registries.SOUND_EVENT, DISABLE_SOUND, DISABLE_SOUNDEVENT);
        Registry.register(Registries.SOUND_EVENT, OPEN_SOUND, OPEN_SOUNDEVENT);
        Registry.register(Registries.SOUND_EVENT, SCROLL_SOUND, SCROLL_SOUNDEVENT);
    }

    public static void playSound(SoundEvent sound) {
        if (mc.player != null && mc.world != null)
            mc.world.playSound(mc.player, mc.player.getBlockPos(), sound, SoundCategory.BLOCKS, (float) 1 / 100f, 1f);
    }
}
