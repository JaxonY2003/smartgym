package com.jaxon.smart_gym.common.response;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        long page,
        long pageSize,
        long total,
        long totalPages
) {

    public static <T> PageResponse<T> of(
            List<T> items,
            long page,
            long pageSize,
            long total) {
        long totalPages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResponse<>(items, page, pageSize, total, totalPages);
    }
}
