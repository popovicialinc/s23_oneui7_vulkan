#!/bin/bash

# Create a backup of the current file
cp /home/alin/Documents/GitHub/gama/app/src/main/java/com/popovicialinc/gama/MatrixPanels.kt /home/alin/Documents/GitHub/gama/app/src/main/java/com/popovicialinc/gama/MatrixPanels.kt.backup

# Restore the file to a working state by removing problematic sections
# This is a simple approach - we'll remove the problematic DisabledCardWrapper usage
# and restore the original working code

# For now, let's just try to compile with the original file
cd /home/alin/Documents/GitHub/gama
./gradlew clean
