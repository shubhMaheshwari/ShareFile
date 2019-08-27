// This is our clint server using which users connect to the main server

// Imports
import java.io.*;				// DataStream Input/Output & File input and output
import java.util.*;				// Vectos and Map
import java.net.*;				// Java socket 
import java.awt.*;				// Threads

// Class defination
public class User{

	// Socket Varibles
	private static String server_ip;				// TCP Server IP
	private static Integer server_port;				// TCP Server Port
	private static Integer port;					// UDP Server Port
	public static Socket Soc;						// Socket to connect to Server using TCP
	public static DatagramSocket SocUDP;			// Socket to connect to Server using UDP

	public static DataInputStream din; 				// Input stream connected to Server
	public static DataOutputStream dout;			// Output stream connected to Server

	// Name and other details
	public static String username;					// Username
	private static Integer chunk_size = 16*1024;	// Chunk Size to transfer data



	// Initialize Data
	User(){
		try{
			System.out.println(server_ip+server_port);
			// Connect to server (TCP & UDP)
			Soc = new Socket(server_ip,server_port);
			SocUDP = new DatagramSocket();

			// Connect Inp/Out Stream
			din = new DataInputStream(Soc.getInputStream());
			dout = new DataOutputStream(Soc.getOutputStream());

			System.out.println("Connected to Host Server at:"+server_ip+":"+server_port + ". UDP:"+port);


			// Test UDP
			String a="test string";
			byte[] file_contents = new byte[1000]; file_contents = a.getBytes();
			DatagramPacket initial = new DatagramPacket(file_contents,file_contents.length,InetAddress.getByName(server_ip),port);
			SocUDP.send(initial);
			
			// Test TCP
			dout.writeUTF(username);

		}
		catch(Exception e){System.out.println("[Error]: Problem in connecting to server. Check Connection");e.printStackTrace(System.out);System.exit(0);}

	}


