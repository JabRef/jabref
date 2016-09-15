package net.sf.jabref.logic.l10n;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.AccessControlContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import javafx.geometry.Dimension2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import com.sun.glass.ui.CommonDialogs;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.embed.HostInterface;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.perf.PerformanceTracker;
import com.sun.javafx.runtime.async.AsyncOperation;
import com.sun.javafx.runtime.async.AsyncOperationListener;
import com.sun.javafx.scene.text.GlyphList;
import com.sun.javafx.scene.text.HitInfo;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.scene.text.TextLayoutFactory;
import com.sun.javafx.scene.text.TextLine;
import com.sun.javafx.scene.text.TextSpan;
import com.sun.javafx.tk.AppletWindow;
import com.sun.javafx.tk.DummyToolkit;
import com.sun.javafx.tk.FileChooserType;
import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.FontMetrics;
import com.sun.javafx.tk.ImageLoader;
import com.sun.javafx.tk.PlatformImage;
import com.sun.javafx.tk.RenderJob;
import com.sun.javafx.tk.ScreenConfigurationAccessor;
import com.sun.javafx.tk.TKClipboard;
import com.sun.javafx.tk.TKDragGestureListener;
import com.sun.javafx.tk.TKDragSourceListener;
import com.sun.javafx.tk.TKDropTargetListener;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.tk.TKScreenConfigurationListener;
import com.sun.javafx.tk.TKStage;
import com.sun.javafx.tk.TKSystemMenu;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.tk.quantum.MasterTimer;
import com.sun.javafx.tk.quantum.QuantumToolkit;
import com.sun.scenario.DelayedRunnable;
import com.sun.scenario.animation.AbstractMasterTimer;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.Filterable;

/**
 * This is dummy {@link Toolkit} for JavaFX which does nothing.
 * We need it to load FXML files in the localization tests without actually rendering anything.
 *
 * Copy of {@link DummyToolkit} with a bit of {@link QuantumToolkit}.
 */
public class LocalizationJavaFXToolkit extends Toolkit {

    private static boolean alreadyInit;
    private ClassLoader ccl;
    private CountDownLatch launchLatch = new CountDownLatch(1);

