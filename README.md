AKKARemoteConnection
====================
Project runs for 12 workers size and batch size of 50000. Can be configured by hardcoding in the program.
1. Normal Mode: 
	unzip Project1-remote
	cd Project1-remote
	sbt compile
	sbt ‘run 4’
	
2. Client Server Mode:
Server settings:
cd Project1
Put IP of server in ./src/main/resources/application.conf
cd ../../..     (in Project1 Folder)
sbt compile
sbt ‘run 4’

Client Setting:
cd Project1-remote
Put IP of client in ./src/main/resources/application.conf
cd ../../..     (in Project1-remote Folder)
sbt compile
sbt ‘run <Give servers IP address>’

Note: Project1-remote runs in both client and server mode. In Project1, even though workers are spawned, work is only given to remote workers to make the output obvious for remote working. Code lines assigning work to the master’s slave have been commented for this.
