/* Mesquite source code.  Copyright 1997 and onward, W. Maddison and D. Maddison. 


Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.gataga.TrimToWholeCodon;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.table.*;

public class TrimToWholeCodon extends DNADataAlterer {

	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	}
	double percentage = 0.0;
	/*.................................................................................................................*/
	/** Called to alter data in those cells selected in table*/
	public boolean alterData(CharacterData dData, MesquiteTable table,  UndoReference undoReference){
		if (dData==null)
			return false;

		if (!(dData instanceof DNAData)){
			MesquiteMessage.warnProgrammer(getName() + " requires DNA data");
			return false;
		}
		DNAData data = (DNAData)dData;
		CodonPositionsSet modelSet = (CodonPositionsSet) data.getCurrentSpecsSet(CodonPositionsSet.class);
		if (modelSet ==null){
			MesquiteMessage.warnProgrammer(getName() + " requires that there be codon positions specified for the matrix");
			return false;
		}
	//	UndoInstructions undoInstructions = data.getUndoInstructionsAllData();
		boolean changed = false;
		boolean passed = false;
		int deleteUntil = -1;
		for (int ic = 0; ic<data.getNumChars() && !passed; ic++){  // check start
			int countWithData = 0;
			if (modelSet.getInt(ic) == 1)
				passed = true;
			else
				deleteUntil = ic;
		}
		if (deleteUntil >=0){
			int numToDelete = deleteUntil+1;
			logln("Deleting first " + (numToDelete)  + " sites");
			data.deleteCharacters(0, numToDelete, false);
			data.deleteInLinked(0, numToDelete, false);
			changed = true;
		}
		passed = false;
		deleteUntil = -1;
		for (int ic = data.getNumChars()-1; ic>=0 && !passed; ic--){  // check end
			if (modelSet.getInt(ic) == 3)
				passed = true;
			else
				deleteUntil = ic;
		}
		if (deleteUntil >=0){
			int ncic = data.getNumChars()-deleteUntil;
			logln("Deleting last " + (ncic)  + " sites");
			data.deleteCharacters(deleteUntil, ncic, false);
			data.deleteInLinked(deleteUntil, ncic, false);
			changed = true;
		}
/*
		if (undoInstructions!=null) {
			undoInstructions.setNewData(data);
			if (undoReference!=null){
				undoReference.setUndoer(undoInstructions);
				undoReference.setResponsibleModule(this);
			}
		}
		*/
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
		return "Trim To Whole Codons";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Trims start of matrix until the first codon position 1, and end of matrix after last codon position 3." ;
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return  304;  
	}

}

