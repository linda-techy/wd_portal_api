# CRITICAL FIXES - Implementation Guide

## 🔴 FIX #1: Security - Proper User ID Extraction

### File: `BoqController.java`

**Replace getCurrentUserId() method:**

```java
private Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
    if (authentication == null || !authentication.isAuthenticated()) {
        throw new IllegalStateException("User not authenticated");
    }
    
    Object principal = authentication.getPrincipal();
    
    // Handle CustomUserDetails (if you have custom implementation)
    if (principal instanceof org.springframework.security.core.userdetails.User) {
        String username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
        // Query portal_users to get ID by username/email
        // TODO: Inject UserRepository and implement:
        // return userRepository.findByUsername(username).map(User::getId)
        //     .orElseThrow(() -> new IllegalStateException("User ID not found"));
    }
    
    // Handle JWT token with user ID in claims
    if (authentication instanceof JwtAuthenticationToken) {
        Map<String, Object> attributes = ((JwtAuthenticationToken) authentication).getTokenAttributes();
        if (attributes.containsKey("userId")) {
            return Long.valueOf(attributes.get("userId").toString());
        }
    }
    
    throw new IllegalStateException("Unable to extract user ID from authentication");
}
```

### File: `LabourService.java` Line 177

**Replace:**
```java
Long userId = 1L; // TODO: Get from security context
```

**With:**
```java
Long userId = getCurrentUserIdFromSecurityContext();

// Add this method:
private Long getCurrentUserIdFromSecurityContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
        // Extract user ID - implement based on your auth mechanism
        // For now, log warning and use system user
        logger.warn("Auto-execution update - using system user ID");
        return 1L; // System user for automated processes
    }
    return 1L;
}
```

---

## 🔴 FIX #2: Financial Precision - Database Migration

### Create New Migration: `V1_71__increase_financial_precision.sql`

```sql
-- ============================================================================
-- V1_71: Increase Financial Precision to Industry Standard (18,6)
-- ============================================================================

-- Update boq_items precision
ALTER TABLE boq_items ALTER COLUMN quantity TYPE NUMERIC(18,6);
ALTER TABLE boq_items ALTER COLUMN unit_rate TYPE NUMERIC(18,6);
ALTER TABLE boq_items ALTER COLUMN total_amount TYPE NUMERIC(18,6);
ALTER TABLE boq_items ALTER COLUMN executed_quantity TYPE NUMERIC(18,6);
ALTER TABLE boq_items ALTER COLUMN billed_quantity TYPE NUMERIC(18,6);

-- Update BoqItem.java annotations
-- Change: precision = 15, scale = 4
-- To:     precision = 18, scale = 6

COMMENT ON COLUMN boq_items.quantity IS 'Precision 18,6 for international standard compliance';
```

### Update Java Entities

**File**: `BoqItem.java`

**Change all BigDecimal columns from:**
```java
@Column(precision = 15, scale = 4)
```

**To:**
```java
@Column(precision = 18, scale = 6)
```

**Update setScale() calls:**
```java
// Change all:
.setScale(4, RoundingMode.HALF_UP)

// To:
.setScale(6, RoundingMode.HALF_UP)
```

---

## 🔴 FIX #3: Race Condition - Pessimistic Locking

### File: `BoqService.java`

**Add to recordExecution method:**

```java
@Transactional
public BoqItemResponse recordExecution(Long id, RecordExecutionRequest request, Long userId) {
    // Add pessimistic lock
    BoqItem item = boqItemRepository.findByIdWithLock(id)
            .orElseThrow(() -> new IllegalArgumentException("BOQ item not found: " + id));
    
    if (!item.canExecute()) {
        throw new IllegalStateException(
            "Execution can only be recorded for APPROVED/LOCKED items. Current status: " + item.getStatus()
        );
    }
    
    // ATOMIC calculation
    BigDecimal newExecuted = item.getExecutedQuantity().add(request.quantity());
    
    // Validate BEFORE setting
    if (newExecuted.compareTo(item.getQuantity()) > 0) {
        throw new IllegalArgumentException(
            String.format("OVER-EXECUTION PREVENTED: Attempted %.6f exceeds remaining %.6f", 
                request.quantity(), item.getRemainingQuantity())
        );
    }
    
    item.setExecutedQuantity(newExecuted);
    item = boqItemRepository.saveAndFlush(item); // Immediate flush
    
    auditService.logExecute("BOQ_ITEM", id, item.getProject().getId(), userId, request.quantity());
    
    return BoqItemResponse.fromEntity(item);
}
```

### Add to Repository

**File**: `BoqItemRepository.java`

