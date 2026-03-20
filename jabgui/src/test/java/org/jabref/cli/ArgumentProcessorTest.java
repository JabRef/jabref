package org.jabref.cli;

import java.util.List;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.UiCommand;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ArgumentProcessorTest {

    private final GuiPreferences preferences = mock(GuiPreferences.class);

    @ParameterizedTest
    @ValueSource(strings = {
            "jabref://",
            "jabref://open",
            "jabref:",
            "jabref://some/path",
            "JABREF://open",
            "JabRef://open"})
    void protocolHandlerUrlProducesFocusCommand(String url) {
        ArgumentProcessor processor = new ArgumentProcessor(
                new String[] {url},
                ArgumentProcessor.Mode.REMOTE_START,
                preferences);

        List<UiCommand> commands = processor.processArguments();

        assertEquals(List.of(new UiCommand.Focus()), commands);
    }

    @Test
    void normalArgumentsAreNotAffectedByProtocolFilter() {
        ArgumentProcessor processor = new ArgumentProcessor(
                new String[] {"--blank"},
                ArgumentProcessor.Mode.REMOTE_START,
                preferences);

        List<UiCommand> commands = processor.processArguments();

        assertEquals(List.of(new UiCommand.BlankWorkspace()), commands);
    }

    @Test
    void protocolHandlerUrlCombinedWithNormalArguments() {
        ArgumentProcessor processor = new ArgumentProcessor(
                new String[] {"jabref://", "--blank"},
                ArgumentProcessor.Mode.REMOTE_START,
                preferences);

        List<UiCommand> commands = processor.processArguments();

        assertEquals(List.of(new UiCommand.BlankWorkspace()), commands);
    }

    @Test
    void emptyArgumentsProduceNoFocusCommand() {
        ArgumentProcessor processor = new ArgumentProcessor(
                new String[] {},
                ArgumentProcessor.Mode.REMOTE_START,
                preferences);

        List<UiCommand> commands = processor.processArguments();

        assertEquals(List.of(), commands);
    }
}
