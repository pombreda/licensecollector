package org.fluidops.ant.task;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;


/**
 * 
 * <b>Introduction</b><p>
 * 
 * Many programming projects rely on third party libraries or modules. When embedding those
 * open source libraries into your product, a proper release documentation must be provided 
 * in which you document that the particular requirements of the licences are met. <p>
 * 
 * In a larger team you can easily loose track of 3rd party libraries that are used in your
 * project and it is hard keep the release documentation up to date. Hence, we developed 
 * a little tool which does this work for us: <p>
 * 
 * License Collector is an ANT task that generates a html file in which all integrated 
 * 3rd party libraries are listed with their license specification and acknowlegdements.
 * The only thing the developer needs to think of is creating an information sheet when 
 * integrating/commiting libraries. Then this small tool will render a nice Third Party 
 * License Term List, and your legal counsel will be happy. <p>
 * 
 * <br>
 * <b>Usage</b><p>
 * 
 * Introduce the task into your antfile <p>
 * 
 * <code>
 * &lt;taskdef name="lcollector" classname="org.fluidops.ant.task.LicenseCollectorTask" classpath="lcollector.jar"/&gt;
 * </code><p>
 * 
 * Then run your task with<p>
 * 
 * <code>
 * &lt;lcollector   
 * 		libraryFolder="${root}/lib"
 * 		licenseFolder="${root}/licenses"
 * 		htmlTemplate="${root}/licenses/mytemplate.html"
 * 		outputFile="${root}/3rdparty.html"
 * /&gt;
 * </code><p>
 * 
 * The parameters are as follows: <p>
 * <ul>
 * <li>libraryFolder: root folder of libraries, start for recursive search for .lic files</li>
 * <li>licenseFolder: folder in which the plain license files are located</li>
 * <li>outputFile: the file path of the rendered html file</li>
 * <li>htmlTemplate: optional, a customized html template</li>
 * </ul>
 * 
 * A .lic file looks like this (property file with key-value pairs):<p>
 * 
 * <pre>
 * #An example license
 * Date=2004-2009
 * License_Type=Apache Software License 2.0
 * Licensor=The Apache Foundation
 * Description=Apache Ant is a Java-based build tool. In theory, it is kind of like make, without make's wrinkles.
 * Library=apache-ant
 * Acknowledgements=see Notice.txt
 * </pre>
 * 
 * The License_Type denotes the type of the license and in the rendering process the
 * tool looks for a file called <code>%License_Type%.license</code> in the specified licenseFolder
 * or its subfolders. It is recommended as good practice to put the '.lic' files in
 * the same folder where the actual library is located.<p>
 * 
 * To render the third party license list, this tool uses a html template, which can
 * be specified using the ant-parameter <code>html_template</code> (optional; if not specified
 * a default file will be used). This file contains the basic layout as well as style 
 * information and it will be used during the rendering process. Each template file should 
 * have a <i>index</i> and a <i>content</i> placeholder, denoted by <b>%index%</b> and 
 * <b>%content</b>, respectively. These placeholders will be replaced with the actual content.
 * 
 * 
 * @author as
 * @author vc
 * 
 * @version 1.0
 * @date February 10th, 2009
 */
public class LicenseCollectorTask extends Task  
{
	/* Configuration */
	public static String LICENSOR = "Licensor";
	public static String LICENSE_TYPE = "License_Type";
	public static String DESCRIPTION = "Description";
	public static String DATE = "Date";
	public static String ACKNOWLEDGEMENTS = "Acknowledgements";
	public static String LIBRARY = "Library";
	
	/* class variables */
	private HashMap<String, ArrayList<LicenseInfo>> licenseMap = new HashMap<String, ArrayList<LicenseInfo>>();
	
	private String libraryFolderPath = null;		// defines where the tool looks for lic files
	private String licenseFolderPath = null;		// defines where the plain licenses are located
    private String htmlTemplate = "/org/fluidops/ant/task/license_template.html";  // defines where the html template is located
	private String outFile = null;					// defines where the rendered output shall be located
	
