package org.nrnr.neverdies.impl.gui.click.impl.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.event.listener.EventListener;
import org.nrnr.neverdies.api.module.Module;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.render.RenderManager;
import org.nrnr.neverdies.impl.event.keyboard.KeyboardInputEvent;
import org.nrnr.neverdies.impl.gui.click.ClickGuiScreen;
import org.nrnr.neverdies.impl.gui.click.component.Frame;
import org.nrnr.neverdies.impl.gui.click.impl.config.setting.ColorButton;
import org.nrnr.neverdies.impl.gui.click.impl.config.setting.ConfigButton;
import org.nrnr.neverdies.init.Managers;
import org.nrnr.neverdies.init.Modules;
import org.nrnr.neverdies.util.chat.ChatUtil;
import org.nrnr.neverdies.util.render.animation.Animation;
import org.nrnr.neverdies.util.render.animation.Easing;
import org.nrnr.neverdies.util.string.EnumFormatter;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.nrnr.neverdies.util.render.animation.Easing.CUBIC_IN_OUT;

public class CategoryFrame extends Frame {
    private final String name;
    private final ModuleCategory category;
    private final List<ModuleButton> moduleButtons = new CopyOnWriteArrayList<>();
    private float off, inner;
    private boolean open;
    private boolean drag;
    private final Animation categoryAnimation = new Animation(false, 200, CUBIC_IN_OUT);


    /**
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public CategoryFrame(ModuleCategory category, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.category = category;
        this.name = EnumFormatter.formatEnum(category);
        for (Module module : Managers.MODULE.getModules()) {
            if (module.getCategory() == category) {
                moduleButtons.add(new ModuleButton(module, this, x, y));
            }
        }
        categoryAnimation.setState(true);
        open = true;
    }

    /**
     * Default constructor with a fixed size for the frame
     * @param category
     * @param x
     * @param y
     */
    public CategoryFrame(ModuleCategory category, float x, float y) {
        this(category, x, y, 100.0f, 16.0f);
    }

    /**
     * Render method for the frame and modules
     */
    @Override
    public void render(DrawContext context, float mouseX, float mouseY, float delta) {
        if (drag) {
            x += ClickGuiScreen.MOUSE_X - px;
            y += ClickGuiScreen.MOUSE_Y - py;
        }

        fheight = 2.0f;
        for (ModuleButton moduleButton : moduleButtons) {
            fheight += moduleButton.getHeight() + 1f;
            if (moduleButton.getScaledTime() < 0.01f) {
                continue;
            }
            fheight += 3.0f * moduleButton.getScaledTime();
            for (ConfigButton<?> configButton : moduleButton.getConfigButtons()) {
                if (!configButton.getConfig().isVisible()) {
                    continue;
                }
                fheight += configButton.getHeight() * moduleButton.getScaledTime();
                if (configButton instanceof ColorButton colorPicker && colorPicker.getScaledTime() > 0.01f) {
                    fheight += colorPicker.getPickerHeight() * colorPicker.getScaledTime() * moduleButton.getScaledTime();
                }
            }
        }

        // Limit the frame to the screen height
        if (y < -(fheight - 10)) {
            y = -(fheight - 10);
        }
        if (y > mc.getWindow().getHeight() - 10) {
            y = mc.getWindow().getHeight() - 10;
        }

        rect(context, Modules.CLICK_GUI.getColor(1.7f));
        if (categoryAnimation.getFactor() < 0.01f) {
            fheight = height;
        } else {
            int outlineColor = Modules.CLICK_GUI.getColor(1.7f);
            int alpha = (int) (60 * categoryAnimation.getFactor());
            int outlineColorWithAlpha = (outlineColor & 0x00FFFFFF) | (alpha << 24);
            int fillColorWithAlpha = (0x10000000 & 0x00FFFFFF) | (alpha << 24);

            drawOutline(context, 0f, 0f, 1.2f, outlineColorWithAlpha, fheight);
            fill(context, x, y + height, width, fheight, fillColorWithAlpha);
        }

        RenderManager.renderText(context, name, x + 3.0f, y + 2f, -1);

        if (categoryAnimation.getFactor() > 0.01f) {
            enableScissor((int) x, (int) (y + height), (int) (x + width), (int) (y + height + fheight * categoryAnimation.getFactor()));
            fill(context, x, y + height, width, fheight, Modules.CLICK_GUI.getColor());
            off = y + height + 1.0f;
            inner = off;
            for (ModuleButton moduleButton : moduleButtons) {
                moduleButton.render(context, x + 1.0f, inner + 1.0f, mouseX, mouseY, delta);
                off += (float) ((moduleButton.getHeight() + 1.0f) * categoryAnimation.getFactor());
                inner += moduleButton.getHeight() + 1.0f;
            }
            disableScissor();
        }

        px = ClickGuiScreen.MOUSE_X;
        py = ClickGuiScreen.MOUSE_Y;
    }

