package mesquite.gataga.SplitSequenceBlocksIntoSeparateTaxa;

import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.duties.*;
import mesquite.lib.characters.*;
import mesquite.lib.table.*;
import mesquite.lib.taxa.Taxa;

public class SplitSequenceBlocksIntoSeparateTaxa extends MolecularDataAlterer {
	
	int referenceSequence = 1;

	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	}

	/*.................................................................................................................*/
	/** Called to alter data in those cells selected in table*/
	public int alterData(CharacterData dData, MesquiteTable table,  UndoReference undoReference){
		if (dData==null)
			return -10;
	
		if (!(dData instanceof MolecularData)){
			MesquiteMessage.warnProgrammer(getName() + " requires molecular sequence data");
			return INCOMPATIBLE_DATA;
		}
		MolecularData data = (MolecularData)dData;

		UndoInstructions undoInstructions = data.getUndoInstructionsAllMatrixCells(null);
		boolean changed = false;
		
		
		Taxa taxa = data.getTaxa();
		
		int it = taxa.getNumTaxa()-1;
		
		while (it>=0) {
			if ((table == null || !table.anyRowSelected()||table.wholeRowSelectedAnyWay(it))) {
				String s = taxa.getTaxonName(it);
				data.incrementSuppressHistoryStamp();

				int numBlocks = data.getNumDataBlocks(it,0, data.getNumChars());

				//Debugg.println("it: " + it + ", numBlocks: " + numBlocks);
				if (numBlocks>1) {
					taxa.addTaxa(it, numBlocks-1, true);
					changed=true;
					for (int count=1; count<numBlocks; count++) {  // now let's rename the taxa
						String provisional = s;
						int num = 2;
						while (taxa.whichTaxonNumber(provisional)>=0){
							provisional = s + " (" + num + ")";
							num++;
						}
						taxa.setTaxonName(it+count, provisional);
					}
					
					MesquiteInteger blockStart = new MesquiteInteger();
					MesquiteInteger blockEnd = new MesquiteInteger();
					for (int count=2; count<=numBlocks; count++) {  // now let's populate the new taxa
						data.getDataBlockBoundaries(it,0,data.getNumChars(), count,  blockStart,  blockEnd); 
						//Debugg.println("   it " + it + ", block: " + count + ", blockStart=" + blockStart.getValue()+ ", blockEnd=" + blockEnd.getValue());
						if (blockStart.isCombinable() && blockEnd.isCombinable())
							for (int ic=blockStart.getValue(); ic<=blockEnd.getValue(); ic++) {
								data.setState(ic, it+count-1, data.getState(ic, it));
							}
						blockStart.setToUnassigned();
						blockEnd.setToUnassigned();
					}
					
					data.getDataBlockBoundaries(it,0,data.getNumChars(), 1,  blockStart,  blockEnd); 
					//Debugg.println("   it " + it + ", block: " + 1 + ", blockStart=" + blockStart.getValue()+ ", blockEnd=" + blockEnd.getValue());
					if (blockStart.isCombinable() && blockEnd.isCombinable())  // now let's clean out the original taxon
						for (int ic=0; ic<data.getNumChars(); ic++) {
							if (ic<blockStart.getValue() || ic>blockEnd.getValue())
								data.setState(ic, it, DNAState.inapplicable);
						}
						
				}
				data.decrementSuppressHistoryStamp();
				data.notifyListeners(this, new Notification(MesquiteListener.DATA_CHANGED));

			}
			it--;
		}

		
		if (undoInstructions!=null) {
			undoInstructions.setNewData(data);
			if (undoReference!=null){
				undoReference.setUndoer(undoInstructions);
				undoReference.setResponsibleModule(this);
			}
		}
		if (changed)
			return SUCCEEDED;
		return MEH;
	}

	/*.................................................................................................................*
	public boolean operateOnTaxa(Taxa taxa){
		boolean changed=false;
		boolean processAll = false;
		int numSelected = taxa.numberSelected();
		if (numSelected<1){
			processAll=true;
		}
		boolean[] selected = new boolean[taxa.getNumTaxa()];

		for (int it = 0; it<taxa.getNumTaxa(); it++) {
			selected[it] = taxa.getSelected(it);
		}
		
		int numMatrices = getProject().getNumberCharMatrices(taxa);
		
		int it = taxa.getNumTaxa()-1;
		
		while (it>=0) {
			if (selected[it] || processAll) {
				String s = taxa.getTaxonName(it);
				
				for (int iM = 0; iM < numMatrices; iM++){
					CharacterData basicData = getProject().getCharacterMatrix(taxa, iM);
					
					if (basicData instanceof MolecularData) {
						MolecularData data = (MolecularData)basicData;
						data.incrementSuppressHistoryStamp();

						int numBlocks = data.getNumSequenceBlocks(it,0, data.getNumChars());

						Debugg.println("it: " + it + ", numBlocks: " + numBlocks);
						if (numBlocks>1) {
							taxa.addTaxa(it+1, numBlocks-1, true);
							changed=true;
							for (int count=1; count<=numBlocks; count++) {  // now let's rename the taxa
								String provisional = s + " ";
								int num = 2;
								while (taxa.whichTaxonNumber(provisional)>=0){
									provisional = s + " (" + num + ")";
									num++;
								}
								int itDup = it+1;
								taxa.setTaxonName(it+count, provisional);
							}
						}
						data.decrementSuppressHistoryStamp();
						data.notifyListeners(this, new Notification(MesquiteListener.DATA_CHANGED));
					}
				}

				
			}
			it--;
		}
		return changed;
	}
	/*.................................................................................................................*/
	public void alterCell(CharacterData ddata, int ic, int it){
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
		return "GATAGA - Split Sequence Blocks into Separate Taxa";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Splits the sequences blocks in a taxon into separate taxa." ;
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return  NEXTRELEASE;  
	}

}


