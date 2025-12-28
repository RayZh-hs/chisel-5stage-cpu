#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

TEST_DIR="tests"
FAILED=0
PASSED=0

echo "Starting integrated tests..."

# Ensure the project is compiled
mill cpu.compile > /dev/null

for hex_file in "$TEST_DIR"/*.hex; do
    test_name=$(basename "$hex_file" .hex)
    expected_file="$TEST_DIR/$test_name.expected"
    
    if [ ! -f "$expected_file" ]; then
        echo -e "${RED}Error: Expected result file $expected_file not found for $test_name${NC}"
        continue
    fi
    
    expected_val=$(cat "$expected_file")
    
    # Run the simulation and capture the output
    # We use -i to avoid mill's background server output if possible, or just grep the result
    output=$(mill -i cpu.run "$hex_file" 1000 false 2>/dev/null)
    
    # Extract the exit code from the output
    actual_val=$(echo "$output" | sed 's/\x1b\[[0-9;]*m//g' | grep -o "Program exited with code: [0-9]*" | awk '{print $NF}')
    
    if [ "$actual_val" == "$expected_val" ]; then
        echo -e "${GREEN}[PASS]${NC} $test_name: Expected $expected_val, Got $actual_val"
        PASSED=$((PASSED + 1))
    else
        echo -e "${RED}[FAIL]${NC} $test_name: Expected $expected_val, Got '$actual_val'"
        echo "Full output:"
        echo "$output"
        FAILED=$((FAILED + 1))
    fi
done

echo "---------------------------------------"
echo "Tests completed: $((PASSED + FAILED))"
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"

if [ $FAILED -ne 0 ]; then
    exit 1
fi
