package ar.edu.unnoba.pm2026.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.unnoba.pm2026.shared.exception.ValidationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DateRangeTest {

    private static final LocalDate FROM = LocalDate.of(2026, 5, 1);
    private static final LocalDate TO = LocalDate.of(2026, 5, 31);

    @Test
    void fromPosteriorAToLanzaValidacion() {
        assertThatThrownBy(() -> new DateRange(TO, FROM))
                .isInstanceOf(ValidationException.class)
                .hasMessage("request.invalid-date-range");
    }

    @Test
    void elMismoDiaEnLosDosExtremosEsValido() {
        assertThat(new DateRange(FROM, FROM).contains(FROM.atTime(23, 59))).isTrue();
    }

    @Test
    void incluyeLosDosExtremosCompletos() {
        DateRange range = new DateRange(FROM, TO);

        assertThat(range.contains(FROM.atStartOfDay())).isTrue();
        assertThat(range.contains(TO.atTime(23, 59, 59))).isTrue();
        assertThat(range.contains(FROM.minusDays(1).atTime(23, 59))).isFalse();
        assertThat(range.contains(TO.plusDays(1).atStartOfDay())).isFalse();
    }

    @Test
    void unExtremoNuloDejaEseLadoAbierto() {
        assertThat(new DateRange(null, TO).contains(LocalDateTime.of(2000, 1, 1, 0, 0))).isTrue();
        assertThat(new DateRange(FROM, null).contains(LocalDateTime.of(2100, 1, 1, 0, 0))).isTrue();
        assertThat(new DateRange(null, null).contains(LocalDateTime.of(2026, 1, 1, 0, 0))).isTrue();
    }

    @Test
    void losLimitesParaLaBaseCubrenElUltimoDiaEntero() {
        DateRange range = new DateRange(FROM, TO);

        assertThat(range.startInclusive()).isEqualTo(LocalDateTime.of(2026, 5, 1, 0, 0));
        assertThat(range.endExclusive()).isEqualTo(LocalDateTime.of(2026, 6, 1, 0, 0));
    }
}
