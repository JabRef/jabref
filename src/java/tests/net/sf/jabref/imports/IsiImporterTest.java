package tests.net.sf.jabref.imports;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.OutputPrinterToNull;
import net.sf.jabref.imports.IsiImporter;

/**
 * Test cases for the IsiImporter
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 */
public class IsiImporterTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();

		if (Globals.prefs == null) {
			Globals.prefs = JabRefPreferences.getInstance();
		}
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testIsRecognizedFormat() throws IOException {

        IsiImporter importer = new IsiImporter();
		assertTrue(importer.isRecognizedFormat(IsiImporterTest.class
			.getResourceAsStream("IsiImporterTest1.isi")));

		assertTrue(importer.isRecognizedFormat(IsiImporterTest.class
			.getResourceAsStream("IsiImporterTestINSPEC.isi")));

		assertTrue(importer.isRecognizedFormat(IsiImporterTest.class
			.getResourceAsStream("IsiImporterTestWOS.isi")));

		assertTrue(importer.isRecognizedFormat(IsiImporterTest.class
			.getResourceAsStream("IsiImporterTestMedline.isi")));
	}

	public void testProcessSubSup() {

		HashMap<String, String> hm = new HashMap<String, String>();
		hm.put("title", "/sub 3/");
		IsiImporter.processSubSup(hm);
		assertEquals("$_3$", hm.get("title"));

		hm.put("title", "/sub   3   /");
		IsiImporter.processSubSup(hm);
		assertEquals("$_3$", hm.get("title"));

		hm.put("title", "/sub 31/");
		IsiImporter.processSubSup(hm);
		assertEquals("$_{31}$", hm.get("title"));

		hm.put("abstract", "/sub 3/");
		IsiImporter.processSubSup(hm);
		assertEquals("$_3$", hm.get("abstract"));

		hm.put("review", "/sub 31/");
		IsiImporter.processSubSup(hm);
		assertEquals("$_{31}$", hm.get("review"));

		hm.put("title", "/sup 3/");
		IsiImporter.processSubSup(hm);
		assertEquals("$^3$", hm.get("title"));

		hm.put("title", "/sup 31/");
		IsiImporter.processSubSup(hm);
		assertEquals("$^{31}$", hm.get("title"));

		hm.put("abstract", "/sup 3/");
		IsiImporter.processSubSup(hm);
		assertEquals("$^3$", hm.get("abstract"));

		hm.put("review", "/sup 31/");
		IsiImporter.processSubSup(hm);
		assertEquals("$^{31}$", hm.get("review"));

		hm.put("title", "/sub $Hello/");
		IsiImporter.processSubSup(hm);
		assertEquals("$_{\\$Hello}$", hm.get("title"));
	}

	public void testImportEntries() throws IOException {
		IsiImporter importer = new IsiImporter();

		List<BibtexEntry> entries = importer.importEntries(IsiImporterTest.class
			.getResourceAsStream("IsiImporterTest1.isi"), new OutputPrinterToNull());
		assertEquals(1, entries.size());
		BibtexEntry entry = entries.get(0);
		assertEquals("Optical properties of MgO doped LiNbO$_3$ single crystals", entry
			.getField("title"));
		assertEquals(
			"James Brown and James Marc Brown and Brown, J. M. and Brown, J. and Brown, J. M. and Brown, J.",
			entry.getField("author"));

		assertEquals(BibtexEntryType.ARTICLE, entry.getType());
		assertEquals("Optical Materials", entry.getField("journal"));
		assertEquals("2006", entry.getField("year"));
		assertEquals("28", entry.getField("volume"));
		assertEquals("5", entry.getField("number"));
		assertEquals("467--72", entry.getField("pages"));

		// What todo with PD and UT?
	}

	public void testImportEntriesINSPEC() throws IOException {
		IsiImporter importer = new IsiImporter();

		List<BibtexEntry> entries = importer.importEntries(IsiImporterTest.class
			.getResourceAsStream("IsiImporterTestInspec.isi"), new OutputPrinterToNull());

		assertEquals(2, entries.size());
		BibtexEntry a = entries.get(0);
		BibtexEntry b = entries.get(1);

		if (a.getField("title").equals(
			"Optical and photoelectric spectroscopy of photorefractive Sn$_2$P$_2$S$_6$ crystals")) {
			BibtexEntry tmp = a;
			a = b;
			b = tmp;
		}

		// Check a
		assertEquals(
			"Second harmonic generation of continuous wave ultraviolet light and production of beta -BaB$_2$O$_4$ optical waveguides",
			a.getField("title"));
		assertEquals(BibtexEntryType.ARTICLE, a.getType());

		assertEquals("Degl'Innocenti, R. and Guarino, A. and Poberaj, G. and Gunter, P.", a
			.getField("author"));
		assertEquals("Applied Physics Letters", a.getField("journal"));
		assertEquals("2006", a.getField("year"));
		assertEquals("#jul#", a.getField("month"));
		assertEquals("89", a.getField("volume"));
		assertEquals("4", a.getField("number"));

		// JI Appl. Phys. Lett. (USA)

		// BP 41103-1
		// EP 41103-41103-3
		// PS 41103-1-3
		// assertEquals("41103-1-3", a.getField("pages"));

		// LA English
		assertEquals("We report on the generation of continuous-wave (cw) ultraviolet"
			+ " (UV) laser light at lambda =278 nm by optical frequency doubling of"
			+ " visible light in beta -BaB$_2$O$_4$ waveguides. Ridge-type "
			+ "waveguides were produced by He$^+$ implantation, photolithography"
			+ " masking, and plasma etching. The final waveguides have core dimension"
			+ " of a few mu m$^2$ and show transmission losses of 5 dB/cm at 532 nm "
			+ "and less than 10 dB/cm at 266 nm. In our first experiments, a second "
			+ "harmonic power of 24 mu W has been generated at 278 nm in an 8 mm long "
			+ "waveguide pumped by 153 mW at 556 nm.".replaceFirst("266", "\n"), a.getField(
			"abstract").toString());
		/*
		 * DE Experimental/ barium compounds; ion implantation; optical harmonic
		 * generation; optical losses; optical pumping; photolithography; solid
		 * lasers; sputter etching; ultraviolet sources; waveguide lasers/
		 * second harmonic generation; continuous-wave light; beta -BaB/sub
		 * 2/O/sub 4/ optical waveguides; UV laser light; optical frequency
		 * doubling; visible light; ridge-type waveguides; He/sup +/
		 * implantation; photolithography masking; plasma etching; transmission
		 * losses; optical pumping; 278 nm; 532 nm; 266 nm; 24 muW; 8 mm; 153
		 * mW; 556 nm; BaB/sub 2/O/sub 4// A4265K Optical harmonic generation,
		 * frequency conversion, parametric oscillation and amplification A4255R
		 * Lasing action in other solids A4260B Design of specific laser systems
		 * B4340K Optical harmonic generation, frequency conversion, parametric
		 * oscillation and amplification B4320G Solid lasers/ wavelength
		 * 2.78E-07 m; wavelength 5.32E-07 m; wavelength 2.66E-07 m; power
		 * 2.4E-05 W; size 8.0E-03 m; power 1.53E-01 W; wavelength 5.56E-07 m/
		 * BaB2O4/ss B2/ss Ba/ss O4/ss B/ss O/ss C1 Degl'Innocenti, R.; Guarino,
		 * A.; Poberaj, G.; Gunter, P.; Nonlinear Opt. Lab., Inst. of Quantum
		 * Electron., Zurich, Switzerland
		 */
		assertEquals("Aip", a.getField("publisher"));
		// PV USA
		// NR 11
		// CO APPLAB
		// SN 0003-6951
		// ID
		// 0003-6951/2006/89(4)/041103-1(3)/$23.00],[0003-6951(20060724)89:4L.41103:SHGC;1-T],[S0003-6951(06)22430-6],[10.1063/1.2234275]
		// UT INSPEC:9027814

		// Check B
		assertEquals(
			"Optical and photoelectric spectroscopy of photorefractive Sn$_2$P$_2$S$_6$ crystals",
			b.getField("title").toString());
		assertEquals(BibtexEntryType.ARTICLE, b.getType());
	}

	public void testImportEntriesWOS() throws IOException {
		IsiImporter importer = new IsiImporter();

		List<BibtexEntry> entries = importer.importEntries(IsiImporterTest.class
			.getResourceAsStream("IsiImporterTestWOS.isi"), new OutputPrinterToNull());

		assertEquals(2, entries.size());
		BibtexEntry a = entries.get(0);
		BibtexEntry b = entries.get(1);

		if (a.getField("title").equals(
			"Optical waveguides in Sn2P2S6 by low fluence MeV He+ ion implantation")) {
			BibtexEntry tmp = a;
			a = b;
			b = tmp;
		}

		assertEquals("Optical and photoelectric spectroscopy of photorefractive Sn2P2S6 crystals",
			a.getField("title"));
		assertEquals("Optical waveguides in Sn2P2S6 by low fluence MeV He+ ion implantation", b
			.getField("title"));

		assertEquals("Journal Of Physics-Condensed Matter", a.getField("journal"));
	}

	public void testIsiAuthorsConvert() {
		assertEquals(
			"James Brown and James Marc Brown and Brown, J. M. and Brown, J. and Brown, J. M. and Brown, J.",
			IsiImporter
				.isiAuthorsConvert("James Brown and James Marc Brown and Brown, J.M. and Brown, J. and Brown, J.M. and Brown, J."));

		assertEquals(
			"Joffe, Hadine and Hall, Janet E. and Gruber, Staci and Sarmiento, Ingrid A. and Cohen, Lee S. and Yurgelun-Todd, Deborah and Martin, Kathryn A.",
			IsiImporter
				.isiAuthorsConvert("Joffe, Hadine; Hall, Janet E; Gruber, Staci; Sarmiento, Ingrid A; Cohen, Lee S; Yurgelun-Todd, Deborah; Martin, Kathryn A"));

	}

	public void testMonthConvert(){
		
		assertEquals("#jun#", IsiImporter.parseMonth("06"));
		assertEquals("#jun#", IsiImporter.parseMonth("JUN"));
		assertEquals("#jun#", IsiImporter.parseMonth("jUn"));
		assertEquals("#may#", IsiImporter.parseMonth("MAY-JUN"));
		assertEquals("#jun#", IsiImporter.parseMonth("2006 06"));
		assertEquals("#jun#", IsiImporter.parseMonth("2006 06-07"));
		assertEquals("#jul#", IsiImporter.parseMonth("2006 07 03"));
		assertEquals("#may#", IsiImporter.parseMonth("2006 May-Jun"));
	}
	
	public void testIsiAuthorConvert() {
		assertEquals("James Brown", IsiImporter.isiAuthorConvert("James Brown"));
		assertEquals("James Marc Brown", IsiImporter.isiAuthorConvert("James Marc Brown"));
		assertEquals("Brown, J. M.", IsiImporter.isiAuthorConvert("Brown, J.M."));
		assertEquals("Brown, J.", IsiImporter.isiAuthorConvert("Brown, J."));
		assertEquals("Brown, J. M.", IsiImporter.isiAuthorConvert("Brown, JM"));
		assertEquals("Brown, J.", IsiImporter.isiAuthorConvert("Brown, J"));
		assertEquals("Brown, James", IsiImporter.isiAuthorConvert("Brown, James"));
		assertEquals("Hall, Janet E.", IsiImporter.isiAuthorConvert("Hall, Janet E"));
		assertEquals("", IsiImporter.isiAuthorConvert(""));
	}

	public void testGetExtensions() {
		// new IsiImporter().getExtensions();
	}

	public void testGetIsCustomImporter() {
		IsiImporter importer = new IsiImporter();
		assertEquals(false, importer.getIsCustomImporter());
	}

	public void testImportIEEEExport() throws IOException {
		IsiImporter importer = new IsiImporter();

		List<BibtexEntry> entries = importer.importEntries(IsiImporterTest.class
			.getResourceAsStream("IEEEImport1.txt"), new OutputPrinterToNull());

		assertEquals(1, entries.size());
		BibtexEntry a = entries.get(0);
		
		assertEquals(a.getType().getName(), BibtexEntryType.ARTICLE, a.getType());
		assertEquals("Geoscience and Remote Sensing Letters, IEEE", a.getField("journal"));
		assertEquals(
			"Improving Urban Road Extraction in High-Resolution " +
			"Images Exploiting Directional Filtering, Perceptual " +
			"Grouping, and Simple Topological Concepts",
			a.getField("title"));

		assertEquals("4", a.getField("volume"));
		assertEquals("3", a.getField("number"));
		
		assertEquals("1545-598X", a.getField("SN"));  

		assertEquals("387--391", a.getField("pages"));

		assertEquals("Gamba, P. and Dell'Acqua, F. and Lisini, G.", a.getField("author"));

		assertEquals("2006", a.getField("year"));

		assertEquals("Perceptual grouping, street extraction, urban remote sensing", a.getField("keywords"));

		assertEquals("In this letter, the problem of detecting urban road " +
				"networks from high-resolution optical/synthetic aperture " +
				"radar (SAR) images is addressed. To this end, this letter " +
				"exploits a priori knowledge about road direction " +
				"distribution in urban areas. In particular, this letter " +
				"presents an adaptive filtering procedure able to capture the " +
				"predominant directions of these roads and enhance the " +
				"extraction results. After road element extraction, to both " +
				"discard redundant segments and avoid gaps, a special " +
				"perceptual grouping algorithm is devised, exploiting " +
				"colinearity as well as proximity concepts. Finally, the road " +
				"network topology is considered, checking for road " +
				"intersections and regularizing the overall patterns using " +
				"these focal points. The proposed procedure was tested on a " +
				"pair of very high resolution images, one from an optical " +
				"sensor and one from a SAR sensor. The experiments show an " +
				"increase in both the completeness and the quality indexes " +
				"for the extracted road network.", a.getField("abstract"));
		
	}
	
	public void testImportEntriesMedline() throws IOException {
		IsiImporter importer = new IsiImporter();

		List<BibtexEntry> entries = importer.importEntries(IsiImporterTest.class
			.getResourceAsStream("IsiImporterTestMedline.isi"), new OutputPrinterToNull());

		assertEquals(2, entries.size());
		BibtexEntry a = entries.get(0);
		BibtexEntry b = entries.get(1);

		if ((a.getField("title")).startsWith("Estrogen")) {
			BibtexEntry tmp = a;
			a = b;
			b = tmp;
		}

		// Check A
		assertEquals(
			"Effects of modafinil on cognitive performance and alertness during sleep deprivation.",
			a.getField("title"));

		assertEquals("Wesensten, Nancy J.", a.getField("author"));
		assertEquals("Curr Pharm Des", a.getField("journal"));
		assertEquals("2006", a.getField("year"));
		assertEquals(null, a.getField("month"));
		assertEquals("12", a.getField("volume"));
		assertEquals("20", a.getField("number"));
		assertEquals("2457--71", a.getField("pages"));
		assertEquals(BibtexEntryType.ARTICLE, a.getType());

		// Check B
		assertEquals(
			"Estrogen therapy selectively enhances prefrontal cognitive processes: a randomized, double-blind, placebo-controlled study with functional magnetic resonance imaging in perimenopausal and recently postmenopausal women.",
			b.getField("title").toString());
		assertEquals(
			"Joffe, Hadine and Hall, Janet E. and Gruber, Staci and Sarmiento, Ingrid A. and Cohen, Lee S. and Yurgelun-Todd, Deborah and Martin, Kathryn A.",
			b.getField("author"));
		assertEquals("2006", b.getField("year"));
		assertEquals("#may#", b.getField("month"));
		assertEquals("13", b.getField("volume"));
		assertEquals("3", b.getField("number"));
		assertEquals("411--22", b.getField("pages"));
		assertEquals(BibtexEntryType.ARTICLE, b.getType());
	}
}
