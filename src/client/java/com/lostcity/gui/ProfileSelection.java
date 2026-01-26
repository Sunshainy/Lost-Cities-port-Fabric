package com.lostcity.gui;

/**
 * Хранит выбранный профиль для текущего создаваемого мира.
 * null = "Disabled" (vanilla generation)
 */
public class ProfileSelection {
    private static String selectedProfile = null;
    
    public static String getSelectedProfile() {
        return selectedProfile;
    }
    
    public static void setSelectedProfile(String profile) {
        selectedProfile = profile;
    }
    
    public static boolean isDisabled() {
        return selectedProfile == null;
    }
    
    public static void reset() {
        selectedProfile = null;
    }
}
