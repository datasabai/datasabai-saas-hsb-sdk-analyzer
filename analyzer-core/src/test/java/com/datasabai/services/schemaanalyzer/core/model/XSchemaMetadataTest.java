package com.datasabai.services.schemaanalyzer.core.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link XSchemaMetadata#toMap()} positions {@code projectionId}
 * directly after {@code id} and emits it empty for non-projection categories
 * (mirrors the xml-parser XSD annotation block behaviour).
 */
class XSchemaMetadataTest {

    @Test
    void standardCategoryEmitsEmptyProjectionIdRightAfterId() {
        XSchemaMetadata meta = XSchemaMetadata.builder()
                .id("xml-tmk-v1-goodsreceipt-grntest")
                .organisationCode("tfl")
                .category("standard")
                .build();

        Map<String, Object> map = meta.toMap();
        List<String> keys = new ArrayList<>(map.keySet());

        assertThat(map).containsEntry("projectionId", "");
        assertThat(keys.indexOf("projectionId")).isEqualTo(keys.indexOf("id") + 1);
        assertThat(keys.indexOf("projectionId")).isLessThan(keys.indexOf("organisationCode"));
    }

    @Test
    void projectionCategoryEmitsProjectionIdValueRightAfterId() {
        XSchemaMetadata meta = XSchemaMetadata.builder()
                .id("xml-tmk-v1-goodsreceipt-grntest")
                .organisationCode("tfl")
                .category("projection")
                .baseStandardId("xml-tmk-v1-goodsreceipt")
                .projectionId("1234")
                .build();

        Map<String, Object> map = meta.toMap();
        List<String> keys = new ArrayList<>(map.keySet());

        assertThat(map).containsEntry("projectionId", "1234");
        // emitted exactly once, immediately after id
        assertThat(keys.indexOf("projectionId")).isEqualTo(keys.indexOf("id") + 1);
        assertThat(keys.stream().filter("projectionId"::equals).count()).isEqualTo(1L);
    }
}
