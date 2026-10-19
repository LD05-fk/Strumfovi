package hr.fer.artconnect.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hr.fer.artconnect.catalog.service.SettingsService;
import hr.fer.artconnect.catalog.web.UploadLimits;
import hr.fer.artconnect.common.domain.SystemSetting;
import hr.fer.artconnect.common.domain.SystemSettingRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {
    @Mock
    SystemSettingRepository settings;

    @InjectMocks
    SettingsService service;

    @Test
    void citaSveVrijednostiIzBaze() {
        var values = List.of(
                setting("upload.artwork.maxImageBytes", "102400"),
                setting("upload.artwork.maxImagesPerArtwork", "10"),
                setting("upload.application.maxImageBytes", "52428800"),
                setting("upload.application.maxPdfBytes", "20971520"),
                setting("upload.application.maxFiles", "30"));
        when(settings.findAllById(any())).thenReturn(values);

        assertThat(service.getUploadLimits())
                .isEqualTo(new UploadLimits(102400, 10, 52428800, 20971520, 30));
    }

    @Test
    void bezPostavkiVracaZadaneVrijednosti() {
        when(settings.findAllById(any())).thenReturn(List.of());

        assertThat(service.getUploadLimits())
                .isEqualTo(new UploadLimits(5242880, 3, 5242880, 10485760, 10));
    }

    @Test
    void zadaneVrijednostiKoristiSamoZaNedostajuceKljuceve() {
        var maxFiles = setting("upload.application.maxFiles", "7");
        when(settings.findAllById(any())).thenReturn(List.of(maxFiles));

        assertThat(service.getUploadLimits())
                .isEqualTo(new UploadLimits(5242880, 3, 5242880, 10485760, 7));
    }

    @ParameterizedTest
    @CsvSource({
            "upload.artwork.maxImageBytes,abc",
            "upload.artwork.maxImageBytes,1.5",
            "upload.artwork.maxImageBytes,9223372036854775808",
            "upload.artwork.maxImageBytes,102399",
            "upload.artwork.maxImageBytes,52428801",
            "upload.artwork.maxImagesPerArtwork,0",
            "upload.artwork.maxImagesPerArtwork,11",
            "upload.application.maxImageBytes,-1",
            "upload.application.maxPdfBytes,52428801",
            "upload.application.maxFiles,0",
            "upload.application.maxFiles,31"
    })
    void odbijaNeispravnuPostavku(String key, String value) {
        var invalidSetting = setting(key, value);
        when(settings.findAllById(any())).thenReturn(List.of(invalidSetting));

        assertThatIllegalStateException().isThrownBy(service::getUploadLimits)
                .withMessageContaining(key);
    }

    private SystemSetting setting(String key, String value) {
        var setting = mock(SystemSetting.class);
        when(setting.getKey()).thenReturn(key);
        when(setting.getValue()).thenReturn(value);
        return setting;
    }
}
