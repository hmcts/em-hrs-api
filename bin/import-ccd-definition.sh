#!/usr/bin/env bash

# Exit on error
set -e

# Get the absolute path of the project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Default values
definition_input_dir="${PROJECT_ROOT}/src/functionalTest/resources"
definition_output_file="${PROJECT_ROOT}/src/functionalTest/resources/CCD_HRS_v1.7-AAT.xlsx"

# If no parameters are provided, use defaults
if [ $# -eq 0 ]; then
    echo "No parameters provided, using default values"
    additional_params=""
else
    additional_params="$@"
fi

echo "Definition input directory: ${definition_input_dir}"
echo "Definition output file: ${definition_output_file}"

# Create output directory if it doesn't exist
mkdir -p "$(dirname "${definition_output_file}")"

# Check if the output file exists, if not, create an empty one
touch "${definition_output_file}"

# Execute the process-definition script to generate the XLSX file
echo "Processing definition files..."
"${PROJECT_ROOT}/bin/utils/process-definition.sh" "${definition_input_dir}" "${definition_output_file}" ${additional_params}

# Import the definition file
echo "Importing definition file..."
"${PROJECT_ROOT}/bin/utils/ccd-import-definition.sh" "${definition_output_file}"
