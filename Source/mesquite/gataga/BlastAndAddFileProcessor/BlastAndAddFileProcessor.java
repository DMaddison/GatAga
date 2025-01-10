/* Mesquite GatAga source code.  Copyright 2012 David Maddison & Wayne Maddison
Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */


package mesquite.gataga.BlastAndAddFileProcessor; 

import java.util.*;
import java.awt.*;
import java.awt.image.*;

import mesquite.categ.lib.DNAData;
import mesquite.categ.lib.MolecularData;
import mesquite.categ.lib.ProteinData;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.lib.table.*;
import mesquite.lib.ui.DoubleField;
import mesquite.lib.ui.ExtensibleDialog;
import mesquite.lib.ui.MesquiteSubmenuSpec;
import mesquite.molec.lib.BLASTResults;
import mesquite.molec.lib.Blaster;
import mesquite.molec.lib.NCBIUtil;

/* ======================================================================== */
public class BlastAndAddFileProcessor extends FileProcessor {
	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
		EmployeeNeed e = registerEmployeeNeed(Blaster.class, getName() + "  needs a Blast module.","");
	}
	MesquiteTable table;
	CharacterData data;
	MesquiteSubmenuSpec mss= null;
	Blaster blasterTask;
	int blastType = Blaster.BLAST;
	int maxHits = 1;
	double  minimumBitScore = 0.0;
	boolean preferencesSet = false;
	//	boolean blastx = false;
	int maxTime = 300;

	double eValueCutoff = 1.0E-50;
	int wordSize = 11;
	boolean optionsQueried = false;
	String[] accessionNumbers;
	String[] ID;
	int[] passNumberOfIDs = null;


	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		blasterTask = (Blaster)hireEmployee(Blaster.class, "Blaster (for " + getName() + ")"); 
		if (blasterTask==null)
			return sorry(getName() + " couldn't start because no Blast module could be obtained.");
		else if (!blasterTask.initialize())
			return false;
		loadPreferences();
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

	Choice blastTypeChoice ;

	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("maxHits".equalsIgnoreCase(tag))
			maxHits = MesquiteInteger.fromString(content);		
		else if ("eValueCutoff".equalsIgnoreCase(tag))
			eValueCutoff = MesquiteDouble.fromString(content);		
		else if ("wordSize".equalsIgnoreCase(tag))
			wordSize = MesquiteInteger.fromString(content);
		else if ("blastType".equalsIgnoreCase(tag))
			blastType = MesquiteInteger.fromString(content);
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(60);	
		StringUtil.appendXMLTag(buffer, 2, "maxHits", maxHits);  
		StringUtil.appendXMLTag(buffer, 2, "eValueCutoff", eValueCutoff);  
		StringUtil.appendXMLTag(buffer, 2, "wordSize", wordSize);  
		StringUtil.appendXMLTag(buffer, 2, "blastType", blastType);  
		return buffer.toString();
	}
	/*.................................................................................................................*/
	public boolean queryOptions() {
		if (optionsQueried)
			return true;
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "Top Blast Matches",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel("Options for Top Blast Matches");
		int oldBlastType = blastType;

		IntegerField numHitsField = dialog.addIntegerField("Number of top hits:", maxHits, 8, 1, Integer.MAX_VALUE);
		DoubleField eValueCutoffField = dialog.addDoubleField("Reject hits with eValues greater than: ", eValueCutoff, 20, 0.0, Double.MAX_VALUE);
		IntegerField wordSizeField = dialog.addIntegerField("word size:", wordSize, 8, 1, Integer.MAX_VALUE);
		blastTypeChoice = dialog.addPopUpMenu("BLAST type for nucleotides", Blaster.getBlastTypeNames(), blastType);
		//	blastXCheckBox.addItemListener(this);

		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			eValueCutoff = eValueCutoffField.getValue();
			// 			blastx = blastXCheckBox.getState();
			blastType = blastTypeChoice.getSelectedIndex();
			if (blastType<0) blastType=oldBlastType;
			maxHits = numHitsField.getValue();
			wordSize = wordSizeField.getValue();
			storePreferences();
		}
		dialog.dispose();
		optionsQueried = true;
		return (buttonPressed.getValue()==0);
	}

	/** if returns true, then requests to remain on even after alterFile is called.  Default is false*/
	public boolean pleaseLeaveMeOn(){
		return false;
	}
	/*.................................................................................................................*/
	public boolean searchOneTaxon(CharacterData data, int it){
		if (data==null || blasterTask==null)
			return false;
		String sequenceName = data.getTaxa().getTaxonName(it);
		MesquiteStringBuffer sequence = new MesquiteStringBuffer(data.getNumChars());
		for (int ic = 0; ic<=data.getNumChars(); ic++) {
			data.statesIntoStringBuffer(ic, it, sequence, false, false, false);
		}
		StringBuffer response = new StringBuffer();
		//blasterTask.setBlastx(blastx);
		blasterTask.setBlastType(blastType);

		int numDatabases = blasterTask.getNumDatabases();
		passNumberOfIDs= new int[0];
		ID=new String[0];

		for (int iDatabase = 0; iDatabase<numDatabases; iDatabase++) {

			String database = blasterTask.getDatabase(iDatabase);

			if (data instanceof ProteinData)
				blasterTask.blastForMatches(database, "blastp", sequenceName, sequence.toString(), true, maxHits, maxTime, eValueCutoff, wordSize, response, true);
			else {	
				blasterTask.basicDNABlastForMatches(database, blastType, sequenceName, sequence.toString(), maxHits, maxTime, eValueCutoff, wordSize, response, true);
			}

			BLASTResults blastResults = new BLASTResults(maxHits);
			blastResults.processResultsFromBLAST(response.toString(), false, eValueCutoff);
			blasterTask.postProcessingCleanup(blastResults);


			accessionNumbers = blastResults.getAccessions();
			//ID = blastResults.getIDs();
			String[] thisID =  blastResults.getIDs();
			ID = StringArray.concatenate(ID, thisID);
			int startThisPass = passNumberOfIDs.length;
			passNumberOfIDs=IntegerArray.addParts(passNumberOfIDs, thisID.length);
			//	IntegerArray.zeroUnassigned(passNumberOfIDs);
			for (int pass=startThisPass; pass<passNumberOfIDs.length; pass++)
				passNumberOfIDs[pass]=iDatabase;
		}
		return ID!=null;
	}

	/*.................................................................................................................*/
	/** Called to alter file. */
	public int processFile(MesquiteFile file){
		MesquiteProject proj = file.getProject();
		if (proj == null)
			return 2;
		boolean success = false;
		if (!queryOptions())
			return 2;
		for (int im = 0; im < proj.getNumberCharMatrices(file); im++){
			CharacterData data = proj.getCharacterMatrix(file, im);
			//Debugg.println checkCompatibility

			if (data instanceof MolecularData) {
				logln("BLASTing matrix " + im + " (" + data.getName() + ", id = " + data.getID() + ")");

				int numOriginalTaxa = data.getNumTaxa();

				for (int it=0; it<numOriginalTaxa; it++){
					if (searchOneTaxon(data, it)){
						success = true;


						//NCBIUtil.getGenBankIDs(accessionNumbers, false,  this, false);
						logln("About to import top matches.", true);
						StringBuffer report = new StringBuffer();

						for (int passNumber=0; passNumber<blasterTask.getNumDatabases(); passNumber++)  {

							int count = 0;
							for (int i=0; i<ID.length && i<passNumberOfIDs.length; i++) {  // find out how many of the IDs belong tho this pass number
								if (passNumberOfIDs[i]==passNumber)
									count++;
							}
							String[] localID = new String[count];
							count=0;
							for (int i=0; i<ID.length && i<passNumberOfIDs.length; i++) {  // now fill a local ID array with the ones that belong to this pass
								if (passNumberOfIDs[i]==passNumber) {
									localID[count]=ID[i];
									count++;
								}
							}

							if (blastType==Blaster.BLASTX && data instanceof DNAData) {
								//	ID = NCBIUtil.getNucIDsFromProtIDs(ID);
								localID = blasterTask.getNucleotideIDsfromProteinIDs(localID);
								//						logln("****AFTER NucToProt IDs: " +StringArray.toString(ID)); 
							}
							//String newSequencesAsFasta = NCBIUtil.fetchGenBankSequencesFromIDs(ID, data instanceof DNAData, this, true, report);	

							StringBuffer blastResponse = new StringBuffer();
							String newSequencesAsFasta = blasterTask.getFastaFromIDs(data.getTaxa().getTaxonName(it), localID,  data instanceof DNAData, blastResponse, passNumber);
							//String newSequencesAsFasta = blasterTask.getFastaFromIDs(localID,  data instanceof DNAData, blastResponse, passNumber);

							if (StringUtil.notEmpty(newSequencesAsFasta))
								NCBIUtil.importFASTASequences(data, newSequencesAsFasta, this, report, -1, it, false, false);
							else
								logln("   Blast database returned no FASTA files in response to query.");
							data.notifyListeners(this, new Notification(MesquiteListener.PARTS_ADDED));
							data.getTaxa().notifyListeners(this, new Notification(MesquiteListener.PARTS_ADDED));
						}

					} else {
						//logln(" fail ");
					}
				}
			}


		}
		if (success)
			return 0;
		return 1;
	}
	/*.................................................................................................................*/
	public String getName() {
		return "GATAGA - BLAST and Add Top Hits";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "BLASTs each sequence in a file to a database and add the top hit(s) to the file." ;
	}

}


