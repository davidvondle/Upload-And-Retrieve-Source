These tools allow you Arduino board to be the link to it's own source stored online.

To install, place the folders "GistUploader" and "GistRetriever" in Documents/Arduino/tools.
On Windows you will need the "DevCon" command line utility, download directly from Microsoft here: http://support.microsoft.com/kb/311272

When you are finished programming your Arduino, select "Save and Upload Source to Github from the "Tools" dropdown menu.

If you ever need the source for that board, plug the board into USB, and select "Retrieve Source". The code will show up in the editor window.

PROTIPS:

Using your own github account:

The github username/password file is in the same folder your Arduino preferences.txt file is.  it is called "gistCredentials".  You can add your own github login information into that file if you prefer to save to your own account. 
in your code you can specify which account you want the code to go to by putting this in the comments at the top:
USE_GITHUB_USERNAME=davidvondle
Keep in mind these accounts are the same account that the retriever uses to find the code, so if you use a personal account and want to retrieve the code on another machine, make sure your credentials are on that machine as well.

Making the code private:

To make the code private, put this string in the comments:
MAKE_PRIVATE_ON_GITHUB
This only really makes sense on your own github account, as the login information for the general account is not secret.

Libraries:
This code supports libraries that are in the same folder as the .ino/.pde file.  

For an extensive write-up on this project read my post on IDEO Labs here: 
http://labs.ideo.com/2012/03/09/arduino-tool-that-connects-each-board-to-its-own-source/

ENJOY!



Copyright 2012 Dave Vondle

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

Dependencies:

This code depends on 
egit, which is under the EDL License:
http://www.eclipse.org/org/documents/edl-v10.php

gson, commons-codec, commons-logging, httpclient, and httpcore are all under the Apache License 2.0
http://www.apache.org/licenses/LICENSE-2.0


