package org.nrnr.neverdies.mixin.gui.screen;


import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.nrnr.neverdies.Neverdies;
import org.nrnr.neverdies.impl.event.gui.chat.ChatInputEvent;
import org.nrnr.neverdies.impl.event.gui.chat.ChatKeyInputEvent;
import org.nrnr.neverdies.impl.event.gui.chat.ChatMessageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @see ChatScreen
 */
@Mixin(ChatScreen.class)
public class MixinChatScreen extends MixinScreen {
    @Shadow
    protected TextFieldWidget chatField;

    /**
     * @param chatText
     */
    // футур компатибилити
    @Inject(method = "onChatFieldUpdate", at = @At(value = "TAIL"))
    private void hookOnChatFieldUpdate(String chatText, CallbackInfo ci) {
        ChatInputEvent chatInputEvent = new ChatInputEvent(chatText);
        Neverdies.EVENT_HANDLER.dispatch(chatInputEvent);
    }

    /**
     * @param keyCode
     * @param scanCode
     * @param modifiers
     * @param cir
     */
    @Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
    private void hookKeyPressed(int keyCode, int scanCode, int modifiers,
                                CallbackInfoReturnable<Boolean> cir) {
        ChatKeyInputEvent keyInputEvent = new ChatKeyInputEvent(keyCode,
                chatField.getText());
        Neverdies.EVENT_HANDLER.dispatch(keyInputEvent);
        if (keyInputEvent.isCanceled()) {
            cir.cancel();
            chatField.setText(keyInputEvent.getChatText());
        }
    }

    /**
     * @param chatText
     * @param addToHistory
     * @param cir
     */
    @Inject(method = "sendMessage", at = @At(value = "HEAD"), cancellable = true)
    private void hookSendMessage(String chatText, boolean addToHistory,
                                 CallbackInfoReturnable<Boolean> cir) {
        ChatMessageEvent.Client chatMessageEvent =
                new ChatMessageEvent.Client(chatText);
        Neverdies.EVENT_HANDLER.dispatch(chatMessageEvent);
        if (chatMessageEvent.isCanceled()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }


    @Override
    protected <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement) {
        return null;
    }
}
