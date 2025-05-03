package org.nrnr.neverdies.impl.gui.click.impl.config.setting;


import net.minecraft.client.gui.DrawContext;
import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.BooleanConfig;
import org.nrnr.neverdies.api.render.RenderManager;
import org.nrnr.neverdies.impl.gui.click.impl.config.CategoryFrame;
import org.nrnr.neverdies.impl.gui.click.impl.config.ModuleButton;
import org.nrnr.neverdies.init.Modules;
import org.nrnr.neverdies.util.render.animation.Animation;

/**
 * @see Config
 */
public class CheckboxButton extends ConfigButton<Boolean> {

    /**
     * @param frame
     * @param config
     */
    public CheckboxButton(CategoryFrame frame, ModuleButton moduleButton, Config<Boolean> config, float x, float y) {
        super(frame, moduleButton, config, x, y);
        config.getAnimation().setState(config.getValue());
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
        BooleanConfig booleanConfig = (BooleanConfig) config;
        x = ix;
        y = iy;
        Animation checkboxAnimation = config.getAnimation();
        if (!booleanConfig.isGroup()) {
            rectGradientComponent(context, checkboxAnimation.getFactor() > 0.01f ?
                            Modules.CLICK_GUI.getColor((float) checkboxAnimation.getFactor()) : 0x00000000,
                    checkboxAnimation.getFactor() > 0.01f ?
                            Modules.CLICK_GUI.getColor1((float) checkboxAnimation.getFactor()) : 0x00000000);
        } else {
            rectGradientComponent(context, 0x00000000, 0x00000000);
        }
        RenderManager.renderText(context, config.getName(), ix + 2.0f, iy + 2.5f, -1);
        if (booleanConfig.isParent()) {
            String s = config.isOpen() ? "-" : "+";
            float colonX = ix + width - RenderManager.textWidth(s) - 2-5;
            RenderManager.renderText(context, s, colonX, iy + 2f, -1);
        }
        if (booleanConfig.isGroup()) {
            String s = config.isOpen() ? "·" : ":";
            float colonX = ix + width - RenderManager.textWidth(s) - 2;
            RenderManager.renderText(context, s, colonX, iy + 2f, -1);
        }
    }


    /**
     * @param mouseX
     * @param mouseY
     * @param button
     */
    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isWithin(mouseX, mouseY)) {
            BooleanConfig booleanConfig = (BooleanConfig) config;
            if (button == 0 && !((BooleanConfig) config).isGroup() && booleanConfig.isVisible()) { // l click
                boolean val = config.getValue();
                config.setValue(!val);
//                if (config.getValue() != false) {
//                    booleanConfig.setOpen(true);
//                }
            } else if (button == 1) {
//                if (booleanConfig.get()) {// r-clickk
                if (booleanConfig.parent || booleanConfig.group) {
                    booleanConfig.setOpen(!booleanConfig.isOpen());
                }
//                }
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

    }


}
