import os
import re

def count_junit_tests():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(script_dir, '..', '..'))

    total = 0
    for root, _, files in os.walk(project_root):
        for file in files:
            if file.endswith(".java"):
                with open(os.path.join(root, file), 'r', encoding='utf-8') as f:
                    in_block_comment = False
                    for line in f:
                        line = line.strip()

                        if '/*' in line:
                            in_block_comment = True
                        if '*/' in line:
                            in_block_comment = False
                            continue
                        if in_block_comment or line.startswith('//'):
                            continue
                        if re.search(r'^\s*@Test\b', line):
                            total += 1
    return total

if __name__ == "__main__":
    print(count_junit_tests())
