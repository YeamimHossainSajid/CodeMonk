package com.codemonk.common.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchCacheKeyGeneratorTest {

    private SearchCacheKeyGenerator keyGenerator;

    @BeforeEach
    void setUp() {
        keyGenerator = new SearchCacheKeyGenerator();
    }

    @Test
    void shouldNamespaceAQueryThatCarriesNoFilters() {
        assertEquals("search:order service", keyGenerator.generate("order service"));
    }

    @Test
    void shouldFoldAwayCaseAndSurroundingWhitespace() {
        assertEquals("search:orderservice", keyGenerator.generate("  OrderService  "));
    }

    @Test
    void shouldCollapseRepeatedWhitespaceInsideTheQuery() {
        assertEquals("search:order service", keyGenerator.generate("order    \t service"));
    }

    @Test
    void shouldAppendFiltersAsSortedPairs() {
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("lang", "java");
        filters.put("author", "sajid");

        assertEquals("search:cache:author=sajid|lang=java", keyGenerator.generate("cache", filters));
    }

    @Test
    void shouldProduceTheSameKeyRegardlessOfFilterOrder() {
        Map<String, String> ordered = new LinkedHashMap<>();
        ordered.put("lang", "java");
        ordered.put("repo", "codemonk");

        Map<String, String> reversed = new LinkedHashMap<>();
        reversed.put("repo", "codemonk");
        reversed.put("lang", "java");

        assertEquals(keyGenerator.generate("cache", ordered), keyGenerator.generate("cache", reversed));
    }

    @Test
    void shouldProduceTheSameKeyRegardlessOfMapImplementation() {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("lang", "java");
        hashMap.put("repo", "codemonk");

        assertEquals(keyGenerator.generate("cache", Map.copyOf(hashMap)), keyGenerator.generate("cache", hashMap));
    }

    @Test
    void shouldOmitTheFilterSegmentWhenThereAreNoFilters() {
        String withoutFilters = keyGenerator.generate("cache");

        assertEquals(withoutFilters, keyGenerator.generate("cache", Map.of()));
        assertEquals(withoutFilters, keyGenerator.generate("cache", null));
    }

    @Test
    void shouldHonourACallerChosenNamespace() {
        assertEquals("repositories:codemonk", keyGenerator.generate("repositories", "CodeMonk", Map.of()));
    }

    @Test
    void shouldReplaceTheCharactersThatDelimitSegments() {
        assertEquals("search:a_b_c", keyGenerator.generate("a:b|c"));
        assertEquals("search:cache:key_name=_value", keyGenerator.generate("cache", Map.of("key=name", "=value")));
    }

    @Test
    void shouldTreatAMissingFilterValueAsEmpty() {
        Map<String, String> filters = new HashMap<>();
        filters.put("lang", null);

        assertEquals("search:cache:lang=", keyGenerator.generate("cache", filters));
    }

    @Test
    void shouldRejectAQueryThatIsNullOrBlank() {
        assertThrows(IllegalArgumentException.class, () -> keyGenerator.generate(null));
        assertThrows(IllegalArgumentException.class, () -> keyGenerator.generate("   "));
    }

    @Test
    void shouldRejectANamespaceThatIsNullOrBlank() {
        assertThrows(IllegalArgumentException.class, () -> keyGenerator.generate(null, "cache", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> keyGenerator.generate("  ", "cache", Map.of()));
    }

    @Test
    void shouldRejectAFilterNameThatIsBlank() {
        Map<String, String> filters = Map.of("  ", "java");

        assertThrows(IllegalArgumentException.class, () -> keyGenerator.generate("cache", filters));
    }

    @Test
    void shouldReturnTheSameKeyForRepeatedCalls() {
        Map<String, String> filters = Map.of("lang", "Java");

        assertEquals(keyGenerator.generate("cache", filters), keyGenerator.generate("cache", filters));
    }
}