	private PrintWriter out = null;					// print stream for html
	private StringBuffer content_buffer = null;		// stringbuffer that contains content
	private StringBuffer index_buffer = null;		// stringbuffer that contains the index
	
	
	
	/**
	 * Constructor for the ant job executor
	 */
	public LicenseCollectorTask() 
	{		
	}
	
	/**
	 * Constructor for manual setup
	 * 
	 * @param libraryFolderPath 	root folder of libraries, start for recursive search for .lic files
	 * @param licenseFolderPath 	folder in which the plain license files are located
	 * @param outFile	the file path of the rendered html file (extension should be htm/html) 
	 */
	public LicenseCollectorTask(String libraryFolderPath, String licenseFolderPath, String outFile) 
	{
		this.libraryFolderPath = libraryFolderPath;
		this.licenseFolderPath = licenseFolderPath;
		this.outFile = outFile;
	}

	/**
	 * Structuring Class keeping information for each license
	 * @author as
	 *
	 */
	private class LicenseInfo 
	{
		public String licensor = "";
		public String license_type = "";
		public String description = "";
		public String date = "";
		public String acknowledgements = "";
		public String library = "";
		
		/**
		 * Constructor for setup
		 * @param licensor
		 * @param license_type
		 * @param description
		 * @param date
		 * @param acknowledgements
		 * @param library
		 */
		public LicenseInfo(String licensor, String license_type, String description, String date, String acknowledgements, String library) 
		{
			this.licensor = licensor;
			this.license_type = license_type;
			this.description = description;
			this.date = date;
			this.acknowledgements = acknowledgements;
			this.library = library;
		}
		
		/**
		 * Returns a String representation of the instance
		 */
		@Override
		public String toString() 
		{
			return license_type + " [" + licensor + ", " + description + ", " + date + ", " + acknowledgements + "]: " + library;
		}
		
		
		/**
		 * check if instance equals object
		 * @param obj check if object equals
		 */
		@Override
		public boolean equals( Object obj) {
			if (obj instanceof LicenseInfo) {
				LicenseInfo l = (LicenseInfo)obj;
				return acknowledgements.equals(l.acknowledgements) && date.equals(l.date) && description.equals(l.description)
							&& library.equals(l.library) && license_type.equals(l.license_type) && licensor.equals(l.licensor);
			}
			
			return false;
		}
	}
	
	
	
	/**
	 * entry point for generation after construction
	 * @throws Exception
	 */
	public void run() throws Exception 
	{
		log("License HTML generator", Project.MSG_INFO);
		if (this.libraryFolderPath==null || this.licenseFolderPath==null || this.outFile==null)
			throw new Exception("Library, License and Outfile parameters must be specified.");
        log("License root folder is set to " + libraryFolderPath, Project.MSG_VERBOSE);
		
		// retrieve the license files (recursive file search)
		ArrayList<File> licenseFiles = getLicenseFiles();
		log("Found " + licenseFiles.size() + " license information files.", Project.MSG_VERBOSE);
		
		// handle the input
		for (File f : licenseFiles)
			handleLicenseFile( f );			
		
		// prepare the output stream and content
		out = new PrintWriter(new BufferedWriter(new FileWriter(this.outFile)));
		content_buffer = new StringBuffer();
		index_buffer = new StringBuffer();
		
		// work with the input
		Set<String> licenseTypes = new TreeSet<String>();
		licenseTypes.addAll( licenseMap.keySet() );
		for (String licenseType : licenseTypes)
			renderLicenseType( licenseType, licenseMap.get( licenseType));
				
		// render content by using template
		readAndRenderContent();
		
		out.flush();
		out.close();
        log("HTML License File created in " + outFile, Project.MSG_INFO);
	}
	
	
	
	/**
	 * entry point for ant task
	 */
	public void execute() throws BuildException {
		try 
		{
			run();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			throw new BuildException("error", e);
		}
	}
	
