package org.jabref.cli;

import java.util.List;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.UiCommand;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ArgumentProcessorTest {

    private final GuiPreferences preferences = mock(GuiPreferences.class);

    @ParameterizedTest
    @ValueSource(strings = {"jabref://", "jabref://open", "jabref:", "jabref://some/path"})
    void protocolHandlerUrlProducesFocusCommand(String url) {
        ArgumentProcessor processor = new ArgumentProcessor(
                new String[] {url},
                ArgumentProcessor.Mode.REMOTE_START,
                preferences);

        List<UiCommand> commands = processor.processArguments();

        assertTrue(commands.stream().anyMatch(UiCommand.Focus.class::isInstance));
    }

    @Test
    void normalArgumentsAreNotAffectedByProtocolFilter() {
        ArgumentProcessor processor = new ArgumentProcessor(
                new String[] {"--blank"},
                ArgumentProcessor.Mode.REMOTE_START,
                preferences);

        List<UiCommand> commands = processor.processArguments();

        assertTrue(commands.stream().anyMatch(UiCommand.BlankWorkspace.class::isInstance));
        assertFalse(commands.stream().anyMatch(UiCommand.Focus.class::isInstance));
    }

    @Test
    void protocolHandlerUrlCombinedWithNormalArguments() {
        ArgumentProcessor processor = new ArgumentProcessor(
                new String[] {"jabref://", "--blank"},
                ArgumentProcessor.Mode.REMOTE_START,
                preferences);

        List<UiCommand> commands = processor.processArguments();

        assertTrue(commands.stream().anyMatch(UiCommand.BlankWorkspace.class::isInstance));
    }

    @Test
    void emptyArgumentsProduceNoFocusCommand() {
        ArgumentProcessor processor = new ArgumentProcessor(
                new String[] {},
                ArgumentProcessor.Mode.REMOTE_START,
                preferences);

        List<UiCommand> commands = processor.processArguments();

        assertFalse(commands.stream().anyMatch(UiCommand.Focus.class::isInstance));
    }

    @Test
    void onlyProtocolHandlerUrlProducesOnlyFocusCommand() {
        ArgumentProcessor processor = new ArgumentProcessor(
                new String[] {"jabref://"},
                ArgumentProcessor.Mode.REMOTE_START,
                preferences);

        List<UiCommand> commands = processor.processArguments();

        assertEquals(1, commands.size());
        assertTrue(commands.getFirst() instanceof UiCommand.Focus);
    }
}
