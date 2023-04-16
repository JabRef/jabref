package org.jabref.gui.mergeentries;

import java.util.stream.Stream;

import org.jabref.gui.mergeentries.newmergedialog.cell.ThreeWayMergeCellViewModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThreeWayMergeCellViewModelTest {

    ThreeWayMergeCellViewModel viewModel;

    @BeforeEach
    void setup() {
        viewModel = new ThreeWayMergeCellViewModel("Hello", 0);
    }

    private static Stream<Arguments> testOddEvenLogic() {
        return Stream.of(
                Arguments.of(true, false, true, false),
                Arguments.of(false, false, true, false),
                Arguments.of(true, true, false, true),
                Arguments.of(false, true, false, true)
        );
    }

    private static Stream<Arguments> isEvenShouldReturnTrueIfRowIndexIsEven() {
        return Stream.of(
                Arguments.of(0, true),
                Arguments.of(100, true),
                Arguments.of(1, false),
                Arguments.of(2, true),
                Arguments.of(9999, false),
                Arguments.of(Integer.MAX_VALUE, false)
        );
    }

    private static Stream<Arguments> isOddShouldReturnTrueIfRowIndexIsOdd() {
        return Stream.of(
                Arguments.of(1, true),
                Arguments.of(101, true),
                Arguments.of(3, true),
                Arguments.of(7777, true),
                Arguments.of(9999, true),
                Arguments.of(Integer.MAX_VALUE, true),
                Arguments.of(0, false)
        );
    }

    private static Stream<Arguments> getTextAndSetTextShouldBeConsistent() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of("Hello"),
                Arguments.of("World"),
                Arguments.of("" + null),
                Arguments.of("Hello, World"),
                Arguments.of("عربي")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOddEvenLogic(boolean setIsOdd, boolean setIsEven, boolean expectedIsOdd, boolean expectedIsEven) {
        viewModel.setOdd(setIsOdd);
        viewModel.setEven(setIsEven);
        assertEquals(expectedIsOdd, viewModel.isOdd());
        assertEquals(expectedIsEven, viewModel.isEven());
    }

    @ParameterizedTest
    @MethodSource
    void isEvenShouldReturnTrueIfRowIndexIsEven(int rowIndex, boolean expected) {
        ThreeWayMergeCellViewModel newViewModel = new ThreeWayMergeCellViewModel("", rowIndex);
        assertEquals(expected, newViewModel.isEven());
    }

    @ParameterizedTest
    @MethodSource
    void isOddShouldReturnTrueIfRowIndexIsOdd(int rowIndex, boolean expected) {
        ThreeWayMergeCellViewModel newViewModel = new ThreeWayMergeCellViewModel("", rowIndex);
        assertEquals(expected, newViewModel.isOdd());
    }

    @ParameterizedTest
    @MethodSource
    void getTextAndSetTextShouldBeConsistent(String setText) {
        viewModel.setText(setText);
        assertEquals(viewModel.getText(), setText);
    }
}