    public static void installToolkit() {
        if (alreadyInit) {
            return;
        }

        try {
            Field field = Toolkit.class.getDeclaredField("TOOLKIT");
            field.setAccessible(true);
            field.set(null, new LocalizationJavaFXToolkit());

            // Start JavaFX thread
            PlatformImpl.startup(() -> {
            });
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        alreadyInit = true;
    }


    @Override
    public boolean isFxUserThread() {
        return true;
    }

    @Override
    public AbstractMasterTimer getMasterTimer() {
        return MasterTimer.getInstance();
    }

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public boolean canStartNestedEventLoop() {
        return false;
    }

    @Override
    public Object enterNestedEventLoop(Object key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void exitNestedEventLoop(Object key, Object rval) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TKStage createTKStage(Window peerWindow, boolean securityDialog, StageStyle stageStyle, boolean primary,
            Modality modality, TKStage owner, boolean rtl, AccessControlContext acc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TKStage createTKPopupStage(Window peerWindow, StageStyle popupStyle, TKStage owner,
            AccessControlContext acc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TKStage createTKEmbeddedStage(HostInterface host, AccessControlContext acc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AppletWindow createAppletWindow(long parent, String serverName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void closeAppletWindow() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TKSystemMenu getSystemMenu() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ImageLoader loadImage(String url, int width, int height, boolean preserveRatio, boolean smooth) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ImageLoader loadImage(InputStream stream, int width, int height, boolean preserveRatio, boolean smooth) {
        return new ImageLoader() {

            @Override
            public Exception getException() {
                return null;
            }

            @Override
            public int getFrameCount() {
                return 0;
            }

            @Override
            public PlatformImage getFrame(int index) {
                return new PlatformImage() {

                    @Override
                    public float getPixelScale() {
                        return 1;
                    }

                    @Override
                    public int getArgb(int x, int y) {
                        return 0;
                    }

                    @Override
                    public void setArgb(int x, int y, int argb) {

                    }

                    @Override
                    public PixelFormat getPlatformPixelFormat() {
                        return null;
                    }

                    @Override
                    public boolean isWritable() {
                        return false;
                    }

                    @Override
                    public PlatformImage promoteToWritableImage() {
                        return null;
                    }

                    @Override
                    public <T extends Buffer> void getPixels(int x, int y, int w, int h,
                            WritablePixelFormat<T> pixelformat, T pixels, int scanlineElems) {

                    }

                    @Override
                    public void getPixels(int x, int y, int w, int h, WritablePixelFormat<ByteBuffer> pixelformat,
                            byte[] pixels, int offset, int scanlineBytes) {

                    }

                    @Override
                    public void getPixels(int x, int y, int w, int h, WritablePixelFormat<IntBuffer> pixelformat,
                            int[] pixels, int offset, int scanlineInts) {

                    }

                    @Override
                    public <T extends Buffer> void setPixels(int x, int y, int w, int h, PixelFormat<T> pixelformat,
                            T pixels, int scanlineBytes) {

                    }

                    @Override
                    public void setPixels(int x, int y, int w, int h, PixelFormat<ByteBuffer> pixelformat,
                            byte[] pixels, int offset, int scanlineBytes) {

                    }

                    @Override
                    public void setPixels(int x, int y, int w, int h, PixelFormat<IntBuffer> pixelformat, int[] pixels,
                            int offset, int scanlineInts) {

                    }

                    @Override
                    public void setPixels(int dstx, int dsty, int w, int h, PixelReader reader, int srcx, int srcy) {

                    }
                };
            }

            @Override
            public int getFrameDelay(int index) {
                return 0;
            }

            @Override
            public int getLoopCount() {
                return 0;
            }

            @Override
            public int getWidth() {
                return 0;
            }

            @Override
            public int getHeight() {
                return 0;
            }
        };
    }

    @Override
    public AsyncOperation loadImageAsync(AsyncOperationListener<? extends ImageLoader> listener, String url, int width,
            int height, boolean preserveRatio, boolean smooth) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ImageLoader loadPlatformImage(Object platformImage) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PlatformImage createPlatformImage(int w, int h) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void startup(final Runnable userStartupRunnable) {
        // Save the context class loader of the launcher thread
        ccl = Thread.currentThread().getContextClassLoader();

        try {
            // Ensure that the toolkit can only be started here
            runToolkit();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        try {
            launchLatch.await();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    private void runToolkit() {
        Thread user = Thread.currentThread();

        // Set context class loader to the same as the thread that called startup
        user.setContextClassLoader(ccl);
        setFxUserThread(user);

        // Initialize JavaFX scene graph
        launchLatch.countDown();

    }

    @Override
    public void defer(Runnable runnable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Future addRenderJob(RenderJob rj) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Object, Object> getContextMap() {
        return new HashMap();
    }

    @Override
    public int getRefreshRate() {
        return 1;
    }

    @Override
    public void setAnimationRunnable(DelayedRunnable animationRunnable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PerformanceTracker getPerformanceTracker() {
        return new PerformanceTracker() {

            @Override
            protected long nanoTime() {
                return 0;
            }

            @Override
            public void doOutputLog() {

            }

            @Override
            public void doLogEvent(String s) {

            }
        };
    }

    @Override
    public PerformanceTracker createPerformanceTracker() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void waitFor(Task t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Object createColorPaint(Color paint) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Object createLinearGradientPaint(LinearGradient paint) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Object createRadialGradientPaint(RadialGradient paint) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Object createImagePatternPaint(ImagePattern paint) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void accumulateStrokeBounds(Shape shape, float[] bbox, StrokeType type, double strokewidth,
            StrokeLineCap cap, StrokeLineJoin join, float miterLimit, BaseTransform tx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean strokeContains(Shape shape, double x, double y, StrokeType type, double strokewidth,
            StrokeLineCap cap, StrokeLineJoin join, float miterLimit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Shape createStrokedShape(Shape shape, StrokeType pgtype, double strokewidth, StrokeLineCap pgcap,
            StrokeLineJoin pgjoin, float miterLimit, float[] dashArray, float dashOffset) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getKeyCodeForChar(String character) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Dimension2D getBestCursorSize(int preferredWidth, int preferredHeight) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMaximumCursorColors() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PathElement[] convertShapeToFXPath(Object shape) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public HitInfo convertHitInfoToFX(Object hit) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Filterable toFilterable(Image img) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FilterContext getFilterContext(Object config) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isForwardTraversalKey(KeyEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isBackwardTraversalKey(KeyEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isNestedLoopRunning() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FontLoader getFontLoader() {
        return new FontLoader() {

            @Override
            public void loadFont(Font font) {

            }

            @Override
            public List<String> getFamilies() {
                return null;
            }

            @Override
            public List<String> getFontNames() {
                return null;
            }

            @Override
            public List<String> getFontNames(String family) {
                return null;
            }

            @Override
            public Font font(String family, FontWeight weight, FontPosture posture, float size) {
                return null;
            }

            @Override
            public Font loadFont(InputStream in, double size) {
                return null;
            }

            @Override
            public Font loadFont(String path, double size) {
                return null;
            }

            @Override
            public FontMetrics getFontMetrics(Font font) {
                return null;
            }

            @Override
            public float computeStringWidth(String string, Font font) {
                return 0;
            }

            @Override
            public float getSystemFontSize() {
                return 0;
            }
        };
    }

    @Override
    public TextLayoutFactory getTextLayoutFactory() {
        return new TextLayoutFactory() {

            @Override
            public TextLayout createLayout() {
                return new TextLayout() {

                    @Override
                    public boolean setContent(TextSpan[] spans) {
                        return false;
                    }

                    @Override
                    public boolean setContent(String string, Object font) {
                        return false;
                    }

                    @Override
                    public boolean setAlignment(int alignment) {
                        return false;
                    }

                    @Override
                    public boolean setWrapWidth(float wrapWidth) {
                        return false;
                    }

                    @Override
                    public boolean setLineSpacing(float spacing) {
                        return false;
                    }

                    @Override
                    public boolean setDirection(int direction) {
                        return false;
                    }

                    @Override
                    public boolean setBoundsType(int type) {
                        return false;
                    }

                    @Override
                    public BaseBounds getBounds() {
                        return null;
                    }

                    @Override
                    public BaseBounds getBounds(TextSpan filter, BaseBounds bounds) {
                        return null;
                    }

                    @Override
                    public BaseBounds getVisualBounds(int type) {
                        return null;
                    }

                    @Override
                    public TextLine[] getLines() {
                        return new TextLine[0];
                    }

                    @Override
                    public GlyphList[] getRuns() {
                        return new GlyphList[0];
                    }

                    @Override
                    public Shape getShape(int type, TextSpan filter) {
                        return null;
                    }

                    @Override
                    public HitInfo getHitInfo(float x, float y) {
                        return null;
                    }

                    @Override
                    public PathElement[] getCaretShape(int offset, boolean isLeading, float x, float y) {
                        return new PathElement[0];
                    }

                    @Override
                    public PathElement[] getRange(int start, int end, int type, float x, float y) {
                        return new PathElement[0];
                    }
                };
            }

            @Override
            public TextLayout getLayout() {
                return null;
            }

            @Override
            public void disposeLayout(TextLayout layout) {

            }
        };
    }

    @Override
    public Object createSVGPathObject(SVGPath svgpath) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Path2D createSVGPath2D(SVGPath svgpath) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean imageContains(Object image, float x, float y) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public com.sun.javafx.tk.TKClipboard getSystemClipboard() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TKClipboard getNamedClipboard(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ScreenConfigurationAccessor setScreenConfigurationListener(TKScreenConfigurationListener listener) {
        return new ScreenConfigurationAccessor() {

            @Override
            public int getMinX(Object obj) {
                return 0;
            }

            @Override
            public int getMinY(Object obj) {
                return 0;
            }

            @Override
            public int getWidth(Object obj) {
                return 0;
            }

            @Override
            public int getHeight(Object obj) {
                return 0;
            }

            @Override
            public int getVisualMinX(Object obj) {
                return 0;
            }

            @Override
            public int getVisualMinY(Object obj) {
                return 0;
            }

            @Override
            public int getVisualHeight(Object obj) {
                return 0;
            }

            @Override
            public int getVisualWidth(Object obj) {
                return 0;
            }

            @Override
            public float getDPI(Object obj) {
                return 0;
            }

            @Override
            public float getUIScale(Object obj) {
                return 0;
            }

            @Override
            public float getRenderScale(Object obj) {
                return 0;
            }
        };
    }

    @Override
    public Object getPrimaryScreen() {
        return null;
    }

    @Override
    public List<?> getScreens() {
        return Collections.emptyList();
    }

    @Override
    public ScreenConfigurationAccessor getScreenConfigurationAccessor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void registerDragGestureListener(TKScene s, Set<TransferMode> tms, TKDragGestureListener l) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void startDrag(TKScene scene, Set<TransferMode> tms, TKDragSourceListener l, Dragboard dragboard) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void enableDrop(TKScene s, TKDropTargetListener l) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void installInputMethodRequests(TKScene scene, InputMethodRequests requests) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object renderToImage(ImageRenderingContext context) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public KeyCode getPlatformShortcutKey() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CommonDialogs.FileChooserResult showFileChooser(TKStage ownerWindow, String title, File initialDirectory,
            String initialFileName, FileChooserType fileChooserType, List<FileChooser.ExtensionFilter> extensionFilters,
            FileChooser.ExtensionFilter selectedFilter) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public File showDirectoryChooser(TKStage ownerWindow, String title, File initialDirectory) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getMultiClickTime() {
        return 0L;
    }

    @Override
    public int getMultiClickMaxX() {
        return 0;
    }

    @Override
    public int getMultiClickMaxY() {
        return 0;
    }

    @Override
    public void requestNextPulse() {

    }

}
