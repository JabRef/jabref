package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
public class DOABFetcherTest {
    private DOABFetcher fetcher;
    private BibEntry David_Opal;
    private BibEntry Ronald_Snijder;
    private BibEntry Andrew_Perrin;
    private BibEntry Felipe_Gonzalez;
    private BibEntry Carl_Marnewick;

    @BeforeEach
    public void setUp() throws Exception {
        fetcher = new DOABFetcher();

        David_Opal = new BibEntry();
        David_Opal.setField(StandardField.AUTHOR, "David Pol");
        David_Opal.setField(StandardField.TITLE, "I Open Fire");
        David_Opal.setField(StandardField.TYPE, "book");
        David_Opal.setField(StandardField.DOI, "10.21983/P3.0086.1.00");
        David_Opal.setField(StandardField.PAGES, "56");
        David_Opal.setField(StandardField.YEAR, "2014");
        David_Opal.setField(StandardField.URI, "https://directory.doabooks.org/handle/20.500.12854/34739");
        David_Opal.setField(StandardField.ABSTRACT, "David Pol presents an ontology of war in the form of " +
                "the lyric poem. “Do you hear what I’m shooting at you?” In I Open Fire, all relation is " +
                "warfare. Minefields compromise movement. Intention aims. Touch burns. Sex explodes bodies. " +
                "Time ticks in bomb countdowns. Sound is sirens. Plenitude is debris. All of it under " +
                "surveillance. “My world is critically injured. It was ambushed.” The poems in this book perform" +
                " the reductions and repetitions endemic to war itself, each one returning the reader to the same," +
                " unthinkable place in which the range of human experience has been so flattened that, despite all" +
                " the explosive action, “Almost nothing is happening.” Against this backdrop, we continue to fall" +
                " in love. But Pol’s poems remind us that this is no reason for optimism. Does love offer a" +
                " delusional escape from war, or are relationships the very definition of combat? These poems take" +
                " up the themes of love, sex, marriage, touch, hope — in short, the many dimensions of" +
                " interpersonal connection — in a world in unprecedentedly critical condition. “And when the night" +
                " goes off the shock wave throws us apart toward each other.”");
        David_Opal.setField(StandardField.LANGUAGE, "English");
        David_Opal.setField(StandardField.KEYWORDS, "poetry, love, warfare");
        David_Opal.setField(StandardField.PUBLISHER, "punctum books");

        Ronald_Snijder = new BibEntry();
        Ronald_Snijder.setField(StandardField.AUTHOR, "Ronald Snijder");
        Ronald_Snijder.setField(StandardField.TITLE, "The deliverance of open access books");
        Ronald_Snijder.setField(StandardField.TYPE, "book");
        Ronald_Snijder.setField(StandardField.DOI, "10.26530/OAPEN_1004809");
        Ronald_Snijder.setField(StandardField.PAGES, "234");
        Ronald_Snijder.setField(StandardField.YEAR, "2019");
        Ronald_Snijder.setField(StandardField.URI, "https://directory.doabooks.org/handle/20.500.12854/26303");
        Ronald_Snijder.setField(StandardField.ABSTRACT, "In many scholarly disciplines, books - not articles" +
                " - are the norm. As print runs become smaller, the question arises whether publishing monographs" +
                " in open access helps to make their contents globally accessible. To answer this question, the" +
                " results of multiple studies on the usage of open access books are presented. The research" +
                " focuses on three areas: economic viability; optimization of open access monographs" +
                " infrastructure and measuring the effects of open access in terms of scholarly impact and" +
                " societal influence. Each chapter reviews a different aspect: book sales, digital dissemination," +
                " open licenses, user communities, measuring usage, developing countries and the effects on" +
                " citations and social media.");
        Ronald_Snijder.setField(StandardField.LANGUAGE, "English");
        Ronald_Snijder.setField(StandardField.KEYWORDS, "Open Access, Monographs, OAPEN Library, " +
                "Directory of Open Access Books");
        Ronald_Snijder.setField(StandardField.PUBLISHER, "Amsterdam University Press");

        Andrew_Perrin = new BibEntry();
        Andrew_Perrin.setField(StandardField.EDITOR, "Andrew Perrin and Loren T. Stuckenbruck");
        Andrew_Perrin.setField(StandardField.TITLE, "Four Kingdom Motifs before and beyond the Book of Daniel");
        Andrew_Perrin.setField(StandardField.TYPE, "book");
        Andrew_Perrin.setField(StandardField.DOI, "10.1163/9789004443280");
        Andrew_Perrin.setField(StandardField.PAGES, "354");
        Andrew_Perrin.setField(StandardField.YEAR, "2020");
        Andrew_Perrin.setField(StandardField.URI, "https://directory.doabooks.org/handle/20.500.12854/68086");
        Andrew_Perrin.setField(StandardField.ABSTRACT, "The four kingdoms motif enabled writers of various " +
                "cultures, times, and places, to periodize history as the staged succession of empires " +
                "barrelling towards an utopian age. The motif provided order to lived experiences under empire" +
                " (the present), in view of ancestral traditions and cultural heritage (the past), and inspired" +
                " outlooks assuring hope, deliverance, and restoration (the future). Four Kingdom Motifs before" +
                " and beyond the Book of Daniel includes thirteen essays that explore the reach and redeployment" +
                " of the motif in classical and ancient Near Eastern writings, Jewish and Christian scriptures," +
                " texts among the Dead Sea Scrolls, Apocrypha and pseudepigrapha, depictions in European" +
                " architecture and cartography, as well as patristic, rabbinic, Islamic, and African writings " +
                "from antiquity through the Mediaeval eras. Readership: Advanced students and scholars of the " +
                "textual formation, apocalyptic theology, and historiographies of the book of Daniel and its " +
                "diverse reception by writers and communities.");
        Andrew_Perrin.setField(StandardField.LANGUAGE, "English");
        Andrew_Perrin.setField(StandardField.KEYWORDS, "Religion");
        Andrew_Perrin.setField(StandardField.PUBLISHER, "Brill");

        Felipe_Gonzalez = new BibEntry();
        Felipe_Gonzalez.setField(StandardField.EDITOR, "Felipe Gonzalez Toro and Antonios Tsourdos");
        Felipe_Gonzalez.setField(StandardField.TITLE, "UAV Sensors for Environmental Monitoring");
        Felipe_Gonzalez.setField(StandardField.TYPE, "book");
        Felipe_Gonzalez.setField(StandardField.DOI, "10.3390/books978-3-03842-754-4");
        Felipe_Gonzalez.setField(StandardField.PAGES, "670");
        Felipe_Gonzalez.setField(StandardField.YEAR, "2018");
        Felipe_Gonzalez.setField(StandardField.URI, "https://directory.doabooks.org/handle/20.500.12854/39793");
        Felipe_Gonzalez.setField(StandardField.ABSTRACT, "The rapid development and growth of UAVs as a " +
                "remote sensing platform, as well as advances in the miniaturization of instrumentation and data" +
                " systems, are catalyzing a renaissance in remote sensing in a variety of fields and disciplines" +
                " from precision agriculture to ecology, atmospheric research, and disaster response. This" +
                " Special Issue was open for submissions that highlight advances in the development and use of" +
                " sensors deployed on UAVs. Topics include, but were not limited, to: • Optical, multi-spectral," +
                " hyperspectral, laser, and optical SAR technologies • Gas analyzers and sensors • Artificial" +
                " intelligence and data mining based strategies from UAVs • UAV onboard data storage," +
                " transmission, and retrieval • Collaborative strategies and mechanisms to control multiple UAVs" +
                " and sensor networks • UAV sensor applications: precision agriculture; pest detection, forestry," +
                " mammal species tracking search and rescue; target tracking, the monitoring of the atmosphere;" +
                " chemical, biological, and natural disaster phenomena; fire prevention, flood prevention;" +
                " volcanic monitoring, pollution monitoring, micro-climates and land use");
        Felipe_Gonzalez.setField(StandardField.LANGUAGE, "English");
        Felipe_Gonzalez.setField(StandardField.KEYWORDS, "UAV sensors, Environmental Monitoring, drones, unmanned aerial vehicles");
        Felipe_Gonzalez.setField(StandardField.PUBLISHER, "MDPI - Multidisciplinary Digital Publishing Institute");

        Carl_Marnewick = new BibEntry();
        Carl_Marnewick.setField(StandardField.AUTHOR, "Carl Marnewick and Wikus Erasmus and Joseph Nazeer");
        Carl_Marnewick.setField(StandardField.TITLE, "The symbiosis between information system project complexity and information system project success");
        Carl_Marnewick.setField(StandardField.TYPE, "book");
        Carl_Marnewick.setField(StandardField.DOI, "10.4102/aosis.2017.itpsc45");
        Carl_Marnewick.setField(StandardField.PAGES, "184");
        Carl_Marnewick.setField(StandardField.YEAR, "2017");
        Carl_Marnewick.setField(StandardField.URI, "https://directory.doabooks.org/handle/20.500.12854/38792");
        Carl_Marnewick.setField(StandardField.ABSTRACT, "Project success is widely covered, and the " +
                "discourse on project complexity is proliferating. The purpose of this book is to merge and" +
                " investigate the two concepts within the context of information system (IS) projects and" +
                " understand the symbiosis between success and complexity in these projects. In this original" +
                " and innovative research, exploratory modelling is employed to identify the aspects that" +
                " constitute the success and complexity of projects based on the perceptions of IS project" +
                " participants. This scholarly book aims at deepening the academic discourse on the relationship" +
                " between the success and complexity of projects and to guide IS project managers towards" +
                " improved project performance through the complexity lens. The research methodology stems from" +
                " the realisation that the complexity of IS projects and its relationship to project success are" +
                " under-documented. A post positivistic approach is applied in order to accommodate the" +
                " subjective interpretation of IS-project participants through a quantitative design. The" +
                " researchers developed an online survey strategy regarding literature concerning the success and" +
                " complexity of projects. The views of 617 participants are documented. In the book, descriptive" +
                " statistics and exploratory factor analysis pave the way for identifying the key success and" +
                " complexity constructs of IS projects. These constructs are used in structural-equation" +
                " modelling to build various validated and predictive models. Knowledge concerning the success" +
                " and complexity of projects is mostly generic with little exposure to the field of IS project" +
                " management. The contribution to current knowledge includes how the success of IS projects" +
                " should be considered as well as what the complexity constructs of IS projects are. The success" +
                " of IS projects encompasses strategic success, deliverable success, process success and the" +
                " ‘unknowns’ of project success. The complexity of IS projects embodies organisational complexity" +
                ", environmental complexity, technical complexity, dynamics and uncertainty. These constructs of" +
                " success and complexity are mapped according to their underlying latent relationships to each" +
                " other. The intended audience of this book is fellow researchers and project and IS specialists," +
                " including information technology managers, executives, project managers, project team members," +
                " the project management office (PMO), general managers and executives that initiate and conduct" +
                " project-related work. The work presented in this first edition of the book is original and has" +
                " not been plagiarised or presented before. It is not a revised version of a thesis or research" +
                " previously published. Comments resulted from the blind peer review process were carefully" +
                " considered and incorporated accordingly.");
        Carl_Marnewick.setField(StandardField.LANGUAGE, "English");
        Carl_Marnewick.setField(StandardField.KEYWORDS, "agile, structural equation modelling, information technology, success, models, strategic alignment, complexity, waterfall, project management, quantitative, Agile software development, Change management, Deliverable, Exploratory factor analysis, South Africa");
        Carl_Marnewick.setField(StandardField.PUBLISHER, "AOSIS");

    }