	/**
	 * Handle a found license file -> add it to the license List
	 * @param f
	 * @throws Exception
	 */
	private void handleLicenseFile( File f) throws Exception 
	{
		log("Handling file: " + f.getAbsolutePath(), Project.MSG_VERBOSE );
		Properties props = new Properties();
		props.load( new FileInputStream(f));
		
		String licenseType = props.getProperty( LICENSE_TYPE);
		
		ArrayList<LicenseInfo> licenseList = licenseMap.get( licenseType );
		if ( licenseList==null ) 
		{
			licenseList = new ArrayList<LicenseInfo>();
			licenseMap.put(licenseType, licenseList);
		}
		
		// check for duplicates, if not -> add it to list
		LicenseInfo li = new LicenseInfo(props.getProperty(LICENSOR), props.getProperty(LICENSE_TYPE),props.getProperty(DESCRIPTION),props.getProperty(DATE),props.getProperty(ACKNOWLEDGEMENTS),props.getProperty(LIBRARY));
		if (!licenseList.contains(li))
			licenseList.add(li);
	}
		
	/**
	 * Render a license type to html code and attach it to the content-string buffer
	 * @param licenseType
	 * @param licenseInfos
	 * @throws Exception
	 */
	private void renderLicenseType( String licenseType, ArrayList<LicenseInfo> licenseInfos) throws Exception 
	{	
		index_buffer.append("<li><a href=\"#" + licenseType.replace(" ", "_") + "\">" + licenseType + "</a></li>");
		content_buffer.append("<p /><a name=\"" + licenseType.replace(" ", "_") + "\"></a><h2>" + licenseType + "</h2><p />");
		
		// try to render the license as pre text
		{
		    BufferedReader lr = null;
		    
    		File license = new File( this.licenseFolderPath + "/" + licenseType + ".license");
    		if (!license.exists()) 
    		{
    		    //try if already delivered in jar
    	        InputStream in = getClass().getResourceAsStream("/org/fluidops/ant/task/" + licenseType + ".license");
    	        if(in!=null)
    	            lr = new BufferedReader( new InputStreamReader(in));
    		}
    		else
    		    lr = new BufferedReader( new FileReader(license));
    		
    		if(lr==null)
    		{
    		    content_buffer.append("<p><strong>No license document attached for '" + licenseType + "'</strong></p>");
    		    log("No license document attached for " + licenseType, Project.MSG_ERR );
    		}
    		else 
    		{
    			content_buffer.append("<pre>");
    			String line = lr.readLine();
    			while (line!=null) 
    			{
    				content_buffer.append( line + '\n' );
    				line = lr.readLine();
    			}
    			content_buffer.append("</pre>");
    			lr.close();
    		}
		}
		
		content_buffer.append("<br /><span style=\"font-size:0.86em; font-weight: bold;\">Copyright Notice and Attribution Chart:</span><br /><br />");
		content_buffer.append("<table width=\"600px\" border=\"1\" class=\"liTable\" style=\"border:1px solid black; border-collapse:collapse;\"><thead>");
		content_buffer.append("<tr><th width=\"5%\">No</th><th width=\"20%\">Licensor</th><th width=\"15%\">Library</th><th width=\"30%\">Description</th><th width=\"10%\">Date(s)</th><th width=\"20%\">Acknowledgements</th></tr>");
		content_buffer.append("</thead><tbody>");
		int count=0;
		for(LicenseInfo info : licenseInfos) 
		{
			log( info.toString(), Project.MSG_VERBOSE );
			String template = "<tr><td>" + Integer.toString(++count) + "</td><td>" + info.licensor + "</td><td>" + info.library + "</td><td>" + info.description + "</td><td>" + info.date + "</td><td>"+ info.acknowledgements +"</td></tr>";
			content_buffer.append( template + '\n' );
		}
		content_buffer.append("</tbody></table>&#xBB; <a href=\"#top\" style=\"font-size:0.76em;s\">top</a><p>&nbsp;</p>");
	}
	
	
	/**
	 * Function to render the content
	 * @throws IOException
	 */
	private void readAndRenderContent() throws IOException, BuildException
	{
        BufferedReader bin = null;	
		InputStream in = getClass().getResourceAsStream(htmlTemplate);
		if(in==null)
            try
            {
                bin = new BufferedReader(new FileReader(htmlTemplate));
            }
            catch (Exception e)
            {
                throw new BuildException("HTML Template can't be found:" + htmlTemplate, e);
            }
        else
		    bin = new BufferedReader( new InputStreamReader(in));
		
		String line = bin.readLine();
		while (line!=null) 
		{			
			// take care that %content% is printed at that position
			if (line.contains("%content%")) 
			{
				out.write( line.substring(0, line.indexOf("%content%")));
				
				out.write( content_buffer.toString() );
				
				out.write( line.substring(line.indexOf("%content%")+9 ));
				out.write( '\n');
			} 
			
			// take care that %index% is printed at that position
			else if (line.contains("%index%")) 
			{
					out.write( line.substring(0, line.indexOf("%index%")));
					
					out.write( index_buffer.toString() );
					
					out.write( line.substring(line.indexOf("%index%")+7 ));
					out.write( '\n');
				} 
			
			// else print the whole line
			else 
			{
				out.write( line + '\n');
			}
	
			line=bin.readLine();
		}
	}
	
	
	@SuppressWarnings("unused")
	private void writeLicenseFile() throws Exception 
	{
		 Properties props = new Properties();
		 
		 props.setProperty(LICENSOR, "Apache Foundation");
		 props.setProperty(LICENSE_TYPE, "Apache License 2.0");
		 props.setProperty(DATE, "2009");
		 props.setProperty(DESCRIPTION, "This is a short description");
		 props.setProperty(ACKNOWLEDGEMENTS, "acks");
		 props.setProperty(LIBRARY, "library");
	
		 props.store( new FileOutputStream( new File(libraryFolderPath + "/myExampleLicense.lic")), "An example license");
	}
		
	
	/**
	 * find all licensefiles, makes use of recursiveFileScan()
	 * @return
	 */
	private ArrayList<File> getLicenseFiles() 
	{
		ArrayList<File> result = new ArrayList<File>();
		recursiveFileScan( new File(libraryFolderPath), result);
		return result;
	}
	
	
	/**
	 * recursively scans the specified directory for .lic files
	 * @param folder
	 * @param result
	 */
	private static void recursiveFileScan(File folder, ArrayList<File> result) 
	{		
		File[] files = folder.listFiles();
		if (files != null) 
		{
			for (int i=0; i<files.length; i++) 
			{
				if (files[i].isDirectory())
					recursiveFileScan( files[i], result);
				else if (files[i].getName().endsWith("lic"))
					result.add( files[i]);
			}
		}
	}

	/** 
	 * Set the root folder in which the tool looks for .lic files
	 * @param libraryFolderPath
	 */
	public void setLibraryFolder( String libraryFolderPath) 
	{
		this.libraryFolderPath = libraryFolderPath;
	}
	
	/**
	 * Set the output file for the rendered license document, extension should be htm or html
	 * @param outputFile
	 */
	public void setOutputFile( String outputFile ) 
	{
		this.outFile = outputFile;
	}

	/**
	 * Set the folder in which to look for the licenses
	 * @param licenseFolderPath
	 */
	public void setLicenseFolder( String licenseFolderPath ) 
	{
		this.licenseFolderPath = licenseFolderPath;
	}

    /**
     * Set the html template as base render
     * @param htmlTemplate
     */
    public void setHtmlTemplate(String htmlTemplate)
    {
        this.htmlTemplate = htmlTemplate;
    }

    /**
     * private test
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception 
    {
        new LicenseCollectorTask("./lib","./licenses","./licenses.htm").run();
    }
}
