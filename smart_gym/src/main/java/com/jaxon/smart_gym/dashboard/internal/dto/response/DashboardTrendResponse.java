package com.jaxon.smart_gym.dashboard.internal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardTrendResponse(
        String metric,
        String unit,
        List<Point> points
) {

    public record Point(
            LocalDate date,
            BigDecimal value
    ) {
    }
}
