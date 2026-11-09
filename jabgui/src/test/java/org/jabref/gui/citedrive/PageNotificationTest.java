package org.jabref.gui.citedrive;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.desktop.os.NativeDesktop;

import com.dlsc.gemsfx.infocenter.NotificationAction;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@NullMarked
class PageNotificationTest {

    private static final String PAGE = "https://example.org/login?state=abc";

    private final DialogService dialogService = mock(DialogService.class);
    private final NativeDesktop nativeDesktop = mock(NativeDesktop.class);
    private final ClipBoardManager clipBoardManager = mock(ClipBoardManager.class);
    private final CiteDriveLoginAction.PageNotification notification = new CiteDriveLoginAction.PageNotification(
            "Title", "Description", "Open page", PAGE, dialogService, nativeDesktop, clipBoardManager);

    @Test
    void offersOpeningAndCopying() {
        assertEquals(List.of("Open page", "Copy link"), notification.getActions().stream().map(NotificationAction::getText).toList());
    }

    @Test
    void openActionOpensThePage() {
        notification.getActions().getFirst().getOnAction().call(notification);

        verify(nativeDesktop).openBrowserShowPopup(PAGE, dialogService);
    }

    @Test
    void copyActionCopiesThePage() {
        notification.getActions().getLast().getOnAction().call(notification);

        verify(clipBoardManager).setContent(PAGE);
    }
}
