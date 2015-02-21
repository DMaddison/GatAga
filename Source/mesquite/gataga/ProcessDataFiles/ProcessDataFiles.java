/* Mesquite GatAga source code.  Copyright 2012 David Maddison & Wayne Maddison
Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.gataga.ProcessDataFiles; 

import java.awt.Choice;
import java.io.*;
import java.util.Vector;

import org.apache.commons.lang.SystemUtils;

import mesquite.lib.*;
import mesquite.io.lib.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.characters.CodonPositionsSet;
import mesquite.lib.duties.*;
import mesquite.lib.system.SystemUtil;
import mesquite.lists.lib.ListModule;
import mesquite.basic.ManageSetsBlock.ManageSetsBlock;
import mesquite.categ.lib.*;
import mesquite.charMatrices.ManageCodonsBlock.ManageCodonsBlock;
import mesquite.gataga.lib.*;


/* ======================================================================== */
public class ProcessDataFiles extends ProcessDataFilesLib { 
	/*.................................................................................................................*/

	/*.................................................................................................................*/
	public String getName() {
		return "Process Data Files";
	}
	/*.................................................................................................................*/
	public String getNameForMenuItem() {
		return "Process Data Files...";
	}
	/*.................................................................................................................*/
	public boolean showCitation() {
		return false;
	}

	/*.................................................................................................................*/
	public String getExplanation() {
		return "Processes a folder of data files.";
	}
}




