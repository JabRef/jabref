package net.sf.jabref.util;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.swing.JEditorPane;
import javax.swing.text.View;

/**
 * DocumentPrinter prints objects of type Document. Text attributes, including
 * fonts, color, and small icons, will be rendered to a printed page.
 * DocumentPrinter computes line breaks, paginates, and performs other
 * formatting.
 * 
 * An HTMLDocument is printed by sending it as an argument to the
 * print(HTMLDocument) method. A PlainDocument is printed the same way. Other
 * types of documents must be sent in a JEditorPane as an argument to the
 * print(JEditorPane) method. Printing Documents in this way will automatically
 * display a print dialog.
 * 
 * As objects which implement the Printable Interface, instances of the
 * DocumentPrinter class can also be used as the argument in the setPrintable
 * method of the PrinterJob class. Instead of using the print() methods detailed
 * above, a programmer may gain access to the formatting capabilities of this
 * class without using its print dialog by creating an instance of
 * DocumentPrinter and setting the document to be printed with the setDocument()
 * or setJEditorPane(). The Document may then be printed by setting the instance
 * of DocumentPrinter in any PrinterJob.
 * 
 * This class is based on "How to print HTML from a jEditorPane - faq269-5935"
 * 
 * http://www.tek-tips.com/faqs.cfm?fid=5935
 * 
 * and was originally called DocumentPrinter.
 * 
 * Copyright 2002 Kei G. Gauthier Suite 301 77 Winsor Street Ludlow, MA 01056
 * 
 * Originally attributed by Kei to
 * 
 * http://www.fawcette.com/javapro/2002_12/online/print_kgauthier_12_10_02/default_pf.aspx
 * 
 * @author Christopher Oezbek
 * 
 * I stripped the class of all the boilerplate I could find (protected and
 * such).
 * 
 */
public class DocumentPrinter {

	/**
	 * Note that pFormat is not the variable name used by the print method of
	 * the DocumentPrintable. Although it would always be expected to reference
	 * the pFormat object, the print method gets its PageFormat as an argument.
	 */
	PageFormat pFormat;

	/**
	 * Global job used by this DocumentPrinter. Initialized upon calling the
	 * constructor.
	 */
	PrinterJob pJob;

	/**
	 * The constructor initializes the pFormat and PJob variables.
	 * 
	 * Caution calling PrinterJob.getPrinterJob() is done here which is
	 * resource-intensive.
	 */
	public DocumentPrinter() {
		pFormat = new PageFormat();
		pJob = PrinterJob.getPrinterJob();
	}

	/**
	 * pageDialog() displays a page setup dialog. Typically this is called by
	 * "File -> Print Setup"
	 */
	public void pageDialog() {
		pFormat = pJob.pageDialog(pFormat);
	}

	/**
	 * print(JEditorPane) prints a Document contained within a JEditorPane if
	 * the user confirms it using the printer dialog shown upon this call.
	 * 
	 * This method is useful when Java does not provide direct access to a
	 * particular Document type, such as a Rich Text Format document. With this
	 * method such a document can be sent to the DocumentPrinter class enclosed
	 * in a JEditorPane.
	 * 
	 * To pass a HTMLDocument call this method with an editorPane that contains
	 * the document
	 * 
	 * <pre>
	 * JEditorPane pane = new JEditorPane();
	 * pane.setContentType(&quot;text/html&quot;);
	 * pane.setDocument(htmlDocument);
	 * </pre>
	 * 
	 * This method is not thread-safe, if that matters to anybody, since it only
	 * uses a single PrinterJob.
	 * 
	 * @param jobName
	 *            (may be null) The print-job will get this attribute set, which
	 *            for instance is used by the Adobe PDF writer to determine an
	 *            initial guess for a filename or which is displayed in the
	 *            printer spooler.
	 * @param jedPane
	 *            The pane which to print. This is done by copying document and
	 *            content type. The original jedPane is not modified.
	 * @return The method will return false if the user canceled the operation,
	 *         true if the pages where send to the printing system successfully
	 *         and will throw an PrinterException to show to the user if an
	 *         error occurred.
	 * 
	 * @throws PrinterException
	 *             Show this exception to the user.
	 */
	public boolean print(String jobName, JEditorPane jedPane)
		throws PrinterException {

		if (!pJob.printDialog())
			return false;

		if (jobName != null)
			pJob.setJobName(jobName);

		JEditorPane pane = new JEditorPane();
		pane.setContentType(jedPane.getContentType());
		pane.setDocument(jedPane.getDocument());

		pJob.setPrintable(new DocumentPrintable(pane), pFormat);

		pJob.print();

		return true;
	}

