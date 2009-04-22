import sys
import os
import re

# Builds a list of all translation keys in the list of lines.
def indexFile(lines):
    allKeys = []
    for line in lines:
        comment = line.find("#")
        index = line.find("=")
        if (comment != 0) and (index > 0):
            allKeys.append(line[0:index])
    allKeys.sort()
    return allKeys
        

# Finds all keys in the first list that are not present in the second list:
def findMissingKeys(first, second):
    missing = []
    for key in first:
        if not key in second:
            missing.append(key)
    return missing


# Appends all the given keys to the file:
def appendMissingKeys(filename, keys):
    file = open(filename, "a")
    file.write("\n")
    for key in keys:
        file.write(key+"=\n")

def handleFileSet(mainFile, files, changeFiles):
    f1 = open(mainFile)
    lines = f1.readlines()
    f1.close()
    keys = indexFile(lines)
    keysPresent = []
    for i in range(0, len(files)):
        f2 = open(files[i])
        lines = f2.readlines()
        f2.close()
        keysPresent.append(indexFile(lines))
        missing = findMissingKeys(keys, keysPresent[i])
        
        print "\n\nFile '"+files[i]+"'\n"
        if len(missing) == 0:
            print "----> No missing keys."
        else:
            print "----> Missing keys:"
            for key in missing:
                print key

            if changeFiles == 1:
                print "Update file?",
                if raw_input() in ['y', 'Y']:
                    appendMissingKeys(files[i], missing)
            print ""
        
        # See if this file has keys that are not in the main file:
        redundant = findMissingKeys(keysPresent[i], keys)
        if len(redundant) > 0:
            print "----> Possible redundant keys (not in English language file):"
            for key in redundant:
                print key
                
            #if changeFiles == 1:
            #   print "Update file?",
            #   if raw_input() in ['y', 'Y']:
            #       removeRedundantKeys(files[i], redundant)
            print ""
        
                
def handleJavaCode(filename, lines, keyList, notTermList):
    
    startLen = 14;
    while len(lines) > 0:
        
        #print lines[0:100].strip()
    
        patt = re.compile(r'Globals\s*.lang\s*\(\s*"[^"]*"[^"]*\)')
        pattInner = re.compile(r'"[^"]*"')
        patt2 = re.compile(r'Globals\s*.lang\s*\(\s*"[^"]*"')
        result = patt.search(lines)
        result2 = patt2.search(lines)
        if result:
            span = result.span()
            theSpan = lines[span[0]:span[1]]
            spanInner = pattInner.search(theSpan).span() # We know there's a match here.
            found = theSpan[spanInner[0]+1:spanInner[1]-1].replace(" ", "_")
            #found = lines[i][span[0]+startLen:span[1]-2].replace(" ", "_")
            if not found == "" and found not in keyList:
                keyList.append(found)
            #else:
            #   print "Not adding: "+found
                
            # Remove the part of the file we have treated:
            if span[1] < len(lines):
                lines = lines[span[1]+1:]
            else:
                lines = ""

        elif result2:
            span = result2.span()
            theSpan = lines[span[0]:span[1]]
            spanInner = pattInner.search(theSpan).span() # We know there's a match here.
            found = theSpan[spanInner[0]+1:spanInner[1]-1].replace(" ", "_")
            print "Not terminated: "+found
            
            # Remove the part of the file we have treated:
            if span[1] < len(lines):
                lines = lines[span[1]+1:]
            else:
                lines = ""
        else:
            lines = "" # End search
            
            
# Find all Java source files in the given directory, and read the lines of each,
# calling handleJavaCode on the contents:   
def handleDir(lists, dirname, fnames):
    keyList, notTermList = lists    
    for file in fnames:
        if len(file) > 6 and file[(len(file)-5):len(file)] == ".java":
            fl = open(dirname+os.sep+file)
            lines = fl.read()
            fl.close()
            #print "Checking Java file '"+file+"'"
            handleJavaCode(file, lines, keyList, notTermList)

            
# Go through subdirectories and call handleDir on all diroctories:                      
def traverseFileTree(dir):
    keyList = []
    notTermList = []
    os.path.walk(dir, handleDir, (keyList, notTermList))
    print "Keys found: "+str(len(keyList))
    return keyList

    
# Searches out all translation calls in the Java source files, and reports which
# are not present in the given resource file.
def findNewKeysInJavaCode(mainFile, dir, update):
    keystempo = []
    keyListtempo = []
    f1 = open(mainFile)
    lines = f1.readlines()
    f1.close()
    keys = indexFile(lines)
    keyList = traverseFileTree(dir)
    
    #to process properly column character
    for key in keys:
       keystempo.append(key.replace("\:",":"))  
    keys=keystempo
    for key in keyList:
       keyListtempo.append(key.replace("\:",":"))  
    keyList=keyListtempo
    
    # Open the file again, for appending:
    if update:
        f1 = open(mainFile, "a")
        f1.write("\n")
        
    # Look for keys that are used in the code, but not present in the language file:
    for key in keyList:
        if key not in keys:
            print "Missing key: "+key
            if update:
                f1.write(key+"="+key+"\n")
    
    # Look for keys in the language file that are not used in the code:
    for key in keys:
        if key not in keyList:
            print "Possible redundant key: "+key
        
    if update:
        f1.close()
    
    
