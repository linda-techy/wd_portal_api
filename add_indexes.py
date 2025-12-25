import psycopg2

# Database connection parameters
db_config = {
    'host': '46.202.164.251',
    'port': 5432,
    'database': 'wdTestDB',
    'user': 'postgres',
    'password': 'Staygether@2025',
    'connect_timeout': 10
}

def execute_indexes():
    """Execute index creation SQL"""
    
    try:
        # Connect to database
        print("Connecting to database...")
        conn = psycopg2.connect(**db_config)
        cur = conn.cursor()
        
        # Read SQL file
        print("Reading index SQL file...")
        with open('src/main/resources/db/migration/V2__add_performance_indexes.sql', 'r') as f:
            sql = f.read()
        
        # Split by statements
        statements = [s.strip() for s in sql.split(';') if s.strip() and not s.strip().startswith('--')]
        
        print(f"Found {len(statements)} SQL statements to execute")
        
        success_count = 0
        skip_count = 0
        
        for i, statement in enumerate(statements, 1):
            if statement:
                try:
                    # Extract index name for reporting
                    if 'CREATE INDEX' in statement:
                        idx_name = statement.split('idx_')[1].split()[0] if 'idx_' in statement else f'index_{i}'
                        print(f"[{i}/{len(statements)}] Creating index: idx_{idx_name}...", end=' ')
                        cur.execute(statement)
                        conn.commit()
                        print("OK")
                        success_count += 1
                    else:
                        print(f"[{i}/{len(statements)}] Executing statement...")
                        cur.execute(statement)
                        conn.commit()
                        success_count += 1
                except psycopg2.errors.DuplicateTable as e:
                    conn.rollback()
                    print("(already exists)")
                    skip_count += 1
                except Exception as e:
                    conn.rollback()
                    print(f"ERROR: {e}")
        
        print(f"\n=== Summary ===")
        print(f"Created: {success_count}")
        print(f"Skipped (already exist): {skip_count}")
        print(f"Total: {len(statements)}")
        
        # Close connection
        cur.close()
        conn.close()
        
        return True
        
    except Exception as e:
        print(f"ERROR: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    print("=" * 60)
    print("Adding Performance Indexes to Database")
    print("=" * 60)
    success = execute_indexes()
    if success:
        print("\nIndexes added successfully!")
    else:
        print("\nFailed to add indexes!")
