// This is our main server to which users must be able to connect from their local machine

// Imports
import java.io.*;				// DataStream Input/Output & File input and output
import java.util.*; 			// Vectos and Map
import java.net.*;				// Java socket
import java.awt.* ;				// Threading
import java.nio.file.*; 		// Path like os.path in python 

// Class defination
public class ShareFile{

	// Socket Varibles
	// private static String ip="127.0.0.1";
	// private static Integer port=9991;
	public static ServerSocket Soc;
	public static DatagramSocket SocUDP;

	// Data Location Details
	public static String main_folder_name="main";
	public static File main_folder = new File(main_folder_name);



	// Users Details 
	public static Vector<String> Usernames = new Vector<String> ();
	public static Vector<Socket> UserSockets = new Vector<Socket> ();

	public static Vector<Integer> User_UDP_Ports = new Vector<Integer> ();


	// Group detials
	public static Vector<Group> Groups = new Vector<Group> (); 
	public static Map<String,Group> UserGroup = new HashMap<String,Group> (); 


	// Initialize Data
	ShareFile(int tcp_port,int udp_port){
		try{
			// Create Server 
			Soc = new ServerSocket(tcp_port);
			SocUDP = new DatagramSocket(udp_port);

			// Check and create folder
			if(main_folder.exists())
				System.out.println("Main folder:"+main_folder_name+" already exists.");
			else{
				main_folder.mkdir();
				System.out.println("Created Main folder:"+main_folder_name);

			}

		}
		catch(Exception e){e.printStackTrace(System.out);System.exit(0);}

	}



	public static void main(String[] args) throws Exception{
		if(args.length!=2)
		{
			System.out.println("[Error]: Specify Port. Usage: java ShareFile <tcp_port> <udp_port>"); System.exit(0);
		}

		Integer tcp_port = Integer.parseInt(args[0]);
		Integer udp_port = Integer.parseInt(args[1]);

		ShareFile ShareFileServer = new ShareFile(tcp_port,udp_port);
		try{
		System.out.println("Thank you for using ShareFile.\nStarting main server.\nTCP:Address=>" + InetAddress.getLocalHost() + ":" + tcp_port +" UDP Port:"+udp_port);
		}
		catch(UnknownHostException e){};
		while(true)
		{
				// Accept New Users
				// try{
					Socket CSoc = Soc.accept(); 
					ShareClient client_ = new ShareClient(CSoc,SocUDP);

				// }catch(Exception e){e.printStackTrace();}	
		}


	}
}

// Each Client is a thread waiting for input from the user 
class ShareClient extends Thread{
	
	// Name
	public String Username; 

	// Network details
	DataInputStream din;
	DataOutputStream dout;

	Socket CSoc;				// TCP
	DatagramSocket CUDP;		// UDP
	public Integer port;	// UDP CLient Port 

	private int chunk_size = 16*1024;

	Path dirpath; 
	File dir;

	// Initialize Socket after for each client
	ShareClient(Socket CSoc_, DatagramSocket CUDP_) throws Exception {

		// Create a new connection between user and server
		CSoc = CSoc_ ; 
		CUDP=CUDP_;

		din = new DataInputStream(CSoc.getInputStream()) ; 
		dout = new DataOutputStream(CSoc.getOutputStream()) ;

		// Test UDP
		byte[] intial = new byte[1000];	// Bytes to store input from UDP
		DatagramPacket recieve_inital = new DatagramPacket(intial, intial.length);	// Create a datagram to load data

		CUDP_.receive(recieve_inital);

		port = recieve_inital.getPort(); //Store UDP details like port

		// Test TCP
		Username = din.readUTF();			// Client first response is the username
		System.out.println("User:"+Username+" logged in");	// Tell the server the user had been connected

		// Adding new user to the ShareFile Class 
		ShareFile.UserSockets.add(CSoc); 
		ShareFile.User_UDP_Ports.add(port);
		ShareFile.Usernames.add(Username); 
		ShareFile.UserGroup.put(Username,null); 

		dirpath = Paths.get(ShareFile.main_folder_name, Username);
		dir = new File(dirpath.toString());
	
		if(dir.exists() && dir.isDirectory())
			System.out.println("Folder:"+dir+" already exists.");
		else{
		dir.mkdir();
		dout.writeUTF("Created Main folder:"+dir);
		}




		start() ; // Run the thread
	}

