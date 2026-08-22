package dev.sahilbasumatary.matchservice.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class PageBounds {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 100;

    private PageBounds() {}

    public static Pageable of(Integer page, Integer size) {
        int p = page == null ? 0 : Math.max(0, page);
        int s = size == null ? DEFAULT_SIZE : Math.min(MAX_SIZE, Math.max(1, size));
        return PageRequest.of(p, s);
    }
}
