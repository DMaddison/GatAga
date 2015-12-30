/* Mesquite source code.  Copyright 1997 and onward, W. Maddison and D. Maddison. 


Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.gataga.DeleteInternalGappy;
/*~~  */

import java.awt.Checkbox;
import java.awt.Dialog;
import java.awt.Label;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.categ.lib.*;
import mesquite.lib.table.*;
import mesquite.lists.lib.ListModule;

/* ======================================================================== */
public class DeleteInternalGappy extends DNADataAlterer  implements AltererWholeCharacterAddRemove {
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

	double gapPercentage = 100.00;

	boolean queryOptions(){
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog queryDialog = new ExtensibleDialog(containerOfModule(), "Delete Sites with Many Internal Gaps",  buttonPressed);
		DoubleField perc = queryDialog.addDoubleField ("Delete sites with % internal gaps >", gapPercentage, 6, 0, 100.00);

		queryDialog.completeAndShowDialog(true);

		boolean ok = (queryDialog.query()==0);

		if (ok) {
			gapPercentage = perc.getValue();
		}

		queryDialog.dispose();   		 

		return ok;
	}
	/*.................................................................................................................*/
	/** Called to alter data in those cells selected in table*/
	public boolean alterData(CharacterData dData, MesquiteTable table,  UndoReference undoReference){
		this.table = table;
		if (!(dData instanceof DNAData)){
			MesquiteMessage.warnProgrammer("Can use " + getName() + " only on DNA data");
			return false;
		}
		DNAData data = (DNAData)dData;
		if (okToInteractWithUser(CAN_PROCEED_ANYWAY, "Querying about options")){ //need to check if can proceed
			boolean ok = queryOptions();
			if (!ok)
				return false;

		}
		UndoInstructions undoInstructions = null;
		if (undoReference!=null)
			undoInstructions =data.getUndoInstructionsAllMatrixCells(new int[] {UndoInstructions.CHAR_DELETED});
		boolean noColumnsSelected =  !((table != null && table.anyColumnSelected()) || data.anySelected());

		deleteGappy(false,  noColumnsSelected,  data,  table); 

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
	boolean deletable(int ic, DNAData data){
		if (gapPercentage >99.99999)
			return false;
		int countWithData = 0;
		int countInternalInapplicable = 0;
		int keepThreshold = (int)((100-gapPercentage-0.000001)/100.00*data.getNumTaxa());
		for (int it = 0; it<data.getNumTaxa(); it++)  // && countWithData<keepThreshold
			if (!data.isInapplicable(ic, it)) 
				countWithData++;
			else if (data.isInternalInapplicable(ic, it)) 
				countInternalInapplicable++;
		if (countWithData>keepThreshold)
			return false;
		int thresholdSkip = (int)((gapPercentage+0.000001)/100.00*(countWithData + countInternalInapplicable));
		if (countInternalInapplicable<=thresholdSkip)
			return false;
		return true;
	}
	/*.................................................................................................................*/
	private void deleteGappy(boolean notify, boolean noColumnsSelected, DNAData data, MesquiteTable table){
		if (data!=null) {
			boolean changed=false;
			for (int ic=data.getNumChars()-1; ic>=0; ic--) {
				if (noColumnsSelected || isSelected(ic, data, table)){
					if (deletable(ic, data)) { 
						data.deleteCharacters(ic, 1, false);
						data.deleteInLinked(ic, 1, false);
						changed = true;
					}
				}

			}

			if (notify) {
				if (changed)
					data.notifyListeners(this, new Notification(MesquiteListener.PARTS_DELETED));  
			}
		}
	}

	/*.................................................................................................................*/
	public boolean showCitation(){
		return false;
	}
	/*.................................................................................................................*/
	public String getName() {
		return "Delete Sites with Many Internal Gaps";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Deletes sites which have many gaps within coding regions." ;
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return NEXTRELEASE;  
	}

}


