# Custom Exports in JabRef

This guide explains how to **modify existing exports** and **add new custom exports** in JabRef.  


---

## 1. Locate the layout files

JabRef uses **layout files** to define how CSV, OpenOffice, or other exports look.

- These files are in the repo under:
- `jabref\jablib\src\main\resources\resource\openoffice`


- Example files:
    - `openoffice-csv.begin.layout` → controls **header row** (column names)
    - `openoffice-csv.layout` → controls **each entry row** (data fields)

---

## 2. Make a copy 

**Do not modify the original files** in the repo.

- Create a folder on your computer, e.g.:  CustomFiles

- Copy these layout files there.
    - `openoffice-csv.begin.layout`
    - `openoffice-csv.layout` 
  
- You will edit these copies for your custom export.

---

## 3. Add a new field

If you want to include a new field, like `Asset` :



1. Open `csv.layout` in the folder you have created → add your field at the **end of the line**:
2. Open `begin.layout` in the folder you have created  → add a **header for your column** at the **end**: 


> ⚠ The name in `csv.layout` (`\asset`) and in `begin.layout` (`"Asset"`) **must match**.

---

## 4. Modify an existing export

To change an existing export:

1. Open the existing layout files (`*.layout` and `*.begin.layout`)
2. Edit **columns or fields** as needed
3. Save the files in your **local folder**
4. Use the steps below to make a **custom export** in JabRef

---

## Download JabRef

You can download the latest version of JabRef from the official website:

[JabRef Official Download Page](https://www.jabref.org/#downloads)

- Choose the version for your operating system (Windows, macOS, Linux)
- Install it following the instructions on the website

---

## 5. Add a custom export in JabRef

1. Open JabRef → go to **Preferences → Custom Export Formats**
2. Click **Add**
3. Fill in the fields:
    - **Format name:** a descriptive name like `Asset Export`
    - **Main layout file:** select your copied `openoffice-csv.layout`
    - **File extension:** `csv`
4. Save and **restart JabRef**
5. If a custom export does not appear, restart JabRef and verify the selected layout file.

> JabRef will now know to use your layout when exporting.

---

## 6. Export your library

1. Open any library (`.bib`) in JabRef
2. Go to **File → Export**
3. Select your custom export (`Asset Export`)
4. Choose a location and file name (e.g., `MyLibrary.csv`)
5. Click **Save / Export**

The exported CSV will now include your **new column and data**.

---

## 7. Summary

- **`begin.layout`** → defines **column headers**
- **`csv.layout`** → defines **row data**
- **Custom export in Preferences** → tells JabRef **which layout to use**
- Always **copy the original layout files** to a safe folder before editing
- Restart JabRef after creating a new export




