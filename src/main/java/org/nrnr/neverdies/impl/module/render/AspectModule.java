package org.nrnr.neverdies.impl.module.render;


import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.BooleanConfig;
import org.nrnr.neverdies.api.config.setting.NumberConfig;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.ToggleModule;

public class AspectModule extends ToggleModule {

    public Config<Boolean> ddd = new BooleanConfig("Hands", "w", true);
    public Config<Float> valueConfig = new NumberConfig<>("Height", "amaynt ov aspecd", 0.1f, 1.78f, 5.0f);
    public Config<Float> valueConfig1 = new NumberConfig<>("Width", "amaynt ov aspecd", 0.1f, 1.78f, 5.0f);

    public AspectModule() {
        super("Aspect", "hvh", ModuleCategory.RENDER);
    }
}
