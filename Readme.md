# ShareFile 
ShareFile is a distributed file system to share data across an intranet.

It works by having a main server connect to each client. Then a user can connect to ShareFile to upload and download data.

We have also implemented groups. Using this a user can share data with its group members.

#### Details
Distributed Systems Assignment 1

Author:

		Shubh Maheshwari
		20161170

## Run Servers
#### Run Main Server: 
```
javac ShareFile.java; java ShareFile <TCP-Port> <UDP-Port>
// eg. javac ShareFile.java; java ShareFile 9991 9090
```

#### Run Client: 
```
javac User.java; java User <TCP-IP> <TCP-Port> <UDP-Port>
// javac User.java; java User 127.0.1.1 9991 9090
```

# Usage/Commands: 
#### Basic Usage 
- **Enter username <username>:** 
    Creates a new user.

- **list_file:** 
    List all files stored by user

- **create_folder <folder_name>:** 
    Create a new folder on the cloud

- **mv <old_location> <new_name>:** 
    move files to new location.
    
    *Make sure to send the new location and not just a directory*    

- **upload <file_location>:** upload file to the cloud using TCP 

- **upload_udp <file_location>:** upload file to the cloud using UDP

- **close:** 
    Closes account.        

#### Group Usage
- **create_group <group_name>:** creates a new group. Adds the user to the group created.

- **list_group:** all the groups ever created on ShareFile

- **join_group <group_name>:** join a particular group

- **leave_group:** leave the group

- **share_msg <str>:** broadcasts message to all the members of the group

- **list_detail <group_name/optional>:**list details about the group. The user and all their files. 

- **get_file <groupname/username/file_path> <new_location>:** Download file from other members data.