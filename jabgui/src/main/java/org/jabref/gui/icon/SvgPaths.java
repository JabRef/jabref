package org.jabref.gui.icon;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class SvgPaths {

    // Toolbar - Entry operations
    static final String NEW_LIBRARY = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";
    static final String OPEN_LIBRARY = "M20 18H4V8h16m0-2h-8l-2-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2z";
    static final String SAVE_LIBRARY = "M15 9H5V5h10m-3 15A3 3 0 0 1 9 17a3 3 0 0 1 3-3a3 3 0 0 1 3 3a3 3 0 0 1-3 3m5-16H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V7l-4-4z";

    // Toolbar - Entry operations
    static final String ADD_ENTRY_IMMEDIATE = "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2m-2 10h-4v4h-2v-4H7v-2h4V7h2v4h4v2z";
    static final String ADD_ENTRY = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";
    static final String DELETE_ENTRY = "M19 4h-3.5l-1-1h-5l-1 1H5v2h14M6 19a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V7H6v12z";

    // Toolbar - Navigation
    static final String ARROW_LEFT = "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13 H20v-2z";
    static final String ARROW_RIGHT = "M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8 -8-8-8z";

    // Toolbar - Edit & Clipboard
    static final String UNDO = "M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88c3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z";
    static final String REDO = "M18.4 10.6C16.55 8.99 14.15 8 11.5 8c-4.65 0-8.58 3.03-9.96 7.22L3.9 16c1.05-3.19 4.05-5.5 7.6-5.5c1.95 0 3.73.72 5.12 1.88L13 16h9V7l-3.6 3.6z";
    static final String CUT = "M19 3L13 9l2 2l6-6V3m-9 9l-2-2l-3.34 3.34A3.99 3.99 0 0 0 2 16 a4 4 0 0 0 4 4c1.31 0 2.47-.63 3.22-1.61L12.5 15l-2.5-3m-6.5 6a2 2 0 0 1-2-2a2 2 0 0 1 2-2 a2 2 0 0 1 2 2a2 2 0 0 1-2 2z";
    static final String COPY = "M19 21H8V7h11m0-2H8a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2m-3-4H4a2 2 0 0 0-2 2v14h2V3h12V1z";
    static final String PASTE = "M19 2h-4.18C14.4.84 13.3 0 12 0c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2m-7 0c.55 0 1 .45 1 1s-.45 1-1 1s-1-.45-1-1s.45-1 1-1m7 18H5V4h2v3h10V4h2v16z";

    // Toolbar - Tools & External
    static final String MAKE_KEY = "M7 14A2 2 0 0 1 5 12A2 2 0 0 1 7 10A2 2 0 0 1 9 12A2 2 0 0 1 7 14M12.6 10C11.8 7.7 9.6 6 7 6A6 6 0 0 0 1 12A6 6 0 0 0 7 18C9.6 18 11.8 16.3 12.6 14 H16V18H20V14H23V10H12.6Z";
    static final String CLEANUP = "M19.36 2.72L20.78 4.14L15.06 9.85C16.13 11.39 16.28 13.24 15.38 14.44L9.06 8.12C10.26 7.22 12.11 7.37 13.65 8.44L19.36 2.72M5.93 17.57C3.92 15.56 2.69 13.16 2.35 10.92L7.23 8.83L14.67 16.27L12.58 21.15C10.34 20.81 7.94 19.58 5.93 17.57Z";

    static final String NOTIFICATIONS = "M12 22a2 2 0 0 0 2-2h-4a2 2 0 0 0 2 2m6-6v-5c0 -3.07-1.64-5.64-4.5-6.32V4a1.5 1.5 0 0 0-3 0v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z";

    static final String GITHUB = "M12 2A10 10 0 0 0 2 12c0 4.42 2.87 8.17 6.84 9.5c.5.08.66-.23.66-.5v-1.69c-2.77.6-3.36-1.34-3.36-1.34c-.46-1.16-1.11-1.47-1.11-1.47c -.91-.62.07-.6.07-.6c1 .07 1.53 1.03 1.53 1.03c.87 1.52 2.34 1.07 2.91.83c .09-.65.35-1.09.63-1.34c-2.22-.25-4.55-1.11-4.55-4.92c0-1.11.38-2 1.03-2.71c -.1-.25-.45-1.29.1-2.64c0 0 .84-.27 2.75 1.02c.79-.22 1.65-.33 2.5-.33c.85 0 1.71.11 2.5.33 c1.91-1.29 2.75-1.02 2.75-1.02c.55 1.35.2 2.39.1 2.64c.65.71 1.03 1.6 1.03 2.71c0 3.82-2.34 4.66-4.57 4.91c.36.31.69.92.69 1.85V21c0 .27.16.59.67.5C19.14 20.16 22 16.42 22 12A10 10 0 0 0 12 2z";

    private SvgPaths() {
    }
}
