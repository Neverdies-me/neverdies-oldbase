package org.nrnr.neverdies.impl.module.client;



import org.nrnr.neverdies.Neverdies;
import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.BooleanConfig;
import org.nrnr.neverdies.api.config.setting.EnumConfig;
import org.nrnr.neverdies.api.config.setting.NumberConfig;
import org.nrnr.neverdies.api.event.listener.EventListener;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.ToggleModule;
import org.nrnr.neverdies.impl.event.config.ConfigUpdateEvent;
import org.nrnr.neverdies.impl.font.FontRenderers;
import org.nrnr.neverdies.init.Modules;

import java.awt.*;


public class CustomFontModule extends ToggleModule {
    // Лечись школота
// TODO   Config<String> font = new StringConfig("Font", "Font for renderer","Verdana");
    public Config<mode> style = new EnumConfig<>("Style", "modes", mode.Plain, mode.values());
    public Config<Float> size = new NumberConfig<>("Size", "", 10.0f, 17.0f, 20.0f);
    public Config<Boolean> shadowConfig = new BooleanConfig("Shadow", "Renders text with a shadow background", true);
    public Config<Float> offset = new NumberConfig<>("Shadow-Offset", "", 0.0f, 1.0f, 1.1f);

    /**
     *
     */
    public CustomFontModule() {
        super("CustomFont", "Changes the client text to custom font rendering",
                ModuleCategory.CLIENT);
    }

    @EventListener
    public void onCfgUpdate(ConfigUpdateEvent event) {
        if (event.getConfig() == style || event.getConfig() == size) {
            try {
                FontRenderers.Verdana = FontRenderers.createVerdana(Modules.CUSTOM_FONT.size.getValue(), Modules.CUSTOM_FONT.getFontMode());
                Neverdies.isFontLoaded = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public int getFontMode() {
        if (style.getValue() == mode.Plain) {
            return Font.PLAIN;
        } else if (style.getValue() == mode.Bold) {
            return Font.BOLD;
        } else if (style.getValue() == mode.Italic) {
            return Font.ITALIC;
        } else if (style.getValue() == mode.BoldItalic) {
            return Font.ITALIC + Font.BOLD;
        }
        return getFontMode();
    }

    /**
     * @return
     */
    public enum mode {
        Plain,
        Bold,
        Italic,
        BoldItalic
    }
}