	/**
	 * Class that actually does the printing.
	 * 
	 */
	class DocumentPrintable implements Printable {

		/**
		 * boolean to allow control over whether pages too wide to fit on a page
		 * will be scaled.
		 */
		boolean scaleWidthToFit = true;

		/**
		 * Used to keep track of when the page to print changes.
		 */
		int currentPage = -1;

		/**
		 * Location of the current page end.
		 */
		double pageEndY = 0;

		/**
		 * Location of the current page start.
		 */
		double pageStartY = 0;

		/**
		 * Stores the JEditorPane that is being printed.
		 */
		JEditorPane pane;

		public DocumentPrintable(JEditorPane pane) {
			this.pane = pane;
		}

		/**
		 * The print method implements the Printable interface. Although
		 * Printables may be called to render a page more than once, each page
		 * is painted in order. We may, therefore, keep track of changes in the
		 * page being rendered by setting the currentPage variable to equal the
		 * pageIndex, and then comparing these variables on subsequent calls to
		 * this method. When the two variables match, it means that the page is
		 * being rendered for the second or third time. When the currentPage
		 * differs from the pageIndex, a new page is being requested.
		 * 
		 * The highlights of the process used print a page are as follows:
		 * 
		 * I. The Graphics object is cast to a Graphics2D object to allow for
		 * scaling. II. The JEditorPane is laid out using the width of a
		 * printable page. This will handle line breaks. If the JEditorPane
		 * cannot be sized at the width of the graphics clip, scaling will be
		 * allowed. III. The root view of the JEditorPane is obtained. By
		 * examining this root view and all of its children, printView will be
		 * able to determine the location of each printable element of the
		 * document. IV. If the scaleWidthToFit option is chosen, a scaling
		 * ratio is determined, and the graphics2D object is scaled. V. The
		 * Graphics2D object is clipped to the size of the printable page. VI.
		 * currentPage is checked to see if this is a new page to render. If so,
		 * pageStartY and pageEndY are reset. VII. To match the coordinates of
		 * the printable clip of graphics2D and the allocation rectangle which
		 * will be used to lay out the views, graphics2D is translated to begin
		 * at the printable X and Y coordinates of the graphics clip. VIII. An
		 * allocation Rectangle is created to represent the layout of the Views.
		 * 
		 * The Printable Interface always prints the area indexed by reference
		 * to the Graphics object. For instance, with a standard 8.5 x 11 inch
		 * page with 1 inch margins the rectangle X = 72, Y = 72, Width = 468,
		 * and Height = 648, the area 72, 72, 468, 648 will be painted
		 * regardless of which page is actually being printed.
		 * 
		 * To align the allocation Rectangle with the graphics2D object two
		 * things are done. The first step is to translate the X and Y
		 * coordinates of the graphics2D object to begin at the X and Y
		 * coordinates of the printable clip, see step VII. Next, when printing
		 * other than the first page, the allocation rectangle must start laying
		 * out in coordinates represented by negative numbers. After page one,
		 * the beginning of the allocation is started at minus the page end of
		 * the prior page. This moves the part which has already been rendered
		 * to before the printable clip of the graphics2D object.
		 * 
		 * X. The printView method is called to paint the page. Its return value
		 * will indicate if a page has been rendered.
		 * 
		 * Although public, print should not ordinarily be called by programs
		 * other than PrinterJob.
		 */
		public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
			double scale = 1.0;
			Graphics2D graphics2D;
			View rootView;
			// I
			graphics2D = (Graphics2D) graphics;
			// II
			pane.setSize((int) pageFormat.getImageableWidth(),
				Integer.MAX_VALUE);
			pane.validate();
			// III
			rootView = pane.getUI().getRootView(pane);
			// IV
			if ((scaleWidthToFit)
				&& (pane.getMinimumSize().getWidth() > pageFormat
					.getImageableWidth())) {
				scale = pageFormat.getImageableWidth()
					/ pane.getMinimumSize().getWidth();
				graphics2D.scale(scale, scale);
			}
			// V
			graphics2D.setClip((int) (pageFormat.getImageableX() / scale),
				(int) (pageFormat.getImageableY() / scale), (int) (pageFormat
					.getImageableWidth() / scale), (int) (pageFormat
					.getImageableHeight() / scale));
			// VI
			if (pageIndex > currentPage) {
				currentPage = pageIndex;
				pageStartY += pageEndY;
				pageEndY = graphics2D.getClipBounds().getHeight();
			}
			// VII
			graphics2D.translate(graphics2D.getClipBounds().getX(), graphics2D
				.getClipBounds().getY());
			// VIII
			Rectangle allocation = new Rectangle(0, (int) -pageStartY,
				(int) (pane.getMinimumSize().getWidth()), (int) (pane
					.getPreferredSize().getHeight()));
			// X
			if (printView(graphics2D, allocation, rootView)) {
				return Printable.PAGE_EXISTS;
			} else {
				pageStartY = 0;
				pageEndY = 0;
				currentPage = -1;
				return Printable.NO_SUCH_PAGE;
			}
		}

