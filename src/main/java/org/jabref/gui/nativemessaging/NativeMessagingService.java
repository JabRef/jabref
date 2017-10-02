package org.jabref.gui.nativemessaging;

import java.util.concurrent.Future;

public interface NativeMessagingService {
     Future<Boolean> isOnline();
}