def lookForDuplicates(file):
    duplicount = 0
    f1 = open(file)
    lines = f1.readlines()
    f1.close()
    mappings = {}
    for line in lines:
        comment = line.find("#")
        index = line.find("=")
        if (comment != 0) and (index > 0):
            key = line[0:index]
            value = line[index+1:].strip()
            if key in mappings:
                mappings[key].append(value)
                duplicount += 1
                print "Duplicate: "+file+": "+key+" =",
                print mappings[key]
            else:
                mappings[key] = [value]
                if value == "":
                    print "Empty value: "+file+": "+key
                #print "New: "+value
    if duplicount == 0:
        print file+": No duplicates found."
        
        
############# Main part ###################

if len(sys.argv) == 1:
    print """This program must be run from the "src" directory right below the jabref base directory.
    
Usage: syncLang.py option   
Option can be one of the following:
    -d: Search the language files for empty and duplicate translations. 
        For each duplicate set found, a list will be printed showing the various 
        translations for the same key. There is currently to option to remove duplicates
        automatically.
        
    -s [-u]: Search the Java source files for language keys. All keys that are found in the source files
        but not in "JabRef_en.properties" are listed. If the -u option is specified, these keys will
        automatically be added to "JabRef_en.properties".
        
        The program will also list "Not terminated" keys. These are keys that are concatenated over 
        more than one line, that the program is not (currently) able to resolve.
        
        Finally, the program will list "Possible redundant keys". These are keys that are present in
        "JabRef_en.properties", but could not be found in the Java source code. Note that the 
        "Not terminated" keys will be likely to appear here, since they were not resolved.
        
    -t [-u]: Compare the contents of "JabRef_en.properties" and "Menu_en.properties" against the other 
        language files. The program will list for all the other files which keys from the English
        file are missing. Additionally, the program will list keys in the other files which are
        not present in the English file - possible redundant keys.
        
        If the -u option is specified, all missing keys will automatically be added to the files.
        There is currently no option to remove redundant keys automatically.
"""
    
elif (len(sys.argv) >= 2) and (sys.argv[1] == "-s"):
    if (len(sys.argv) >= 3) and (sys.argv[2] == "-u"):
        update = 1
    else:
        update = 0
    findNewKeysInJavaCode("resource/JabRef_en.properties", ".", update)
    
elif (len(sys.argv) >= 2) and (sys.argv[1] == "-t"):
    if (len(sys.argv) >= 3) and (sys.argv[2] == "-u"):
        changeFiles = 1
    else:
        changeFiles = 0
        
    handleFileSet("resource/JabRef_en.properties", ("resource/JabRef_de.properties",\
        "resource/JabRef_fr.properties", "resource/JabRef_it.properties",\
        "resource/JabRef_nl.properties", "resource/JabRef_da.properties",\
        "resource/JabRef_no.properties", "resource/JabRef_tr.properties",\
        "resource/JabRef_zh.properties", "resource/JabRef_zh.properties.UTF8"), changeFiles)
    handleFileSet("resource/Menu_en.properties", ("resource/Menu_de.properties",\
        "resource/Menu_fr.properties", "resource/Menu_it.properties",\
        "resource/Menu_nl.properties", "resource/Menu_da.properties",\
        "resource/Menu_no.properties", "resource/Menu_tr.properties",\
        "resource/Menu_zh.properties", "resource/Menu_zh.properties.UTF8"), changeFiles)
        
elif (len(sys.argv) >= 2) and (sys.argv[1] == "-d"):
    files = ("resource/JabRef_en.properties", "resource/JabRef_de.properties",\
        "resource/JabRef_fr.properties", "resource/JabRef_it.properties",\
        "resource/JabRef_no.properties", "resource/JabRef_nl.properties",\
        "resource/JabRef_da.properties", "resource/JabRef_tr.properties",\
        "resource/JabRef_zh.properties", "resource/JabRef_zh.properties.UTF8",\
        "resource/Menu_en.properties", "resource/Menu_de.properties",\
        "resource/Menu_fr.properties", "resource/Menu_it.properties",\
        "resource/Menu_no.properties", "resource/Menu_nl.properties",\
        "resource/Menu_da.properties", "resource/Menu_tr.properties",\
        "resource/Menu_zh.properties", "resource/Menu_zh.properties.UTF8")
    for file in files:
        lookForDuplicates(file)
