package hr.fer.artconnect.catalog.web;

import hr.fer.artconnect.catalog.service.SettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {
    private final SettingsService settings;

    public SettingsController(SettingsService settings) {
        this.settings = settings;
    }

    @GetMapping("/public")
    public UploadLimits getPublicSettings() {
        return settings.getUploadLimits();
    }
}
