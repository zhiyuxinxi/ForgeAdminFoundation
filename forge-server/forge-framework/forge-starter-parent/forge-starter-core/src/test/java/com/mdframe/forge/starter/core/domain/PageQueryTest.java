package com.mdframe.forge.starter.core.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageQueryTest {
    @Test
    void databasePageUsesThePublicNormalizationContract() {
        for (Integer number : new Integer[]{null, -1, 0, 2}) {
            for (Integer size : new Integer[]{null, -1, 0, 20, 101, Integer.MAX_VALUE}) {
                PageQuery query = new PageQuery();
                query.setPageNum(number);
                query.setPageSize(size);
                var page = query.toPage();
                assertEquals(number == null || number < 1 ? 1L : number.longValue(), page.getCurrent());
                assertEquals(size == null || size < 1 ? 10L : Math.min(size, 100), page.getSize());
            }
        }
    }
}
