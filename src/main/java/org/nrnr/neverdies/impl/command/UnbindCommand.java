package org.nrnr.neverdies.impl.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import org.nrnr.neverdies.api.command.Command;
import org.nrnr.neverdies.api.command.ModuleArgumentType;
import org.nrnr.neverdies.api.module.Module;
import org.nrnr.neverdies.api.module.ToggleModule;
import org.nrnr.neverdies.util.chat.ChatUtil;
import org.lwjgl.glfw.GLFW;

/**
 * @author chronos
 * @since 1.0
 */
public class UnbindCommand extends Command {
    /**
     * Constructor for the UnbindCommand.
     */
    public UnbindCommand() {
        super("Unbind", "Unbinds a module", literal("unbind"));
    }

    @Override
    public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("module", ModuleArgumentType.module())
                        .executes(c -> {
                            Module module = ModuleArgumentType.getModule(c, "module");
                            if (module == null) {
                                ChatUtil.error("Module not found!");
                                return 0;
                            }

                            if (module instanceof ToggleModule toggleModule) {
                                toggleModule.keybind(GLFW.GLFW_KEY_UNKNOWN);
                                ChatUtil.clientSendMessage("§7%s§f is unbound", module.getName());
                            } else {
                                ChatUtil.error("Module is not toggleable!");
                            }

                            return 1;
                        }))
                .executes(c -> {
                    ChatUtil.error("Must provide a module name.");
                    return 1;
                });
    }
}
