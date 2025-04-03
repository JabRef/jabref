package org.jabref.logic.journals;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class LtwaRepositoryTest {
    private JournalAbbreviationRepository repository;

    @BeforeEach
    void setUp() {
        this.repository = JournalAbbreviationLoader.loadBuiltInRepository();
    }

    @ParameterizedTest
    @MethodSource("provideJournalTitlesAndAbbreviations")
    void testLtwaAbbreviations(String fullTitle, String expectedAbbreviation) {
        assertEquals(expectedAbbreviation, repository.getLtwaAbbreviation(fullTitle));
    }

    private static Stream<Arguments> provideJournalTitlesAndAbbreviations() {
        return Stream.of(
                arguments("Journal of Polymer Science Part A", "J. Polym. Sci. A"),
                arguments("Proceedings of the Institution of Mechanical Engineers, Part A", "Proc. Inst. Mech. Eng. A"),
                arguments("Bulletin of the Section of Logic", "Bull. Sect. Log."),
                arguments("The Lancet", "Lancet"),
                arguments("Baha'i Studies Review", "Baha'i Stud. Rev."),
                arguments("Journal of Shi'a Islamic Studies", "J. Shi'a Islam. Stud."),
                arguments("The Mariner's Mirror", "Mar. Mirror"),
                arguments("The Mechanics' Institute Review", "Mech. Inst. Rev."),
                arguments("Journal of Children's Orthopaedics", "J. Child. Orthop."),
                arguments("Annali dell'Istituto Superiore di Sanità", "Ann. Ist. Super. Sanità"),
                arguments("In Practice", "In Practice"),
                arguments("In the Library with the Lead Pipe", "In Libr. Lead Pipe"),
                arguments("Off our backs", "Off our backs"),
                arguments("Volume!", "Volume!"),
                arguments("Australasian Journal of Educational Technology", "Australas. J. Educ. Technol."),
                arguments("Real-World Economics Review", "Real-World Econ. Rev."),
                arguments("Annals of Clinical & Laboratory Science", "Ann. Clin. Lab. Sci."),
                arguments("Journal of Early Christian Studies", "J. Early Christ. Stud."),
                arguments("Journal of Crustacean Biology", "J. Crustac. Biol."),
                arguments("Carniflora Australis", "Carniflora Aust."),
                arguments("Humana.Mente", "Humana.Mente"),
                arguments("Spunti e ricerche", "Spunti ric."),
                arguments("Journal of Chemical Physics A", "J. Chem. Phys. A"),
                arguments("Romanian Journal of Physics", "Rom. J. Phys."),
                arguments("Labor History", "Labor Hist."),
                arguments("Archiv Orientální", "Arch. Orient."),
                arguments("Ślaski Kwartalnik Historyczny Sobótka", "Śl. Kwart. Hist. Sobótka"),
                arguments("Mitteilungen der Österreichischen Geographischen Gesellschaft", "Mitt. Österr. Geogr. Ges."),
                arguments("Inorganica Chimica Acta", "Inorg. Chim. Acta"),
                arguments("Comptes rendus de l'Académie des Sciences", "C. r. Acad. Sci."),
                arguments("Proceedings of the National Academy of Sciences of the United States of America", "Proc. Natl. Acad. Sci. U. S. A."),
                arguments("Scando-Slavica", "Scando-Slav."),
                arguments("International Journal of e-Collaboration", "Int. J. e-Collab."),
                arguments("Proceedings of A. Razmadze Mathematical Institute", "Proc. A. Razmadze Math. Inst."),
                arguments("Proceedings of the 2024 Conference on Science", "Proc. 2024 Conf. Sci."),
                arguments("IEEE Power and Energy Magazine", "IEEE Power Energy Mag."),
                arguments("IEEE Transactions on Automatic Control", "IEEE Trans. Autom. Control"),
                arguments("E.S.A. bulletin", "E.S.A. bull."),
                arguments("Acta Universitatis Carolinae. Iuridica", "Acta Univ. Carol., Iurid."),
                arguments("Physical Review. A", "Phys. Rev., A"),
                arguments("Physical Review. D", "Phys. Rev., D"),
                arguments("Physical Review. E", "Phys. Rev., E"),
                arguments("Physical Review. I", "Phys. Rev., I")
        );
    }
}
