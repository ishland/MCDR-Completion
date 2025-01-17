package cn.dancingsnow.mcdrc.mixin.client;

import cn.dancingsnow.mcdrc.client.MCDRCommandClient;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.command.CommandSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public abstract class ChatInputSuggestorMixin {

    @Shadow
    @Final
    private TextFieldWidget textField;

    @Shadow
    @Nullable
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    protected static int getStartOfCurrentWord(String input) {
        return 0;
    }

    @Shadow public abstract void show(boolean narrateFirstSuggestion);

    @Shadow private boolean completingSuggestions;

    @Shadow @Nullable private ChatInputSuggestor.@Nullable SuggestionWindow window;

    @Inject(method = "refresh()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getCursor()I", shift = At.Shift.AFTER), cancellable = true)
    public void refreshMixin(CallbackInfo ci) {
        String text = this.textField.getText();
        if (text.startsWith("!")) {
            String string = text.substring(0, this.textField.getCursor());
            if (this.window == null || !this.completingSuggestions) {
                int word = getStartOfCurrentWord(string);
                Collection<String> suggestion = MCDRCommandClient.getSuggestion(string);
                if (suggestion.size() > 0) {
                    this.pendingSuggestions = CommandSource.suggestMatching(suggestion,
                            new SuggestionsBuilder(string, word));
                    this.pendingSuggestions.thenRun(() -> {
                        if (this.pendingSuggestions.isDone()) {
                            this.show(true);
                        }
                    });
                }
            }
            ci.cancel();
        }
    }
}
