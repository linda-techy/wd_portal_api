#!/bin/bash
# API Endpoint Testing Script for WD Portal API
# Tests all fixed URL endpoints and error scenarios

set -e

# Configuration
API_BASE_URL="${API_BASE_URL:-http://localhost:8081}"
TOKEN="${JWT_TOKEN:-}"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function to make authenticated requests
api_call() {
    local method=$1
    local endpoint=$2
    local data=$3
    
    if [ -z "$TOKEN" ]; then
        echo -e "${RED}ERROR: JWT_TOKEN environment variable not set${NC}"
        exit 1
    fi
    
    if [ -z "$data" ]; then
        curl -s -X "$method" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -w "\nHTTP_STATUS:%{http_code}" \
            "${API_BASE_URL}${endpoint}"
    else
        curl -s -X "$method" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "$data" \
            -w "\nHTTP_STATUS:%{http_code}" \
            "${API_BASE_URL}${endpoint}"
    fi
}

# Test function
test_endpoint() {
    local test_name=$1
    local method=$2
    local endpoint=$3
    local expected_status=$4
    local data=$5
    
    echo -e "\n${YELLOW}Testing:${NC} $test_name"
    echo "  Endpoint: $method $endpoint"
    echo "  Expected: HTTP $expected_status"
    
    response=$(api_call "$method" "$endpoint" "$data")
    actual_status=$(echo "$response" | grep "HTTP_STATUS" | cut -d':' -f2)
    
    if [ "$actual_status" = "$expected_status" ]; then
        echo -e "  ${GREEN}✓ PASS${NC} (HTTP $actual_status)"
        ((TESTS_PASSED++))
    else
        echo -e "  ${RED}✗ FAIL${NC} (Expected $expected_status, got $actual_status)"
        echo "  Response: $response"
        ((TESTS_FAILED++))
    fi
}

echo "========================================="
echo "  WD Portal API Endpoint Tests"
echo "========================================="
echo "Base URL: $API_BASE_URL"
echo ""

# =============================================================================
# 1. VIEW360 ENDPOINTS (Fixed URLs with /api prefix)
# =============================================================================
echo -e "\n${GREEN}=== View360 Endpoints ===${NC}"

test_endpoint \
    "Get View360 tours by project" \
    "GET" \
    "/api/view360/project/1" \
    "200"

test_endpoint \
    "Delete View360 tour" \
    "DELETE" \
    "/api/view360/999999" \
    "404"

# =============================================================================
# 2. BOQ ENDPOINTS (Fixed URLs with /api prefix)
# =============================================================================
echo -e "\n${GREEN}=== BOQ Endpoints ===${NC}"

test_endpoint \
    "Get BOQ summary for project" \
    "GET" \
    "/api/boq/project/1/summary" \
    "200"

# =============================================================================
# 3. GALLERY ENDPOINTS (Fixed URLs with /api prefix)
# =============================================================================
echo -e "\n${GREEN}=== Gallery Endpoints ===${NC}"

test_endpoint \
    "Get gallery image count" \
    "GET" \
    "/api/gallery/project/1/count" \
    "200"

# =============================================================================
# 4. ACCOUNTS PAYABLE ENDPOINTS (Fixed URLs with /api prefix)
# =============================================================================
echo -e "\n${GREEN}=== Accounts Payable Endpoints ===${NC}"

test_endpoint \
    "Get accounts payable aging" \
    "GET" \
    "/api/accounts-payable/aging" \
    "200"

test_endpoint \
    "Get vendor outstanding" \
    "GET" \
    "/api/accounts-payable/vendor-outstanding" \
    "200"

test_endpoint \
    "Get pending invoices" \
    "GET" \
    "/api/accounts-payable/invoices/pending" \
    "200"

# =============================================================================
# 5. SITE VISITS ENDPOINTS (Fixed URLs with /api prefix)
# =============================================================================
echo -e "\n${GREEN}=== Site Visits Endpoints ===${NC}"

test_endpoint \
    "Get all active visits" \
    "GET" \
    "/api/site-visits/all-active" \
    "200"

test_endpoint \
    "Get visits by project" \
    "GET" \
    "/api/site-visits/project/1" \
    "200"

test_endpoint \
    "Get visit types" \
    "GET" \
    "/api/site-visits/types" \
    "200"

# =============================================================================
# 6. SITE REPORTS ENDPOINTS (Error Handling Tests)
# =============================================================================
echo -e "\n${GREEN}=== Site Reports Error Handling ===${NC}"

test_endpoint \
    "Get non-existent site report (404)" \
    "GET" \
    "/api/site-reports/999999" \
    "404"

test_endpoint \
    "Search site reports" \
    "GET" \
    "/api/site-reports/search?page=0&size=10" \
    "200"

# =============================================================================
# 7. CORRELATION ID TESTS
# =============================================================================
echo -e "\n${GREEN}=== Correlation ID Tests ===${NC}"

echo "Testing correlation ID in error responses..."
response=$(api_call "GET" "/api/site-reports/999999")
if echo "$response" | grep -q "correlationId"; then
    echo -e "  ${GREEN}✓ PASS${NC} Correlation ID present in error response"
    ((TESTS_PASSED++))
else
    echo -e "  ${RED}✗ FAIL${NC} Correlation ID missing from error response"
    ((TESTS_FAILED++))
fi

# =============================================================================
# SUMMARY
# =============================================================================
echo ""
echo "========================================="
echo "  Test Summary"
echo "========================================="
echo -e "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests Failed: ${RED}$TESTS_FAILED${NC}"
echo "Total Tests: $((TESTS_PASSED + TESTS_FAILED))"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}All tests passed! ✓${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed! ✗${NC}"
    exit 1
fi
