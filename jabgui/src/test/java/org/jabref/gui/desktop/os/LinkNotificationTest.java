package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.clipboard.ClipBoardManager;

import com.dlsc.gemsfx.infocenter.Notification;
import com.dlsc.gemsfx.infocenter.NotificationAction;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@NullMarked
class LinkNotificationTest {

    private static final String PAGE = "https://example.org/login?state=abc";

    private final DialogService dialogService = mock(DialogService.class);
    private final NativeDesktop nativeDesktop = mock(NativeDesktop.class);
    private final ClipBoardManager clipBoardManager = mock(ClipBoardManager.class);
    private final LinkNotification notification = new LinkNotification(
            "Title", "Description", PAGE, dialogService, clipBoardManager, Notification.OnClickBehaviour.NONE)
            .withOpenAction("Open page", nativeDesktop);

    @Test
    void offersOpeningAndCopying() {
        assertEquals(List.of("Open page", "Copy link"), notification.getActions().stream().map(NotificationAction::getText).toList());
    }

    @Test
    void openActionOpensThePage() throws IOException {
        notification.getActions().getFirst().getOnAction().call(notification);

        verify(nativeDesktop).openBrowser(PAGE);
    }

    @Test
    void failedOpenKeepsTheNotificationForCopying() throws IOException {
        doThrow(new IOException("no browser")).when(nativeDesktop).openBrowser(PAGE);
        LinkNotification removedAfterUse = new LinkNotification(
                "Title", "Description", PAGE, dialogService, clipBoardManager, Notification.OnClickBehaviour.REMOVE)
                .withOpenAction("Open page", nativeDesktop);

        assertEquals(Notification.OnClickBehaviour.NONE, removedAfterUse.getActions().getFirst().getOnAction().call(removedAfterUse));
    }

    @Test
    void actionsKeepOrRemoveTheNotificationAsConfigured() {
        LinkNotification removedAfterUse = new LinkNotification(
                "Title", "Description", PAGE, dialogService, clipBoardManager, Notification.OnClickBehaviour.REMOVE)
                .withOpenAction("Open page", nativeDesktop);

        assertEquals(Notification.OnClickBehaviour.NONE, notification.getActions().getLast().getOnAction().call(notification));
        assertEquals(Notification.OnClickBehaviour.REMOVE, removedAfterUse.getActions().getLast().getOnAction().call(removedAfterUse));
    }

    @Test
    void withoutOpenActionOffersOnlyCopying() {
        LinkNotification copyOnly = new LinkNotification(
                "Title", "Description", PAGE, dialogService, clipBoardManager, Notification.OnClickBehaviour.REMOVE);

        assertEquals(List.of("Copy link"), copyOnly.getActions().stream().map(NotificationAction::getText).toList());
    }

    @Test
    void copyActionCopiesThePage() {
        notification.getActions().getLast().getOnAction().call(notification);

        verify(clipBoardManager).setContent(PAGE);
    }
}
