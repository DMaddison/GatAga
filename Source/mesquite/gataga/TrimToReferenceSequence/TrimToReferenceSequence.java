package mesquite.gataga.TrimToReferenceSequence;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.table.*;

public class TrimToReferenceSequence extends MolecularDataAlterer implements AltererSimpleCell {
	
	int referenceSequence = 1;

	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	}

	/*.................................................................................................................*/
	/** Called to alter data in those cells selected in table*/
	public boolean alterData(CharacterData dData, MesquiteTable table,  UndoReference undoReference){
		if (dData==null)
			return false;
	
		if (!(dData instanceof MolecularData)){
			MesquiteMessage.warnProgrammer(getName() + " requires molecular sequence data");
			return false;
		}
		MolecularData data = (MolecularData)dData;

		UndoInstructions undoInstructions = data.getUndoInstructionsAllMatrixCells(new int[] {UndoInstructions.CHAR_DELETED});
		boolean changed = false;
		
		int startReference = data.firstApplicable(referenceSequence-1);
		int endReference = data.lastApplicable(referenceSequence-1);
		
		for (int it = 0; it<data.getNumTaxa(); it++)
			if (it!=referenceSequence-1 && (table == null || !table.anyRowSelected()||table.wholeRowSelectedAnyWay(it))) {
				for (int ic = 0; ic<startReference; ic++){  // check start
					data.setState(ic, it, DNAState.inapplicable);
				}
				for (int ic = data.getNumChars()-1; ic>endReference; ic--){
					data.setState(ic, it, DNAState.inapplicable);
				}
			}
		if (undoInstructions!=null) {
			undoInstructions.setNewData(data);
			if (undoReference!=null){
				undoReference.setUndoer(undoInstructions);
				undoReference.setResponsibleModule(this);
			}
		}
		return changed;
	}
	/*.................................................................................................................*/
	public void alterCell(CharacterData ddata, int ic, int it){
	}

	/*.................................................................................................................*/
	public boolean isPrerelease() {
		return true;
	}
	/*.................................................................................................................*/
	public boolean showCitation(){
		return false;
	}
	/*.................................................................................................................*/
	public String getName() {
		return "Trim to Reference Sequence";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Trims a sequence so that its ends match those of a reference sequence." ;
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return  NEXTRELEASE;  
	}

}

