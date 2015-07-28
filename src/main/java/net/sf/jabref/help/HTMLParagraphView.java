package net.sf.jabref.help;

import javax.swing.text.Element;
import javax.swing.text.FlowView;
import javax.swing.text.View;
import javax.swing.text.html.ParagraphView;

class HTMLParagraphView extends ParagraphView {

    private static final int MAX_VIEW_SIZE = 100;


    public HTMLParagraphView(Element elem) {
        super(elem);
        strategy = new HTMLParagraphView.HTMLFlowStrategy();
    }


    private static class HTMLFlowStrategy extends FlowStrategy {

        @Override
        protected View createView(FlowView fv, int startOffset, int spanLeft, int rowIndex) {
            View res = super.createView(fv, startOffset, spanLeft, rowIndex);
            if ((res.getEndOffset() - res.getStartOffset()) > HTMLParagraphView.MAX_VIEW_SIZE) {
                res = res.createFragment(startOffset, startOffset + HTMLParagraphView.MAX_VIEW_SIZE);
            }
            return res;
        }

    }


    @Override
    public int getResizeWeight(int axis) {
        return 0;
    }
}
