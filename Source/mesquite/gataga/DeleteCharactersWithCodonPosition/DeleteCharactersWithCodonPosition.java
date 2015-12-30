/* Mesquite source code.  Copyright 1997 and onward, W. Maddison and D. Maddison. 


Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.gataga.DeleteCharactersWithCodonPosition;
/*~~  */


import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.categ.lib.*;
import mesquite.lib.table.*;

/* ======================================================================== */
public class DeleteCharactersWithCodonPosition extends DNADataAlterer  implements AltererWholeCharacterAddRemove {
	MesquiteTable table;
	CharacterData data;
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	}

	boolean isSelected(int ic, DNAData data, MesquiteTable table){
		if (table!= null && table.isColumnSelected(ic))
			return true;
		if (data.getSelected(ic))
			return true;
		return false;
	}
	int codonPositionToDelete =0;

	boolean queryOptions(){
		codonPositionToDelete =  MesquiteInteger.queryInteger(containerOfModule(), "Delete Characters With Codon Position", "Delete Characters With Codon Position (0 = noncoding; 1, 2, 3)", 0, 0, 3, true);
		return MesquiteInteger.isCombinable(codonPositionToDelete);
	}
	/*.................................................................................................................*/
	/** Called to alter data in those cells selected in table*/
	public boolean alterData(CharacterData data, MesquiteTable table,  UndoReference undoReference){
		this.table = table;
		if (okToInteractWithUser(CAN_PROCEED_ANYWAY, "Querying about options")){ //need to check if can proceed
			boolean ok = queryOptions();
			if (!ok)
				return false;

		}
		UndoInstructions undoInstructions = null;
		if (undoReference!=null)
			undoInstructions =data.getUndoInstructionsAllData();
		CodonPositionsSet codPosSet = (CodonPositionsSet)data.getCurrentSpecsSet(CodonPositionsSet.class);
		if (codPosSet == null)
			return false;
		deleteDeletable(data,  table, codPosSet); 

		if (undoReference!=null){
			if (undoInstructions!=null){
				undoInstructions.setNewData(data);
				undoReference.setUndoer(undoInstructions);
				undoReference.setResponsibleModule(this);
			}
		}
		return true;
	}
	/*.................................................................................................................*/
	public void alterCell(CharacterData ddata, int ic, int it){
		/*CategoricalData data = (CategoricalData)ddata;
		if (data.isUnassigned(ic, it))
			data.setState(ic, it, CategoricalState.inapplicable);
		 */
	}

	/*.................................................................................................................*/
	public boolean isPrerelease() {
		return true;
	}
	/*.................................................................................................................*/
	private void deleteDeletable(CharacterData data, MesquiteTable table, CodonPositionsSet codPosSet){
		if (data!=null) {
			for (int ic=data.getNumChars()-1; ic>=0; ic--) {
				if (codPosSet != null) {
					int cP = codPosSet.getInt(ic);
					if (cP == codonPositionToDelete){
						data.deleteCharacters(ic, 1, false);
						data.deleteInLinked(ic, 1, false);
					}
				}

			}
		}
	}

	/*.................................................................................................................*/
	public boolean showCitation(){
		return false;
	}
	/*.................................................................................................................*/
	public String getName() {
		return "Delete Characters With Codon Position";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Deletes characters that are of a particular codon position." ;
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return NEXTRELEASE;  
	}

}


