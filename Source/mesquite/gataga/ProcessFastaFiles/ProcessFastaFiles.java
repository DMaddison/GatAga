/* Mesquite GatAga source code.  Copyright 2012 David Maddison & Wayne Maddison
Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.gataga.ProcessFastaFiles; 


import mesquite.lib.*;
import mesquite.dmanager.lib.*;


/* ======================================================================== */
public class ProcessFastaFiles extends ProcessDataFilesLib { 
	
	/*.................................................................................................................*/
	private void append(StringBuffer sb, int numTabs, String content) {
		sb.append("\n");
		for (int i=0; i<numTabs; i++)
			sb.append("\t");
		sb.append(content+";");
	}
	/*.................................................................................................................*/
	public String appendToFile(boolean proteinCoding) {  
		long charID = System.currentTimeMillis();
		long taxID = charID+1;
		StringBuffer sb = new StringBuffer(1000);
		sb.append("\n\n");
		sb.append("\nBegin MESQUITE;");
		append(sb,1,"MESQUITESCRIPTVERSION 2");
		append(sb,1,"tell ProjectCoordinator");

		append(sb,1,"getWindow");
		append(sb,1,"tell It");
		append(sb,2,"setSize 1000 600");
		append(sb,1,"endTell");

		append(sb,1,"getEmployee #mesquite.minimal.ManageTaxa.ManageTaxa");
		append(sb,1,"tell It");
		append(sb,2,"setID 0 " +taxID + "");
		append(sb,1,"endTell");

		append(sb,1,"getEmployee #mesquite.charMatrices.ManageCharacters.ManageCharacters");
		append(sb,1,"tell It");
		append(sb,2,"setID 0 " +charID + "");
		append(sb,1,"endTell");

		append(sb,1,"getEmployee  #mesquite.minimal.ManageTaxa.ManageTaxa");
		append(sb,1,"tell It");
		append(sb,2,"showTaxa #" + taxID + " #mesquite.lists.TaxonList.TaxonList");
		append(sb,2,"tell It");
		append(sb,3,"setTaxa #" + taxID + "");
		append(sb,3,"getWindow");
		append(sb,3,"tell It");

		append(sb,3,"newAssistant  #mesquite.lists.NumForTaxaList.NumForTaxaList");

		append(sb,3,"tell It");
		append(sb,4, "suppress");
		append(sb,4, "setValueTask #mesquite.molec.NumberStopsInTaxon.NumberStopsInTaxon");
		append(sb,4,"tell It");
		append(sb,5,"getEmployee #mesquite.charMatrices.CharMatrixCoordIndep.CharMatrixCoordIndep");
		append(sb,5,"tell It");
		append(sb,6,"setCharacterSource #mesquite.charMatrices.StoredMatrices.StoredMatrices");
		append(sb,6,"tell It");
		append(sb,6,"setDataSet #"+ charID);
		append(sb,6,"endTell");
		append(sb,5,"endTell");
		append(sb,4,"endTell");
		append(sb,4, "desuppress");

		append(sb,3,"endTell");
		append(sb,3,"endTell");
		append(sb,2,"endTell");
		append(sb,1,"endTell");


		append(sb,1,"getEmployee  #mesquite.charMatrices.BasicDataWindowCoord.BasicDataWindowCoord");
		append(sb,1,"tell It");
		append(sb,2,"showDataWindow #" + charID + " #mesquite.charMatrices.BasicDataWindowMaker.BasicDataWindowMaker");

		append(sb,2,"tell It");
		append(sb,3,"getWindow");

		append(sb,3,"tell It");
		append(sb,4,"getTable");
		append(sb,4,"tell It");
		append(sb,5,"rowNamesWidth 250");
		append(sb,4,"endTell");
		if (proteinCoding)
			append(sb,4,"colorCells  #mesquite.charMatrices.ColorByAA.ColorByAA");
		else
			append(sb,4,"colorCells  #mesquite.charMatrices.ColorByState.ColorByState");
		append(sb,3,"endTell");
		append(sb,3,"showWindow");
		append(sb,3,"getWindow");
		append(sb,3,"tell It");
		append(sb,4,"forceAutosize");
		append(sb,3,"endTell");
		append(sb,2,"endTell");
		append(sb,1,"endTell");
		append(sb,1,"endTell");
		sb.append("\nend;\n");

		return sb.toString();
	}


	
	/*.................................................................................................................*/
	boolean proteinCoding = false;  //QUERY about this???

	protected void adjustFile(MesquiteFile fileToWrite){
		MesquiteFile.appendFileContents(fileToWrite.getPath(), appendToFile(proteinCoding), true);
	}
	/*.................................................................................................................*/
	protected String getImporterName(){
		return "#InterpretFastaDNA";
	}


	/*.................................................................................................................*/
	public String getName() {
		return "Process and Convert FASTA Files";
	}
	/*.................................................................................................................*/
	public String getNameForMenuItem() {
		return "Process and Convert FASTA Files...";
	}
	/*.................................................................................................................*/
	public boolean showCitation() {
		return false;
	}

	/*.................................................................................................................*/
	public String getExplanation() {
		return "Processes a folder of FASTA files and converts to NEXUS files.";
	}
}




