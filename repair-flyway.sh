#!/bin/bash

echo "========================================"
echo "Flyway Repair Script"
echo "========================================"
echo ""
echo "This will repair the Flyway schema history table"
echo "to update checksums for modified migrations."
echo ""
read -p "Press enter to continue..."

echo ""
echo "Running Flyway repair..."
echo ""

# Load environment variables from .env file if it exists
if [ -f .env ]; then
    echo "Loading environment variables from .env..."
    export $(cat .env | xargs)
fi

# Run Flyway repair
mvn flyway:repair -Dflyway.url="$DB_URL" -Dflyway.user="$DB_USERNAME" -Dflyway.password="$DB_PASSWORD"

echo ""
echo "========================================"
echo "Flyway repair completed!"
echo ""
echo "You can now start your application."
echo "========================================"
