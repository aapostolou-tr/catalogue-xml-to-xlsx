package xml_to_excel;

import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import sheet_converter.AttributeSheetConverter;
import sheet_converter.CatalogueSheetConverter;
import sheet_converter.HierarchySheetConverter;
import sheet_converter.NotesSheetConverter;
import sheet_converter.SheetConverter;
import sheet_converter.TermSheetConverter;


/**
 * Convert a catalogue from XML format to excel format. The excel contains 5 sheets:
 * - catalogue
 * - hierarchy
 * - attribute
 * - term
 * - releaseNotes
 * @author avonva
 *
 */
public class XmlCatalogueToExcel {
	
	public static final String WORKING_DIR = "xslt\\";

	// the name of the xslt which filter only the catalogue information from the input xml
	public static final String CATALOGUE_XSLT_NAME = "catalogue.xslt";

	// the name of the xslt which filter only the hierarchy information from the input xml
	public static final String HIERARCHY_XSLT_NAME = "hierarchy.xslt";
	
	// the name of the xslt which filter only the attribute information from the input xml
	public static final String ATTRIBUTE_XSLT_NAME = "attribute.xslt";
		
	// the name of the xslt which filter only the term information from the input xml
	public static final String TERM_XSLT_NAME = "term.xslt";
	
	// the name of the xslt which filter only the release notes information from the input xml
	public static final String NOTES_XSLT_NAME = "releaseNotes.xslt";

	
	// the name of the sheet which will contain the catalogue data
	public static final String CATALOGUE_SHEET_NAME = "catalogue";

	// the name of the sheet which will contain the hierarchy data
	public static final String HIERARCHY_SHEET_NAME = "hierarchy";

	// the name of the sheet which will contain the attribute data
	public static final String ATTRIBUTE_SHEET_NAME = "attribute";

	// the name of the sheet which will contain the term data
	public static final String TERM_SHEET_NAME = "term";
	
	// the name of the sheet which will contain the release notes data
	public static final String NOTES_SHEET_NAME = "releaseNotes";

	
	// the name of the main node for the catalogue node
	public static final String CATALOGUE_ROOT_NODE = "message";

	// the name of the main node for hierarchies
	public static final String HIERARCHY_ROOT_NODE = "hierarchy";

	// the name of the main node for attributes
	public static final String ATTRIBUTE_ROOT_NODE = "attribute";
	
	// the name of the main node for terms
	public static final String TERM_ROOT_NODE = "term";
	
	// the name of the main node for release notes
	public static final String NOTES_ROOT_NODE = NotesSheetConverter.OP_INFO_NODE;
	
	
	// the xml file which has to be converted
	private String inputXml;
	
	// the xlsx file which has to be created
	private String outputXlsx;
	
