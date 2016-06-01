
package mesquite.gataga.FetchGenBank; 


import java.awt.*;
import java.util.Iterator;
import java.util.List;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.categ.lib.*;
import mesquite.molec.lib.*;
import org.dom4j.*;

/* ======================================================================== */
public class FetchGenBank extends UtilitiesAssistant { 
	String genBankNumbers;
	String[] originalGeneNames = null;
	String[] standardizedGeneNames = null;
	String[] fragmentNames = null;
	String currentFragmentName = "";
	String translationTable="";
	boolean isDNA = true;
	
	MesquiteBoolean saveAsFasta = new MesquiteBoolean(true);
	MesquiteBoolean saveAsXML = new MesquiteBoolean(true);
	MesquiteBoolean includeTaxonNameIntoFASTAFile = new MesquiteBoolean(true);
	MesquiteBoolean includeVoucherCodeIntoFASTAFile = new MesquiteBoolean(true);
	MesquiteBoolean includeGeneInfoIntoFASTAFile = new MesquiteBoolean(false);
	String publicationCode ="PUB001";
	String voucherCodePrefix="";

	boolean lastTokenForVoucherCode = true;


	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName){
		loadPreferences();
		addMenuItem(null, "Fetch GenBank Sequences...", makeCommand("fetchGenBank", this));
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
	public void processSingleXMLPreference (String tag, String content) {
		if ("voucherCodePrefix".equalsIgnoreCase(tag))
			voucherCodePrefix= content;
		if ("saveAsFasta".equalsIgnoreCase(tag))
			saveAsFasta.setValue(MesquiteBoolean.fromTrueFalseString(content));
		if ("saveAsXML".equalsIgnoreCase(tag))
			saveAsXML.setValue(MesquiteBoolean.fromTrueFalseString(content));
		if ("includeTaxonNameIntoFASTAFile".equalsIgnoreCase(tag))
			includeTaxonNameIntoFASTAFile.setValue(MesquiteBoolean.fromTrueFalseString(content));
		if ("includeGeneInfoIntoFASTAfile".equalsIgnoreCase(tag))
			includeGeneInfoIntoFASTAFile.setValue(MesquiteBoolean.fromTrueFalseString(content));
		if ("includeVoucherCodeIntoFASTAFile".equalsIgnoreCase(tag))
			includeVoucherCodeIntoFASTAFile.setValue(MesquiteBoolean.fromTrueFalseString(content));
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(60);	
		StringUtil.appendXMLTag(buffer, 2, "voucherCodePrefix",voucherCodePrefix);
		StringUtil.appendXMLTag(buffer, 2, "saveAsFasta",saveAsFasta);
		StringUtil.appendXMLTag(buffer, 2, "saveAsXML",saveAsXML);
		StringUtil.appendXMLTag(buffer, 2, "includeGeneInfoIntoFASTAfile",includeGeneInfoIntoFASTAFile);
		StringUtil.appendXMLTag(buffer, 2, "includeVoucherCodeIntoFASTAFile",includeVoucherCodeIntoFASTAFile);
		StringUtil.appendXMLTag(buffer, 2, "includeTaxonNameIntoFASTAFile",includeTaxonNameIntoFASTAFile);
		
		return buffer.toString();
	}


	/*.................................................................................................................*/
	public boolean queryOptions() {
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "Fetch GenBank Sequences",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel("Accession Numbers (separated by commas); \nranges with commas allowed:");

		genBankNumbers = "";
		TextArea numbersArea = dialog.addTextArea("",  5);
		RadioButtons radioButtons = dialog.addRadioButtons(new String[] { "save FASTA file", "save XML file", "save both FASTA and XML file"}, 2);
		Checkbox includeTaxonNameCheckbox = dialog.addCheckBox("include taxon name in FASTA file",includeTaxonNameIntoFASTAFile.getValue());
		Checkbox includeVoucherCodeCheckbox = dialog.addCheckBox("include voucher code in FASTA file",includeVoucherCodeIntoFASTAFile.getValue());
		Checkbox includeGeneInfoCheckbox = dialog.addCheckBox("include gene information in FASTA file",includeGeneInfoIntoFASTAFile.getValue());
		SingleLineTextField voucherCodePrefixField = dialog.addTextField("Voucher code prefix",voucherCodePrefix, 40);
		SingleLineTextField publicationCodeField = dialog.addTextField("Publication code",publicationCode, 40);


		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			genBankNumbers = numbersArea.getText();
			int radioValue = radioButtons.getValue();
			if (radioValue==0){
				saveAsFasta.setValue(true);
				saveAsXML.setValue(false);
			} else if (radioValue==1) {
				saveAsFasta.setValue(false);
				saveAsXML.setValue(true);
			} else {
				saveAsFasta.setValue(true);
				saveAsXML.setValue(true);
			}
			includeTaxonNameIntoFASTAFile.setValue(includeTaxonNameCheckbox.getState());
			includeVoucherCodeIntoFASTAFile.setValue(includeVoucherCodeCheckbox.getState());
			includeGeneInfoIntoFASTAFile.setValue(includeGeneInfoCheckbox.getState());
			publicationCode= publicationCodeField.getText();
			voucherCodePrefix = voucherCodePrefixField.getText();
			storePreferences();
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}
	/*.................................................................................................................*/
	String getVoucherInfo(Element featureTableElement) {
		List featureElements = featureTableElement.elements();
		for (Iterator iter = featureElements.iterator(); iter.hasNext();) {   // this is going through all of the notices
			Element featureElement = (Element) iter.next();
			Element gbFeatureQualElement = featureElement.element("GBFeature_quals");
			List qualifierElements = gbFeatureQualElement.elements();
			for (Iterator iter2 = qualifierElements.iterator(); iter2.hasNext();) {   // this is going through all of the notices
				Element qualifierElement = (Element) iter2.next();
				Element gbQualifierName = qualifierElement.element("GBQualifier_name");
				if ("specimen_voucher".equalsIgnoreCase(gbQualifierName.getText())) {
					Element gbQualifierValue = qualifierElement.element("GBQualifier_value");
					return gbQualifierValue.getText();

				}
			}
		}
		return "";
	}
	/*.................................................................................................................*/
	String translateGeneName(String originalGeneName) {
		for (int i=0;i<originalGeneNames.length; i++) {
			if (originalGeneName.toLowerCase().indexOf(originalGeneNames[i].toLowerCase())>=0){
				currentFragmentName= fragmentNames[i];
				return standardizedGeneNames[i];
			}
		}
		return originalGeneName;
	}
	/*.................................................................................................................*/
	String getGeneInfo(Element featureTableElement, boolean useTranslationTable) {
		String geneName = "";
		currentFragmentName="";
		List featureElements = featureTableElement.elements();
		for (Iterator iter = featureElements.iterator(); iter.hasNext();) {   // this is going through all of the notices
			Element featureElement = (Element) iter.next();
			Element gbFeatureQualElement = featureElement.element("GBFeature_quals");
			List qualifierElements = gbFeatureQualElement.elements();
			for (Iterator iter2 = qualifierElements.iterator(); iter2.hasNext();) {   // this is going through all of the notices
				Element qualifierElement = (Element) iter2.next();
				Element gbQualifierName = qualifierElement.element("GBQualifier_name");
				if ("product".equalsIgnoreCase(gbQualifierName.getText())) {
					Element gbQualifierValue = qualifierElement.element("GBQualifier_value");
					geneName = gbQualifierValue.getText();
					if (useTranslationTable && StringUtil.notEmpty(geneName)) {
						return translateGeneName(geneName);
					}
				}
			}
		}
		return geneName;
	}


	/*.................................................................................................................*/
	synchronized void processGenBankXML (String gbxml, MesquiteString accessionNumber, MesquiteString taxonName, MesquiteString voucherCode, MesquiteString sequence, MesquiteString geneName){
		Document doc = XMLUtil.getDocumentFromString("GBSet", gbxml);
		Element root = doc.getRootElement();
		Element gbSeqElement = root.element("GBSeq");
		Element accessionElement = gbSeqElement.element("GBSeq_primary-accession");
		String accession = accessionElement.getText();
		Element taxonNameElement = gbSeqElement.element("GBSeq_organism");
		String taxName = taxonNameElement.getText();
		Element sequenceElement = gbSeqElement.element("GBSeq_sequence");
		String seq = sequenceElement.getText();
		Element featureTableElement = gbSeqElement.element("GBSeq_feature-table");

		String voucherInfo = getVoucherInfo(featureTableElement);
		String voucherC = null;
		if (StringUtil.notEmpty(voucherInfo)) {
			if (lastTokenForVoucherCode)
				voucherC=StringUtil.getLastItem(voucherInfo, ":", " ");
			else
				voucherC = voucherInfo;
		}

		String geneN = getGeneInfo(featureTableElement, true);

		if (accessionNumber!=null)
			accessionNumber.setValue(accession);
		if (taxonName!=null)
			taxonName.setValue(taxName);
		if (voucherCode!=null && voucherC!=null)
			voucherCode.setValue(voucherC);
		if (sequence!=null)
			sequence.setValue(seq);
		if (geneName!=null)
			geneName.setValue(geneN);
	}

	/*.................................................................................................................*/
	/** Called to operate on the data in all cells.  Returns true if data altered*/
	public synchronized boolean fetchGenBank(){ 
		logln("\nFetching GenBank entries: "  + genBankNumbers);
		try {
			String directory = MesquiteFile.chooseDirectory("Choose directory into which files will be saved:");
			if (StringUtil.blank(directory))
				return false;
			if (!directory.endsWith(MesquiteFile.fileSeparator))
				directory+=MesquiteFile.fileSeparator;
			originalGeneNames = new String[]{"28S", "large subunit", "18S", "small subunit", "cytochrome oxidase", "COI", "Arginine", "carbomyl", "CAD", "wingless", "topoisomerase", "muscle", "MSP", "polymerase", "spectrin"};
			standardizedGeneNames = new String[]{"28S", "28S", "18S", "18S", "COI", "COI", "ArgK", "CAD", "CAD", "wg", "Topo", "MSP", "MSP", "Pol2", "AS"};
			fragmentNames = new String[]{"", "", "", "", "COIBC", "COIBC", "", "CAD4","CAD4", "", "", "", "","", ""};


			String[] accessionNumbers = StringUtil.delimitedTokensToStrings(genBankNumbers,',',true);
			for (int i=0; i<accessionNumbers.length; i++) 
				if (!StringUtil.blank(accessionNumbers[i])) 				
					logln ("Accession numbers " + accessionNumbers[i]);

			logln("Querying for IDs of entries.");
			String[] idList = NCBIUtil.getGenBankIDs(accessionNumbers, isDNA,  this, true);
			if (idList==null)
				return false;
			logln("\nIDs acquired.");
			/*for (int i=0; i<idList.length; i++) 
						if (!StringUtil.blank(idList[i])) 				
							logln ("To Fetch " + idList[i]);*/

			MesquiteString taxonName= new MesquiteString();
			MesquiteString voucherCode= new MesquiteString();
			MesquiteString sequence= new MesquiteString();
			MesquiteString geneName= new MesquiteString();
			MesquiteString accessionNumber= new MesquiteString();
			boolean sequencesFetched = false;
			logln("\nRequesting sequences.\n");
			StringBuffer report = new StringBuffer();
			String[] sequences = NCBIUtil.fetchGenBankSequenceStrings(idList,isDNA, this, true, "gb", "xml", report);
			if (sequences!=null && sequences.length>0) {
				for (int i=0; i<sequences.length; i++) {
					if (StringUtil.notEmpty(sequences[i])) {
						String fileName = sequences[i] ;
						String fileContent = "";
						taxonName.setValue("");
						voucherCode.setValue("");
						sequence.setValue("");
						geneName.setValue("");
						accessionNumber.setValue("");
						processGenBankXML(sequences[i],accessionNumber, taxonName, voucherCode, sequence, geneName);
						String filePath = directory;
						if (!voucherCode.isBlank()){
							filePath += "&v";
							filePath += voucherCodePrefix;
							filePath += voucherCode.getValue()+"_";
						}
						if (!geneName.isBlank())
							filePath += "&g"+geneName.getValue()+"_";
						if (StringUtil.notEmpty(currentFragmentName))
							filePath += "&f"+currentFragmentName+"_";
						if (StringUtil.notEmpty(publicationCode))
							filePath += "&p"+publicationCode+"_";
						if (!accessionNumber.isBlank())
							filePath += "&a"+accessionNumber.getValue();
						if (!taxonName.isBlank())
							filePath += "_&n"+taxonName.getValue();

						StringBuffer fileContents = new StringBuffer();
						if (saveAsFasta.getValue()) {
							fileContents.append(">");
							if (includeTaxonNameIntoFASTAFile.getValue()) 
								if (!taxonName.isBlank())
									fileContents.append(taxonName.getValue()+" ");
							if (includeGeneInfoIntoFASTAFile.getValue()) {
								if (!geneName.isBlank())
									fileContents.append(geneName.getValue()+ " ");
								if (StringUtil.notEmpty(currentFragmentName))
									fileContents.append(currentFragmentName+ " ");
							}
							if (includeVoucherCodeIntoFASTAFile.getValue()) 
								if (!voucherCode.isBlank())
									fileContents.append(voucherCode.getValue()+ " ");
							fileContents.append(StringUtil.lineEnding());
							if (!sequence.isBlank())
								fileContents.append(sequence.getValue());
							fileContents.append(StringUtil.lineEnding());
							MesquiteFile.putFileContents(filePath+".fas", fileContents.toString(), true);
						}
						if (saveAsXML.getValue()) {
							fileContents.append(sequences[i]);
							MesquiteFile.putFileContents(filePath+".xml", fileContents.toString(), true);
						}

						sequencesFetched=true;
					}
				}
			}


			log(report.toString());
			log("Fetching completed");
			return sequencesFetched;


		} catch ( Exception e ){
			// better warning
			return false;
		}
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Fetches GenBanks sequences and saves them in separate files.", null, commandName, "fetchGenBank")) {

			if (queryOptions()) {
				fetchGenBank();
			}
		}
		else
			return  super.doCommand(commandName, arguments, checker);
		return null;
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
	public String getNameForMenuItem() {
		return "Fetch GenBank Sequences...";
	}
	/*.................................................................................................................*/
	public String getName() {
		return "Fetch GenBank Sequences";
	}

	/*.................................................................................................................*/
	public String getExplanation() {
		return "Fetches GenBank nucleotide sequences given their GenBank accession numbers saves them to files.";
	}
}





