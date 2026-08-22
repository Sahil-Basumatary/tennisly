package dev.sahilbasumatary.matchservice.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class PageBoundsTest {

    @Test
    void defaultsAndCapsPageSize() {
        Pageable def = PageBounds.of(null, null);
        assertEquals(0, def.getPageNumber());
        assertEquals(50, def.getPageSize());
        assertEquals(100, PageBounds.of(-2, 500).getPageSize());
        assertEquals(0, PageBounds.of(-2, 500).getPageNumber());
        assertEquals(1, PageBounds.of(3, 0).getPageSize());
    }
}
