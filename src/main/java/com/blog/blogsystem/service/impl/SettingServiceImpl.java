package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.entity.Setting;
import com.blog.blogsystem.repository.SettingRepository;
import com.blog.blogsystem.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService {

    private final SettingRepository settingRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getAllSettings() {
        List<Setting> settings = settingRepository.findAll();
        return settings.stream()
                .collect(Collectors.toMap(
                        Setting::getKey,
                        setting -> setting.getValue() == null ? "" : setting.getValue(),
                        (existing, replacement) -> replacement,
                        HashMap::new
                ));
    }

    @Override
    @Transactional
    public void updateSettings(Map<String, String> newSettings) {
        newSettings.forEach((key, value) -> {
            Setting setting = settingRepository.findById(key)
                    .orElse(Setting.builder()
                            .key(key)
                            .description("Cấu hình hệ thống động")
                            .build());
            setting.setValue(value);
            settingRepository.save(setting);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public String getSettingValue(String key, String defaultValue) {
        return settingRepository.findById(key)
                .map(Setting::getValue)
                .orElse(defaultValue);
    }
}
