package com.chatting.ui.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Set;

public class SecurePrefs {

    private static SharedPreferences sharedPreferences;

    public static void init(Context context) {
        if (sharedPreferences == null) {
            try {
                MasterKey masterKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                sharedPreferences = EncryptedSharedPreferences.create(
                        context,
                        "secure_prefs",
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to initialize encrypted preferences", e);
            }
        }
    }

    public static void putString(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    public static String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public static void putBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public static void putInt(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    public static int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    public static void putLong(String key, long value) {
        sharedPreferences.edit().putLong(key, value).apply();
    }

    public static long getLong(String key, long defaultValue) {
        return sharedPreferences.getLong(key, defaultValue);
    }

    public static void putFloat(String key, float value) {
        sharedPreferences.edit().putFloat(key, value).apply();
    }

    public static float getFloat(String key, float defaultValue) {
        return sharedPreferences.getFloat(key, defaultValue);
    }

    public static void putStringSet(String key, Set<String> set) {
        sharedPreferences.edit().putStringSet(key, set).apply();
    }

    public static Set<String> getStringSet(String key, Set<String> defaultSet) {
        return sharedPreferences.getStringSet(key, defaultSet);
    }

    public static void remove(String key) {
        sharedPreferences.edit().remove(key).apply();
    }

    public static void clear() {
        sharedPreferences.edit().clear().apply();
    }

    public static java.util.Map<String, ?> getAll() {
        return sharedPreferences.getAll();
    }

    public static void removeByPrefix(String prefix) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        java.util.Map<String, ?> allEntries = sharedPreferences.getAll();
        for (String key : allEntries.keySet()) {
            if (key.startsWith(prefix)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }
}