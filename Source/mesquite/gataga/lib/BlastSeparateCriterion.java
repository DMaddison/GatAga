package mesquite.gataga.lib;

import mesquite.lib.*;
import mesquite.molec.lib.*;

import java.awt.event.*;

public abstract class BlastSeparateCriterion extends MesquiteModule implements ActionListener {
	protected String[] subdirectoryNames = null;

	public Class getDutyClass() {
		return BlastSeparateCriterion.class;
	}

	public void initialize() {
	}

	public abstract String[] getSubdirectoryNames();

	public abstract int getCriterionMatch(BLASTResults blastResults);

	public  String getDirectoryName(int index){
		if (subdirectoryNames!=null && index>=0 && index<subdirectoryNames.length)
			return subdirectoryNames[index];
		return null;
	}

	public boolean queryOptions() {
		return true;
	}


	public boolean isActive(){
		return true;
	}

	public boolean pleaseRecord(){
		return true;
	}

	public boolean isEditable(){
		return true;
	}


	public String getParameters(){
		return "";
	}

	public String getDescriptionOfThisMatch(int index){
		return "";
	}

	public void actionPerformed(ActionEvent e) {
		queryOptions();
	}

}