    /**
     * Draw the frame outline
     */
    protected void drawOutline(DrawContext context, float offsetX, float offsetY, float outlineWidth, int outlineColor, float contentHeight) {
        if (categoryAnimation.getFactor() > 0.01f) {
            float outlineX1 = x - offsetX - outlineWidth;
            float outlineY1 = y - offsetY - outlineWidth;
            float outlineX2 = x + width + offsetX;
            float outlineY2 = y + height + offsetY + contentHeight + outlineWidth;

            int alpha = (int) (170 * categoryAnimation.getFactor());
            int outlineColorWithAlpha = (outlineColor & 0x00FFFFFF) | (alpha << 24);

            fill(context, outlineX1, outlineY1, outlineX2 - outlineX1 + outlineWidth, outlineWidth, outlineColorWithAlpha);
            fill(context, outlineX1, outlineY2 - outlineWidth, outlineX2 - outlineX1 + outlineWidth, outlineWidth, outlineColorWithAlpha);
            fill(context, outlineX1, outlineY1 + outlineWidth, outlineWidth, outlineY2 - outlineY1 - outlineWidth * 2, outlineColorWithAlpha);
            fill(context, outlineX2, outlineY1 + outlineWidth, outlineWidth, outlineY2 - outlineY1 - outlineWidth * 2, outlineColorWithAlpha);
        }
    }

    /**
     * Mouse click handler
     */
    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT && isWithin(mouseX, mouseY)) {
            open = !open;
            if (Modules.SOUNDS.isEnabled()) {
                Modules.SOUNDS.playOpenSound();
            }
            categoryAnimation.setState(open);
        }
        if (open) {
            for (ModuleButton button : moduleButtons) {
                button.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    /**
     * Mouse release handler
     */
    @Override
    public void mouseReleased(double mouseX, double mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        drag = false;
        if (open) {
            for (ModuleButton button : moduleButtons) {
                button.mouseReleased(mouseX, mouseY, mouseButton);
            }
        }
    }



    /**
     * @param mx
     * @param my
     * @return
     */
    public boolean isWithinTotal(float mx, float my) {
        return isMouseOver(mx, my, x, y, width, getTotalHeight());
    }

    /**
     * Update global offset
     */
    public void offset(float in) {
        off += in;
        inner += in;
    }

    /**
     * Get the category of the frame
     */
    public ModuleCategory getCategory() {
        return category;
    }

    /**
     * Get the total height of the frame
     */
    public float getTotalHeight() {
        return height + fheight;
    }

    /**
     * Get the module buttons
     */
    public List<ModuleButton> getModuleButtons() {
        return moduleButtons;
    }

    public void setDragging(boolean drag) {
        this.drag = drag;
    }

    public boolean isDragging() {
        return drag;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ModuleButton moduleButton : moduleButtons) {
            moduleButton.keyPressed(keyCode, scanCode, modifiers);
        }
    }
}
