package hr.fer.artconnect.catalog.service;

import hr.fer.artconnect.catalog.web.UploadLimits;
import hr.fer.artconnect.common.domain.SystemSettingRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SettingsService {
    private static final List<String> UPLOAD_KEYS = List.of(
            "upload.artwork.maxImageBytes", "upload.artwork.maxImagesPerArtwork",
            "upload.application.maxImageBytes", "upload.application.maxPdfBytes",
            "upload.application.maxFiles");

    private final SystemSettingRepository settings;

    public SettingsService(SystemSettingRepository settings) {
        this.settings = settings;
    }

    public UploadLimits getUploadLimits() {
        Map<String, String> values = new HashMap<>();
        for (var setting : settings.findAllById(UPLOAD_KEYS)) {
            values.put(setting.getKey(), setting.getValue());
        }

        return new UploadLimits(
                readLimit(values, "upload.artwork.maxImageBytes", 5242880, 102400, 52428800),
                (int) readLimit(values, "upload.artwork.maxImagesPerArtwork", 3, 1, 10),
                readLimit(values, "upload.application.maxImageBytes", 5242880, 102400, 52428800),
                readLimit(values, "upload.application.maxPdfBytes", 10485760, 102400, 52428800),
                (int) readLimit(values, "upload.application.maxFiles", 10, 1, 30));
    }

    private long readLimit(Map<String, String> values, String key, long defaultValue, long min, long max) {
        String value = values.get(key);
        if (value == null) {
            return defaultValue;
        }

        long limit;
        try {
            limit = Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Postavka nije cijeli broj: " + key, exception);
        }
        if (limit < min || limit > max) {
            throw new IllegalStateException("Postavka je izvan dopuštenog raspona: " + key);
        }
        return limit;
    }
}
