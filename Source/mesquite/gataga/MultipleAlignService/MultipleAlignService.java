package mesquite.gataga.MultipleAlignService;

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
public class MultipleAlignService extends CategDataAlterer {
	MultipleSequenceAligner aligner;
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		aligner= (MultipleSequenceAligner)hireNamedEmployee(MultipleSequenceAligner.class, arguments);
		if (aligner !=null) {
			aligner = (MultipleSequenceAligner)hireNamedEmployee(MultipleSequenceAligner.class, arguments);
			if (aligner == null)
				return sorry(getName() + " couldn't start because the requested aligner wasn't successfully hired.");
		}
		else {
			aligner = (MultipleSequenceAligner)hireEmployee(MultipleSequenceAligner.class, "Aligner");
			if (aligner == null)
				return sorry(getName() + " couldn't start because no aligner module obtained.");
		}
		aligner.setQueryOptionsOnceOnly(true);  //Debugg.println for use in multiple matrix settings
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
   	/** Called to alter data in those cells selected in table*/
   	public boolean alterData(CharacterData data, MesquiteTable table,  UndoReference undoReference){
		if (data==null)
			return false;
		
		if (!(data instanceof DNAData))
			return false;
		long[][] m  = aligner.alignSequences((MCategoricalDistribution)data.getMCharactersDistribution(), null, 0, data.getNumChars()-1, 0, data.getNumTaxa()-1);
		Debugg.println(" m " + m);
		if (m != null)
			Debugg.println(" m.length " + m.length);
		boolean success = AlignUtil.integrateAlignment(m, (MolecularData)data,  0, data.getNumChars()-1, 0, data.getNumTaxa()-1);
		return success;
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
		return "Multiple Sequence Alignment";
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


