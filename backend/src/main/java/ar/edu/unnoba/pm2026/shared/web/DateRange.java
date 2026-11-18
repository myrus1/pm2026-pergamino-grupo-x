package ar.edu.unnoba.pm2026.shared.web;

import ar.edu.unnoba.pm2026.shared.exception.ValidationException;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Rango de fechas de los reportes del TP6 (fromDate/toDate, las dos inclusive). Un extremo nulo deja ese lado
// abierto. 400 si fromDate es posterior a toDate.
public record DateRange(LocalDate from, LocalDate to) {

    public DateRange {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("request.invalid-date-range",
                    "La fecha 'fromDate' no puede ser posterior a 'toDate'");
        }
    }

    public boolean contains(LocalDateTime moment) {
        LocalDate day = moment.toLocalDate();
        return (from == null || !day.isBefore(from)) && (to == null || !day.isAfter(to));
    }

    // Limites para consultar la base: [inicio del primer dia, inicio del dia siguiente al ultimo)
    public LocalDateTime startInclusive() {
        return from.atStartOfDay();
    }

    public LocalDateTime endExclusive() {
        return to.plusDays(1).atStartOfDay();
    }
}
