/* Mesquite source code.  Copyright 1997 and onward, W. Maddison and D. Maddison. 



Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/
package mesquite.gataga.WriteTreePrunedToMatrix; 

import java.util.*;
import java.awt.*;
import java.awt.image.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.lib.table.*;

/* ======================================================================== */
public class WriteTreePrunedToMatrix extends FileProcessor {
	String directoryPath;
	String treeDescription;
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
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return 303;  
	}
	/*.................................................................................................................*/
   	 public boolean isPrerelease(){
   	 	return false;
   	 }
	/*.................................................................................................................*/
   	 public boolean isSubstantive(){
   	 	return true; //not really, but to force checking of prerelease
   	 }
 	/*.................................................................................................................*/
   	/** if returns true, then requests to remain on even after alterFile is called.  Default is false*/
   	public boolean pleaseLeaveMeOn(){
   		return false;
   	}
	/*.................................................................................................................*/
   	/** Called to alter file. */
   	public int processFile(MesquiteFile file, MesquiteString notice){
   		if (notice == null)
   			return 2;
   		MesquiteProject proj = file.getProject();
   		if (proj == null)
   			return 2;
		if (okToInteractWithUser(CAN_PROCEED_ANYWAY, "Asking for tree")){ //need to check if can proceed
			treeDescription = MesquiteString.queryString(containerOfModule(), "Tree to be saved as pruned", "Give the parenthesis notation (with taxon names) of the tree to be pruned", "");
			if (StringUtil.blank(treeDescription))
				return 2;
			directoryPath = MesquiteFile.chooseDirectory("Where to save tree files?"); //MesquiteFile.saveFileAsDialog("Base name for files (files will be named <name>1.nex, <name>2.nex, etc.)", baseName);
			if (StringUtil.blank(directoryPath))
				return 2;
		}
 		boolean success = false;
   		for (int im = 0; im < proj.getNumberCharMatrices(file); im++){
   			CharacterData data = proj.getCharacterMatrix(file, im);
   			Taxa taxa = data.getTaxa();
   			long[] ids = taxa.getTaxaIDs(); //to later prune new ones
			MesquiteTree t = new MesquiteTree(taxa);
			t.setPermitTaxaBlockEnlargement(true);  //Debugg.println playing
			Debugg.println(" tree " + treeDescription);

			if (t.readTree(treeDescription)) {
				//tree read, but it might have added taxa.  Now, prune the added taxa
				for (int it = taxa.getNumTaxa()-1; it>=0; it--){
					if (LongArray.indexOf(ids, taxa.getTaxon(it).getID()) <0)
							taxa.deleteTaxa(it, 1,  true);
				}
				t.reconcileTaxa(MesquiteListener.PARTS_DELETED, null);
				String prunedTree = t.writeTreeSimpleByNames();
				String treeFileName = 	file.getFileName()+ ".tre";
				Debugg.println("writing tree " + prunedTree);
				MesquiteFile.putFileContents(directoryPath + MesquiteFile.fileSeparator + treeFileName, prunedTree, true);	
				success = true;
			}
 			
   		}
   			
   		return 0;
   	}
	/*.................................................................................................................*/
	 public String getName() {
	return "Write Tree Pruned to Matrix";
	 }
		/*.................................................................................................................*/
	/*.................................................................................................................*/
 	/** returns an explanation of what the module does.*/
 	public String getExplanation() {
 		return "Writes a tree file whose tree includes only those taxa in the matrix(matrices) of the file." ;
   	 }
   	 
}


