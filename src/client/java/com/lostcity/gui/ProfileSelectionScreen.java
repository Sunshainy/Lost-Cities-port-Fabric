package com.lostcity.gui;

import com.lostcity.config.LostCityConfig;
import com.lostcity.config.ProfileConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI для выбора профиля Lost Cities при создании мира.
 * Fabric порт оригинального GuiLCConfig.
 */
public class ProfileSelectionScreen extends Screen {
    private final Screen parent;
    private String selectedProfile = null; // null = "Disabled" (vanilla generation)
    private List<String> availableProfiles;
    private ButtonWidget profileButton;
    
    public ProfileSelectionScreen(Screen parent) {
        super(Text.literal("Lost Cities Configuration"));
        this.parent = parent;
        this.availableProfiles = new ArrayList<>();
        
        // Получаем доступные профили из конфига
        LostCityConfig config = com.lostcity.LostCityMod.getConfig();
        if (config != null && config.getProfiles() != null) {
            availableProfiles.addAll(config.getProfiles().keySet());
        }
        
        // Загружаем текущее значение из конфига
        if (config != null) {
            if ("disabled".equals(config.selectedProfile)) {
                this.selectedProfile = null; // null = "Disabled" в GUI
            } else {
                this.selectedProfile = config.selectedProfile;
            }
        } else {
            // По умолчанию "Disabled"
            this.selectedProfile = null;
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int startY = this.height / 4;
        
        // Кнопка выбора профиля
        this.profileButton = ButtonWidget.builder(
            this.getProfileButtonText(),
            button -> {
                this.toggleProfile();
                button.setMessage(this.getProfileButtonText());
            }
        ).dimensions(centerX - 100, startY, 200, 20).build();
        
        this.addDrawableChild(profileButton);
        
        // Кнопка "Done"
        this.addDrawableChild(ButtonWidget.builder(
            ScreenTexts.DONE,
            button -> this.close()
        ).dimensions(centerX - 100, this.height - 28, 200, 20).build());
        
        // Информационный текст
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Select Lost Cities profile"),
            button -> {}
        ).dimensions(centerX - 100, startY - 30, 200, 20).build());
    }
    
    private Text getProfileButtonText() {
        if (selectedProfile == null) {
            return Text.literal("Profile: Disabled (Vanilla)");
        } else {
            return Text.literal("Profile: " + selectedProfile);
        }
    }
    
    private void toggleProfile() {
        if (selectedProfile == null) {
            // null -> первый профиль
            if (!availableProfiles.isEmpty()) {
                selectedProfile = availableProfiles.get(0);
            }
        } else {
            // Переключаемся на следующий профиль
            int currentIndex = availableProfiles.indexOf(selectedProfile);
            if (currentIndex == -1 || currentIndex >= availableProfiles.size() - 1) {
                // Последний профиль -> null (Disabled)
                selectedProfile = null;
            } else {
                selectedProfile = availableProfiles.get(currentIndex + 1);
            }
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        
        // Заголовок
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            this.title,
            this.width / 2,
            20,
            0xFFFFFF
        );
        
        // Описание текущего профиля
        String description = getProfileDescription();
        context.drawTextWithShadow(
            this.textRenderer,
            description,
            this.width / 2 - this.textRenderer.getWidth(description) / 2,
            this.height / 4 + 30,
            0xAAAAAA
        );
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private String getProfileDescription() {
        if (selectedProfile == null) {
            return "Vanilla world generation (Lost Cities disabled)";
        } else {
            // Получаем описание профиля из конфига
            LostCityConfig config = com.lostcity.LostCityMod.getConfig();
            if (config != null) {
                ProfileConfig profile = config.getProfile(selectedProfile);
                if (profile != null) {
                    return "City spawn: " + (int)(profile.getCityChance() * 100) + "%";
                }
            }
            return "Lost Cities generation with profile: " + selectedProfile;
        }
    }
    
    @Override
    public void close() {
        // Сохраняем выбранный профиль в конфиг
        LostCityConfig config = com.lostcity.LostCityMod.getConfig();
        if (config != null) {
            if (selectedProfile == null) {
                config.selectedProfile = "disabled";
            } else {
                config.selectedProfile = selectedProfile;
            }
            // TODO: Сохранить конфиг на диск (когда будет реализована система сохранения)
        }
        
        ProfileSelection.setSelectedProfile(selectedProfile);
        
        // Возвращаемся к родительскому экрану
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
