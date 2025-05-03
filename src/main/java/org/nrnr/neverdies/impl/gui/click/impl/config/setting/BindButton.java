package org.nrnr.neverdies.impl.gui.click.impl.config.setting;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.MacroConfig;
import org.nrnr.neverdies.api.macro.Macro;
import org.nrnr.neverdies.api.render.RenderManager;
import org.nrnr.neverdies.impl.gui.click.impl.config.CategoryFrame;
import org.nrnr.neverdies.impl.gui.click.impl.config.ModuleButton;
import org.nrnr.neverdies.impl.module.client.ClickGuiModule;
import org.nrnr.neverdies.init.Modules;
import org.nrnr.neverdies.util.chat.ChatUtil;

import static org.lwjgl.glfw.GLFW.*;

/**
 * @author chronos
 * @since 1.0
 */
public class BindButton extends ConfigButton<Macro> {
    // Check for whether we are listening for an input
    private boolean listening;

    /**
     * @param frame
     * @param config
     * @param x
     * @param y
     */
    public BindButton(CategoryFrame frame, ModuleButton moduleButton, Config<Macro> config, float x, float y) {
        super(frame, moduleButton, config, x, y);
    }

    /**
     * @param context
     * @param ix
     * @param iy
     * @param mouseX
     * @param mouseY
     * @param delta
     */
    @Override
    public void render(DrawContext context, float ix, float iy, float mouseX,
                       float mouseY, float delta) {
        ClickGuiModule.CLICK_GUI_SCREEN.setCloseOnEscape(!listening);

        x = ix;
        y = iy;
        final Macro macro = config.getValue();
        String val = listening ? "..." : macro.getKeyName();
        rect(context, 0x00000000);
        RenderManager.renderText(context, config.getName() + Formatting.GRAY
                + " " + val, ix + 2.0f, iy + 3.5f, -1);

        // Capture key input while listening
//        if (listening) {
//            for (int key = GLFW_KEY_SPACE; key < GLFW_KEY_LAST; key++) {
//                if (glfwGetKey(mc.getWindow().getHandle(), key) == GLFW_PRESS) {
//                    System.out.println("Bind Pressed: " + key);
//
//                    // Handle unbinding
//                    if (key == GLFW_KEY_ESCAPE || key == GLFW_KEY_BACKSPACE) {
//                        ((MacroConfig) config).setValue(GLFW_KEY_UNKNOWN);
//                    } else {
//                        ((MacroConfig) config).setValue(key);
//                    }
//
//                    listening = false;
//                    break; // Stop checking after detecting one key press
//                }
//            }
//        }
    }


    /**
     * @param mouseX
     * @param mouseY
     * @param button
     */
    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isWithin(mouseX, mouseY)) {
            ChatUtil.clientSendMessage("Clicked");
            switch (button) {
                case GLFW_MOUSE_BUTTON_1 -> listening = true;
                case GLFW_MOUSE_BUTTON_2 -> {
                    listening = false;
                    ((MacroConfig) config).setValue(-1);
                }
            }
        }
    }

    /**
     * @param mouseX
     * @param mouseY
     * @param button
     */
    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {

    }

    /**
     * @param keyCode
     * @param scanCode
     * @param modifiers
     */
    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        System.out.println("Key pressed!");
        if (listening) {
            System.out.println("Bind Pressed: " + keyCode);
            // unbind
            if (keyCode == GLFW_KEY_ESCAPE || keyCode == GLFW_KEY_BACKSPACE) {
                ((MacroConfig) config).setValue(GLFW_KEY_UNKNOWN);
            } else {
                ((MacroConfig) config).setValue(keyCode);
            }
            listening = false;
        }
    }

    public boolean isListening() {
        return listening;
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }
}
