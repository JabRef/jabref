package org.jabref.logic.journals;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
public class LtwaRepositoryTest {
    private JournalAbbreviationRepository repository;

    @BeforeEach
    void setUp() {
        this.repository = JournalAbbreviationLoader.loadBuiltInRepository();
    }

    @ParameterizedTest
    @MethodSource("provideJournalTitlesAndAbbreviations")
    void ltwaAbbreviations(String fullTitle, String expectedAbbreviation) {
        assertEquals(expectedAbbreviation, repository.getLtwaAbbreviation(fullTitle).get());
    }

    private static Stream<Arguments> provideJournalTitlesAndAbbreviations() {
        return Stream.of(
                Arguments.of("Journal of Polymer Science Part A", "J. Polym. Sci. A"),
                Arguments.of("Proceedings of the Institution of Mechanical Engineers, Part A", "Proc. Inst. Mech. Eng. A"),
                Arguments.of("Bulletin of the Section of Logic", "Bull. Sect. Log."),
                Arguments.of("The Lancet", "Lancet"),
                Arguments.of("Baha'i Studies Review", "Baha'i Stud. Rev."),
                Arguments.of("Journal of Shi'a Islamic Studies", "J. Shi'a Islam. Stud."),
                Arguments.of("The Mariner's Mirror", "Mar. Mirror"),
                Arguments.of("The Mechanics' Institute Review", "Mech. Inst. Rev."),
                Arguments.of("Journal of Children's Orthopaedics", "J. Child. Orthop."),
                Arguments.of("Annali dell'Istituto Superiore di Sanità", "Ann. Ist. Super. Sanità"),
                Arguments.of("In Practice", "In Practice"),
                Arguments.of("In the Library with the Lead Pipe", "In Libr. Lead Pipe"),
                Arguments.of("Off our backs", "Off our backs"),
                Arguments.of("Volume!", "Volume!"),
                Arguments.of("Australasian Journal of Educational Technology", "Australas. J. Educ. Technol."),
                Arguments.of("Real-World Economics Review", "Real-World Econ. Rev."),
                Arguments.of("Annals of Clinical & Laboratory Science", "Ann. Clin. Lab. Sci."),
                Arguments.of("Journal of Early Christian Studies", "J. Early Christ. Stud."),
                Arguments.of("Journal of Crustacean Biology", "J. Crustac. Biol."),
                Arguments.of("Carniflora Australis", "Carniflora Aust."),
                Arguments.of("Humana.Mente", "Humana.Mente"),
                Arguments.of("Spunti e ricerche", "Spunti ric."),
                Arguments.of("Journal of Chemical Physics A", "J. Chem. Phys. A"),
                Arguments.of("Romanian Journal of Physics", "Rom. J. Phys."),
                Arguments.of("Archiv Orientální", "Arch. Orient."),
                Arguments.of("Ślaski Kwartalnik Historyczny Sobótka", "Śl. Kwart. Hist. Sobótka"),
                Arguments.of("Mitteilungen der Österreichischen Geographischen Gesellschaft", "Mitt. Österr. Geogr. Ges."),
                Arguments.of("Inorganica Chimica Acta", "Inorg. Chim. Acta"),
                Arguments.of("Comptes rendus de l'Académie des Sciences", "C. r. Acad. Sci."),
                Arguments.of("Proceedings of the National Academy of    Sciences of the United States of America", "Proc. Natl. Acad. Sci. U. S. A."),
                Arguments.of("Scando-Slavica", "Scando-Slav."),
                Arguments.of("International Journal of e-Collaboration", "Int. J. e-Collab."),
                Arguments.of("Proceedings of A. Razmadze Mathematical Institute", "Proc. A. Razmadze Math. Inst."),
                Arguments.of("Proceedings of the 2024 Conference on Science", "Proc. 2024 Conf. Sci."),
                Arguments.of("IEEE Power and Energy Magazine", "IEEE Power Energy Mag."),
                Arguments.of("IEEE Transactions on Automatic Control", "IEEE Trans. Autom. Control"),
                Arguments.of("E.S.A. bulletin", "E.S.A. bull."),
                Arguments.of("Acta Universitatis Carolinae. Iuridica", "Acta Univ. Carol., Iurid."),
                Arguments.of("Physical Review. A", "Phys. Rev., A"),
                Arguments.of("Physical Review. D", "Phys. Rev., D"),
                Arguments.of("Physical Review. E", "Phys. Rev., E"),
                Arguments.of("Physical Review. I", "Phys. Rev., I")
        );
    }
}