	/**
	 * Start the converter from command line
	 * @param args
	 */
	public static void main ( String[] args ) {
		
		System.out.println ( "#### Remember to increase the RAM max limit if you are converting big catalogues! (e.g. -Xms1024m) ####" );
		
		if ( args.length != 2 ) {
			
			System.err.println ( "Wrong number of arguments. Please specify the input catalogue xml and the output xlsx file path "
					+ "(example: java -jar xmlToExcel.jar D:\\catalogue.xml D:\\output.xlsx)" );
			
			return;
		}
		
		// convert the xml to excel
		XmlCatalogueToExcel converter = new XmlCatalogueToExcel( args[0], args[1] );
		try {
			converter.convertXmlToExcel ();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Constructor
	 * Convert the catalogue from the .XML format to the .xlsx format (excel workbook)
	 * The input parameter is the xml file to be converted
	 * @parm XmlFilename, the file to be converted
	 */
	public XmlCatalogueToExcel( String inputXml, String outputXlsx ) {
		this.inputXml = inputXml;
		this.outputXlsx = outputXlsx;
	}
	
	
	/**
	 * Convert the xml catalogue file into an excel file with 4 sheet
	 * @throws TransformerException
	 */
	public void convertXmlToExcel () throws TransformerException {

		// create a new workbook
		SXSSFWorkbook workbook = new SXSSFWorkbook();
		
		// convert catalogue sheet
		ConversionPerformer cat = new ConversionPerformer( workbook, 
				inputXml, CATALOGUE_XSLT_NAME ) {
			
			@Override
			public SheetConverter getConverter( String filename ) {
				// create a catalogue sheet converter to parse the xml file
				CatalogueSheetConverter catConverter = new CatalogueSheetConverter( 
						filename, CATALOGUE_ROOT_NODE );
				return catConverter;
			}
		};

		cat.convert( CATALOGUE_SHEET_NAME );
		
		
		// convert hierarchy sheet
		ConversionPerformer hier = new ConversionPerformer( workbook, 
				inputXml, HIERARCHY_XSLT_NAME ) {
			
			@Override
			public SheetConverter getConverter(String inputFilename) {
				// create a hierarchy sheet converter to parse the xml file
				HierarchySheetConverter hierarchyConverter = new HierarchySheetConverter( 
						inputFilename, HIERARCHY_ROOT_NODE );
				return hierarchyConverter;
			}
		};
		
		hier.convert( HIERARCHY_SHEET_NAME );
		
		
		// convert attr sheet
		ConversionPerformer attr = new ConversionPerformer( workbook, 
				inputXml, ATTRIBUTE_XSLT_NAME ) {
			
			@Override
			public SheetConverter getConverter(String inputFilename) {
				
				// create a attribute sheet converter to parse the xml file
				AttributeSheetConverter attrConverter = new AttributeSheetConverter( 
						inputFilename, ATTRIBUTE_ROOT_NODE );
				
				return attrConverter;
			}
		};
		
		attr.convert( ATTRIBUTE_SHEET_NAME );
		
		
		// convert term sheet
		ConversionPerformer term = new ConversionPerformer( workbook, 
				inputXml, TERM_XSLT_NAME ) {
			
			@Override
			public SheetConverter getConverter(String inputFilename) {
				
				// create a term converter, we need the hierarchy and attribute sheet to
				// create the term sheet
				TermSheetConverter termConverter = new TermSheetConverter( 
						inputFilename, TERM_ROOT_NODE, 
						hier.getSheet(), attr.getSheet() );
				
				// set as master hierarchy code the catalogue code
				termConverter.setMasterHierarchyCode( 
						SheetConverter.getSheetColumn( 
								cat.getSheet(), "code" ).get(0) );
				
				return termConverter;
			}
		};
		
		term.convert( TERM_SHEET_NAME );

		
		// create release notes sheet
		ConversionPerformer notes = new ConversionPerformer( workbook, inputXml, NOTES_XSLT_NAME ) {
			
			@Override
			public SheetConverter getConverter(String inputFilename) {
				NotesSheetConverter notesConverter = new NotesSheetConverter( 
						inputFilename, NOTES_ROOT_NODE );
				
				return notesConverter;
			}
		};
		
		notes.convert( NOTES_SHEET_NAME );

		// create a term converter, we need the hierarchy and attribute sheet to
		// create the term sheet

		
		// filter the input xml to get only the data related to the catalogue
		/*String outputFilename = filterXml( workbook, CATALOGUE_XSLT_NAME, CATALOGUE_SHEET_NAME );
		
		// create a catalogue sheet converter to parse the xml file
		CatalogueSheetConverter catConverter = new CatalogueSheetConverter( outputFilename, CATALOGUE_ROOT_NODE );
		
		// create the empty catalogue sheet
		Sheet catSheet = catConverter.buildSheet( workbook, CATALOGUE_SHEET_NAME );
		
		// parse the xml and insert the data
		catConverter.parse();
		
		// delete the temp file
		deleteFile( outputFilename );*/
		
		/*
		System.out.println ( "Creating hierarchy sheet..." );
		
		// filter the xml to get only the data related to the hierarchies
		outputFilename = filterXml( workbook, HIERARCHY_XSLT_NAME, HIERARCHY_SHEET_NAME );

		// create a hierarchy sheet converter to parse the xml file
		HierarchySheetConverter hierarchyConverter = new HierarchySheetConverter( outputFilename, HIERARCHY_ROOT_NODE );
		
		// create the hierarchy sheet
		Sheet hierarchySheet = hierarchyConverter.buildSheet( workbook, HIERARCHY_SHEET_NAME );
		
		// !!! => important <= !!! add the master hierarchy first!
		hierarchyConverter.addMasterHierarchy ( catSheet );
		
		// then populate the hierarchy sheet with other hierarchies
		hierarchyConverter.parse();
		
		// delete the temp file
		deleteFile( outputFilename );

*/
		/*
		System.out.println ( "Creating attribute sheet..." );
		
		// filter the xml to get only the data related to the attributes
		outputFilename = filterXml( workbook, ATTRIBUTE_XSLT_NAME, ATTRIBUTE_SHEET_NAME );

		// create a attribute sheet converter to parse the xml file
		AttributeSheetConverter attrConverter = new AttributeSheetConverter( outputFilename, ATTRIBUTE_ROOT_NODE );

		// create the attribute sheet
		Sheet attrSheet = attrConverter.buildSheet( workbook, ATTRIBUTE_SHEET_NAME );

		// then populate the attribute sheet
		attrConverter.parse();

		// delete the temp file
		deleteFile( outputFilename );*/
		
/*
		System.out.println ( "Creating term sheet..." );
		
		// filter the xml to get only the data related to the terms
		outputFilename = filterXml( workbook, TERM_XSLT_NAME, TERM_SHEET_NAME );

		// create a term converter, we need the hierarchy and attribute sheet to
		// create the term sheet
		TermSheetConverter termConverter = new TermSheetConverter( outputFilename, TERM_ROOT_NODE, 
				hierarchySheet, attrSheet );
		
		// set as master hierarchy code the catalogue code
		termConverter.setMasterHierarchyCode( SheetConverter.getSheetColumn( catSheet, "code" ).get(0) );
		
		// create the sheet and input its data
		termConverter.buildSheet( workbook, TERM_SHEET_NAME );
		termConverter.parse();
		
		// delete the temp file
		deleteFile( outputFilename );
		
		System.out.println ( "Creating release note sheet..." );
		
		// filter the xml to get only the data related to the release notes
		outputFilename = filterXml( workbook, NOTES_XSLT_NAME, NOTES_SHEET_NAME );

		// create a term converter, we need the hierarchy and attribute sheet to
		// create the term sheet
		NotesSheetConverter notesConverter = new NotesSheetConverter( outputFilename, NOTES_ROOT_NODE );

		// create the sheet and input its data
		notesConverter.buildSheet( workbook, NOTES_SHEET_NAME );
		notesConverter.parse();
		
		// delete the temp file
		deleteFile( outputFilename );
		*/
		
		System.out.println ( "Writing the excel file..." );
		
		// save the results into the excel file
		FileOutputStream fileOut = null;
		try {

			fileOut = new FileOutputStream( outputXlsx );
			
			// remove limits of dimensions for the workbook
			ZipSecureFile.setMinInflateRatio( 0 );
			
			workbook.write( fileOut );
			System.out.println ( "Done" );

		} catch ( IOException e) {
			e.printStackTrace();
			System.out.println ( "Failed" );
		}
		finally {
		
			// close resources
			try {
				fileOut.flush();
				fileOut.close();
				workbook.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
