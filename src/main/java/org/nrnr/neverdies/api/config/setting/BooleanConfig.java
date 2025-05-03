package org.nrnr.neverdies.api.config.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.nrnr.neverdies.api.config.Config;

import java.util.function.Supplier;


public class BooleanConfig extends Config<Boolean> {
    public boolean parent = false;
    public boolean group = false;
    private boolean open = false;
    public BooleanConfig(String name,Boolean val) {
        super(name, "", val);
    }

    public BooleanConfig(String name,Boolean val, Supplier<Boolean> visible) {
        super(name, "", val, visible);
        configAnimation.setState(val);
    }
    public BooleanConfig(String name, String desc, Boolean val) {
        super(name, desc, val);
    }

    public BooleanConfig(String name, String desc, Boolean val, Supplier<Boolean> visible) {
        super(name, desc, val, visible);
        configAnimation.setState(val);
    }

    @Override
    public void setValue(Boolean in) {
        super.setValue(in);
        configAnimation.setState(in);
    }

    @Override
    public JsonObject toJson() {
        JsonObject configObj = super.toJson();
        configObj.addProperty("value", getValue());
        return configObj;
    }

    @Override
    public Boolean fromJson(JsonObject jsonObj) {
        if (jsonObj.has("value")) {
            JsonElement element = jsonObj.get("value");
            return element.getAsBoolean();
        }
        return null;
    }

    public BooleanConfig setParent() {
        parent = true;
        return this;
    }

    public boolean isParent() {
        return parent;
    }

    public BooleanConfig setGroup() {
        group = true;
        return this;
    }

    public boolean isGroup() {
        return group;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

}
/*  Parent Пример:
    public Config<Boolean> capes = new BooleanConfig("Capes","Custom capes",false).setParent(); // выдаем функции парент
    Config<capeMode> capeModeConfig = new EnumConfig<>("Mode", "image mode", capeMode.ZOV, capeMode.values(), () -> capes.isOpen()); // делаем функцию зависимой от capes (если capes парент фукнция)

    Group Пример:
    public Config<Boolean> capes = new BooleanConfig("Capes","Custom capes",false).setGroup(); // выдаем характеристику group
    Config<capeMode> capeModeConfig = new EnumConfig<>("Mode", "image mode", capeMode.ZOV, capeMode.values(), () -> capes.isOpen()); // делаем функцию зависимой от capes (если capes парент фукнция)

 */