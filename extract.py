import csv
import os
import subprocess
import uuid
from concurrent.futures import ProcessPoolExecutor, as_completed

# Constants for input and output directories
INPUT_DIR = 'input'
OUTPUT_DIR = 'output'
METHOD_CSV = os.path.join(INPUT_DIR, 'method-p.csv')
METHOD_OUTPUT_CSV = os.path.join(OUTPUT_DIR, 'method-p-source.csv')


# Function to run a Git command in a project directory
def run_git_command(project_path, git_command):
    result = subprocess.run(git_command, cwd=project_path, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    if result.returncode != 0:
        raise Exception(f"Git command failed: {git_command}\nError: {result.stderr}")
    return result.stdout


def find_package_path_instance(input_project_dir, class_file):
    """
    Search for the class file within the project directory.
    Return the file path if exactly one instance is found.
    If ambiguous (i.e., found in multiple locations), return None.
    Use early stopping to avoid unnecessary searching once the file is found.
    """
    java_file_path = None

    # Walk through the project directory looking for the class file
    for root, dirs, files in os.walk(input_project_dir):
        if root.startswith(os.path.join(input_project_dir, '.git')):
            continue
        # Check if the class file exists in the current directory
        current_file_path = os.path.join(root, class_file)
        if os.path.exists(current_file_path):
            # If we already found a file earlier, we have ambiguity
            if java_file_path:
                print(f"Ambiguity: found {class_file} in multiple locations, skipping.")
                return None
            # Otherwise, mark the first found instance
            java_file_path = current_file_path

        # Stop walking into subdirectories if we've found the file
        if java_file_path:
            dirs[:] = []  # Clear dirs to stop further traversal in this path

    return java_file_path


def extract_class(row, input_project_dir, output_project_dir):
    project = row[0]
    hash_val = row[1]
    long_name = row[2]
    parent = row[3]
    num_bugs = row[76]

    # Convert parent package to a directory path (e.g., org.antlr.v4.test.tool -> org/antlr/v4/test/tool)
    class_file = parent.replace('.', os.path.sep)
    if class_file.__contains__('$'):
        class_file = class_file.split('$', 1)[0]

    # Search for all instances of the package path within the project directory
    java_file_path = find_package_path_instance(input_project_dir, f"{class_file}.java")

    if not java_file_path:
        print(f"Java file {class_file} not found or ambiguous in {project}, skipping.")
        return None

    # Generate unique GUID and create output Java file
    guid = str(uuid.uuid4())
    output_java_file = os.path.join(output_project_dir, f"{guid}.java")

    # Extract the full source code (ideally, you'd parse this and extract only the method)
    try:
        with open(java_file_path, 'r') as src_file:
            java_source_code = src_file.read()

        # Write the full source (or method) to the new file
        with open(output_java_file, 'w') as out_file:
            out_file.write(java_source_code)
    except Exception as e:
        print(f"Failed to read/write Java source for {long_name}: {e}")
        return None

    # Return the extracted method details for the output CSV
    relative_path = os.path.relpath(output_java_file, OUTPUT_DIR)
    print(f"Finished Extracting {parent} from {java_file_path}")
    return [project, relative_path, hash_val, long_name, parent, num_bugs]


def process_csv():
    # Read the CSV and group rows by project
    with open(METHOD_CSV, 'r') as csvfile:
        reader = csv.reader(csvfile)
        headers = next(reader)  # Skip the header
        project_rows = {}

        for row in reader:
            project = row[0]
            if project == 'mct':  # Ignore 'mct' project
                continue
            if project not in project_rows:
                project_rows[project] = []
            project_rows[project].append(row)

    # Multi-processing for concurrent project processing
    with ProcessPoolExecutor() as executor:
        # Submit a parallel process for each project
        future_to_project = {executor.submit(process_project, project, rows): project for project, rows in
                             project_rows.items()}
        all_results = []

        for future in as_completed(future_to_project):
            project = future_to_project[future]
            try:
                result = future.result()
                if result:
                    all_results.append(result)  # Collect each project's results as a list
            except Exception as e:
                print(f"Error processing project {project}: {e}")

    # Flatten the list of lists after all processes are done
    extracted_methods = [item for sublist in all_results for item in sublist]

    # Write the method-p-source.csv file
    with open(METHOD_OUTPUT_CSV, 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        writer.writerow(['Project', 'Source-file', 'Hash', 'Long-Name', 'Parent', 'Number-Of-Bugs'])  # CSV headers
        writer.writerows(extracted_methods)


def process_project(project, rows):
    input_project_dir = os.path.join(INPUT_DIR, project)
    output_project_dir = os.path.join(OUTPUT_DIR, project)

    # Ensure output directory exists for this project
    os.makedirs(output_project_dir, exist_ok=True)

    # Skip projects that are missing
    if not os.path.exists(input_project_dir):
        print(f"Project directory {input_project_dir} not found, skipping.")
        return []

    extracted_methods = []

    # Process each row sequentially within the project
    for row in rows:
        hash_val = row[1]

        # Checkout the specific commit
        try:
            run_git_command(input_project_dir, ['git', 'switch', '--discard-changes','--detach', hash_val])
        except Exception as e:
            print(f"Failed to checkout {hash_val} in {project}: {e}")
            continue

        # Extract the method from the source file
        result = extract_class(row, input_project_dir, output_project_dir)
        if result:
            extracted_methods.append(result)

    return extracted_methods


if __name__ == '__main__':
    # Create output directory if it doesn't exist
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    process_csv()
