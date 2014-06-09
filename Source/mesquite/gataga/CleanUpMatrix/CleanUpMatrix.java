package mesquite.gataga.CleanUpMatrix;

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
public class CleanUpMatrix extends CategDataAlterer {
	CategDataAlterer aligner;
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		aligner= (CategDataAlterer)hireNamedEmployee(CategDataAlterer.class, "#MultipleAlignService");
		return true;
	}
	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
   	public boolean requestPrimaryChoice(){
   		return true;  
   	}
	/*.................................................................................................................*
	public void processSingleXMLPreference (String tag, String content) {
		if ("matchFraction".equalsIgnoreCase(tag)) {
			matchFraction = MesquiteDouble.fromString(content);
		}
	
		preferencesSet = true;
}
/*.................................................................................................................*
public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "matchFraction", matchFraction);  

		preferencesSet = true;
		return buffer.toString();
	}
	/*.................................................................................................................*
	public boolean queryOptions(int it, int max) {
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog queryFilesDialog = new ExtensibleDialog(containerOfModule(), "Shift Other To Match",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		queryFilesDialog.addLabel("Shift Other To Match");
		
		if (!MesquiteInteger.isCombinable(it1) || !MesquiteInteger.isCombinable(it2) || it1>max || it2>max){
			if (it>=max) {
				it1=1;
			} else
				it1 = it+2;  // it+1 to bump it over one, +2 because of the translation to 1-based numbering
			it2=max;
		}
		IntegerField it1Field =  queryFilesDialog.addIntegerField ("First Sequence", it1, 4, 1, max);
		IntegerField it2Field =  queryFilesDialog.addIntegerField ("Last Sequence", it2, 4, 1, max);
		DoubleField matchFractionField = queryFilesDialog.addDoubleField ("Fraction of Match", matchFraction, 6, 0.00001, 1.0);
		
		queryFilesDialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			matchFraction = matchFractionField.getValue();
			it1 = it1Field.getValue();
			it2 = it2Field.getValue();
			storePreferences();
		}
		queryFilesDialog.dispose();
		return (buttonPressed.getValue()==0);
	}
	/*.................................................................................................................*/
   	public void alterCell(CharacterData data, int ic, int it){
   	}
	/*.................................................................................................................*
	boolean findMatchInSequence(CharacterData data, int masterRow, int masterStart, int masterEnd, int it, int start, MesquiteInteger matchEnd){
		if (data.dataMatches(it, start, masterRow, masterStart, masterEnd, matchEnd, false, true, matchFraction, cs1, cs2)) {
			return true;
		}
		return false;
	}
	/*.................................................................................................................*
	boolean findMatch(CharacterData data, MesquiteTable table, int masterRow, int masterStart, int masterEnd, int it, MesquiteInteger matchStart, MesquiteInteger matchEnd){
		for (int i = 0; i<data.getNumChars(); i++) {  // cycle through possible starting points of match
			if (findMatchInSequence(data,masterRow, masterStart, masterEnd, it, i, matchEnd)){
				matchStart.setValue(i);
				return true;
			}
		}
		return false;
	}
	/*.................................................................................................................*/

	/*.................................................................................................................*
	private boolean alignTouchedToDropped(int rowToAlign, int recipientRow){
		MesquiteNumber score = new MesquiteNumber();
		PairwiseAligner aligner;
		if (aligner==null) {
			aligner = new PairwiseAligner(true,false, subs,gapOpen.getValue(), gapExtend.getValue(), gapOpenTerminal.getValue(), gapExtendTerminal.getValue(), alphabetLength);
			//aligner.setUseLowMem(true);
		}
		if (aligner!=null){
			//aligner.setUseLowMem(data.getNumChars()>aligner.getCharThresholdForLowMemory());
			originalCheckSum = ((CategoricalData)data).storeCheckSum(0, data.getNumChars()-1,rowToAlign, rowToAlign);
			aligner.setAllowNewInternalGaps(allowNewGaps.getValue());
			long[][] aligned = aligner.alignSequences((MCategoricalDistribution)data.getMCharactersDistribution(), recipientRow, rowToAlign,MesquiteInteger.unassigned,MesquiteInteger.unassigned,true,score);
			if (aligned==null) {
				logln("Alignment failed!");
				return false;
			}
			logln("Align " + (rowToAlign+1) + " onto " + (recipientRow+1));
			long[] newAlignment = Long2DArray.extractRow(aligned,1);

			int[] newGaps = aligner.getGapInsertionArray();
			if (newGaps!=null)
				alignUtil.insertNewGaps((MolecularData)data, newGaps);
			Rectangle problem = alignUtil.forceAlignment((MolecularData)data, 0, data.getNumChars()-1, rowToAlign, rowToAlign, 1, aligned);

			((CategoricalData)data).examineCheckSum(0, data.getNumChars()-1,rowToAlign, rowToAlign, "Bad checksum; alignment has inapproppriately altered data!", warnCheckSum, originalCheckSum);
			return true;
		}
		return false;
	}
	/*.................................................................................................................*/
	private void processData(DNAData data, Taxa taxa, boolean proteinCoding) {
		Debugg.println(" reverseComplementSequencesIfNecessary");
		MolecularDataUtil.reverseComplementSequencesIfNecessary(data, module, taxa, 0, taxa.getNumTaxa(), false, false);
		Debugg.println(" pairwiseAlignMatrix");
		aligner.alterData(data, null,  null);
	//	MolecularDataUtil.pairwiseAlignMatrix(this, data, 0, false);
		if (proteinCoding){
			Debugg.println(" setCodonPositionsToMinimizeStops");
			MolecularDataUtil.setCodonPositionsToMinimizeStops(data, module, taxa, 0, taxa.getNumTaxa());
			//MolecularDataUtil.shiftToMinimizeStops(data, module, taxa, 0, taxa.getNumTaxa());
		}
// then alter taxon names
		//open character matrix
		// color by amino acid if protein coding
		
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
		return "Clean Up Matrix";
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


