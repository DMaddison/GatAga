package mesquite.gataga.BlastEValueCriterion;


import mesquite.gataga.lib.*;
import mesquite.lib.*;
import mesquite.molec.lib.BLASTResults;

public class BlastEValueCriterion extends BlastSeparateCriterion {
	double bestBLASTSearchesCutoff = 0.0;
	
	static final int BESTEVALUE = 0;
	static final int HIGHEREVALUE = 1;
	static int numCategories = 2;

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		loadPreferences();
		return true;
	}
	
	/*.................................................................................................................*/
	public String[] getSubdirectoryNames() {
		subdirectoryNames = new String[numCategories];
		if (bestBLASTSearchesCutoff==0.0) {
			subdirectoryNames[BESTEVALUE] = "Best eValue=0";
		}
		else {
			subdirectoryNames[BESTEVALUE] = "Best eValue<=";
		}
		subdirectoryNames[HIGHEREVALUE] = "Higher eValues";
		return subdirectoryNames;
	}
	/*.................................................................................................................*/
	public String getDescriptionOfThisMatch(int index){
		if (index==BESTEVALUE) {
			if (bestBLASTSearchesCutoff==0.0) {
				return "These are sequences whose top hit had an eValue of 0.0";
			}
			else {
				return "These are sequences whose top hit had an eValue less than or equal to "+ bestBLASTSearchesCutoff;
			}
		} else {
			return "These are sequences whose top hit had an eValue greater than "+ bestBLASTSearchesCutoff;
		}
	}


	/*.................................................................................................................*/
	public int getCriterionMatch(BLASTResults blastResult) {
		if (blastResult.geteValue(0)<=bestBLASTSearchesCutoff)
			return BESTEVALUE;
		return HIGHEREVALUE;
	
	}
	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		 if ("bestBLASTSearchesCutoff".equalsIgnoreCase(tag))
			bestBLASTSearchesCutoff = MesquiteDouble.fromString(content);		
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(60);	
		StringUtil.appendXMLTag(buffer, 2, "bestBLASTSearchesCutoff", bestBLASTSearchesCutoff);  
		return buffer.toString();
	}

	/*.................................................................................................................*/
	public boolean queryOptions() {
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), getName() + " Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel(getName() + " Options");
		
		DoubleField bestBLASTSearchesCutoffField = dialog.addDoubleField("Separate FASTA files with eValue <=", bestBLASTSearchesCutoff, 20, 0.0, Double.MAX_VALUE);

		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			bestBLASTSearchesCutoff = bestBLASTSearchesCutoffField.getValue();
			storePreferences();
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}

	/*.................................................................................................................*/
	public String getParameters() {
		StringBuffer sb= new StringBuffer();
		sb.append("\n   Separate FASTA files with eValues <= " + bestBLASTSearchesCutoff);
		return sb.toString();
	}
	

	/*.................................................................................................................*/
	public String getName() {
		return "eValue";
	}


}
