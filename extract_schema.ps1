$json = Get-Content 'N:\Projects\wd projects git\wd_portal_api\database_schema.json' -Raw | ConvertFrom-Json
$output = @()

foreach ($tableName in $json.PSObject.Properties.Name) {
    $table = $json.$tableName
    $columns = @()
    
    foreach ($col in $table.columns) {
        $columns += [PSCustomObject]@{
            Name = $col.name
            Type = $col.type
            Nullable = $col.nullable
            Default = $col.default
        }
    }
    
    $output += [PSCustomObject]@{
        Table = $tableName
        Columns = $columns
    }
}

$output | ConvertTo-Json -Depth 10 | Out-File 'N:\Projects\wd projects git\wd_portal_api\schema_extract.json' -Encoding UTF8

# Also output to console in readable format
foreach ($tableInfo in $output) {
    Write-Host "`n=== TABLE: $($tableInfo.Table) ===" -ForegroundColor Cyan
    foreach ($col in $tableInfo.Columns) {
        $defaultStr = if ($col.Default) { " DEFAULT: $($col.Default)" } else { "" }
        Write-Host "  - $($col.Name): $($col.Type) [Nullable: $($col.Nullable)]$defaultStr"
    }
}
