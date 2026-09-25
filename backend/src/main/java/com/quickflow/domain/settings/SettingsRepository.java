package com.quickflow.domain.settings;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsRepository extends JpaRepository<AppSettings, Long> {
}
