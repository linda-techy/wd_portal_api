import psycopg2
import json

# Database connection parameters
db_config = {
    'host': '46.202.164.251',
    'port': 5432,
    'database': 'wdTestDB',
    'user': 'postgres',
    'password': 'Staygether@2025',
    'connect_timeout': 10
}

def fetch_schema():
    """Fetch complete database schema from PostgreSQL"""
    
    try:
        # Connect to database
        print("Connecting to database...")
        conn = psycopg2.connect(**db_config)
        cur = conn.cursor()
        
        schema_data = {}
        
        # 1. Get all tables
        print("\nFetching tables...")
        cur.execute("""
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_schema = 'public' 
            ORDER BY table_name
        """)
        tables = [row[0] for row in cur.fetchall()]
        print(f"Found {len(tables)} tables")
        
        # 2. Get columns for each table
        print("Fetching columns...")
        cur.execute("""
            SELECT 
                c.table_name,
                c.column_name,
                c.data_type,
                c.column_default,
                c.is_nullable,
                c.character_maximum_length,
                c.numeric_precision,
                c.numeric_scale,
                c.udt_name
            FROM information_schema.columns c
            WHERE c.table_schema = 'public'
            ORDER BY c.table_name, c.ordinal_position
        """)
        
        columns_data = cur.fetchall()
        
        # Organize by table
        for table in tables:
            schema_data[table] = {
                'columns': [],
                'primary_keys': [],
                'foreign_keys': [],
                'unique_constraints': []
            }
        
        for col in columns_data:
            table_name = col[0]
            if table_name in schema_data:
                schema_data[table_name]['columns'].append({
                    'name': col[1],
                    'type': col[2],
                    'default': col[3],
                    'nullable': col[4],
                    'max_length': col[5],
                    'precision': col[6],
                    'scale': col[7],
                    'udt_name': col[8]
                })
        
        # 3. Get primary keys
        print("Fetching primary keys...")
        cur.execute("""
            SELECT
                tc.table_name,
                kcu.column_name,
                tc.constraint_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu 
                ON tc.constraint_name = kcu.constraint_name
                AND tc.table_schema = kcu.table_schema
            WHERE tc.table_schema = 'public'
                AND tc.constraint_type = 'PRIMARY KEY'
            ORDER BY tc.table_name
        """)
        
        for row in cur.fetchall():
            table_name, column_name, constraint_name = row
            if table_name in schema_data:
                schema_data[table_name]['primary_keys'].append({
                    'column': column_name,
                    'constraint': constraint_name
                })
        
        # 4. Get foreign keys
        print("Fetching foreign keys...")
        cur.execute("""
            SELECT
                tc.table_name,
                kcu.column_name,
                ccu.table_name AS foreign_table_name,
                ccu.column_name AS foreign_column_name,
                tc.constraint_name
            FROM information_schema.table_constraints AS tc
            JOIN information_schema.key_column_usage AS kcu
                ON tc.constraint_name = kcu.constraint_name
                AND tc.table_schema = kcu.table_schema
            JOIN information_schema.constraint_column_usage AS ccu
                ON ccu.constraint_name = tc.constraint_name
                AND ccu.table_schema = tc.table_schema
            WHERE tc.table_schema = 'public'
                AND tc.constraint_type = 'FOREIGN KEY'
            ORDER BY tc.table_name
        """)
        
        for row in cur.fetchall():
            table_name, column_name, foreign_table, foreign_column, constraint_name = row
            if table_name in schema_data:
                schema_data[table_name]['foreign_keys'].append({
                    'column': column_name,
                    'references_table': foreign_table,
                    'references_column': foreign_column,
                    'constraint': constraint_name
                })
        
        # 5. Get unique constraints
        print("Fetching unique constraints...")
        cur.execute("""
            SELECT
                tc.table_name,
                kcu.column_name,
                tc.constraint_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu 
                ON tc.constraint_name = kcu.constraint_name
                AND tc.table_schema = kcu.table_schema
            WHERE tc.table_schema = 'public'
                AND tc.constraint_type = 'UNIQUE'
            ORDER BY tc.table_name
        """)
        
        for row in cur.fetchall():
            table_name, column_name, constraint_name = row
            if table_name in schema_data:
                schema_data[table_name]['unique_constraints'].append({
                    'column': column_name,
                    'constraint': constraint_name
                })
        
        # Save to JSON file
        output_file = 'database_schema.json'
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(schema_data, f, indent=2, default=str)
        
        print(f"\nSchema exported to {output_file}")
        
        # Close connection
        cur.close()
        conn.close()
        
        return schema_data
        
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
        return None

if __name__ == "__main__":
    print("=" * 60)
    print("Database Schema Extractor")
    print("=" * 60)
    schema = fetch_schema()
    if schema:
        print(f"\nSuccessfully fetched schema for {len(schema)} tables")
    else:
        print("\nFailed to fetch schema")
