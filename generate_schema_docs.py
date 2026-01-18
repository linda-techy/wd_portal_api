#!/usr/bin/env python3
"""
Generate comprehensive database schema documentation from database_schema.json
"""

import json
from collections import defaultdict
from typing import Dict, List, Tuple

def load_schema(json_file: str) -> Dict:
    """Load schema from JSON file"""
    with open(json_file, 'r', encoding='utf-8') as f:
        return json.load(f)

def get_data_type_mapping():
    """Return PostgreSQL to Java data type mappings"""
    return {
        'bigint': 'Long',
        'integer': 'Integer',
        'smallint': 'Short',
        'numeric': 'BigDecimal',
        'decimal': 'BigDecimal',
        'real': 'Float',
        'double precision': 'Double',
        'character varying': 'String',
        'varchar': 'String',
        'text': 'String',
        'boolean': 'Boolean',
        'date': 'LocalDate',
        'timestamp without time zone': 'LocalDateTime',
        'timestamp with time zone': 'ZonedDateTime',
        'time without time zone': 'LocalTime',
        'jsonb': 'String',
        'json': 'String',
        'uuid': 'UUID',
        'bytea': 'byte[]',
        'int8': 'Long',
        'int4': 'Integer',
        'int2': 'Short',
        'float8': 'Double',
        'float4': 'Float',
    }

def format_column_type(col: Dict) -> str:
    """Format column type with precision/scale if applicable"""
    col_type = col['type']
    if col['max_length']:
        return f"{col_type}({col['max_length']})"
    elif col['precision'] and col['scale']:
        return f"{col_type}({col['precision']},{col['scale']})"
    elif col['precision']:
        return f"{col_type}({col['precision']})"
    return col_type

def build_relationship_graph(schema: Dict) -> Dict[str, List[Tuple[str, str]]]:
    """Build relationship graph from foreign keys"""
    relationships = defaultdict(list)
    for table_name, table_data in schema.items():
        for fk in table_data.get('foreign_keys', []):
            relationships[table_name].append((
                fk['column'],
                fk['references_table'],
                fk['references_column']
            ))
    return relationships

def generate_table_doc(table_name: str, table_data: Dict, relationships: Dict) -> str:
    """Generate documentation for a single table"""
    lines = [f"## {table_name}", ""]
    
    # Columns
    lines.append("### Columns")
    lines.append("")
    lines.append("| Column Name | Data Type | Nullable | Default | Notes |")
    lines.append("|-------------|-----------|----------|---------|-------|")
    
    for col in table_data['columns']:
        col_type = format_column_type(col)
        nullable = "✗" if col['nullable'] == 'NO' else "✓"
        default = col['default'] if col['default'] else "-"
        
        # Check if it's a primary key
        is_pk = any(pk['column'] == col['name'] for pk in table_data.get('primary_keys', []))
        notes = "🔑 PK" if is_pk else "-"
        
        # Check if it's a foreign key
        fk_info = None
        for fk in table_data.get('foreign_keys', []):
            if fk['column'] == col['name']:
                fk_info = f"🔗 FK → `{fk['references_table']}.{fk['references_column']}`"
                break
        
        if fk_info:
            notes = fk_info if notes == "-" else f"{notes}, {fk_info}"
        
        lines.append(f"| `{col['name']}` | `{col_type}` | {nullable} | {default} | {notes} |")
    
    lines.append("")
    
    # Primary Keys
    if table_data.get('primary_keys'):
        lines.append("### Primary Key")
        lines.append("")
        for pk in table_data['primary_keys']:
            lines.append(f"- `{pk['column']}`")
        lines.append("")
    
    # Foreign Keys
    if table_data.get('foreign_keys'):
        lines.append("### Foreign Keys")
        lines.append("")
        for fk in table_data['foreign_keys']:
            lines.append(f"- `{fk['column']}` → `{fk['references_table']}.{fk['references_column']}`")
        lines.append("")
    
    # Unique Constraints
    if table_data.get('unique_constraints'):
        lines.append("### Unique Constraints")
        lines.append("")
        for uc in table_data['unique_constraints']:
            lines.append(f"- `{uc['column']}`")
        lines.append("")
    
    return "\n".join(lines)

