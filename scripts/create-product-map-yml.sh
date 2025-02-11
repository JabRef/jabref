#!/bin/bash

# Define output file
WORKFLOW_FILE=".github/workflows/product-map.yml"

# Find all .java files, remove leading ./, and format as GitHub Action input
EXPECTED_FILES=$(find . -type f -name "*.java" | sed 's|^\./||' | awk '{print "\"" $0 "\""}' | paste -sd "," -)
EXPECTED_FILES="(${EXPECTED_FILES})"

# Generate the workflow file
cat > "$WORKFLOW_FILE" <<EOF
name: Product Map Generation

on:
  push:
    branches:
      - main
  pull_request:

jobs:
  generate-map:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: ProductMap Map Generation
        uses: product-map/product-map-action@v1.0.15
        with:
          github_token: \${{ secrets.GITHUB_TOKEN }}
          expected_files: $EXPECTED_FILES
          user_email: koppdev@gmail.com
EOF

echo "GitHub Actions workflow generated at $WORKFLOW_FILE"