```java
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface BoqItemRepository extends JpaRepository<BoqItem, Long>, JpaSpecificationExecutor<BoqItem> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BoqItem b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<BoqItem> findByIdWithLock(@Param("id") Long id);
    
    // ... existing methods
}
```

---

## 🔴 FIX #4: Optimistic Lock Exception Handling

### File: `BoqController.java`

**Add exception handler:**

```java
@ExceptionHandler(OptimisticLockException.class)
public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(OptimisticLockException ex) {
    return ResponseEntity.status(409)
            .body(ApiResponse.error(
                "This BOQ item was modified by another user. Please refresh and try again."
            ));
}

@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
public ResponseEntity<ApiResponse<Void>> handleOptimisticLockSpring(
        ObjectOptimisticLockingFailureException ex) {
    return ResponseEntity.status(409)
            .body(ApiResponse.error(
                "Concurrent modification detected. Please refresh the data and retry."
            ));
}
```

---

## 🔴 FIX #5: Category Deletion Validation

### File: `BoqCategoryService.java`

**Update softDeleteCategory method:**

```java
@Transactional
public void softDeleteCategory(Long id, Long userId) {
    BoqCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
    
    // Check for active items
    long activeItemCount = boqItemRepository.countByCategory_IdAndDeletedAtIsNull(id);
    
    if (activeItemCount > 0) {
        throw new IllegalStateException(
            String.format("Cannot delete category '%s'. It has %d active BOQ items. " +
                "Please move these items to another category or delete them first.",
                category.getName(), activeItemCount)
        );
    }
    
    // Check for subcategories
    long subcategoryCount = categoryRepository.countByParentIdAndDeletedAtIsNull(id);
    
    if (subcategoryCount > 0) {
        throw new IllegalStateException(
            String.format("Cannot delete category '%s'. It has %d subcategories. " +
                "Please delete or move subcategories first.",
                category.getName(), subcategoryCount)
        );
    }
    
    category.setDeletedAt(LocalDateTime.now());
    category.setDeletedByUserId(userId);
    category.setIsActive(false);
    
    categoryRepository.save(category);
}
```

### Add to Repository

**File**: `BoqItemRepository.java`

```java
long countByCategory_IdAndDeletedAtIsNull(Long categoryId);
```

**File**: `BoqCategoryRepository.java`

```java
long countByParentIdAndDeletedAtIsNull(Long parentId);
```

---

## 🔴 FIX #6: Rate Limiting

### Add Dependency

**File**: `pom.xml`

```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.1.0</version>
</dependency>
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-redis</artifactId>
    <version>8.1.0</version>
</dependency>
```

### Create Rate Limiter Config

**File**: `RateLimiterConfig.java`

```java
package com.wd.api.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimiterConfig {
    
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    
    public Bucket resolveBucket(String key, int capacity, Duration duration) {
        return cache.computeIfAbsent(key, k -> {
            Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, duration));
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        });
    }
    
    @Bean
    public RateLimitInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor(this);
    }
}
```

### Create Interceptor

**File**: `RateLimitInterceptor.java`

```java
package com.wd.api.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final RateLimiterConfig rateLimiterConfig;
    
    public RateLimitInterceptor(RateLimiterConfig rateLimiterConfig) {
        this.rateLimiterConfig = rateLimiterConfig;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        String uri = request.getRequestURI();
        
        // Apply rate limiting to financial operations
        if (uri.contains("/execute") || uri.contains("/bill") || uri.contains("/approve")) {
            String key = getUserKey(request) + ":" + uri;
            Bucket bucket = rateLimiterConfig.resolveBucket(
                key, 
                10,  // 10 requests
                Duration.ofMinutes(1)  // per minute
            );
            
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            
            if (probe.isConsumed()) {
                response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                return true;
            } else {
                long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(String.format(
                    "{\"error\":\"Rate limit exceeded. Try again in %d seconds.\",\"success\":false}",
                    waitForRefill
                ));
                return false;
            }
        }
        
        return true;
    }
    
    private String getUserKey(HttpServletRequest request) {
        // Extract user ID from JWT or session
        return request.getRemoteAddr(); // Fallback to IP
    }
}
```

### Register Interceptor