def generate_relationship_diagram(relationships: Dict) -> str:
    """Generate Mermaid diagram for relationships"""
    lines = ["## Entity Relationship Overview", ""]
    lines.append("```mermaid")
    lines.append("erDiagram")
    lines.append("")
    
    # Group relationships by referenced table
    ref_groups = defaultdict(list)
    for table, fks in relationships.items():
        for col, ref_table, ref_col in fks:
            ref_groups[ref_table].append((table, col, ref_col))
    
    # Generate relationships
    added_rels = set()
    for table, fks in relationships.items():
        for col, ref_table, ref_col in fks:
            rel_key = (table, ref_table)
            if rel_key not in added_rels:
                lines.append(f"    {ref_table} ||--o{{ {table} : \"has\"")
                added_rels.add(rel_key)
    
    lines.append("```")
    lines.append("")
    return "\n".join(lines)

def generate_main_documentation(schema: Dict) -> str:
    """Generate main documentation"""
    lines = []
    
    # Header
    lines.append("# WallDot Builders - Database Schema Documentation")
    lines.append(f"**Total Tables:** {len(schema)}")
    lines.append("**Database:** PostgreSQL (wdTestDB)")
    lines.append("")
    
    # Table of Contents
    lines.append("## Table of Contents")
    lines.append("")
    for i, table_name in enumerate(sorted(schema.keys()), 1):
        lines.append(f"{i}. [{table_name}](#{table_name.replace('_', '-')})")
    lines.append("")
    lines.append("---")
    lines.append("")
    
    # Data Type Mappings
    lines.append("## Data Type Mappings (PostgreSQL → Java)")
    lines.append("")
    lines.append("| PostgreSQL Type | Java Type | Notes |")
    lines.append("|----------------|-----------|-------|")
    type_mapping = get_data_type_mapping()
    for pg_type, java_type in sorted(type_mapping.items()):
        notes = "64-bit integer" if java_type == "Long" else "32-bit integer" if java_type == "Integer" else ""
        lines.append(f"| `{pg_type}` | `{java_type}` | {notes} |")
    lines.append("")
    lines.append("---")
    lines.append("")
    
    # Build relationships
    relationships = build_relationship_graph(schema)
    
    # Generate table documentation
    for table_name in sorted(schema.keys()):
        lines.append(generate_table_doc(table_name, schema[table_name], relationships))
        lines.append("---")
        lines.append("")
    
    # Relationship Diagram
    lines.append(generate_relationship_diagram(relationships))
    lines.append("")
    
    # Best Practices
    lines.append("## Best Practices")
    lines.append("")
    lines.append("### Foreign Key Constraints")
    lines.append("")
    lines.append("- Always check for related records before deleting parent entities")
    lines.append("- Use cascade delete only for non-critical audit/log data (e.g., activity_feeds)")
    lines.append("- Business-critical entities (tasks, invoices, payments) should require explicit deletion")
    lines.append("- Handle `DataIntegrityViolationException` with clear error messages")
    lines.append("")
    lines.append("### Nullable vs Non-Nullable Fields")
    lines.append("")
    lines.append("- Fields marked as `nullable: NO` must always have values")
    lines.append("- Use `@Column(nullable = false)` in JPA entities for non-nullable fields")
    lines.append("- Validate required fields at the service layer before persistence")
    lines.append("")
    lines.append("### Data Type Considerations")
    lines.append("")
    lines.append("- Use `BigDecimal` for monetary values (numeric/decimal types)")
    lines.append("- Use `LocalDate` for dates without time")
    lines.append("- Use `LocalDateTime` for timestamps without timezone")
    lines.append("- Use `String` for JSONB fields (parse as needed)")
    lines.append("- Use `Long` for all ID fields (bigint)")
    lines.append("")
    lines.append("### Cascade Delete Rules")
    lines.append("")
    lines.append("The following entities can be safely cascaded on project deletion:")
    lines.append("- `activity_feeds` - Audit logs")
    lines.append("")
    lines.append("The following entities require explicit deletion:")
    lines.append("- `tasks` - Business data")
    lines.append("- `project_invoices` - Financial records")
    lines.append("- `receipts` - Payment records")
    lines.append("- `purchase_orders` - Procurement data")
    lines.append("- `subcontract_work_orders` - Contract data")
    lines.append("- All other business-critical entities")
    lines.append("")
    
    return "\n".join(lines)

def main():
    """Main function"""
    print("Generating database schema documentation...")
    
    # Load schema
    schema = load_schema('database_schema.json')
    print(f"Loaded schema for {len(schema)} tables")
    
    # Generate documentation
    doc = generate_main_documentation(schema)
    
    # Write to file
    output_file = 'DATABASE_SCHEMA.md'
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(doc)
    
    print(f"Documentation generated: {output_file}")
    print(f"Total tables documented: {len(schema)}")

if __name__ == "__main__":
    main()
