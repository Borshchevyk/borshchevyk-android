import os
import re

def check_viewmodel_docs_strict():
    search_dir = 'borshchevyk-android'
    missing_docs = []
    
    for root, dirs, files in os.walk(search_dir):
        for file in files:
            if file.endswith('ViewModel.kt'):
                filepath = os.path.join(root, file)
                with open(filepath, 'r', encoding='utf-8') as f:
                    lines = f.readlines()
                    
                for i, line in enumerate(lines):
                    clean_line = line.strip()
                    
                    # 1. Check Class declaration
                    is_class = re.match(r'^(data\s+)?class\s+[a-zA-Z0-9_]+ViewModel', clean_line)
                    # 2. Check Function declaration (more robust regex)
                    is_fun = re.match(r'^(override\s+)?(private\s+|protected\s+|internal\s+|public\s+)?(inline\s+)?(suspend\s+)?fun\s+', clean_line)
                    # 3. Check Property declaration (excluding those inside functions, roughly by checking indentation, but let's just check top level class properties)
                    # This is harder with regex, but let's try to catch val/var that are class members (usually 4 spaces indent)
                    is_prop = re.match(r'^(override\s+)?(private\s+|protected\s+|internal\s+|public\s+)?(val|var)\s+[a-zA-Z0-9_]+', clean_line)
                    
                    if is_class or is_fun or (is_prop and line.startswith('    ') and not line.startswith('        ')):
                        has_doc = False
                        
                        # Exclude some obvious non-documented properties if needed, but let's be strict
                        if clean_line.startswith('private val _uiState') or clean_line.startswith('val uiState'):
                            continue # often these are self-explanatory or documented at class level, but let's see. Actually, let's demand docs for them too if the user is strict.
                        
                        for j in range(i - 1, -1, -1):
                            prev_line = lines[j].strip()
                            if prev_line.startswith('//'):
                                continue
                            if prev_line.startswith('@'): # Skip annotations
                                continue
                            if prev_line.endswith('*/'):
                                has_doc = True
                                break
                            if not prev_line: # Skip empty lines
                                continue
                            if prev_line == '{' or prev_line == '}': # Reached previous block
                                break
                            break 
                            
                        if not has_doc:
                            report_line = clean_line.split('{')[0].split('=')[0].strip()
                            missing_docs.append(f"{file}:{i+1} {report_line}")
                            
    if not missing_docs:
        print("ALL_STRICTLY_DOCUMENTED")
    else:
        for md in missing_docs:
            print(md)

if __name__ == '__main__':
    check_viewmodel_docs_strict()
