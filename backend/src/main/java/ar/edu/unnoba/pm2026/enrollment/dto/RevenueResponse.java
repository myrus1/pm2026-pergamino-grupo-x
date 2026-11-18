package ar.edu.unnoba.pm2026.enrollment.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RevenueResponse {

    private BigDecimal total;
}
