package com.lostcity.gui;

/**
 * Хранит выбранный профиль для текущего создаваемого мира.
 * null = "Disabled" (vanilla generation)
 */
public class ProfileSelection {
    private static String selectedProfile = "default";
    
    public static String getSelectedProfile() {
        return selectedProfile;
    }
    
    public static void setSelectedProfile(String profile) {
        selectedProfile = profile;
    }
    
    public static boolean isDisabled() {
        return "disabled".equals(selectedProfile) || selectedProfile == null;
    }
    
    public static void reset() {
        selectedProfile = "default";
    }
}
