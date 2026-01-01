$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8081"

# 1. Create Test User
Write-Host "Creating Test User..."
Invoke-RestMethod -Uri "$baseUrl/auth/create-test-user" -Method Post

# 2. Login
Write-Host "Logging in..."
$loginBody = @{
    email = "admin@test.com"
    password = "password"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $loginResponse.accessToken
$headers = @{
    Authorization = "Bearer $token"
    "Content-Type" = "application/json"
}

# 2.1 Get Current User ID
# 2.1 Get Current User ID
$userId = $loginResponse.user.id
if (-not $userId) {
    # Fallback if field is named different in JSON (e.g. userInfo vs user)
    $userId = $loginResponse.userInfo.id
}
Write-Host "User ID: $userId"

# 3. Get Project ID (Assuming first one)
$projects = Invoke-RestMethod -Uri "$baseUrl/customer-projects" -Method Get -Headers $headers
if ($projects.data.Count -eq 0) {
    Write-Host "No projects found. Creating a test project..."
    $projBody = @{
        name = "Procurement Test Project"
        location = "Test Location"
        state = "Kerala"
        district = "Ernakulam"
        sqfeet = 1000
        contractType = "TURNKEY"
    } | ConvertTo-Json
    $newProj = Invoke-RestMethod -Uri "$baseUrl/customer-projects" -Method Post -Body $projBody -Headers $headers -ContentType "application/json"
    $projectId = $newProj.id
    Write-Host "Created Test Project: $($newProj.name) (ID: $projectId)"
} else {
    $projectId = $projects.data[0].id
    Write-Host "Using Existing Project ID: $projectId"
}

# 4. Create Vendor
Write-Host "Creating Vendor..."
$vendorBody = @{
    name = "AutoTest Vendor $(Get-Random)"
    phone = "98765$(Get-Random -Minimum 10000 -Maximum 99999)"
    email = "vendor$(Get-Random)@test.com"
    vendorType = "MATERIAL"
    active = $true
} | ConvertTo-Json
$vendor = Invoke-RestMethod -Uri "$baseUrl/api/procurement/vendors" -Method Post -Body $vendorBody -Headers $headers -ContentType "application/json"
$vendorId = $vendor.id
Write-Host "Vendor Created: $($vendor.name) (ID: $vendorId)"

# 5. Create Material
Write-Host "Creating Material..."
$materialBody = @{
    name = "AutoTest Material $(Get-Random)"
    unit = "NOS"
    category = "Construction"
    active = $true
} | ConvertTo-Json
$material = Invoke-RestMethod -Uri "$baseUrl/api/inventory/materials" -Method Post -Body $materialBody -Headers $headers -ContentType "application/json"
$materialId = $material.id
Write-Host "Material Created: $($material.name) (ID: $materialId)"

# 6. Create Purchase Order
Write-Host "Creating Purchase Order..."
$poBody = @{
    vendorId = $vendorId
    projectId = $projectId
    poDate = (Get-Date).ToString("yyyy-MM-dd")
    expectedDeliveryDate = (Get-Date).AddDays(7).ToString("yyyy-MM-dd")
    createdById = $userId
    items = @(
        @{
            description = "Test Item"
            quantity = 10
            unit = "Nos"
            rate = 500
            gstPercentage = 18
            amount = 5900
            materialId = $materialId
        }
    )
    totalAmount = 5000
    gstAmount = 900
    netAmount = 5900
} | ConvertTo-Json -Depth 5

$po = Invoke-RestMethod -Uri "$baseUrl/api/procurement/purchase-orders" -Method Post -Body $poBody -Headers $headers -ContentType "application/json"
Write-Host "PO Created: $($po.poNumber) (ID: $($po.id))"
$poId = $po.id

# 7. Record GRN
Write-Host "Recording GRN..."
$grnBody = @{
    poId = $poId
    receivedById = $userId
    invoiceNumber = "INV-$((Get-Random))"
    invoiceDate = (Get-Date).ToString("yyyy-MM-dd")
    challanNumber = "CH-$((Get-Random))"
    notes = "Auto Verified GRN"
} | ConvertTo-Json
$grn = Invoke-RestMethod -Uri "$baseUrl/api/procurement/grn" -Method Post -Body $grnBody -Headers $headers -ContentType "application/json"
Write-Host "GRN Recorded: $($grn.grnNumber)"

# 8. Verify Stock
Write-Host "Verifying Stock..."
$stock = Invoke-RestMethod -Uri "$baseUrl/api/inventory/stock/project/$projectId" -Method Get -Headers $headers
$itemStock = $stock | Where-Object { $_.materialId -eq $materialId }
if ($itemStock -and $itemStock.currentQuantity -ge 10) {
    Write-Host "SUCCESS: Stock updated correctly to $($itemStock.currentQuantity)"
} else {
    Write-Error "FAILURE: Stock not updated correctly or not found."
}
