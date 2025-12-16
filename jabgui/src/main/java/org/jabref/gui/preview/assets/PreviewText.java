package org.jabref.gui.preview.assets;

public final class PreviewText {

    private PreviewText() {
    }

    public static String getPreviewText(String baseUrl, String text) {
        return """
                <html>
                    <head>
                        <base href="%s">
                    </head>
                    <body id="previewBody">
                        <div id="content"> %s </div>
                    </body>
                </html>
                """.formatted(baseUrl, text);
    }
}
