package com.wd.api.util;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building JPA Specifications dynamically
 * Provides reusable methods for common filter patterns
 * 
 * Enterprise-grade pattern for type-safe, dynamic query building
 * 
 * @param <T> The entity type
 */
public class SpecificationBuilder<T> {
    
    /**
     * Build a search specification across multiple text fields
     * Uses case-insensitive LIKE with wildcards
     * 
     * @param search The search query
     * @param fields The field names to search in
     * @return Specification that matches if ANY field contains the search term
     */
    public Specification<T> buildSearch(String search, String... fields) {
        if (search == null || search.trim().isEmpty() || fields == null || fields.length == 0) {
            return null;
        }
        
        return (root, query, cb) -> {
            String searchPattern = "%" + search.trim().toLowerCase() + "%";
            List<Predicate> searchPredicates = new ArrayList<>();
            
            for (String field : fields) {
                try {
                    Expression<String> fieldExpr = getNestedField(root, field);
                    searchPredicates.add(
                        cb.like(cb.lower(fieldExpr), searchPattern)
                    );
                } catch (Exception e) {
                    // Skip invalid field names
                }
            }
            
            return searchPredicates.isEmpty() 
                ? cb.conjunction() 
                : cb.or(searchPredicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Build an exact match specification for a field
     * 
     * @param field The field name
     * @param value The value to match
     * @return Specification for exact match, or null if value is null
     */
    public Specification<T> buildEquals(String field, Object value) {
        if (value == null) {
            return null;
        }
        
        return (root, query, cb) -> {
            try {
                return cb.equal(getNestedField(root, field), value);
            } catch (Exception e) {
                return cb.conjunction();
            }
        };
    }
    
    /**
     * Build a case-insensitive LIKE specification for a text field
     * 
     * @param field The field name
     * @param value The value to match (adds wildcards automatically)
     * @return Specification for LIKE match, or null if value is null
     */
    public Specification<T> buildLike(String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        return (root, query, cb) -> {
            try {
                Expression<String> fieldExpr = getNestedField(root, field);
                return cb.like(
                    cb.lower(fieldExpr), 
                    "%" + value.trim().toLowerCase() + "%"
                );
            } catch (Exception e) {
                return cb.conjunction();
            }
        };
    }
    
    /**
     * Build a date range specification (inclusive)
     * 
     * @param field The date field name
     * @param start The start date (inclusive)
     * @param end The end date (inclusive)
     * @return Specification for date range, or null if both dates are null
     */
    public Specification<T> buildDateRange(String field, LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return null;
        }
        
        return (root, query, cb) -> {
            try {
                Expression<LocalDate> fieldExpr = getNestedField(root, field);
                List<Predicate> predicates = new ArrayList<>();
                
                if (start != null) {
                    predicates.add(cb.greaterThanOrEqualTo(fieldExpr, start));
                }
                if (end != null) {
                    predicates.add(cb.lessThanOrEqualTo(fieldExpr, end));
                }
                
                return cb.and(predicates.toArray(new Predicate[0]));
            } catch (Exception e) {
                return cb.conjunction();
            }
        };
    }
    
    /**
     * Build a date-time range specification (inclusive)
     * 
     * @param field The date-time field name
     * @param start The start date-time (inclusive)
     * @param end The end date-time (inclusive)
     * @return Specification for date-time range, or null if both are null
     */
    public Specification<T> buildDateTimeRange(String field, LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return null;
        }
        
        return (root, query, cb) -> {
            try {
                Expression<LocalDateTime> fieldExpr = getNestedField(root, field);
                List<Predicate> predicates = new ArrayList<>();
                
                if (start != null) {
                    predicates.add(cb.greaterThanOrEqualTo(fieldExpr, start));
                }
                if (end != null) {
                    predicates.add(cb.lessThanOrEqualTo(fieldExpr, end));
                }
                
                return cb.and(predicates.toArray(new Predicate[0]));
            } catch (Exception e) {
                return cb.conjunction();
            }
        };
    }
    
    /**
     * Build a numeric range specification (inclusive)
     * 
     * @param field The numeric field name
     * @param min The minimum value (inclusive)
     * @param max The maximum value (inclusive)
     * @return Specification for numeric range, or null if both are null
     */
    public <N extends Number & Comparable<N>> Specification<T> buildNumericRange(
            String field, N min, N max) {
        if (min == null && max == null) {
            return null;
        }
        
        return (root, query, cb) -> {
            try {
                Expression<N> fieldExpr = getNestedField(root, field);
                List<Predicate> predicates = new ArrayList<>();
                
                if (min != null) {
                    predicates.add(cb.greaterThanOrEqualTo(fieldExpr, min));
                }
                if (max != null) {
                    predicates.add(cb.lessThanOrEqualTo(fieldExpr, max));
                }
                
                return cb.and(predicates.toArray(new Predicate[0]));
            } catch (Exception e) {
                return cb.conjunction();
            }
        };
    }
    
    /**
     * Build an IN clause specification
     * 
     * @param field The field name
     * @param values The list of values
     * @return Specification for IN clause, or null if values is null/empty
     */
    public <V> Specification<T> buildIn(String field, List<V> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        
        return (root, query, cb) -> {
            try {
                Expression<V> fieldExpr = getNestedField(root, field);
                return fieldExpr.in(values);
            } catch (Exception e) {
                return cb.conjunction();
            }
        };
    }
    
    /**
     * Build a boolean field specification
     * 
     * @param field The boolean field name
     * @param value The boolean value
     * @return Specification for boolean match, or null if value is null
     */
    public Specification<T> buildBoolean(String field, Boolean value) {
        if (value == null) {
            return null;
        }
        
        return (root, query, cb) -> {
            try {
                Expression<Boolean> fieldExpr = getNestedField(root, field);
                return value ? cb.isTrue(fieldExpr) : cb.isFalse(fieldExpr);
            } catch (Exception e) {
                return cb.conjunction();
            }
        };
    }
    
    /**
     * Combine multiple specifications with AND logic
     * Skips null specifications
     * 
     * @param specs The specifications to combine
     * @return Combined specification, or null if all are null
     */
    @SafeVarargs
    public final Specification<T> and(Specification<T>... specs) {
        if (specs == null || specs.length == 0) {
            return null;
        }
        
        Specification<T> result = null;
        for (Specification<T> spec : specs) {
            if (spec != null) {
                result = result == null ? spec : result.and(spec);
            }
        }
        return result;
    }
    
    /**
     * Combine multiple specifications with OR logic
     * Skips null specifications
     * 
     * @param specs The specifications to combine
     * @return Combined specification, or null if all are null
     */
    @SafeVarargs
    public final Specification<T> or(Specification<T>... specs) {
        if (specs == null || specs.length == 0) {
            return null;
        }
        
        Specification<T> result = null;
        for (Specification<T> spec : specs) {
            if (spec != null) {
                result = result == null ? spec : result.or(spec);
            }
        }
        return result;
    }
    
    /**
     * Helper method to get nested fields (e.g., "customer.name")
     * Handles dot notation for joined entities
     * 
     * @param root The root entity
     * @param fieldPath The field path (can include dots for nested fields)
     * @return The field expression
     */
    @SuppressWarnings("unchecked")
    private <V> Expression<V> getNestedField(Root<T> root, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        Expression<?> expr = root;
        
        for (String part : parts) {
            expr = ((Root<?>) expr).get(part);
        }
        
        return (Expression<V>) expr;
    }
}