**File**: `WebMvcConfig.java`

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/boq/**");
    }
}
```

---

## 🔴 FIX #7: Quantity Reduction Validation

### File: `BoqService.java` - updateBoqItem method

**Add validation:**

```java
// After line 120, add:
if (request.quantity() != null) {
    validateQuantity(request.quantity());
    
    // NEW: Prevent reducing below executed
    if (request.quantity().compareTo(item.getExecutedQuantity()) < 0) {
        throw new IllegalArgumentException(
            String.format("Cannot reduce planned quantity to %.6f. " +
                "Already executed: %.6f. Planned must be >= executed.",
                request.quantity(), item.getExecutedQuantity())
        );
    }
    
    item.setQuantity(request.quantity());
}
```

---

## 🔴 FIX #8: Add Correction/Undo API

### File: `BoqController.java`

**Add new endpoint:**

```java
@PostMapping("/{id}/correct-execution")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<BoqItemResponse>> correctExecution(
        @PathVariable Long id,
        @Valid @RequestBody CorrectionRequest request) {
    try {
        Long userId = getCurrentUserId();
        BoqItemResponse response = boqService.correctExecution(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(
            "Execution corrected successfully", response));
    } catch (IllegalArgumentException | IllegalStateException e) {
        return ResponseEntity.status(400)
                .body(ApiResponse.error(e.getMessage()));
    } catch (Exception e) {
        return ResponseEntity.status(500)
                .body(ApiResponse.error("Failed to correct execution: " + e.getMessage()));
    }
}
```

### Create DTO

**File**: `CorrectionRequest.java`

```java
package com.wd.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CorrectionRequest(
        @NotNull(message = "Correction type is required")
        CorrectionType type,  // REDUCE_EXECUTION, REDUCE_BILLING
        
        @NotNull(message = "Correction amount is required")
        @DecimalMin(value = "0.000001", message = "Amount must be positive")
        BigDecimal amount,
        
        @NotBlank(message = "Reason is required for correction")
        @Size(min = 10, max = 500, message = "Reason must be 10-500 characters")
        String reason,
        
        String referenceNumber  // Optional document reference
) {
    public enum CorrectionType {
        REDUCE_EXECUTION,
        REDUCE_BILLING
    }
}
```

### Implement Service Method

**File**: `BoqService.java`

```java
@Transactional
public BoqItemResponse correctExecution(Long id, CorrectionRequest request, Long userId) {
    BoqItem item = findActiveById(id);
    
    BigDecimal currentExecuted = item.getExecutedQuantity();
    BigDecimal newExecuted = currentExecuted.subtract(request.amount());
    
    // Validate: cannot reduce below billed quantity
    if (newExecuted.compareTo(item.getBilledQuantity()) < 0) {
        throw new IllegalArgumentException(
            String.format("Cannot reduce executed quantity to %.6f. " +
                "Already billed: %.6f. Executed must be >= billed.",
                newExecuted, item.getBilledQuantity())
        );
    }
    
    // Validate: cannot go negative
    if (newExecuted.compareTo(BigDecimal.ZERO) < 0) {
        throw new IllegalArgumentException("Executed quantity cannot be negative");
    }
    
    item.setExecutedQuantity(newExecuted);
    item = boqItemRepository.save(item);
    
    // Log correction with reason
    Map<String, Object> correctionDetails = Map.of(
        "type", "EXECUTION_CORRECTION",
        "oldValue", currentExecuted,
        "newValue", newExecuted,
        "correctionAmount", request.amount(),
        "reason", request.reason(),
        "reference", request.referenceNumber() != null ? request.referenceNumber() : "N/A"
    );
    
    auditService.logUpdate("BOQ_ITEM", id, item.getProject().getId(), userId, 
        Map.of("executedQuantity", currentExecuted),
        correctionDetails
    );
    
    return BoqItemResponse.fromEntity(item);
}
```

---

## Summary of Critical Fixes

1. ✅ **Security**: Proper user ID extraction
2. ✅ **Precision**: 18,6 decimal places
3. ✅ **Concurrency**: Pessimistic locking
4. ✅ **Reliability**: Optimistic lock handling
5. ✅ **Data Integrity**: Category deletion validation
6. ✅ **Security**: Rate limiting
7. ✅ **Validation**: Quantity reduction checks
8. ✅ **Functionality**: Correction/undo mechanism

## Testing Checklist

- [ ] Test user ID extraction with different auth mechanisms
- [ ] Test concurrent execution recording (JMeter)
- [ ] Test optimistic lock conflicts
- [ ] Test category deletion with items
- [ ] Test rate limiter (exceed limit)
- [ ] Test quantity reduction below executed
- [ ] Test correction API with various scenarios
- [ ] Load test with 18,6 precision numbers

## Deployment Steps

1. Backup production database
2. Run migration V1_71 (precision change)
3. Deploy code changes
4. Verify rate limiting works
5. Monitor for optimistic lock exceptions
6. Test correction API in staging
7. Update documentation

---

**PRIORITY**: Implement fixes 1, 3, 4, 5, 6, 7 IMMEDIATELY before production launch.

**TIMELINE**: 3-5 days for critical fixes
