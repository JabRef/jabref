import sys
import os
import re

keyFiles = {}

# Builds a list of all translation keys in the list of lines.
def indexFile(lines):
    allKeys = []
    for line in lines:
        comment = line.find("#")
        if (comment != 0):
            index = line.find("=")
            while ((index > 0) and (line[index-1]=="\\")):
                index = line.find("=", index+1)
            if (index > 0):
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
    #Extract first string parameter from Globals.lang call. E.g., Globals.lang("Default")
    patt = re.compile(r'Globals\s*\.\s*lang\s*\(\s*"((\\"|[^"])*)"[^"]*')
    #Find multiline Globals lang statements. E.g.:
    #Globals.lang("This is my string" +
    # "with a long text")
    patt2 = re.compile(r'Globals\s*\.\s*lang\s*\(([^)])*$')

    for linenum, curline in enumerate(lines.split("\n")):
        #Remove Java single line comments
        if curline.find("http://") < 0:
            curline = re.sub("//.*", "", curline)

        result = patt.search(curline)
        result2 = patt2.search(curline)
        if result:
            found = result.group(1)
            found = found.replace(" ", "_")
            #replace characters that need to be escaped in the language file
            found = found.replace("=", r"\=").replace(":",r"\:")
            #replace Java-escaped " to plain "
            found = found.replace(r'\"','"')
            if not found == "" and found not in keyList:
                keyList.append(found)
                keyFiles[found] = (filename, linenum)
                #print "Adding ", found
            #else:
            #   print "Not adding: "+found
                
        if result2 and curline.find('",') < 0:
            print "%s:%d: Not terminated: %s"%(filename, linenum+1, curline)
            
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
            handleJavaCode(dirname + "/" + file, lines, keyList, notTermList)

            
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
   
    # Open the file again, for appending:
    if update:
        f1 = open(mainFile, "a")
        f1.write("\n")
        
    # Look for keys that are used in the code, but not present in the language file:
    for keyOrig in keyList:
        key = keyOrig.replace("\\:",":").replace("\\=", "=")
        if key not in keys:
            fileName, lineNum = keyFiles[keyOrig]
            print "%s:%i:Missing key: %s"%(fileName, lineNum + 1, key)
	    value = key
	    key = key.replace(":", "\\:").replace("=", "\\=")

            if update:
                f1.write(key+"="+value+"\n")
    
    # Look for keys in the language file that are not used in the code:
    for key in keys:
        if key not in keyList:
            print "Possible redundant key: "+key
        
    if update:
        f1.close()
    
    
def lookForDuplicates(file, displayKeys):
    duplicount = 0
    f1 = open(file)
    lines = f1.readlines()
    f1.close()
    mappings = {}
    emptyVals = 0
    for line in lines:
        comment = line.find("#")
        index = line.find("=")
        if (comment != 0) and (index > 0):
            key = line[0:index]
            value = line[index+1:].strip()
            if key in mappings:
                mappings[key].append(value)
                duplicount += 1
                if displayKeys:
		  print "Duplicate: "+file+": "+key+" =",
		  print mappings[key]
            else:
                mappings[key] = [value]
                if value == "":
		    emptyVals = emptyVals + 1
                    if displayKeys:
		      print "Empty value: "+file+": "+key
                #print "New: "+value
    if duplicount > 0:
	dupstring = str(duplicount)+" duplicates. "
    else:
	dupstring = ""
    if emptyVals > 0:
        emptStr = str(emptyVals)+" empty values. "
    else:
	emptStr = ""
    if duplicount+emptyVals > 0:
	okString = ""
    else:
	okString = "ok"
    print file+": "+dupstring+emptStr+okString
    #print file+": "+str(emptyVals)+" empty values."
        
        
############# Main part ###################

if len(sys.argv) == 1:
    print """This program must be run from the "src" directory right below the jabref base directory.
    
Usage: syncLang.py option   
Option can be one of the following:
 
    -c: Search the language files for empty and duplicate translations. Display only
        counts for duplicated and empty values in each language file.

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
        "resource/JabRef_ja.properties", "resource/JabRef_pt_BR.properties",\
        "resource/JabRef_nl.properties", "resource/JabRef_da.properties",\
        "resource/JabRef_no.properties", "resource/JabRef_tr.properties",\
        "resource/JabRef_vi.properties", "resource/JabRef_in.properties", \
        "resource/JabRef_zh.properties"), changeFiles)
    handleFileSet("resource/Menu_en.properties", ("resource/Menu_de.properties",\
        "resource/Menu_fr.properties", "resource/Menu_it.properties",\
        "resource/Menu_ja.properties", "resource/Menu_pt_BR.properties",\
        "resource/Menu_nl.properties", "resource/Menu_da.properties",\
        "resource/Menu_es.properties",\
        "resource/Menu_no.properties", "resource/Menu_tr.properties",\
        "resource/Menu_vi.properties", "resource/Menu_in.properties",\
        "resource/Menu_zh.properties"), changeFiles)
        
elif (len(sys.argv) >= 2) and ((sys.argv[1] == "-d") or (sys.argv[1] == "-c")):
    files = ("resource/JabRef_en.properties", "resource/JabRef_de.properties",\
        "resource/JabRef_fr.properties", "resource/JabRef_it.properties",\
        "resource/JabRef_ja.properties", "resource/JabRef_pt_BR.properties",\
        "resource/JabRef_no.properties", "resource/JabRef_nl.properties",\
        "resource/JabRef_da.properties",\
        "resource/JabRef_tr.properties",\
        "resource/JabRef_vi.properties", "resource/JabRef_in.properties",\
        "resource/JabRef_zh.properties",\
        "resource/Menu_en.properties", "resource/Menu_de.properties",\
        "resource/Menu_fr.properties", "resource/Menu_it.properties",\
        "resource/Menu_ja.properties", "resource/Menu_pt_BR.properties",\
        "resource/Menu_no.properties", "resource/Menu_nl.properties",\
        "resource/Menu_da.properties", "resource/Menu_es.properties", \
        "resource/Menu_tr.properties",\
        "resource/Menu_vi.properties", "resource/Menu_in.properties",\
        "resource/Menu_zh.properties")
    for file in files:
        lookForDuplicates(file, sys.argv[1] == "-d")
