import sys

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

				

if (len(sys.argv) < 2) or not (sys.argv[1] == "-u"):
	changeFiles = 0
else:
	changeFiles = 1
handleFileSet("JabRef_en.properties", ("JabRef_de.properties", "JabRef_fr.properties", "JabRef_no.properties"), changeFiles)
handleFileSet("Menu_en.properties", ("Menu_de.properties", "Menu_fr.properties", "Menu_no.properties"), changeFiles)