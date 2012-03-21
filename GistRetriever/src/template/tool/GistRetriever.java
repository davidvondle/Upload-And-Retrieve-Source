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
 * @modified	03/21/2012
 * @version		0.1
 */

 package template.tool;
 
 import java.awt.FileDialog;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.GistFile;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.GistService;

import processing.app.*;
import processing.app.tools.*;
import processing.core.PApplet;
 
 
 
 public class GistRetriever implements Tool {
 
 // when creating a tool, the name of the main class which implements Tool
 // must be the same as the value defined for project.name in your build.properties
 
	Editor editor; 
	static LinkedHashMap table = new LinkedHashMap();
	static final String GIST_FILE = "gistCredentials.txt";
	static File gistCredFile;
	Gist gist = new Gist();
	Gist newGist = new Gist();
	boolean foundFirstGist=false;
 
	public String getMenuTitle() {
		return "Retrieve Source";
	}
 
	public void init(Editor editor) {
		this.editor=editor;
	}
	
	
	
	public void run() {
	      String serialNumber; 
	      String username="";
	      String password="";
	      
	      //first look for different github accounts
	      gistCredFile = Base.getSettingsFile(GIST_FILE);
			try {
		          load(new FileInputStream(gistCredFile));
		          //now "table" should hold username/password pairs

	        } catch (Exception ex) {
	          Base.showError("Error reading github credentials",
	                         "Error reading github credentials. " +
	                         "Please delete (or move)\n" +
	                         gistCredFile.getAbsolutePath() +
	                         " and restart Arduino.", ex);
	        }
	      
	      //then find gist
	      try {
	        int timeout = 2000;
	        InetAddress address = InetAddress.getByName("api.github.com");
	        if (address.isReachable(timeout)){
	          serialNumber=findSerialNumber();
	          if (!serialNumber.isEmpty()){
	        	  Iterator iterator = table.keySet().iterator();
	        	  boolean foundSource = false;
	        	  
	        	  while (iterator.hasNext() && foundSource==false) {
						Object key = (String) iterator.next();
			 		    username=(String) key;
			 		    password=(String) table.get(key);
			 		    foundSource=retrieveFromGitHub(serialNumber,username, password);
	        	  }
	        	  
	        	  if(!foundSource){
	        		  System.out.println("No source was found for this board.");
	        	  }else{
	        		  for (String key : gist.getFiles().keySet()) {
				          if(key.contains(".pde") || key.contains(".ino")){
				        	  editor.getSketch().setCurrentCode(0);//goes to original pane
				        	  editor.setText(gist.getFiles().get(key).getContent()); //gets the first sketch, puts it in the window
				          }else{ //make libraries
				        	  File newDirectory = new File(editor.getSketch().getFolder(), "temp_gist");
				        	  File testFile = new File(newDirectory, gist.getFiles().get(key).getFilename());
				        	  PrintWriter writer = PApplet.createWriter(testFile);
			        		  writer.println(gist.getFiles().get(key).getContent());
			        		  writer.flush();
			        		  writer.close();
			        		  // now do the work of adding the file
			        		  editor.getSketch().addFile(testFile);
		        		    
			        		  testFile.delete();
			        		  newDirectory.delete();
				          }
				        }
		              System.out.println("Found Source: " + gist.getHtmlUrl());
	        	  }
	          }else{
	            System.out.println("Could not find your board, make sure it's plugged into USB.");
	          }
	        }else{
	          System.out.println("github service is unavailable, cannot retrieve source.");
	        }
	      } catch (Exception e) {
	        System.out.println("You are not connected to the internet, cannot retrieve source.");
	        System.out.println(e);
	      }
	    }
	
	    
	    private boolean retrieveFromGitHub(String serialNumber, String username, String password) { 
	    	
	    	
	      GitHubClient client = new GitHubClient().setCredentials(username, password);
	      GistService service = new GistService(client);
	      
	      try{
	        List<Gist> gists = service.getGists(username);
	        Boolean foundMatchingGist=false;
	        for (int i = gists.size(); --i >= 0;){  //backwards so the first one found is the oldest one
	        	newGist = (Gist)gists.get(i);
	          if(newGist.getDescription().toUpperCase().contains(serialNumber.toUpperCase())){ //found the last matching gist , uppercase because windows capitalizes everything...
	            if(foundMatchingGist==true){ //if one has already been found then an extra was made in error and needs to be cleaned up
	              //delete the spurious gist
	              service.deleteGist(newGist.getId());
	            }else{
	              newGist=service.getGist(newGist.getId());//get it again because the other capture only gets the meta-data
	              if(!foundFirstGist){
	            	  gist=newGist;
	            	  foundFirstGist=true;
	              }else if(newGist.getHistory().get(0).getCommittedAt().after(gist.getHistory().get(0).getCommittedAt())){//look for most recent gist in case you have the same board on multiple accounts
	            	  gist=newGist;
	              }
	              foundMatchingGist=true;
	            }
	          }
	        }
	        if(foundMatchingGist==false){ //if no gist exists for the board
	          	return false;
	        }else{
	        	return true;
	        }
	      }catch(IOException e){
	        System.out.println(e.getMessage());
	        return false;
	      }
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
		        	if(line.indexOf("Arduino") > 0  || line.indexOf("FT232R") > 0 || line.indexOf("Vendor ID: 0x20a0") > 0){ //Vendor ID: 0x20a0 is freetronics
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
			    String response="";
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
	    		    }else if(response.contains("No matching devices found")){
	    			    pb = new ProcessBuilder("cmd", "/c", ("\""+Base.getSketchbookFolder().getAbsolutePath()+"\\tools\\devcon.exe\""), "find", "USB\\VID_20A0*");//freetronics? either shows up as VID_20A0 or VID_20a0
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
	    	        	}
		        	}
	        	}
		    }
		    return "";
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
 }



