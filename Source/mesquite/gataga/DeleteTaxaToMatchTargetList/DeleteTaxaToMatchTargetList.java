/* Mesquite source code.  Copyright 1997 and onward, W. Maddison and D. Maddison. 



Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.gataga.DeleteTaxaToMatchTargetList; 

import mesquite.lib.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.duties.*;

/* ======================================================================== */
public class DeleteTaxaToMatchTargetList extends FileProcessor {
	String[] targetList = null;
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	}
	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
	public boolean requestPrimaryChoice(){
		return false;  
	}
	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return true;
	}
	/*.................................................................................................................*/
	public boolean isSubstantive(){
		return true; //not really, but to force checking of prerelease
	}

	/** if returns true, then requests to remain on even after alterFile is called.  Default is false*/
	public boolean pleaseLeaveMeOn(){
		return false;
	}
	/*.................................................................................................................*/
	/** Called to alter file. */
	public int processFile(MesquiteFile file){
		if (targetList == null || okToInteractWithUser(CAN_PROCEED_ANYWAY, "Asking for target list")){ //need to check if can proceed
			MesquiteString directoryName = new MesquiteString();
			MesquiteString fileName = new MesquiteString();
			String path = MesquiteFile.openFileDialog("Target list of taxon names",  directoryName,  fileName);
			targetList = MesquiteFile.getFileContentsAsStrings(path);
			/*if (targetList != null){
				Debugg.println("Target names");
				for (int i= 0; i<targetList.length; i++)
					Debugg.println("   " + targetList[i]);
			}*/
		}
		if (targetList == null){
			if (okToInteractWithUser(CAN_PROCEED_ANYWAY, "No target list"))
				alert("No Target List obtained");
			return 2;
		}
		if (okToInteractWithUser(CAN_PROCEED_ANYWAY, "No target list"))
			alert("Target List [taxon names in square brackets]: " + StringArray.toString(targetList));

		for (int im = 0; im < proj.getNumberTaxas(file); im++){
			Taxa taxa = proj.getTaxa(file, im);

			MesquiteMessage.println("Deleting taxa not matching list, in block " + im + " (" + taxa.getName() + ", id = " + taxa.getID() + ")");
			boolean deleted = false;
			for (int iL = 0; iL< targetList.length; iL++){
				for (int it=taxa.getNumTaxa()-1; it>=0; it--){
					String name = taxa.getTaxonName(it);
					if (StringArray.indexOfIgnoreCase(targetList, name)<0){
						taxa.deleteTaxa(it, 1, false);
						deleted = true;
					}

				}
			}
			if (deleted)
				taxa.notifyListeners(this, new Notification(MesquiteListener.PARTS_DELETED));
		}
		return 0;

	}
	/*.................................................................................................................*/
	public String getName() {
		return "Delete any taxa whose names are NOT in a target list";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Deletes any taxa whose names aren't in a list of names in a file." ;
	}

}