    @Test
    public void TestGetName() {
        assertEquals("DOAB", fetcher.getName());
    }

    @Test
    public void TestPerformSearch() throws FetcherException {
        List<BibEntry> entries;
        entries = fetcher.performSearch("i open fire");
        assertEquals(Collections.singletonList(David_Opal), entries);
    }

     @Test
    public void TestPerformSearch2() throws FetcherException {
        List<BibEntry> entries;
        entries = fetcher.performSearch("the deliverance of open access books");
        assertEquals(Collections.singletonList(Ronald_Snijder), entries);
    }

    @Test
    public void TestPerformSearch3() throws FetcherException {
        List<BibEntry> entries;
        entries = fetcher.performSearch("Four Kingdom Motifs before and beyond the Book of Daniel");
        assertEquals(Collections.singletonList(Andrew_Perrin), entries);
    }

    @Test
    public void TestPerformSearch4() throws FetcherException {
        List<BibEntry> entries;
        entries = fetcher.performSearch("UAV Sensors for Environmental Monitoring");
        assertEquals(Collections.singletonList(Felipe_Gonzalez), entries);
    }

    @Test
    public void TestPerformSearch5() throws FetcherException {
        List<BibEntry> entries;
        entries = fetcher.performSearch("The symbiosis between information system project complexity and information system project success");
        assertEquals(Collections.singletonList(Carl_Marnewick), entries);
    }

}
