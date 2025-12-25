import json

# Load the schema data
with open('database_schema.json', 'r', encoding='utf-8') as f:
    schema = json.load(f)

# Generate markdown documentation
md_content = []
md_content.append("# WallDot Builders - Database Schema Documentation\n")
md_content.append(f"**Total Tables:** {len(schema)}\n")
md_content.append(f"**Database:** PostgreSQL (wdTestDB)\n\n")

# Create table of contents
md_content.append("## Table of Contents\n")
for i, table_name in enumerate(sorted(schema.keys()), 1):
    md_content.append(f"{i}. [{table_name}](#{table_name.replace('_', '-')})\n")

md_content.append("\n---\n\n")

# PostgreSQL to Java type mapping reference
md_content.append("## Data Type Mappings (PostgreSQL → Java)\n\n")
md_content.append("| PostgreSQL Type | Java Type | Notes |\n")
md_content.append("|----------------|-----------|-------|\n")
md_content.append("| `bigint` | `Long` | 64-bit integer |\n")
md_content.append("| `integer` | `Integer` | 32-bit integer |\n")
md_content.append("| `varchar(n)` | `String` | Variable character with max length |\n")
md_content.append("| `text` | `String` | Unlimited text |\n")
md_content.append("| `boolean` | `Boolean` | True/False |\n")
md_content.append("| `numeric(p,s)` | `BigDecimal` | Precise decimal numbers |\n")
md_content.append("| `double precision` | `Double` | Floating point |\n")
md_content.append("| `date` | `LocalDate` | Date without time |\n")
md_content.append("| `timestamp` | `LocalDateTime` | Date and time |\n")
md_content.append("| `uuid` | `UUID` | Universally unique identifier |\n")
md_content.append("| `jsonb` | `String` or custom | JSON binary format |\n\n")

md_content.append("---\n\n")

# Document each table
for table_name in sorted(schema.keys()):
    table_data = schema[table_name]
    
    md_content.append(f"## {table_name}\n\n")
    
    # Columns table
    md_content.append("### Columns\n\n")
    md_content.append("| Column Name | Data Type | Nullable | Default | Notes |\n")
    md_content.append("|-------------|-----------|----------|---------|-------|\n")
    
    for col in table_data['columns']:
        col_name = col['name']
        data_type = col['type']
        
        # Add length/precision info
        if col['max_length']:
            data_type += f"({col['max_length']})"
        elif col['precision'] and col['scale'] is not None:
            data_type += f"({col['precision']},{col['scale']})"
        elif col['precision']:
            data_type += f"({col['precision']})"
            
        nullable = "✓" if col['nullable'] == 'YES' else "✗"
        default = col['default'] if col['default'] else "-"
        if default and len(default) > 30:
            default = default[:27] + "..."
        
        # Identify if primary key
        is_pk = any(pk['column'] == col_name for pk in table_data['primary_keys'])
        is_fk = any(fk['column'] == col_name for fk in table_data['foreign_keys'])
        is_unique = any(uk['column'] == col_name for uk in table_data['unique_constraints'])
        
        notes = []
        if is_pk:
            notes.append("🔑 PK")
        if is_fk:
            fk_ref = next((fk for fk in table_data['foreign_keys'] if fk['column'] == col_name), None)
            if fk_ref:
                notes.append(f"🔗 FK → `{fk_ref['references_table']}.{fk_ref['references_column']}`")
        if is_unique:
            notes.append("🔒 UNIQUE")
            
        notes_str = " ".join(notes) if notes else "-"
        
        md_content.append(f"| `{col_name}` | `{data_type}` | {nullable} | `{default}` | {notes_str} |\n")
    
    md_content.append("\n")
    
    # Primary Keys
    if table_data['primary_keys']:
        md_content.append("### Primary Key\n\n")
        pk_columns = [pk['column'] for pk in table_data['primary_keys']]
        if len(pk_columns) == 1:
            md_content.append(f"- `{pk_columns[0]}`\n\n")
        else:
            md_content.append(f"- Composite: `{', '.join(pk_columns)}`\n\n")
    
    # Foreign Keys
    if table_data['foreign_keys']:
        md_content.append("### Foreign Keys\n\n")
        for fk in table_data['foreign_keys']:
            md_content.append(f"- `{fk['column']}` → `{fk['references_table']}.{fk['references_column']}`\n")
        md_content.append("\n")
    
    # Unique Constraints
    if table_data['unique_constraints']:
        md_content.append("### Unique Constraints\n\n")
        for uk in table_data['unique_constraints']:
            md_content.append(f"- `{uk['column']}`\n")
        md_content.append("\n")
    
    md_content.append("---\n\n")

# Write to file
output_file = 'DATABASE_SCHEMA.md'
with open(output_file, 'w', encoding='utf-8') as f:
    f.write(''.join(md_content))

print(f"Documentation generated: {output_file}")
print(f"Total tables documented: {len(schema)}")
print(f"Total columns: {sum(len(t['columns']) for t in schema.values())}")
print(f"Total foreign keys: {sum(len(t['foreign_keys']) for t in schema.values())}")