	// Main Function that runs on running programs
	public static void main(String[] args) throws IOException{
	
		if(args.length!=3)		// Check correct number of arguments passed to the program
		{
			System.out.println("[Error]: Specify Port. Usage: java ShareFile <server-ip> <server-port> <port>"); System.exit(0);
		}

		// Initialize data
		server_ip = args[0];			
		server_port = Integer.parseInt(args[1]);
		port = Integer.parseInt(args[2]);
	
		// Define Inputstream for command input 
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		String inputLine=null;			// String where all the commands are stored


		// Get user name
		try {
			InetAddress ip;
			ip = InetAddress.getLocalHost();
			username = ip.getHostName();
		} catch (UnknownHostException e) {e.printStackTrace();}

		System.out.print("Enter username(Defualt:" + username+"):");
		inputLine=bufferedReader.readLine();

		if(inputLine.length() > 0)
			username = inputLine;

		// Initialize new user
		User user = new User();
		
		// Define Receive Message Thread
		new Thread(new RecievedMessagesHandler(din,username)).start();

		// Avaible Commands
		// System.out.println("Avaible Commands");
		// System.out.println("close");



		// Get commands
		while(true){
			try{

				try{Thread.sleep(200);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
				System.out.print(username+">>>");
				inputLine=bufferedReader.readLine();
				if(inputLine.length() == 0)
					continue;
				StringTokenizer tokenizedCommand = new StringTokenizer(inputLine);
				dout.writeUTF(inputLine);
				String command = tokenizedCommand.nextToken();

				if(command.equals("close")){
					System.out.println("Bye :)");
					Soc.close();
					din.close(); 
					dout.close();
					SocUDP.close();
					System.exit(0);
				}

				else if(command.equals("list_file")){			// List files in the main folder 
					
					System.out.println("Files on cloud:");

				}

				else if(command.equals("create_folder")){			// List files in the main folder 
					if(tokenizedCommand.countTokens() == 0){
						System.out.println("Pass the new directory name. Usage: create <dirname>");	
						continue;
					}
				}

				else if(command.equals("mv")){			// List files in the main folder 
					if(tokenizedCommand.countTokens() != 2){
						System.out.println("Usage: mv <old_location> <new_location>");	
						continue;
					}
				}


				else if(command.equals("upload")){			// Upload using TCP
					
					if(tokenizedCommand.countTokens() == 0){		// If no filename is passes
						System.out.println("Pass the file location. Usage: upload <file_location>");
						continue;
					}
					String path = tokenizedCommand.nextToken().replaceFirst("~", System.getProperty("user.home"));		// Replace first ~ with full path as java doesn't do that on its own

					File upload_file = new File(path);	// Get upload file and location
					byte[] file_contents = new byte[chunk_size];

					if(!upload_file.isFile()){		// Not a file
						System.out.println("File:"+upload_file.toString()+" is not a file.");
						dout.writeUTF(String.valueOf(-1));
						continue;
					}
					else if(!upload_file.exists()){			// Check if location is correct
						System.out.println("File:"+upload_file.toString()+" doesn't exists");
						dout.writeUTF(String.valueOf(-1));
						continue;
					}

					// Start uploading file
					System.out.println("Sending file");
					FileInputStream fpin = new FileInputStream(upload_file);
					BufferedInputStream bpin = new BufferedInputStream(fpin);
					long fileLength =  upload_file.length(), current=0, start = System.currentTimeMillis();
					
					dout.writeUTF(String.valueOf(fileLength));
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

				else if(command.equals("upload_udp")){		// Upload using UPD


					if(tokenizedCommand.countTokens() == 0){		// If no filename is passes
						System.out.println("Pass the file location. Usage: upload_udp <file_location>");
						continue;
					}

					String path = tokenizedCommand.nextToken().replaceFirst("~", System.getProperty("user.home"));		// Replace first ~ with full path as java doesn't do that on its own
					File upload_file = new File(path);	// Get upload file and location
					byte[] file_contents = new byte[chunk_size];

					if(!upload_file.isFile()){		// Not a file
						System.out.println("File:"+upload_file.toString()+" is actually a dir. Uploading dirs not implemented");
						dout.writeUTF(String.valueOf(-1));
						continue;
					}
					else if(!upload_file.exists()){			// Check if location is correct
						System.out.println("File:"+upload_file.toString()+" doesn't exists");
						dout.writeUTF(String.valueOf(-1));
						
						continue;
					}

					// Start uploading file
					FileInputStream fpin = new FileInputStream(upload_file);
					BufferedInputStream bpin = new BufferedInputStream(fpin);
					long fileLength =  upload_file.length(), current=0, start = System.currentTimeMillis();
					
					dout.writeUTF(String.valueOf(fileLength));
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
						
						DatagramPacket sendPacket = new DatagramPacket(file_contents,size,InetAddress.getByName(server_ip),port);
                        SocUDP.send(sendPacket);
						System.out.print("Sending file ..."+(current*100/fileLength)+"% complete\r");
					}
					bpin.close();
					fpin.close();
					System.out.println("\nFile Sent. Time:" + (System.currentTimeMillis() - start)/1000 + "secs");
				}


				else if(command.equals("create_group")){			// List files in the main folder 
					if(tokenizedCommand.countTokens() != 1){
						System.out.println("Usage: create_group <group_name>");	
						continue;
					}

					// Sleep till group is created (1sec)

				}

				else if(command.equals("list_group")){			// List files in the main folder 
					System.out.println("ShareFile contains following groups:");
					// Sleep till group is created (1sec)

				}

				else if(command.equals("join_group")){			// Leave the previous group and join the new group 
					if(tokenizedCommand.countTokens() == 0){		// If no filename is passes
						System.out.println("Pass the group_name. Usage: join_group <group_name>");
						continue;
					}
				}

				else if(command.equals("leave_group")){			// Leave the group 
				}

				else if(command.equals("share_msg")){			// Share message on the group 
					if(tokenizedCommand.countTokens() == 0){		// If no message is passes
						System.out.println("Pass the message. Can't share nothing. Usage: share_msg <msg>");
						continue;
					}

				}

				else if(command.equals("list_detail")){			// Share message on the group 
					// continue;
				}

				else if(command.equals("get_file")){			// Download file from cloud 
					// continue;
					if(tokenizedCommand.countTokens() < 1){		// If no message is passes
						System.out.println("Usage: get_file <groupname/username/file_path> <new_location/optional>");
						continue;
					}

				}
				


				else{
					System.out.println("Unrecognized Command");
				}

			}
			catch(Exception e){e.printStackTrace();}
	

		}


	}

}

class RecievedMessagesHandler implements Runnable {
	private DataInputStream server;				// Location from where we are getting the data
	private String username;					// Username 
	private int chunk_size = 16*1024;
	// Initialize
	public RecievedMessagesHandler(DataInputStream server,String username) {
		this.server = server; this.username = username;			
	}


	@Override
	public void run() {
		String inputLine=null;
		while(true)
		{
			try {
				inputLine=server.readUTF();
				StringTokenizer st = new StringTokenizer(inputLine);
				if(st.nextToken().equals("FILE"))
				{
					//File recienve
					String fileName=st.nextToken();
					fileName = fileName.replaceFirst("~", System.getProperty("user.home"));
					int fileLength = Integer.parseInt(st.nextToken());
					System.out.println("Recieving file "+fileName);
					byte[] file_contents = new byte[chunk_size];

					FileOutputStream fpout = new FileOutputStream(fileName);
					BufferedOutputStream bpout = new BufferedOutputStream(fpout);

					int bytesRead=0,size=chunk_size;
					if(size>fileLength)size=fileLength;
					while((bytesRead=server.read(file_contents,0,size))!=-1 && fileLength>0)
					{
						bpout.write(file_contents,0,size);
						fileLength-=size; if(size>fileLength)size=fileLength;
					}
					bpout.flush();
					bpout.close();
					fpout.close();
					System.out.println("File Recieved");
				}
				else
					System.out.println(inputLine);
			}
			catch(Exception e) {e.printStackTrace(System.out); break;}
		}
	}
}

