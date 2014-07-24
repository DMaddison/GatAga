/* Mesquite source code.  Copyright 1997-2011 W. Maddison and D. Maddison.

Version 2.75, September 2011.
Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.gataga.TaxonNamesToMatchTargetList; 

import mesquite.lib.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.duties.*;

/* ======================================================================== */
public class TaxonNamesToMatchTargetList extends FileProcessor {
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
	public boolean alterFile(MesquiteFile file){
		boolean success = false;
		if (targetList == null || okToInteractWithUser(CAN_PROCEED_ANYWAY, "Asking for target list")){ //need to check if can proceed
			MesquiteString directoryName = new MesquiteString();
			MesquiteString fileName = new MesquiteString();
			String path = MesquiteFile.openFileDialog("Target list of taxon names",  directoryName,  fileName);
			targetList = MesquiteFile.getFileContentsAsStrings(path);
		}
   		if (targetList == null)
   			return false;
   		for (int im = 0; im < proj.getNumberTaxas(file); im++){
   			Taxa taxa = proj.getTaxa(file, im);
   			//Debugg.println checkCompatibility
   			
   			Debugg.println("Altering taxa block " + im + " (" + taxa.getName() + ", id = " + taxa.getID() + ")");
   			for (int iL = 0; iL< targetList.length; iL++){
   				String target = targetList[iL];
   				int count = 0;
   				int iMatched = -1;
   				for (int it=0; it<taxa.getNumTaxa() && count<2; it++){
   					String name = taxa.getTaxonName(it);
   					if (StringUtil.indexOfIgnoreCase(name, target)>=0){
   						count++;
   						if (count == 1)
   							iMatched = it;
   						else if (count>1)
   							iMatched = -1;
   					}
   						
   				}
   				if (iMatched >=0)
   					taxa.setTaxonName(iMatched, target);
   			}
   		}
		return true;

	}
	/*.................................................................................................................*/
	public String getName() {
		return "Adjust taxon names to match target list";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Changes name of taxon if it is the only one matching a target substring from a list in a file." ;
	}

}