	// Wait for input from the user, run the command and return the output 
	public void run() {

		// Run functional variables 
		boolean exit = false;						// Make true when user leaves
		String commandfromClient = new String();	// Store commands here	
		
		while(!exit){
			
			try {
				commandfromClient = din.readUTF();
				StringTokenizer tokenizedCommand = new StringTokenizer(commandfromClient);
				String command=tokenizedCommand.nextToken();

				// User wants to leave
				if(command.equals("close")){

					// [TODO] Remove from the group

					// Remove details from ShareFile
					ShareFile.Usernames.remove(Username); 
					ShareFile.UserSockets.remove(CSoc); 
					ShareFile.User_UDP_Ports.remove(port);
					System.out.println("User:"+Username + " exited");
					exit = true;
				}

				else if(command.equals("list_file")){

					// Recurse over all the tree
					File[] listOfFiles = dir.listFiles();
					for (int i = 0; i < listOfFiles.length; i++) {
						dout.writeUTF(listOfFiles[i].toString());
					}
				}

				else if(command.equals("create_folder")){

					if(tokenizedCommand.countTokens() != 1){continue;}		// If no filenames is passed
						
					String dir_name = tokenizedCommand.nextToken();
					Path dirpath = Paths.get(ShareFile.main_folder_name, Username,dir_name);
					File dir = new File(dirpath.toString());

					if(dir.exists())
						System.out.println("Folder:"+dir_name+" already exists. Rename or delete previous folder");
					else{
					dir.mkdir();
					dout.writeUTF("Created Main folder:"+dir);
					}

				}

				else if(command.equals("mv")){

					if(tokenizedCommand.countTokens() != 2){continue;}		// If no filename is passed
						
					String old_name = tokenizedCommand.nextToken();
					String new_name = tokenizedCommand.nextToken();
					old_name = Paths.get(ShareFile.main_folder_name,Username, old_name).toString();
					new_name = Paths.get(ShareFile.main_folder_name,Username, new_name).toString();
					
					System.out.println("Moving " + old_name + " to " + new_name);
					try {
						File oldFile = new File(old_name);
					 
						if(oldFile.renameTo(new File(new_name))){
							dout.writeUTF("The file was moved successfully to the new folder");
						} 
						else{
							dout.writeUTF("The File was not moved.");
						}
					 
					} catch (Exception e) {e.printStackTrace();}

				}

				else if(command.equals("create_group")){

					if(tokenizedCommand.countTokens() != 1){continue;}		// If no filename is passed
						
					// Created a new group 
					String group_name = tokenizedCommand.nextToken();

					// Check if group already exists
					for(int i=0;i<ShareFile.Groups.size(); i++)
					if(ShareFile.Groups.get(i).GroupName.equals(group_name)){
						dout.writeUTF("Group already exits. Use join_group "+ group_name+" to change group");
						continue;
						}


					Group new_group = new Group(ShareFile.main_folder_name,group_name,Username);
					ShareFile.Groups.add(new_group);	 // Add group in the group list		
					ShareFile.UserGroup.put(Username,new_group); // Add user to the group		


					// Print the details to user
					dout.writeUTF("Created new group:" + group_name);
					dout.writeUTF(Username + " added to " + group_name);

					System.out.println("Created new group:" + group_name);

				}


				else if(command.equals("list_group")){
					for (int i = 0; i < ShareFile.Groups.size(); i++) {
						dout.writeUTF(ShareFile.Groups.get(i).GroupName);
					}
				}


				else if(command.equals("leave_group")){

					// Check if in a group
					if(ShareFile.UserGroup.get(Username)==null){
						dout.writeUTF("First be in a group to leave it.");
						continue;
					}

					// Update the group 
					Group member_group = ShareFile.UserGroup.get(Username);
					member_group.Members.remove(member_group.Members.indexOf(Username));

					// Update sharelist
					ShareFile.UserGroup.remove(Username);

					// Print
					member_group.Notify("\n" + Username + " left");
					System.out.println(Username + " left group:" + member_group.GroupName );
				}

				else if(command.equals("join_group")){

					if(tokenizedCommand.countTokens() != 1){continue;}		// If no groupname is passed

					String new_group_name = tokenizedCommand.nextToken();

				
					// Find if the group exists
					Group new_group = null;
					for (int i = 0; i < ShareFile.Groups.size(); i++)
						if(ShareFile.Groups.get(i).GroupName.equals(new_group_name))
							new_group = ShareFile.Groups.get(i);
					if(new_group == null){
						dout.writeUTF("Group:" + new_group_name + " doesn't exists");
						continue;
					}

					// Update sharelist
					ShareFile.UserGroup.put(Username,new_group);

					// Update the group 
					new_group.Members.add(Username);
					new_group.Notify("\n" + Username + " joined");

					// Print
					System.out.println("Added " + Username + " to " + new_group_name);

				}

				else if(command.equals("share_msg")){

					if(tokenizedCommand.countTokens() == 0){continue;}		// If no groupname is passed

					// Find if the group exists
					Group new_group = ShareFile.UserGroup.get(Username);
					if(new_group == null){
						dout.writeUTF("User not in any group. Join a group to share");
						continue;
					}

					String message = Username + " said:";
					while(tokenizedCommand.hasMoreTokens()){message = message + " " + tokenizedCommand.nextToken();}

					new_group.Notify(message);
					System.out.print("Sent message:"+message);
				}


				else if(command.equals("list_detail")){

					// Find if the group exists

					Group group;
					if(tokenizedCommand.countTokens() == 0){
						group = ShareFile.UserGroup.get(Username);
						if(group == null){
							dout.writeUTF("Join a group.Usage: list_detail <group_name/optional>");
							continue;
						}
					}		// If no groupname is passed

					else{

						String group_name = tokenizedCommand.nextToken();
						group = null;
						for (int i = 0; i < ShareFile.Groups.size(); i++)
							if(ShareFile.Groups.get(i).GroupName.equals(group_name))
								group = ShareFile.Groups.get(i);
						if(group == null){
							dout.writeUTF("Group:" + group_name + " doesn't exists");
							continue;
						}
					}

					// Group exists print the users in the group
					dout.writeUTF("::Group Details::");
					dout.writeUTF("Users:");
					for(int i=0;i<group.Members.size();i++){
						dout.writeUTF("User:" + group.Members.get(i));
						dout.writeUTF(group.Members.get(i) + "\'s Files:");
						
						File user_dir = new File(Paths.get(ShareFile.main_folder_name,group.Members.get(i)).toString());
						// List all the files by user
						File[] listOfFiles = user_dir.listFiles();
						for (int j = 0; j < listOfFiles.length; j++) {
							dout.writeUTF("\t" + listOfFiles[j].toString());
						}

					}

				}
				else if(command.equals("get_file")){

					if(tokenizedCommand.countTokens() < 1){
						continue;
					}	

					// Details 
					StringTokenizer tokenizedDetails = new StringTokenizer(tokenizedCommand.nextToken(),"/");
					if(tokenizedDetails.countTokens() < 3){
						dout.writeUTF("[Error]:  All 3 arguments => groupname/username/file_path not sent");
						continue;
					}					

					String group_name = tokenizedDetails.nextToken();
					String username = tokenizedDetails.nextToken();
					String location = tokenizedDetails.nextToken();
					String new_location;

					System.out.println(commandfromClient);

					// Check for new location
					if(tokenizedCommand.hasMoreTokens())
						new_location = tokenizedCommand.nextToken();
					else
						new_location = "../";

					// Check group exists
					Group group = null;
					for (int i = 0; i < ShareFile.Groups.size(); i++)
						if(ShareFile.Groups.get(i).GroupName.equals(group_name))
							group = ShareFile.Groups.get(i);
					if(group == null){
						dout.writeUTF("Group:" + group_name + " doesn't exists");
						continue;
					}

					// Check member exists username
					if(!group.Members.contains(username)){
						dout.writeUTF("User:"+username + " was never part of this group.");
						continue;
					}

					// Check file exists and file is not a diretory 
					location = Paths.get(ShareFile.main_folder_name, username,location).toString();
					File download_file = new File(location);

					if(!download_file.exists() || download_file.isDirectory()){
						dout.writeUTF("File:"+location + " doesn't exists or is a diretory.");
						continue;	
					}

					// Start uploading file
					FileInputStream fpin = new FileInputStream(location);
					BufferedInputStream bpin = new BufferedInputStream(fpin);
					long fileLength =  download_file.length(), current=0, start = System.currentTimeMillis();
					byte[] file_contents = new byte[chunk_size];
					// Start downloading the file and sending it using UPD or TCP 
					dout.writeUTF("FILE " + new_location + " " + fileLength);

					while(current!=fileLength)
					{
						int size=chunk_size;
						if(fileLength - current >= size) current+=size;
						else {
							size = (int)(fileLength-current);
							current=fileLength;
						}
							file_contents = new byte[size];
						bpin.read(file_contents,0,size);
						dout.write(file_contents);
						System.out.print("Sending file ..."+(current*100/fileLength)+"% complete\r");
					}
					bpin.close();
					fpin.close();
					System.out.println("\nFile Sent. Time:" + (System.currentTimeMillis() - start)/1000 + "secs");
				}


				else if(command.equals("upload")){

					if(tokenizedCommand.countTokens() == 0){continue;}		// If no filename is passed

					// Find the new location on the file
					String filename = tokenizedCommand.nextToken();
					String[] fileSplitted = filename.split("/",0);
					filename = Paths.get(ShareFile.main_folder_name, Username,fileSplitted[fileSplitted.length-1]).toString();

					// Get length of total file
					Integer fileLength = Integer.parseInt(din.readUTF());

					// If no file is being sent the user sends a -1
					if(fileLength == -1)
						continue;

					System.out.print(Username + " uploaded new file:" +filename);

					FileOutputStream fout = new FileOutputStream(filename);
					BufferedOutputStream bfout = new BufferedOutputStream(fout);
					System.out.println(" Size:"+fileLength);

					byte[] file_contents = new byte[chunk_size];
					int bytesRead=0,size=chunk_size;
					if(size>fileLength)size=fileLength;
					while((bytesRead=din.read(file_contents,0,size))!=-1 && fileLength>0)
					{
						bfout.write(file_contents,0,size);
						fileLength-=size; if(size>fileLength) size=fileLength;
					
						// [TODO]: Filelength is wrong check again.
					}

					bfout.flush();
					bfout.close();
					fout.close();
					System.out.println("Uploaded using TCP");
				}

				else if(command.equals("upload_udp")){

					if(tokenizedCommand.countTokens() == 0){continue;}		// If no filename is passed

					// Find the new location on the file
					String filename = tokenizedCommand.nextToken();
					String[] fileSplitted = filename.split("/",0);
					filename = Paths.get(ShareFile.main_folder_name, Username,fileSplitted[fileSplitted.length-1]).toString();

					// Get length of total file
					Integer fileLength = Integer.parseInt(din.readUTF());

					// If no file is being sent the user sends a -1
					if(fileLength == -1)
						continue;

					System.out.print(Username + " uploaded new file:" +filename);

					FileOutputStream fout = new FileOutputStream(filename);
					BufferedOutputStream bfout = new BufferedOutputStream(fout);
					System.out.println(" Size:"+fileLength);

					byte[] file_contents = new byte[chunk_size];
					int bytesRead=0,size=chunk_size;
					if(size>fileLength)size=fileLength;

					// Receiving data using UDP
					DatagramPacket packetUDP;
					while(fileLength>0)
					{
						packetUDP = new DatagramPacket(file_contents,size);
						CUDP.receive(packetUDP);
						bfout.write(file_contents,0,size);
						fileLength-=size; if(size>fileLength) size=fileLength;
						System.out.println(fileLength);
					}

					bfout.flush();
					bfout.close();
					fout.close();
					System.out.println("Uploaded using UDP");
				}

				


			}
			catch(IOException e){e.printStackTrace(System.out);exit=true;};
		}

	}
}


class Group{

	// Data Location Details
	public String GroupName;
	public File group_folder;

	// Users Details 
	public Vector<String> Members = new Vector<String> ();

	// Initialize Data
	Group(String main_folder,String group_name,String new_member){
		try{
			
			// Check and create folder
			GroupName = group_name;
			System.out.println(GroupName);

			String dirpath = Paths.get(ShareFile.main_folder_name, GroupName).toString();

			group_folder = new File(dirpath);

			if(!group_folder.isDirectory()){			// Check if location already exits
				group_folder.mkdir();
			}



			Members.add(new_member);

		}
		catch(Exception e){e.printStackTrace(System.out);System.exit(0);}

	}

    public void Notify(String msg) {
        for(int i=0;i<this.Members.size();i++){
            try {
                Socket sendSoc = ShareFile.UserSockets.elementAt(ShareFile.Usernames.indexOf(this.Members.elementAt(i)));
                System.out.println("Sending message to:" + this.Members.elementAt(i));
                DataOutputStream senddout = new DataOutputStream(sendSoc.getOutputStream());

                senddout.writeUTF(msg + "\n" + this.Members.elementAt(i) + ">>>"); // When printing don't forget to print newline;
            }
            catch(Exception e){e.printStackTrace();}

        }
    }

}