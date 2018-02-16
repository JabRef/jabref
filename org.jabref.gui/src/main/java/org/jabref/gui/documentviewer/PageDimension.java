package org.jabref.gui.documentviewer;

public abstract class PageDimension {

    public static PageDimension ofFixedWidth(int width) {
        return new FixedWidthPageDimension(width);
    }

    public static PageDimension ofFixedHeight(int height) {
        return new FixedHeightPageDimension(height);
    }

    public static PageDimension ofFixedWidth(double width) {
        return ofFixedWidth((int) width);
    }

    public static PageDimension ofFixedHeight(double height) {
        return ofFixedHeight((int) height);
    }

    public abstract int getWidth(double aspectRatio);

    public abstract int getHeight(double aspectRatio);

    private static class FixedWidthPageDimension extends PageDimension {
        private final int width;

        public FixedWidthPageDimension(int width) {
            this.width = width;
        }

        @Override
        public int getWidth(double aspectRatio) {
            return width;
        }

        @Override
        public int getHeight(double aspectRatio) {
            return (int) (width / aspectRatio);
        }
    }

    private static class FixedHeightPageDimension extends PageDimension {
        private final int height;

        public FixedHeightPageDimension(int height) {
            this.height = height;
        }

        @Override
        public int getWidth(double aspectRatio) {
            return (int) (aspectRatio * height);
        }

        @Override
        public int getHeight(double aspectRatio) {
            return height;
        }
    }
}
