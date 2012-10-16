package net.sf.jabref.specialfields;

import java.util.ArrayList;

import javax.swing.ImageIcon;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;

public class RankCompact extends Rank {

	private static RankCompact INSTANCE = null;

	public RankCompact() {
		ArrayList<SpecialFieldValue> values = new ArrayList<SpecialFieldValue>();
    	//lab.setName("i");
		values.add(new SpecialFieldValue(this, null, "clearRank", Globals.lang("Clear rank"), null, Globals.lang("No rank information")));
		values.add(new SpecialFieldValue(this, "rank1", "setRank1", Globals.lang("Set rank to one star"), GUIGlobals.getImage("rankc1"), Globals.lang("One star")));
		values.add(new SpecialFieldValue(this, "rank2", "setRank2", Globals.lang("Set rank to two stars"), GUIGlobals.getImage("rankc2"), Globals.lang("Two stars")));
		values.add(new SpecialFieldValue(this, "rank3", "setRank3", Globals.lang("Set rank to three stars"), GUIGlobals.getImage("rankc3"), Globals.lang("Three stars")));
		values.add(new SpecialFieldValue(this, "rank4", "setRank4", Globals.lang("Set rank to four stars"), GUIGlobals.getImage("rankc4"), Globals.lang("Four stars")));
		values.add(new SpecialFieldValue(this, "rank5", "setRank5", Globals.lang("Set rank to five stars"), GUIGlobals.getImage("rankc5"), Globals.lang("Five stars")));
		this.setValues(values);
		TEXT_DONE_PATTERN = "Set rank %0 for %1 entries";
	}

	public static RankCompact getInstance() {
		if (INSTANCE  == null) {
			INSTANCE = new RankCompact();
		}
		return INSTANCE;
	}	
	
    public ImageIcon getRepresentingIcon() {
    	return GUIGlobals.getImage("ranking");
    }

}
