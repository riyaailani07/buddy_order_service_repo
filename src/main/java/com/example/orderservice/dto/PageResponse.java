package com.example.orderservice.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Stable, framework-independent pagination envelope.
 *
 * @param content       the page of results
 * @param page          zero-based page index
 * @param size          requested page size
 * @param totalElements total matching records across all pages
 * @param totalPages    total number of pages
 * @param first         true when this is the first page
 * @param last          true when this is the last page
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    /**
     * Maps a Spring {@link Page} of entities into a {@link PageResponse} of DTOs.
     */
    public static <E, D> PageResponse<D> from(Page<E> page, Function<E, D> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
