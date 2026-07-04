package com.blog.blogsystem.service;

import java.util.Map;

public interface SettingService {
    Map<String, String> getAllSettings();
    void updateSettings(Map<String, String> settings);
    String getSettingValue(String key, String defaultValue);
}