		/**
		 * printView is a recursive method which iterates through the tree
		 * structure of the view sent to it. If the view sent to printView is a
		 * branch view, that is one with children, the method calls itself on
		 * each of these children. If the view is a leaf view, that is a view
		 * without children which represents an actual piece of text to be
		 * painted, printView attempts to render the view to the Graphics2D
		 * object.
		 * 
		 * I. When any view starts after the beginning of the current printable
		 * page, this means that there are pages to print and the method sets
		 * pageExists to true. II. When a leaf view is taller than the printable
		 * area of a page, it cannot, of course, be broken down to fit a single
		 * page. Such a View will be printed whenever it intersects with the
		 * Graphics2D clip. III. If a leaf view intersects the printable area of
		 * the graphics clip and fits vertically within the printable area, it
		 * will be rendered. IV. If a leaf view does not exceed the printable
		 * area of a page but does not fit vertically within the Graphics2D clip
		 * of the current page, the method records that this page should end at
		 * the start of the view. This information is stored in pageEndY.
		 */
		boolean printView(Graphics2D graphics2D, Shape allocation, View view) {
			boolean pageExists = false;
			Rectangle clipRectangle = graphics2D.getClipBounds();
			Shape childAllocation;
			View childView;

			if (view.getViewCount() > 0
				&& !view.getElement().getName().equalsIgnoreCase("td")) {
				for (int i = 0; i < view.getViewCount(); i++) {
					childAllocation = view.getChildAllocation(i, allocation);
					if (childAllocation != null) {
						childView = view.getView(i);
						if (printView(graphics2D, childAllocation, childView)) {
							pageExists = true;
						}
					}
				}
			} else {
				// I
				if (allocation.getBounds().getMaxY() >= clipRectangle.getY()) {
					pageExists = true;
					// II
					if ((allocation.getBounds().getHeight() > clipRectangle
						.getHeight())
						&& (allocation.intersects(clipRectangle))) {
						view.paint(graphics2D, allocation);
					} else {
						// III
						if (allocation.getBounds().getY() >= clipRectangle
							.getY()) {
							if (allocation.getBounds().getMaxY() <= clipRectangle
								.getMaxY()) {
								view.paint(graphics2D, allocation);
							} else {
								// IV
								if (allocation.getBounds().getY() < pageEndY) {
									pageEndY = allocation.getBounds().getY();
								}
							}
						}
					}
				}
			}
			return pageExists;
		}
	}
}