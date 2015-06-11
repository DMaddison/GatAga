/* Mesquite source code.  Copyright 1997 and onward, W. Maddison and D. Maddison. 


Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.gataga.SetGroupsFromCodonPositions;
/*~~  */

import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Label;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.categ.lib.*;
import mesquite.lib.table.*;
import mesquite.lists.lib.CharListPartitionUtil;
import mesquite.lists.lib.ListModule;

/* ======================================================================== */
public class SetGroupsFromCodonPositions extends DNADataAlterer {
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

	/*int startingPos = 0;
	double skipPercentage = 100.00;

	boolean queryOptions(){
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog queryDialog = new ExtensibleDialog(containerOfModule(), "Setting Codon Positions",  buttonPressed);
		queryDialog.addLabel("Setting Codon Positions of selected characters", Label.CENTER);
		RadioButtons choices = queryDialog.addRadioButtons (new String[]{"123123...", "23123...", "3123...", "Minimize Stop Codons"}, 0);
		DoubleField skip = queryDialog.addDoubleField ("Skip (and set non-coding) sites with % internal gaps >", skipPercentage, 6, 0, 100.00);

		queryDialog.completeAndShowDialog(true);

		boolean ok = (queryDialog.query()==0);

		if (ok) {
			startingPos = choices.getValue();
			skipPercentage = skip.getValue();
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
		data = dData;
		CodonPositionsSet modelSet = (CodonPositionsSet) data.getCurrentSpecsSet(CodonPositionsSet.class);
		if (modelSet ==null)
			return false;
		DNAData data = (DNAData)dData;
		CharacterPartition  partition= new CharacterPartition("Partition by Codon Positions", data.getNumChars(), null, data);
		partition.addToFile(data.getFile(), getProject(), findElementManager(CharacterPartition.class));
		data.setCurrentSpecsSet(partition, CharacterPartition.class);
		CharactersGroupVector groups = (CharactersGroupVector)getProject().getFileElement(CharactersGroupVector.class, 0);
		CharactersGroup[] pos = new CharactersGroup[4];
		pos[0] = (CharactersGroup)groups.getElement("noncoding"); //0
		if (pos[0] == null){
			pos[0] = new CharactersGroup();
			pos[0].setName("noncoding");
			pos[0].addToFile(data.getFile(), data.getProject(), null);
			pos[0].setColor(Color.gray);
		}
		pos[1] = (CharactersGroup)groups.getElement("pos1");
		if (pos[1] == null){
			pos[1] = new CharactersGroup();
			pos[1].setName("pos1");
			pos[1].addToFile(data.getFile(), data.getProject(), null);
			pos[1].setColor(Color.blue);
		}
		pos[2] = (CharactersGroup)groups.getElement("pos2");
		if (pos[2] == null){
			pos[2] = new CharactersGroup();
			pos[2].setName("pos2");
			pos[2].addToFile(data.getFile(), data.getProject(), null);
			pos[2].setColor(Color.green);
		}
		pos[3] = (CharactersGroup)groups.getElement("pos3");
		if (pos[3] == null){
			pos[3] = new CharactersGroup();
			pos[3].setName("pos3");
			pos[3].addToFile(data.getFile(), data.getProject(), null);
			pos[3].setColor(Color.red);
		}
		for (int ic=0; ic<data.getNumChars(); ic++) {
			int position = modelSet.getInt(ic);
			if (position<4)
				partition.setProperty(pos[position], ic);
		}

		//find if there are character groups called "pos1", "pos2", etc..  If they exist, use them; otherwise make new ones

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
	public boolean showCitation(){
		return false;
	}
	/*.................................................................................................................*/
	public String getName() {
		return "Set Groups from Codon Positions";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Sets character groups (partitions) from codon positions." ;
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return 304;  
	}

}


