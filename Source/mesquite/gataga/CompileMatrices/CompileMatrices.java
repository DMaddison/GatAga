/* Mesquite source code.  Copyright 1997-2011 W. Maddison and D. Maddison.

Version 2.75, September 2011.
Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */
package mesquite.gataga.CompileMatrices; 

import java.awt.FileDialog;

import mesquite.lib.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.duties.*;

/* ======================================================================== */
public class CompileMatrices extends FileProcessor {
	String saveFile = null;
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
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

	/** if returns true, then requests to remain on even after alterFile is called.  Default is false*/
	public boolean pleaseLeaveMeOn(){
		return false;
	}
	/*.................................................................................................................*/
	/** Called to alter file. */
	public boolean processFile(MesquiteFile file){
		
		if (saveFile == null || okToInteractWithUser(CAN_PROCEED_ANYWAY, "Asking for file to save")){ //need to check if can proceed
			
			MesquiteFileDialog fdlg= new MesquiteFileDialog(containerOfModule(), "Output File for Matrices(s)", FileDialog.SAVE);
			fdlg.setBackground(ColorTheme.getInterfaceBackground());
			fdlg.setVisible(true);
			String fileName=fdlg.getFile();
			String directory=fdlg.getDirectory();
			// fdlg.dispose();
			if (StringUtil.blank(fileName) || StringUtil.blank(directory))
				return false;
			saveFile = MesquiteFile.composePath(directory, fileName);
		}
		if (saveFile == null)
			return false;
		Listable[] matrices = proj.getFileElements(CharacterData.class);	
   		if (matrices == null)
   			return false;
		for (int im = 0; im < matrices.length; im++){
			CharacterData data = (CharacterData)matrices[im];
   			if (data.getFile() == file){
   				
				MesquiteFile.appendFileContents(saveFile, "BEGIN CHARACTERS;" + StringUtil.lineEnding() + "\tTITLE " + ParseUtil.tokenize(file.getFileName()) + ";" + StringUtil.lineEnding() , true);
				MesquiteFile.appendFileContents(saveFile, "\tDIMENSIONS NCHAR= " + data.getNumChars() + " ;" + StringUtil.lineEnding() , true);
				MesquiteFile.appendFileContents(saveFile, "\tFORMAT DATATYPE = DNA GAP = - MISSING = ?;" + StringUtil.lineEnding() + "\tMATRIX" + StringUtil.lineEnding() , true);
				for (int it = 0; it < data.getNumTaxa(); it++){
					MesquiteFile.appendFileContents(saveFile, "\t" + ParseUtil.tokenize(data.getTaxa().getTaxonName(it)) + "\t" , true);
   					StringBuffer description = new StringBuffer();
   					for (int ic =0; ic<data.getNumChars(); ic++){
						data.statesIntoNEXUSStringBuffer(ic, it, description);
  					}
   					MesquiteFile.appendFileContents(saveFile, description.toString() + StringUtil.lineEnding(), true);
  				}
				MesquiteFile.appendFileContents(saveFile, "\t;" + StringUtil.lineEnding() + "END;" + StringUtil.lineEnding() + StringUtil.lineEnding() , true);
  			}
   		}
		return true;

	}
	/*
BEGIN CHARACTERS;
	TITLE  Character_Matrix;
	DIMENSIONS  NCHAR=20;
	FORMAT DATATYPE = STANDARD GAP = - MISSING = ? SYMBOLS = "  0 1 2 3";
	MATRIX
	SarindaCutleri.MRB193                  10102101111111010100
	SitticusPalustris.d030                 11012010011010100100
	ThiodinineIndetEcuador.MRB024          01100010121100211100
	FreyaDecorata.d211                     01111110000101010111
	TrydarssusCfNobilitatus.MRB270         00010010121100110101
;
END;
*/

	/*.................................................................................................................*/
	public String getName() {
		return "Compile Matrices into One File";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Puts matrices from this file into a partial NEXUS file." ;
	}

}


