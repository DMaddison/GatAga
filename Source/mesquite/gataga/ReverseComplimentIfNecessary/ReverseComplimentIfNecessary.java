package mesquite.gataga.ReverseComplimentIfNecessary;

import java.util.*;
import java.lang.*;
import java.awt.*;
import java.awt.image.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.categ.lib.*;
import mesquite.lib.table.*;
import mesquite.align.lib.*;



/* ======================================================================== */
public class ReverseComplimentIfNecessary extends CategDataAlterer {
	
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	}
	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
   	public boolean requestPrimaryChoice(){
   		return true;  
   	}
	
	/*.................................................................................................................*/
   	public void alterCell(CharacterData data, int ic, int it){
   	}
	
	/*.................................................................................................................*/
	private void processData(DNAData data, Taxa taxa, boolean proteinCoding) {
		MolecularDataUtil.reverseComplementSequencesIfNecessary(data, module, taxa, 0, taxa.getNumTaxa(), false);
	}
	/*.................................................................................................................*/
   	/** Called to alter data in those cells selected in table*/
   	public boolean alterData(CharacterData data, MesquiteTable table,  UndoReference undoReference){
		if (data==null)
			return false;
		
		if (!(data instanceof DNAData))
			return false;
	//	try{
		processData((DNAData)data,data.getTaxa(),true);
//		}
//		catch (ArrayIndexOutOfBoundsException e){
//			return false;
//		}
		return true;
   	}
   	
	/*.................................................................................................................*/
  	 public boolean showCitation() {
		return true;
   	 }
 	/*.................................................................................................................*/
   	 public boolean isSubstantive(){
   	 	return true;
   	 }
	/*.................................................................................................................*/
   	 public boolean isPrerelease(){
   	 	return true;
   	 }
	   /*.................................................................................................................*/
   	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
   	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
   	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
      	public int getVersionOfFirstRelease(){
      		return -100;  
    }
	/*.................................................................................................................*/
    	 public String getName() {
		return "Reverse Complement If Necessary";
   	 }
    		/*.................................................................................................................*
    	 public String getNameForMenuItem() {
		return "Shift Other To Match...";
   	 }
	/*.................................................................................................................*/
 	/** returns an explanation of what the module does.*/
 	public String getExplanation() {
 		return "Blah blah blah." ;
   	 }
   	 
}


