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
			print "-> No missing keys."
		else:
			if changeFiles == 1:
				appendMissingKeys(files[i], missing)
			print "-> Missing keys:"
			for key in missing:
				print key+"="

				
def handleJavaCode(filename, lines, keyList, notTermList):
	
	startLen = 14;
	while len(lines) > 0:
		
		#print lines[0:100].strip()
	
		patt = re.compile(r'Globals.lang\s*\(\s*"[^"]*"\s*\)')
		pattInner = re.compile(r'"[^"]*"')
		patt2 = re.compile(r'Globals.lang\s*\(\s*"[^"]*"')
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
			#	print "Not adding: "+found
				
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
	for key in keyList:
		if key not in keys:
			print "Missing key: "+key
			if update:
				f1.write(key+"=\n")
	
	# Look for keys in the language file that are not used in the code:
	for key in keys:
		if key not in keyList:
			print "Possible redundant key: "+key
		
	if update:
		f1.close()
	
	
	
####### Main part ###################3
if (len(sys.argv) >= 2) and (sys.argv[1] == "-s"):
	if (len(sys.argv) >= 3) and (sys.argv[2] == "-u"):
		update = 1
	else:
		update = 0
	findNewKeysInJavaCode("resource/JabRef_en.properties", ".", update)
	
else:					
	if (len(sys.argv) < 2) or not (sys.argv[1] == "-u"):
		changeFiles = 0
	else:
		changeFiles = 1
		
	handleFileSet("resource/JabRef_en.properties", ("resource/JabRef_de.properties", "resource/JabRef_fr.properties",\
		"resource/JabRef_no.properties"), changeFiles)
	handleFileSet("resource/Menu_en.properties", ("resource/Menu_de.properties", "resource/Menu_fr.properties",\
		"resource/Menu_no.properties"), changeFiles)