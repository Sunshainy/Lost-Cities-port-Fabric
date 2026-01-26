package com.lostcity.mixin.client;

import com.lostcity.gui.ProfileSelectionScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для добавления кнопки "Lost Cities" на экран создания мира.
 */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
    
    @Shadow
    protected abstract <T extends net.minecraft.client.gui.Element & net.minecraft.client.gui.Drawable & net.minecraft.client.gui.Selectable> T addDrawableChild(T drawableElement);
    
    @Inject(method = "init", at = @At("RETURN"))
    private void addLostCitiesButton(CallbackInfo ci) {
        CreateWorldScreen screen = (CreateWorldScreen) (Object) this;
        
        // Добавляем кнопку "Cities" в правом верхнем углу
        ButtonWidget citiesButton = ButtonWidget.builder(
            Text.literal("Cities"),
            button -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    client.setScreen(new ProfileSelectionScreen(screen));
                }
            }
        ).dimensions(screen.width - 100, 40, 70, 20).build();
        
        this.addDrawableChild(citiesButton);
    }
}
