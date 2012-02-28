/**
 * you can put a one sentence description of your tool here.
 *
 * (c) 2011
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 * @author		Dave Vondle http://labs.ideo.com
 * @modified	02/27/2012
 * @version		0.1
 */

 package template.tool;
 
import java.awt.SystemColor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.GistFile;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.GistService;

import processing.app.*;
import processing.app.debug.Target;
import processing.app.tools.*;
import processing.core.PApplet;
 
 
 public class GistUploader implements Tool {
 
 // when creating a tool, the name of the main class which implements Tool
 // must be the same as the value defined for project.name in your build.properties
 
	Editor editor; 
	
	static Hashtable defaults;
	static Hashtable table = new Hashtable();
	static final String GIST_FILE = "gistCredentials.txt";
	static File gistCredFile;
	
	
	
 
	public String getMenuTitle() {
		return "Upload Source to Github";
	}
	 

	public void init(Editor editor) {
		this.editor=editor;
		
	    // clone the hash table
		loadDefaults();
	    defaults = (Hashtable) table.clone();

	      // next load user preferences file
	    gistCredFile = Base.getSettingsFile(GIST_FILE);
	      if (!gistCredFile.exists()) {
	        // create a new preferences file if none exists
	        // saves the defaults out to the file
	        save();

	      } else {
	        // load the previous preferences file

	        try {
	          load(new FileInputStream(gistCredFile));

	        } catch (Exception ex) {
	          Base.showError("Error reading github credentials",
	                         "Error reading github credentials. " +
	                         "Please delete (or move)\n" +
	                         gistCredFile.getAbsolutePath() +
	                         " and restart Arduino.", ex);
	        }
	      }  
	}
	 
	public void run() {
		try {
            int timeout = 2000;
            InetAddress address = InetAddress.getByName("api.github.com");
            if (address.isReachable(timeout)){
            	getSerialNumberAndSend();
            }else{
              System.out.println("github service is unavailable, source will not be sent.");
              System.out.println("Make sure to save locally!");
            }
        }catch (Exception e) {
            System.out.println("You are not connected to the internet, source will not be sent.");
            System.out.println("Make sure to save locally!");
            System.out.println(e);
        }
    }
 

 	private void getSerialNumberAndSend() { 
		 String serialNumber=findSerialNumber();
		 if (!serialNumber.isEmpty()){
		   uploadToGitHub(serialNumber);
		 }else{
		   System.out.println("Could not find your board, make sure it's plugged into USB.");
		 }
 	}
 	
	private void uploadToGitHub(String serialNumber) { 
			String username="";
			String password="";
			Boolean personalCredentialsFound = false;
			
			SketchCode[] theSketches;
			String[] theSketchesContent;
			
			GitHubClient client;
		    GistService service;
		    GistFile[] file;
		    String[] filename;
		    Gist gist = new Gist();
		    Map<String,GistFile> mp=new HashMap<String, GistFile>();
			
			theSketches=editor.getSketch().getCode();
			theSketchesContent = new String[theSketches.length];
			file=new GistFile[theSketches.length];
			filename = new String[theSketches.length];
			int indexOfUsername=-1;
			boolean makePrivate=false;
			
			for(int j =0; j<theSketches.length; j++){
				file[j]=new GistFile();
				if(theSketches[j]==editor.getSketch().getCurrentCode()){//so you don't need to save
					theSketchesContent[j]=editor.getText();
				}else{
					theSketchesContent[j]=theSketches[j].getProgram();
				}
	        	if(theSketches[j].getExtension()=="ino" || theSketches[j].getExtension()=="pde"){
	        		indexOfUsername=theSketchesContent[j].indexOf("USE_GITHUB_USERNAME=");
	        		makePrivate=theSketchesContent[j].contains("MAKE_PRIVATE_ON_GITHUB");
	        		
	        		if(!personalCredentialsFound)
		        		if(indexOfUsername!=-1){//if the user specifies a username in the code comments
		        			personalCredentialsFound=true;
		    				username=theSketchesContent[j].substring(indexOfUsername+20);
		    			    username=username.substring(0,username.indexOf('\n')).trim();
		    			    password=(String) table.get(username);
		    			}else{ //defaults to first entry in gistCredentials.txt
		    				Enumeration e = table.keys();
		    				String key="";
		    				while (e.hasMoreElements()) {//i'm a hack to get to the first entry
		    	 		      key = (String) e.nextElement();
		    	 		    }
		    	 		   username=key;
		    	 		   password=(String) table.get(username);
		    			}
	        	}
			}
			System.out.println("Sending source to "+username+"'s github account...");
		
			client = new GitHubClient().setCredentials(username, password);
		    service = new GistService(client);
	      
	      try{
	        List<Gist> gists = service.getGists(username);
	        Boolean foundMatchingGist=false;
	        for (int i = gists.size(); --i >= 0;){  //backwards so the first one found is the oldest one
	        	gist = (Gist)gists.get(i);
	          
	        	if(gist.getDescription().toUpperCase().contains(serialNumber.toUpperCase())){ //found the last matching gist. toUpperCase is because Windows capitalizes the letters I hope this isn't a problem!
		            if(foundMatchingGist==true){ //if one has already been found then an extra was made in error and needs to be cleaned up
		            	//delete the spurious gist
		              service.deleteGist(gist.getId());
		            }else if(gist.isPublic()==!makePrivate){// rewrite the gist if there is already one and the privacy settings are the same
		            	for (String key : gist.getFiles().keySet()) {// "delete" old ones, if this wasn't here every time you saved a sketch with a new name it would not overwrite the old sketch
				            boolean matchingFile=false;
			            	for(int j =0; j<theSketches.length; j++){
			            		if((theSketches[j].getPrettyName()+"."+theSketches[j].getExtension()).contains(gist.getFiles().get(key).getFilename())){
			            			matchingFile=true;
			            		}
			            	}
			            	if(!matchingFile){
			            	service.updateGist(gist.setFiles(Collections.singletonMap(key, new GistFile())));
			            	}
				        }
		            	for(int j =0; j<theSketches.length; j++){
				        	file[j].setContent(theSketchesContent[j]);
				            filename[j] = theSketches[j].getPrettyName()+"."+theSketches[j].getExtension();
				            mp.put(filename[j], file[j]);
						}
		            	gist.setFiles(mp);
	
		            	service.updateGist(gist);
		            	System.out.println(new String("You can find the source online at: " + gist.getHtmlUrl()));
		            	foundMatchingGist=true;
		            }else{ //if the privacy settings have changed, delete the file and make a new one
		            	service.deleteGist(gist.getId());
		            	foundMatchingGist=false;
		            }
	        	}
	        }
	        if(foundMatchingGist==false){ //if no gist exists for the board
	        	gist = new Gist().setDescription(new String("The file that is currently on an "+ getCurrentBoard() + " with a serial number of "+ serialNumber));
		        gist.setPublic(!makePrivate);                  //user can make it private by entering MAKE_PRIVATE_ON_GITHUB in the comments
	        	for(int j =0; j<theSketches.length; j++){
	        		file[j].setContent(theSketchesContent[j]);
	        		filename[j] = theSketches[j].getPrettyName()+"."+theSketches[j].getExtension();
	        		mp.put(filename[j], file[j]);
	        	}
	        	gist.setFiles(mp);
	        	gist = service.createGist(gist);
		        System.out.println(new String("You can find the source online at: " + gist.getHtmlUrl()));
	        }
	      }catch(Exception e){
	        System.out.println("Failed. Login credentials incorrect, please correct gistCredentials.txt");
	        System.out.println(e);
	      }
	}
	
	public String getCurrentBoard() {
	    //System.out.println("rebuilding boards menu");
	    for (Target target : Base.targetsTable.values()) {
	      for (String board : target.getBoards().keySet()) {
	        if (target.getName().equals(Preferences.get("target")) &&
	            board.equals(Preferences.get("board"))) {
	          return target.getBoards().get(board).get("name");
	        }
	      }
	    }
	    return "";
	}
	  
 
	public String findSerialNumber() {
	    if (Base.isMacOS()) {
	      String getUsbArgs[] = new String[2];
	      getUsbArgs[0]="system_profiler";
	      getUsbArgs[1]="SPUSBDataType";
	      try{
	        Process process = new ProcessBuilder(getUsbArgs).start();
	        InputStream is = process.getInputStream();
	        InputStreamReader isr = new InputStreamReader(is);
	        BufferedReader br = new BufferedReader(isr);
	        String line;
	  
	        boolean foundArduino=false;
	        boolean foundSerial=false;
	        int serialNumPosition; 
	        while ((line = br.readLine()) != null && !foundSerial) {
	          if(line.indexOf("Arduino") > 0  || line.indexOf("FT232R") > 0){
	            foundArduino=true;
	          }
	          if(foundArduino){
	            serialNumPosition = line.indexOf("Serial Number");
	            if(serialNumPosition > 0){
	              foundSerial=true; 
	             return line.substring((serialNumPosition+15));
	            }
	          }
	        }
	        if(foundSerial==false){
	          return "";
	        }
	      }
	      catch(IOException e){
	        System.out.println(e.getMessage());
	      }
	    }else if (Base.isLinux()){
	    	String response = "";
		    ProcessBuilder pb = new ProcessBuilder("bash", "-c", ("udevadm info --name="+Preferences.get("serial.port")+" --attribute-walk | grep ATTRS{serial}"));
		    pb.redirectErrorStream(true);
		    try {
			    Process shell = pb.start();
			    // To capture output from the shell
			    InputStream shellIn = shell.getInputStream();
			    shell.waitFor();
			    response = convertStreamToStr(shellIn);
			    shellIn.close();
			    if(response.contains("ATTRS{serial}")){
	        		return response.substring((response.indexOf("\"")+1), (response.indexOf("\n")-1));
	        	}else{
	        		return("");
	        	}	
			}catch (IOException e) {
			    System.out.println("Error occured while executing Linux command. Error Description: "
			    + e.getMessage());
		    }catch (InterruptedException e) {
			    System.out.println("Error occured while executing Linux command. Error Description: "
			    + e.getMessage());
		    }
	    }else if (Base.isWindows()){
	    	String response = "";
		    ProcessBuilder pb = new ProcessBuilder("cmd", "/c", ("\""+Base.getSketchbookFolder().getAbsolutePath()+"\\tools\\devcon.exe\""), "find", "USB\\VID_2341*");//non FTDI
		    pb.redirectErrorStream(true);
		    try {
			    Process shell = pb.start();
			    // To capture output from the shell
			    InputStream shellIn = shell.getInputStream();
			    shell.waitFor();
			    response = convertStreamToStr(shellIn);
			    shellIn.close();   	
			}catch (IOException e) {
			    System.out.println("Error occured while executing Windows command. Error Description: "
			    + e.getMessage());
		    }catch (InterruptedException e) {
			    System.out.println("Error occured while executing Windows command. Error Description: "
			    + e.getMessage());
		    }
		    if(response.contains("USB\\VID")){
        		return response.substring((response.lastIndexOf("\\")+1), (response.indexOf(" ")));
        	}else if(response.contains("No matching devices found")){
        		pb = new ProcessBuilder("cmd", "/c", ("\""+Base.getSketchbookFolder().getAbsolutePath()+"\\tools\\devcon.exe\""), "find", "FTDI*");// FTDI
        		try {
    			    Process shell = pb.start();
    			    // To capture output from the shell
    			    InputStream shellIn = shell.getInputStream();
    			    shell.waitFor();
    			    response = convertStreamToStr(shellIn);
    			    shellIn.close();   	
    			}catch (IOException e) {
    			    System.out.println("Error occured while executing Windows command. Error Description: "
    			    + e.getMessage());
    		    }catch (InterruptedException e) {
    			    System.out.println("Error occured while executing Windows command. Error Description: "
    			    + e.getMessage());
    		    }
    		    if(response.contains("FTDI")){
            		return response.substring((response.lastIndexOf("+")+1), (response.lastIndexOf("+")+9));
    		    }
        	}
	    }
	    return("");
	}


 public static String convertStreamToStr(InputStream is) throws IOException {
	
	 if (is != null) {
		 Writer writer = new StringWriter();
		
		 char[] buffer = new char[1024];
		 try {
			 Reader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"));
			 int n;
			 while ((n = reader.read(buffer)) != -1) {
				 writer.write(buffer, 0, n);
			 }
		 } finally {
			 is.close();
		 }
		 	return writer.toString();
	 }
	 else {
		 return "";
	 }
 }




	
	static protected void load(InputStream input) throws IOException {
	    load(input, table);
	  }
	
	static public void load(InputStream input, Map table) throws IOException {  
	    String[] lines = PApplet.loadStrings(input);  // Reads as UTF-8
	    for (String line : lines) {
	      if ((line.length() == 0) ||
	          (line.charAt(0) == '#')) continue;

	      // this won't properly handle = signs being in the text
	      int equals = line.indexOf('=');
	      if (equals != -1) {
	        String key = line.substring(0, equals).trim();
	        String value = line.substring(equals + 1).trim();
	        table.put(key, value);
	      }
	    }
	  }
	
	public void loadDefaults() { 
		//bottom to top
		table.put("arduinoboard", "1knowmysource");
	}
	
	static protected void save() {
//	    try {
	    // on startup, don't worry about it
	    // this is trying to update the prefs for who is open
	    // before Preferences.init() has been called.
	    if (gistCredFile == null) return;

	    // Fix for 0163 to properly use Unicode when writing preferences.txt
	    PrintWriter writer = PApplet.createWriter(gistCredFile);
	    writer.println("# add usernames and passwords for github accounts here");
	    writer.println("# the format is <username>=<password>");
	    writer.println("# in the comments of your code specify the account you want in the comments with:");
	    writer.println("# USE_GITHUB_USERNAME=<username>");
	    writer.println("# if no account is specified it will use the first one");
	    writer.println("#");
	    Enumeration e = table.keys(); //properties.propertyNames();
	    while (e.hasMoreElements()) {
	      String key = (String) e.nextElement();
	      writer.println(key + "=" + ((String) table.get(key)));
	    }

	    writer.flush();
	    writer.close();
	  }
	    
 }
