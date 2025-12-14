package io.stepprflow.core.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NestedPathResolver Tests")
class NestedPathResolverTest {

    private NestedPathResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new NestedPathResolver();
    }

    @Nested
    @DisplayName("getValue() method")
    class GetValueTests {

        @Test
        @DisplayName("Should get simple top-level value")
        void shouldGetSimpleValue() {
            Map<String, Object> map = new HashMap<>();
            map.put("name", "John");

            Object result = resolver.getValue(map, "name");

            assertThat(result).isEqualTo("John");
        }

        @Test
        @DisplayName("Should get nested value with dot notation")
        void shouldGetNestedValue() {
            Map<String, Object> address = new HashMap<>();
            address.put("city", "Paris");
            address.put("country", "France");

            Map<String, Object> map = new HashMap<>();
            map.put("address", address);

            Object result = resolver.getValue(map, "address.city");

            assertThat(result).isEqualTo("Paris");
        }

        @Test
        @DisplayName("Should get deeply nested value")
        void shouldGetDeeplyNestedValue() {
            Map<String, Object> street = new HashMap<>();
            street.put("name", "Rue de Rivoli");
            street.put("number", 42);

            Map<String, Object> address = new HashMap<>();
            address.put("street", street);

            Map<String, Object> map = new HashMap<>();
            map.put("address", address);

            Object result = resolver.getValue(map, "address.street.name");

            assertThat(result).isEqualTo("Rue de Rivoli");
        }

        @Test
        @DisplayName("Should get array element by index")
        void shouldGetArrayElement() {
            List<String> items = new ArrayList<>();
            items.add("apple");
            items.add("banana");
            items.add("cherry");

            Map<String, Object> map = new HashMap<>();
            map.put("items", items);

            Object result = resolver.getValue(map, "items[1]");

            assertThat(result).isEqualTo("banana");
        }

        @Test
        @DisplayName("Should get nested value inside array element")
        void shouldGetNestedValueInsideArrayElement() {
            Map<String, Object> item1 = new HashMap<>();
            item1.put("name", "Product A");
            item1.put("price", 100);

            Map<String, Object> item2 = new HashMap<>();
            item2.put("name", "Product B");
            item2.put("price", 200);

            List<Map<String, Object>> items = new ArrayList<>();
            items.add(item1);
            items.add(item2);

            Map<String, Object> map = new HashMap<>();
            map.put("items", items);

            Object result = resolver.getValue(map, "items[1].name");

            assertThat(result).isEqualTo("Product B");
        }

        @Test
        @DisplayName("Should get nested array inside object")
        void shouldGetNestedArrayInsideObject() {
            List<String> tags = new ArrayList<>();
            tags.add("urgent");
            tags.add("important");

            Map<String, Object> order = new HashMap<>();
            order.put("tags", tags);

            Map<String, Object> map = new HashMap<>();
            map.put("order", order);

            Object result = resolver.getValue(map, "order.tags[0]");

            assertThat(result).isEqualTo("urgent");
        }

        @Test
        @DisplayName("Should return null for non-existent path")
        void shouldReturnNullForNonExistentPath() {
            Map<String, Object> map = new HashMap<>();
            map.put("name", "John");

            Object result = resolver.getValue(map, "address.city");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null for null map")
        void shouldReturnNullForNullMap() {
            Object result = resolver.getValue(null, "name");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null for null path")
        void shouldReturnNullForNullPath() {
            Map<String, Object> map = new HashMap<>();
            map.put("name", "John");

            Object result = resolver.getValue(map, null);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null for empty path")
        void shouldReturnNullForEmptyPath() {
            Map<String, Object> map = new HashMap<>();
            map.put("name", "John");

            Object result = resolver.getValue(map, "");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should handle array index out of bounds gracefully")
        void shouldHandleArrayIndexOutOfBounds() {
            List<String> items = new ArrayList<>();
            items.add("apple");

            Map<String, Object> map = new HashMap<>();
            map.put("items", items);

            assertThatThrownBy(() -> resolver.getValue(map, "items[5]"))
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }
    }

    @Nested
    @DisplayName("setValue() method")
    class SetValueTests {

        @Test
        @DisplayName("Should set simple top-level value")
        void shouldSetSimpleValue() {
            Map<String, Object> map = new HashMap<>();

            resolver.setValue(map, "name", "John");

            assertThat(map.get("name")).isEqualTo("John");
        }

        @Test
        @DisplayName("Should set nested value with dot notation")
        void shouldSetNestedValue() {
            Map<String, Object> address = new HashMap<>();
            Map<String, Object> map = new HashMap<>();
            map.put("address", address);

            resolver.setValue(map, "address.city", "Paris");

            assertThat(address.get("city")).isEqualTo("Paris");
        }

        @Test
        @DisplayName("Should create intermediate maps if they don't exist")
        void shouldCreateIntermediateMaps() {
            Map<String, Object> map = new HashMap<>();

            resolver.setValue(map, "address.street.name", "Rue de Rivoli");

            assertThat(map).containsKey("address");
            @SuppressWarnings("unchecked")
            Map<String, Object> address = (Map<String, Object>) map.get("address");
            assertThat(address).containsKey("street");
            @SuppressWarnings("unchecked")
            Map<String, Object> street = (Map<String, Object>) address.get("street");
            assertThat(street.get("name")).isEqualTo("Rue de Rivoli");
        }

        @Test
        @DisplayName("Should set array element by index")
        void shouldSetArrayElement() {
            List<String> items = new ArrayList<>();
            items.add("apple");
            items.add("banana");
            items.add("cherry");

            Map<String, Object> map = new HashMap<>();
            map.put("items", items);

            resolver.setValue(map, "items[1]", "mango");

            assertThat(items.get(1)).isEqualTo("mango");
        }

        @Test
        @DisplayName("Should set nested value inside array element")
        void shouldSetNestedValueInsideArrayElement() {
            Map<String, Object> item1 = new HashMap<>();
            item1.put("name", "Product A");

            List<Map<String, Object>> items = new ArrayList<>();
            items.add(item1);

            Map<String, Object> map = new HashMap<>();
            map.put("items", items);

            resolver.setValue(map, "items[0].name", "Updated Product");

            assertThat(item1.get("name")).isEqualTo("Updated Product");
        }

        @Test
        @DisplayName("Should overwrite existing value")
        void shouldOverwriteExistingValue() {
            Map<String, Object> map = new HashMap<>();
            map.put("name", "John");

            resolver.setValue(map, "name", "Jane");

            assertThat(map.get("name")).isEqualTo("Jane");
        }

        @Test
        @DisplayName("Should set value in nested array inside object")
        void shouldSetValueInNestedArrayInsideObject() {
            List<String> tags = new ArrayList<>();
            tags.add("urgent");
            tags.add("important");

            Map<String, Object> order = new HashMap<>();
            order.put("tags", tags);

            Map<String, Object> map = new HashMap<>();
            map.put("order", order);

            resolver.setValue(map, "order.tags[0]", "critical");

            assertThat(tags.get(0)).isEqualTo("critical");
        }

        @Test
        @DisplayName("Should handle null map gracefully")
        void shouldHandleNullMapGracefully() {
            // Should not throw exception
            resolver.setValue(null, "name", "John");
        }

        @Test
        @DisplayName("Should handle null path gracefully")
        void shouldHandleNullPathGracefully() {
            Map<String, Object> map = new HashMap<>();

            // Should not throw exception
            resolver.setValue(map, null, "John");

            assertThat(map).isEmpty();
        }

        @Test
        @DisplayName("Should handle empty path gracefully")
        void shouldHandleEmptyPathGracefully() {
            Map<String, Object> map = new HashMap<>();

            // Should not throw exception
            resolver.setValue(map, "", "John");

            assertThat(map).isEmpty();
        }
    }

    @Nested
    @DisplayName("Complex scenarios")
    class ComplexScenarioTests {

        @Test
        @DisplayName("Should handle mixed nested structures")
        void shouldHandleMixedNestedStructures() {
            // Create: order.items[0].variants[1].sku
            Map<String, Object> variant1 = new HashMap<>();
            variant1.put("sku", "SKU-001");

            Map<String, Object> variant2 = new HashMap<>();
            variant2.put("sku", "SKU-002");

            List<Map<String, Object>> variants = new ArrayList<>();
            variants.add(variant1);
            variants.add(variant2);

            Map<String, Object> item = new HashMap<>();
            item.put("variants", variants);

            List<Map<String, Object>> items = new ArrayList<>();
            items.add(item);

            Map<String, Object> order = new HashMap<>();
            order.put("items", items);

            Map<String, Object> map = new HashMap<>();
            map.put("order", order);

            // Get
            Object result = resolver.getValue(map, "order.items[0].variants[1].sku");
            assertThat(result).isEqualTo("SKU-002");

            // Set
            resolver.setValue(map, "order.items[0].variants[1].sku", "SKU-NEW");
            assertThat(variant2.get("sku")).isEqualTo("SKU-NEW");
        }

        @Test
        @DisplayName("Should preserve map type when creating intermediate maps")
        void shouldPreserveMapType() {
            Map<String, Object> map = new LinkedHashMap<>();

            resolver.setValue(map, "a.b.c", "value");

            assertThat(map.get("a")).isInstanceOf(LinkedHashMap.class);
        }

        @Test
        @DisplayName("Should handle numeric string values in path")
        void shouldHandleNumericStringPath() {
            Map<String, Object> map = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            data.put("123", "numeric-key-value");
            map.put("data", data);

            Object result = resolver.getValue(map, "data.123");

            assertThat(result).isEqualTo("numeric-key-value");
        }
    }

    @Nested
    @DisplayName("hasValue() method")
    class HasValueTests {

        @Test
        @DisplayName("Should return true when path exists")
        void shouldReturnTrueWhenPathExists() {
            Map<String, Object> map = new HashMap<>();
            map.put("name", "John");

            boolean result = resolver.hasValue(map, "name");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true when nested path exists")
        void shouldReturnTrueWhenNestedPathExists() {
            Map<String, Object> address = new HashMap<>();
            address.put("city", "Paris");

            Map<String, Object> map = new HashMap<>();
            map.put("address", address);

            boolean result = resolver.hasValue(map, "address.city");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when path does not exist")
        void shouldReturnFalseWhenPathDoesNotExist() {
            Map<String, Object> map = new HashMap<>();
            map.put("name", "John");

            boolean result = resolver.hasValue(map, "address");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when value is null but key exists")
        void shouldReturnTrueWhenValueIsNullButKeyExists() {
            Map<String, Object> map = new HashMap<>();
            map.put("name", null);

            boolean result = resolver.hasValue(map, "name");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true for existing array element")
        void shouldReturnTrueForExistingArrayElement() {
            List<String> items = new ArrayList<>();
            items.add("apple");
            items.add("banana");

            Map<String, Object> map = new HashMap<>();
            map.put("items", items);

            boolean result = resolver.hasValue(map, "items[0]");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for out of bounds array index")
        void shouldReturnFalseForOutOfBoundsArrayIndex() {
            List<String> items = new ArrayList<>();
            items.add("apple");

            Map<String, Object> map = new HashMap<>();
            map.put("items", items);

            boolean result = resolver.hasValue(map, "items[5]");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for negative array index")
        void shouldReturnFalseForNegativeArrayIndex() {
            List<String> items = new ArrayList<>();
            items.add("apple");

            Map<String, Object> map = new HashMap<>();
            map.put("items", items);

            boolean result = resolver.hasValue(map, "items[-1]");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when array notation used on non-list")
        void shouldReturnFalseWhenArrayNotationUsedOnNonList() {
            Map<String, Object> map = new HashMap<>();
            map.put("items", "not a list");

            boolean result = resolver.hasValue(map, "items[0]");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when intermediate path is not a map")
        void shouldReturnFalseWhenIntermediatePathIsNotAMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("name", "John");

            boolean result = resolver.hasValue(map, "name.first");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when nested path does not exist")
        void shouldReturnFalseWhenNestedPathDoesNotExist() {
            Map<String, Object> address = new HashMap<>();
            address.put("city", "Paris");

            Map<String, Object> map = new HashMap<>();
            map.put("address", address);

            boolean result = resolver.hasValue(map, "address.country");

            assertThat(result).isFalse();
        }
    }
}
