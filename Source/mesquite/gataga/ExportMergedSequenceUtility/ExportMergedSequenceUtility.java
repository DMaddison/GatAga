package mesquite.gataga.ExportMergedSequenceUtility;

/* Mesquite Chromaseq source code.  Copyright 2005-2011 David Maddison and Wayne Maddison.
Version 1.0   December 2011
Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */



import java.awt.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.categ.lib.*;
import mesquite.molec.lib.*;

/* ======================================================================== */
public class ExportMergedSequenceUtility extends DataUtility { 
	
	int referenceSequence = 0;
	
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName){
		return true;
	}
	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
	public boolean requestPrimaryChoice(){
		return true;  
	}
	/*.................................................................................................................*/
	public boolean isSubstantive(){
		return true;
	}

	/*.................................................................................................................*/
	/** Called to operate on the data in all cells.  Returns true if data altered*/
	public boolean operateOnData(CharacterData data){ 
		if (data==null || !(data instanceof CategoricalData))
			return false;
		long[] states = new long[data.getNumChars()];
		CategoricalData cData = (CategoricalData)data;
		for (int ic=0; ic<states.length; ic++) {
			states[ic]=0;
		}

		for (int it=0; it<data.getNumTaxa(); it++) {
			if (it!=referenceSequence) {
				for (int ic=0; ic<data.getNumChars(); ic++) {
					long s1 = cData.getState(ic, it);
					states[ic] = CategoricalState.mergeStates(states[ic], s1);
				}
			}
		}
		
		for (int ic=0; ic<states.length; ic++) {
			if (CategoricalState.cardinality(states[ic])>1)
				states[ic] = CategoricalState.setUncertainty(states[ic], true);
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append(">"+getProject().getHomeFileName()+" [Merged]"+StringUtil.lineEnding());
		cData.statesIntoStringBufferCore(0, states,  sb,  false,  true,  true);
		
		String filePath = module.getProject().getHomeFile().getDirectoryName() + getProject().getHomeFileName()+" [Merged].fas";
		MesquiteFile.putFileContents(filePath, sb.toString(), true);
		

		
//		MesquiteFile.putFileContents(rootDir+fileNames[i], fileContents[i], true);

		
		
		return false;
	}
	/*.................................................................................................................*/
	public Snapshot getSnapshot(MesquiteFile file) {
		Snapshot temp = new Snapshot();
		return temp;
	}
	public CompatibilityTest getCompatibilityTest(){
		return new RequiresAnyMolecularData();
	}



	/*.................................................................................................................*/
	public String getName() {
		return "Export Merged Sequence Utility";
	}
	/*.................................................................................................................*/
	public String getNameForMenuItem() {
		return "Export Merged Sequence Utility";
	}

	/*.................................................................................................................*/
	public String getExplanation() {
		return "Exports the merger of all sequences in a matrix (.";
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return NEXTRELEASE;  
	}
	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return true;
	}

}









