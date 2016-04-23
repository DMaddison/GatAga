/* Mesquite source code.  Copyright 1997 and onward, W. Maddison and D. Maddison. 


Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/
package mesquite.gataga.MoveDataToOtherEnd;
/*~~  */

import java.util.*;
import java.lang.*;
import java.awt.*;
import java.awt.event.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.categ.lib.*;
import mesquite.lib.table.*;



/* ======================================================================== */
public class MoveDataToOtherEnd extends MolecularDataAlterer implements AltererAlignShift  {
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	}
	/*.................................................................................................................*
   	public boolean requestPrimaryChoice(){
   		return true;  
   	}
   	
	/*.................................................................................................................*/
   	/** Called to alter data in those cells selected in table*/
   	public boolean alterData(CharacterData cData, MesquiteTable table,  UndoReference undoReference){
		if (!(cData instanceof MolecularData))
			return false;
		boolean found = false;
		MolecularData data = (MolecularData)cData;

		MesquiteInteger row= new MesquiteInteger();
		MesquiteInteger firstColumn= new MesquiteInteger();
		MesquiteInteger lastColumn= new MesquiteInteger();
		boolean dataChanged = false;
		MesquiteInteger charAdded = new MesquiteInteger();
		MesquiteInteger distanceMoved = new MesquiteInteger();
		boolean warned=false;
		MesquiteBoolean warnCheckSum = new MesquiteBoolean(true);


		if (table.anythingSelected()){
			while (table.nextSingleRowBlockSelected(row,firstColumn,lastColumn)) {
				int it = row.getValue();
				int[] startingFreqArray = ((CategoricalData)data).getStateFrequencyArrayOfTaxon(it)	;
				int icStart = firstColumn.getValue();
				int icEnd = lastColumn.getValue();
				int shiftAmount=0;
				int firstData = data.firstApplicable(it);
				int lastData=data.lastApplicable(it);
				int firstDataSelected = icStart;
				int lastDataSelected = icEnd;
				for (int i=icStart; i<=icEnd; i++) {
					if (!data.isInapplicable(i, it)) {
						firstDataSelected=i;
						break;
					}
				}
				for (int i=icEnd; i>=icStart; i--) {
					if (!data.isInapplicable(i, it)) {
						lastDataSelected=i;
						break;
					}
				}
				
				if (icStart==0 && icEnd==data.getNumChars()-1) {
					logln("Cannot move entire matrix to other end");
					// warning
				} else if (icStart==0) {  // then we are at the start
					// need to move the block from firstDataSelected to lastDataSelected to just past lastData
					int charactersNeeded = (lastDataSelected-firstDataSelected +1) - (data.getNumChars()-1 - lastData);
					if (charactersNeeded>0) {
						data.addCharacters(data.getNumChars(), charactersNeeded, true);
					}
					data.moveDataBlock(firstData,  icEnd, lastData+1, it, it, false, true);
					int[] freqArray = ((CategoricalData)data).getStateFrequencyArrayOfTaxon(it);
					if (!data.stateFrequencyArraysEqual(startingFreqArray, freqArray)) {
						MesquiteMessage.discreetNotifyUser("State frequencies within taxon " + it + " changed!");					}
					dataChanged=true;
				} else if (icEnd==data.getNumChars()-1) {  // then we are at the end
					int charactersNeeded = (lastDataSelected-firstDataSelected +1) - (firstData);
					if (charactersNeeded>0) {
						data.addCharacters(-1, charactersNeeded, true);
						icStart+= charactersNeeded;
						lastData+= charactersNeeded;
					}
					data.moveDataBlock(icStart, lastData,firstData-(lastDataSelected-firstDataSelected +1)+charactersNeeded, it, it, false, true);
					int[] freqArray = ((CategoricalData)data).getStateFrequencyArrayOfTaxon(it);
					if (!data.stateFrequencyArraysEqual(startingFreqArray, freqArray)) {
						MesquiteMessage.discreetNotifyUser("State frequencies within taxon " + it + " changed!");					}
					dataChanged=true;
				} else  {
					if (!warned)
						MesquiteMessage.discreetNotifyUser("Your selection needs to include either the first character or last character (but not both!). [Character range selected = " + (icStart+1) + " to " + (icEnd+1) + ", taxon "+ (it+1) + ".]");
					warned=true;
				}
			}
		}
		if (dataChanged) {
			data.notifyListeners(this, new Notification(MesquiteListener.DATA_CHANGED));
			data.notifyInLinked(new Notification(MesquiteListener.DATA_CHANGED));
		}
		return found;
   	}
   	/*.................................................................................................................*/
  	 public boolean showCitation() {
		return false;
   	 }
 	/*.................................................................................................................*/
   	 public boolean isSubstantive(){
   	 	return true;
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
  	/*.................................................................................................................*/
    	 public String getNameForMenuItem() {
		return "Move Sequences To Other End";
   	 }
	/*.................................................................................................................*/
    	 public String getName() {
		return "Move Sequences To Other End";
   	 }
	/*.................................................................................................................*/
 	/** returns an explanation of what the module does.*/
 	public String getExplanation() {
 		return "Moves any selected data to the other end of the matrix." ;
   	 }
   	 
}


