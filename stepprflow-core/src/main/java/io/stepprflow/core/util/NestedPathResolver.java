package io.stepprflow.core.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

/**
 * Utility class for resolving and setting values in nested Map structures
 * using dot-notation paths with array index support.
 *
 * <p>Supports paths like:</p>
 * <ul>
 *   <li>{@code "name"} - simple top-level key</li>
 *   <li>{@code "address.city"} - nested key</li>
 *   <li>{@code "items[0]"} - array element by index</li>
 *   <li>{@code "items[0].name"} - nested key inside array element</li>
 *   <li>{@code "order.items[0].variants[1].sku"} - complex nested
 *   path</li>
 * </ul>
 */
@Component
public class NestedPathResolver {

    /**
     * Get value at a dot-notation path in a nested Map.
     *
     * @param map  the root map to traverse
     * @param path the dot-notation path (e.g., "address.city"
     *             or "items[0].name")
     * @return the value at the path, or null if not found
     */
    public Object getValue(final Map<String, Object> map,
                          final String path) {
        if (map == null || path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = map;

        for (String part : parts) {
            if (current == null) {
                return null;
            }

            current = resolvePathSegment(current, part);
        }

        return current;
    }

    /**
     * Set value at a dot-notation path in a nested Map.
     * Creates intermediate maps if they don't exist.
     *
     * @param map   the root map to modify
     * @param path  the dot-notation path (e.g., "address.city"
     *              or "items[0].name")
     * @param value the value to set
     */
    public void setValue(final Map<String, Object> map,
                        final String path,
                        final Object value) {
        if (map == null || path == null || path.isEmpty()) {
            return;
        }

        String[] parts = path.split("\\.");
        Object current = map;

        // Navigate to the parent of the target
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            current = navigateOrCreate(current, part);
            if (current == null) {
                return;
            }
        }

        // Set the final value
        String lastPart = parts[parts.length - 1];
        setValueAtSegment(current, lastPart, value);
    }

    /**
     * Check if a path exists in the map.
     *
     * @param map  the root map to check
     * @param path the dot-notation path
     * @return true if the path exists (even if value is null),
     *         false otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean hasValue(final Map<String, Object> map,
                           final String path) {
        if (map == null || path == null || path.isEmpty()) {
            return false;
        }

        String[] parts = path.split("\\.");
        Object current = map;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            boolean isLastPart = (i == parts.length - 1);

            if (part.contains("[")) {
                ArraySegment segment = parseArraySegment(part);

                if (!(current instanceof Map)) {
                    return false;
                }

                Object arrayOrList =
                        ((Map<String, Object>) current).get(segment.name);
                if (!(arrayOrList instanceof List<?> list)) {
                    return false;
                }

                if (segment.index < 0 || segment.index >= list.size()) {
                    return false;
                }

                current = list.get(segment.index);
            } else {
                if (!(current instanceof Map)) {
                    return false;
                }

                Map<String, Object> mapCurrent = (Map<String, Object>) current;
                if (isLastPart) {
                    return mapCurrent.containsKey(part);
                }

                if (!mapCurrent.containsKey(part)) {
                    return false;
                }

                current = mapCurrent.get(part);
            }
        }

        return true;
    }

    /**
     * Resolve a single path segment (may contain array notation).
     *
     * @param current the current object
     * @param part the path part to resolve
     * @return the resolved object
     */
    @SuppressWarnings("unchecked")
    private Object resolvePathSegment(final Object current,
                                     final String part) {
        if (part.contains("[")) {
            ArraySegment segment = parseArraySegment(part);

            Object obj = current;
            if (current instanceof Map) {
                obj = ((Map<String, Object>) current).get(segment.name);
            }

            if (obj instanceof List<?> list) {
                obj = list.get(segment.index);
            }

            return obj;
        } else {
            if (current instanceof Map) {
                return ((Map<String, Object>) current).get(part);
            }
            return null;
        }
    }

    /**
     * Navigate to or create a path segment.
     *
     * @param current the current object
     * @param part the path part to navigate
     * @return the navigated object
     */
    @SuppressWarnings("unchecked")
    private Object navigateOrCreate(final Object current,
                                   final String part) {
        if (part.contains("[")) {
            ArraySegment segment = parseArraySegment(part);

            Object obj = current;
            if (current instanceof Map) {
                obj = ((Map<String, Object>) current).get(segment.name);
            }

            if (obj instanceof List) {
                return ((List<?>) obj).get(segment.index);
            }

        } else {
            if (current instanceof Map) {
                Map<String, Object> mapCurrent = (Map<String, Object>) current;
                Object next = mapCurrent.get(part);

                if (Objects.isNull(next)) {
                    next = new LinkedHashMap<String, Object>();
                    mapCurrent.put(part, next);
                }

                return next;
            }
        }
        return null;
    }

    /**
     * Set value at a single path segment.
     *
     * @param current the current object
     * @param part the path part
     * @param value the value to set
     */
    @SuppressWarnings("unchecked")
    private void setValueAtSegment(final Object current,
                                  final String part,
                                  final Object value) {
        if (part.contains("[")) {
            ArraySegment segment = parseArraySegment(part);

            if (current instanceof Map) {
                Object list =
                        ((Map<String, Object>) current).get(segment.name);
                if (list instanceof List) {
                    ((List<Object>) list).set(segment.index, value);
                }
            }
        } else {
            if (current instanceof Map) {
                ((Map<String, Object>) current).put(part, value);
            }
        }
    }

    /**
     * Parse an array segment like "items[0]" into name and index.
     *
     * @param part the path part to parse
     * @return the parsed array segment
     */
    private ArraySegment parseArraySegment(final String part) {
        int bracketStart = part.indexOf('[');
        int bracketEnd = part.indexOf(']');

        String name = part.substring(0, bracketStart);
        int index = Integer.parseInt(part.substring(bracketStart + 1,
                bracketEnd));

        return new ArraySegment(name, index);
    }

    /**
     * Internal record to hold parsed array segment.
     *
     * @param name the array name
     * @param index the array index
     */
    private record ArraySegment(String name, int index) { }
}
