package mesquite.gataga.BlastMatchingNamesCriterion;

import java.awt.Checkbox;

import mesquite.gataga.lib.*;
import mesquite.lib.*;
import mesquite.molec.lib.BLASTResults;

public class BlastMatchingNamesCriterion extends BlastSeparateCriterion {

	String matchList = null;
	int minNumToMatch = 1;
	int maxNumToMatch = 1;
	boolean onlyMatchOnce = false;
	boolean matchNoOthers = false;
	String[] matchInDefinitions = null;

	static final int UNMATCHED = 0;

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		loadPreferences();
		return true;
	}
	
	public void intialize() {
		

	}
	
	/*.................................................................................................................*/
	public boolean isActive() {
		return StringUtil.notEmpty(matchList);
	}

	/*.................................................................................................................*/
	public String[] getSubdirectoryNames() {
		int range = maxNumToMatch-minNumToMatch+1;
		subdirectoryNames = new String[range+1];
		subdirectoryNames[UNMATCHED] = "Unmatched";
		for (int i=1; i<=range; i++) 
			subdirectoryNames[i] = "Match "+ (minNumToMatch+i-1);
		return subdirectoryNames;
	}

	/*.................................................................................................................*/
	public int getCriterionMatch(BLASTResults blastResult) {
		if (matchInDefinitions==null)
			matchInDefinitions = getMatchList(matchList);
		int count = blastResult.hitsSatisfyMatches(matchInDefinitions, minNumToMatch, maxNumToMatch, matchNoOthers, onlyMatchOnce);
		if (count>0) {
			return count-minNumToMatch+1;
		}
		return UNMATCHED;
	}

	
	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		 if ("onlyMatchOnce".equalsIgnoreCase(tag))
			onlyMatchOnce = MesquiteBoolean.fromTrueFalseString(content);
		else if ("matchNoOthers".equalsIgnoreCase(tag))
			matchNoOthers = MesquiteBoolean.fromTrueFalseString(content);
		else if ("minNumToMatch".equalsIgnoreCase(tag))
			minNumToMatch = MesquiteInteger.fromString(content);
		else if ("maxNumToMatch".equalsIgnoreCase(tag))
			maxNumToMatch = MesquiteInteger.fromString(content);
		 else if ("matchList".equalsIgnoreCase(tag))
			 matchList = StringUtil.cleanXMLEscapeCharacters(content);
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(60);	
		StringUtil.appendXMLTag(buffer, 2, "onlyMatchOnce", onlyMatchOnce);  
		StringUtil.appendXMLTag(buffer, 2, "matchNoOthers", matchNoOthers);  
		StringUtil.appendXMLTag(buffer, 2, "minNumToMatch", minNumToMatch);  
		StringUtil.appendXMLTag(buffer, 2, "maxNumToMatch", maxNumToMatch);  
		StringUtil.appendXMLTag(buffer, 2, "matchList", matchList);  
		return buffer.toString();
	}

	/*.................................................................................................................*/
	private String[] getMatchList(String matchList) { 
		if (StringUtil.blank(matchList))
			return null;
		Parser parser = new Parser();
		parser.setString(matchList);
		parser.setWhitespaceString(",; ");
		int numItems = parser.getNumberOfTokens();
		if (numItems<=0)
			return null;
		String[] matchDef = new String[numItems];
		for (int i=0; i<numItems; i++) {
			matchDef[i]=parser.getNextToken();
		}

		return matchDef;
	}

	/*.................................................................................................................*/
	public boolean queryOptions() {
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), getName() + " Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel(getName() + " Options");

		IntegerField minMatchesField = dialog.addIntegerField("Sequester FASTA files with at least ", minNumToMatch, 4, 1, Integer.MAX_VALUE);
		dialog.suppressNewPanel();
		IntegerField maxMatchesField = dialog.addIntegerField("hits and at most ", maxNumToMatch, 4, 1, Integer.MAX_VALUE);
		dialog.suppressNewPanel();
		dialog.addLabel("hits");
		SingleLineTextField matchListField = dialog.addTextField("that match these names:", matchList, 40, true);
		Checkbox onlyMatchOnceBox = dialog.addCheckBox("and only one hit per match", onlyMatchOnce);
		dialog.suppressNewPanel();
		Checkbox matchNoOthersBox = dialog.addCheckBox("and all hits must match one in the list ", matchNoOthers);

		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			onlyMatchOnce = onlyMatchOnceBox.getState();
			matchNoOthers = matchNoOthersBox.getState();
			minNumToMatch = minMatchesField.getValue();
			maxNumToMatch = maxMatchesField.getValue();
			matchList = matchListField.getText();
			matchInDefinitions = getMatchList(matchList);
			storePreferences();
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}
	/*.................................................................................................................*/
	public String getParameters() {
		StringBuffer sb= new StringBuffer();
		sb.append("\n   Separate FASTA files with ");
		if (minNumToMatch==maxNumToMatch) {
			if (minNumToMatch==1)
				sb.append("\n     1 hit ");
			else 
				sb.append("\n     " + minNumToMatch + " hits ");
		}
		else 
			sb.append("\n     between " + minNumToMatch + " and "+ maxNumToMatch + " hits ");
		sb.append("\n     that match these names: " + matchList);
		if (onlyMatchOnce) 
			sb.append("\n     with each name only matched once");
		if (matchNoOthers) 
			sb.append("\n     and all hits must match at least one name in the list");
		return sb.toString();
	}
	/*.................................................................................................................*/
	
	public String getName() {
		return "Matching List of Names";
	}


}
