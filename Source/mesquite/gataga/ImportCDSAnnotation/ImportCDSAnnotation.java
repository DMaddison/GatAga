/* Mesquite source code.  Copyright 1997 and onward, W. Maddison and D. Maddison. 


Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.gataga.ImportCDSAnnotation;
/*~~  */

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Dialog;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.categ.lib.*;
import mesquite.lib.table.*;
import mesquite.lists.lib.ListModule;

/* ======================================================================== */
public class ImportCDSAnnotation extends DNADataAlterer implements ActionListener{
	MesquiteTable table;
	CharacterData data;
	Taxa taxa = null;
	String referenceTaxon = "";
	SingleLineTextField referenceTaxonField = null;
	String annotationPath = "";
	SingleLineTextField annotationPathField = null;
	boolean deleteOutsideCDS = false;
	boolean deleteNotInReference = false;
	String[][] annotations;

	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		loadPreferences();
		return true;
	}
	public void endJob(){
		storePreferences();  //also after options chosen
		super.endJob();
	}


	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("annotationPath".equalsIgnoreCase(tag)){  
			annotationPath = StringUtil.cleanXMLEscapeCharacters(content);
		}
		else	if ("referenceTaxon".equalsIgnoreCase(tag)){   
			referenceTaxon = StringUtil.cleanXMLEscapeCharacters(content);
		}
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 1, "annotationPath", annotationPath);  
		StringUtil.appendXMLTag(buffer, 1, "referenceTaxon", referenceTaxon);  
		return buffer.toString();
	}
	/*.................................................................................................................*/
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equalsIgnoreCase("annotationBrowse")) {
			MesquiteString directoryName = new MesquiteString();
			MesquiteString fileName = new MesquiteString();
			annotationPath = MesquiteFile.openFileDialog("Choose Annotation File", directoryName, fileName);
			if (StringUtil.notEmpty(annotationPath))
				annotationPathField.setText(annotationPath);
		}
		else if (e.getActionCommand().equalsIgnoreCase("chooseRefTaxon")) {
			if (taxa == null)
				return;
			Taxon t = taxa.userChooseTaxon(containerOfModule(), "Choose reference taxon to which the annotation refers");
			if (t == null)
				return;
			referenceTaxon = t.getName();
			if (StringUtil.notEmpty(referenceTaxon))
				referenceTaxonField.setText(referenceTaxon);
		}
	}

	boolean queryOptions(){
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog queryDialog = new ExtensibleDialog(containerOfModule(), "Importing CDS annotation",  buttonPressed);
		queryDialog.addLabel("Importing CDS annotation", Label.CENTER);

		referenceTaxonField = queryDialog.addTextField("Reference taxon:",referenceTaxon, 40);
		Button referenceTaxonButton = queryDialog.addAListenedButton("Choose...", null, this);
		referenceTaxonButton.setActionCommand("chooseRefTaxon");

		annotationPathField = queryDialog.addTextField("Path to Annotation File:",annotationPath, 40);
		Button annotationFileBrowseButton = queryDialog.addAListenedButton("Browse...", null, this);
		annotationFileBrowseButton.setActionCommand("annotationBrowse");

		Checkbox deleteOutsideCDSCB = queryDialog.addCheckBox ("Delete sites outside CDS", false);

		Checkbox deleteNotInReferenceCB = queryDialog.addCheckBox ("Delete sites not in reference taxon", false);
		
		queryDialog.completeAndShowDialog(true);
		boolean ok = (queryDialog.query()==0);

		if (ok) {
			annotationPath = annotationPathField.getText();
			annotations = MesquiteFile.getTabDelimitedTextFile(annotationPath, false);
			referenceTaxon = referenceTaxonField.getText();
			deleteOutsideCDS = deleteOutsideCDSCB.getState();
			deleteNotInReference = deleteNotInReferenceCB.getState();
		}

		queryDialog.dispose();   		 

		return ok;
	}

	int findRow(String target){
		if (annotations == null)
			return -1;
		if (annotations.length == 0)
			return -1;
		for (int i = 0; i<annotations.length; i++){
			String rowName = annotations[i][0];
			if (target.indexOf(rowName)>=0)
				return i;
		}
		return -1;
	}
	int getStartCDS(int whichRow){
		if (annotations == null)
			return -1;
		if (whichRow < 0 || whichRow >= annotations.length)
			return -1;
		if (annotations[whichRow].length < 2)
			return -1;
		String s = annotations[whichRow][1];
		int k = MesquiteInteger.fromString(s);
		if (MesquiteInteger.isCombinable(k)){
			return k;
		}
		return -1;
	}
	int getEndCDS(int whichRow){
		if (annotations == null)
			return -1;
		if (whichRow < 0 || whichRow >= annotations.length)
			return -1;
		if (annotations[whichRow].length < 3)
			return -1;
		String s = annotations[whichRow][2];
		int k = MesquiteInteger.fromString(s);
		if (MesquiteInteger.isCombinable(k)){
			return k;
		}
		return -1;
	}
	int getFirstPosition(int whichRow){
		if (annotations == null)
			return 1;
		if (whichRow < 0 || whichRow >= annotations.length)
			return 1;
		if (annotations[whichRow].length < 5)
			return 1;
		String s = annotations[whichRow][4];
		int k = MesquiteInteger.fromString(s);
		if (MesquiteInteger.isCombinable(k)){
			return k;
		}
		return 1;
	}
	boolean getReverse(int whichRow){
		if (annotations == null)
			return false;
		if (whichRow < 0 || whichRow >= annotations.length)
			return false;
		if (annotations[whichRow].length < 4)
			return false;
		String s = annotations[whichRow][3];
		if (s.indexOf("-")>=0)
			return true;
		return false;
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
		taxa = data.getTaxa();
		if (okToInteractWithUser(CAN_PROCEED_ANYWAY, "Querying about options")){ //need to check if can proceed
			boolean ok = queryOptions();
			if (!ok)
				return false;

		}
		UndoInstructions undoInstructions = data.getUndoInstructionsAllData();

		if (annotations == null)
			annotations = MesquiteFile.getTabDelimitedTextFile(annotationPath, false);
		int refTaxon = taxa.whichTaxonNumber(referenceTaxon);
		if (refTaxon < 0){
			MesquiteMessage.warnUser("WARNING: Reference taxon " + referenceTaxon + " not found in data matrix in file " + getProject().getHomeFile().getFileName());
		}
		//use name of this file to find 
		String fileName = getProject().getHomeFileName();
		int whichRow = findRow(fileName);
		if (whichRow <0){
			MesquiteMessage.warnUser("WARNING: No reference to file " + fileName + " found in CSV file ");
			return false;
		}
		int startCDS = getStartCDS(whichRow);
		int endCDS = getEndCDS(whichRow);
		boolean isReverse = getReverse(whichRow);
		int firstPosition = getFirstPosition(whichRow);

		//Debugg.println("ROW "  + whichRow + " startCDS "  + startCDS + " endCDS "  + endCDS + " isReverse "  + isReverse+ " firstPosition "  + firstPosition + " refTaxon " + refTaxon);
		processMatrix(refTaxon, startCDS, endCDS, isReverse, firstPosition, data);
		if (undoInstructions!=null){
			undoInstructions.setNewData(data);
			if (undoReference!=null){
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
	private void processMatrix(int refTaxon, int start,  int end, boolean isReverse, int firstPosition, DNAData data){
		if (data!=null) {
			boolean changed=false;
			MesquiteNumber num = new MesquiteNumber();
			if (isReverse) {
				int lastPosition = (firstPosition + (end-start))% 3;
				Debugg.println("last positition " + lastPosition);
				if (lastPosition == 0)
					lastPosition = 3;
				num.setValue(lastPosition);
			}
			else {
				num.setValue(firstPosition);
			}
			CodonPositionsSet modelSet = (CodonPositionsSet) data.getCurrentSpecsSet(CodonPositionsSet.class);
			if (modelSet == null) {
				modelSet= new CodonPositionsSet("Codon Positions", data.getNumChars(), data);
				modelSet.addToFile(data.getFile(), getProject(), findElementManager(CodonPositionsSet.class)); //THIS
				data.setCurrentSpecsSet(modelSet, CodonPositionsSet.class);
			}
			int alignmentStart= -1;
			int alignmentEnd = -1;
			if (modelSet != null) {
				int countRef = 0;
				for (int ic=0; ic<data.getNumChars(); ic++) {
					if (!data.isInapplicable(ic,  refTaxon)){
						countRef++;
						if (countRef == start)
							alignmentStart = ic;
						if (countRef == end)
							alignmentEnd = ic;
						if (countRef<start || countRef>end) { 
							modelSet.setValue(ic, 0);

						}
						else {
							modelSet.setValue(ic, num);
							if (isReverse){ //adjusting for next to come
								num.setValue(num.getIntValue()-1);
								if (num.getIntValue()<1)
									num.setValue(3);
							}
							else {
								num.setValue(num.getIntValue()+1);
								if (num.getIntValue()>3)
									num.setValue(1);
							}
						}
						changed = true;
					}
				}

			}
			if (isReverse || deleteOutsideCDS || deleteNotInReference){
				if (deleteOutsideCDS){  // delete from end+1 to numchars, then from 0 to start-1
					if (alignmentEnd >=0 && alignmentEnd<data.getNumChars()-1){
						data.deleteCharacters(alignmentEnd+1, data.getNumChars()-alignmentEnd-1, false);
						data.deleteInLinked(alignmentEnd+1, data.getNumChars()-alignmentEnd-1, false);
					}
					if (alignmentStart>0){
						data.deleteCharacters(0, alignmentStart, false);
						data.deleteInLinked(0, alignmentStart, false);
					}
				}
				if (deleteNotInReference){
					for (int ic=data.getNumChars()-1; ic>0; ic--) {
						if (data.isInapplicable(ic,  refTaxon)){
							data.deleteCharacters(ic, 1, false);
							data.deleteInLinked(ic, 1, false);
						}
					}

				}
				if (isReverse){
						data.reverseComplement(0, data.getNumChars()-1, true);  
				}
				data.notifyListeners(this, new Notification(MesquiteListener.DATA_CHANGED));  
			}
			else if (changed)
					data.notifyListeners(this, new Notification(AssociableWithSpecs.SPECSSET_CHANGED));  //not quite kosher; HOW TO HAVE MODEL SET LISTENERS??? -- modelSource

		}
	}
	/*.................................................................................................................*
	}	/*.................................................................................................................*/
	public boolean showCitation(){
		return false;
	}
	/*.................................................................................................................*/
	public String getName() {
		return "Import CDS Annotation for Reference Taxon";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Imports CDS annotation for a reference taxon, from tab-delimited text file." ;
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return NEXTRELEASE;  
	}

}


