[[_TOC_]]

# Introduction
The UDP Test project is designed to test sending and receiving User Datagram Packets (UDP)
between devices on a local network and devices in containers running in a Windows environment,
specifically containers started by Compose.

It should be possible to run the containers (via compose or individually) in Linux, but because it's
a development friendly system, it is less difficult to be successful.

# Getting Started
1. Install required support software
    1. [Podman Desktop](https://podman.io)
        1. Comes with Podman, which comes with compose
    2. [Windows Subsystem for Linux version 2 (WSLv2)](https://learn.microsoft.com/en-us/windows/wsl/install)
    3. (Suggested) Android Studio
    4. (Optional) [Wireshark](https://www.wireshark.org)
2. Software dependencies
    1. (Container) alpine/java:11.0.24 
3. Latest releases

# Build and Test
1. Build the software
    1. From the project root, './gradlew clean build'
    2. From Android Studio, build the project
2. (Optional) Install the App to android devices
    1. Turn on developer options on the Android Device
        1. Settings > About phone
        2. Tap **Build number** seven times until a message says "You are now a developer"
        3. Settings > Developer options (may be under System > Advanced)
        4. Toggle on USB Debugging 
    2. Connect Android device to host computer via USB
    3. From command line:
        1. adb devices, verify the Android device is listed
        2. adb install <filename.apk>, either:
            - app/build/outputs/apk/debug/app-debug.apk
            - app/build/outputs/apk/release/app-release-unsigned.apk
    4. From Android studio
        1. Right click 'app' package
        2. Select 'Run'
3. Build the containers
    1. Build the server container, 'podman build --tag udpserver -f Containerfile.server'
    2. (Optional) Build the client container, 'podman build --tag udpclient -f Containerfile.client'
4. Prepare the environment
    1. Connect to the network testing will be performed on, aids in firewall configuration 
    2. Check the host computer firewall, may require Admin privileges
        - Instructions for Windows Defender:
            1. Control Panel > System and Security > Windows Defender Firewall
            2. (Left Gutter) Allow an app or feature through Windows Defender Firewall
                - Look for rules preventing traffic on the communications ports
            3. (Left Gutter) Change notification settings
                - Turn on when traffic is blocked
    3. Check the HyperV Firewall
        1. Open a Windows PowerShell, preferably as an Administrator
        2. Get the HyperV creator id: '$hvc = (Get-NetFirewallHyperVVMCreator).VMCreatorId'
            - Should be '{40E0AC32-46A5-438A-A0B2-2B479E8F2E90}' but may change in the future so...
        3. Get the current HyperV settings: 'Get-NetFirewallHyperVVMSetting -PolicyStore ActiveStore -Name $hvc'
            - Results should look something like:
              Name                  : {40E0AC32-46A5-438A-A0B2-2B479E8F2E90}
              Enabled               : **True**
              DefaultInboundAction  : **Block**
              DefaultOutboundAction : Allow
              LoopbackEnabled       : True
              AllowHostPolicyMerge  : True
            - If the _Enabled_ flag is _False_ then enable it in the next step
            - If the _DefaultInboundAction_ is _Allow_ then consider Blocking them
        4. (Suggested) Enable the HyperV firewall: 'Set-NetFirewallHyperVVMSetting -Name $hvc -Enabled true'
        5. (Optional) Set the default incoming action to Block: 'Set-NetFirewallHyperVVMSetting -Name $hvc -DefaultInboundAction Block' 
        6. Add HyperV firewall rules for the incoming (server) ports
            1. Determine the incoming server ports, e.g. 11000 and 12000
            2. 'New-NetFirewallHyperVRule -Name <name> -DisplayName "<display name>" -Direction Inbound -VMCreatorId $hvc -Protocol UDP -LocalPorts 11000,12000'
                - name = Some unique string without spaces, seems like a lot of rules contain a GUID but whatever
                - display name = String to display for rule
        7. Verify the rules were created: 'Get-NetFirewallHyperVRule -VMCreatorId $hvc'
            - Find the rule by Name or DisplayName to verify it was added 
            - Results should look something like:
              Name                  : WslCore-Allow-Inbound-ICMPv4-1-40e0ac32-46a5-438a-a0b2-2b479e8f2e90
              DisplayName           : WslCore Inbound ICMPv4 Default Allow Rule
              Direction             : Inbound
              VMCreatorId           : {40E0AC32-46A5-438A-A0B2-2B479E8F2E90}
              Protocol              : ICMPv4
              LocalAddresses        : Any
              LocalPorts            : {3, 11}
              RemoteAddresses       : Any
              RemotePorts           : Any
              Action                : Allow
              Enabled               : True
              EnforcementStatus     : OK
              PolicyStoreSourceType : Local
              Profiles              : Any
              PortStatuses          : << SNIP >>

              Name                  : WslCore-Allow-Inbound-ICMPv6-1-40e0ac32-46a5-438a-a0b2-2b479e8f2e90
              DisplayName           : WslCore Inbound ICMPv6 Default Allow Rule
              Direction             : Inbound
              VMCreatorId           : {40E0AC32-46A5-438A-A0B2-2B479E8F2E90}
              Protocol              : ICMPv6
              LocalAddresses        : Any
              LocalPorts            : {135, 136, 1, 3}
              RemoteAddresses       : Any
              RemotePorts           : Any
              Action                : Allow
              Enabled               : True
              EnforcementStatus     : OK
              PolicyStoreSourceType : Local
              Profiles              : Any
              PortStatuses          : << SNIP >>
        8. Setup the Windows Subsystem for Linux (WSL) network
            1. Edit the ~/.wslconfig file
                - Must do this as the user that runs the WSL
                - Set the networking mode to 'Mirrored'
                - Set the firewall to 'true'
                - The file will look something like:
                  [wsl2]
                  networkingMode=Mirrored
                  firewall=true
            2. Restart WSL to have the file take effect
                1. Stop WSL: 'podman machine stop' 
                2. Start WSL: 'podman machine start'
        9. Setup the devices on the same network 
5. Run the containers in compose: 'podman compose -f Compose.yml up'
    - Creates two udpservers: "udpserver-1" on Port 11000 and "udpserver-2" on Port 12000
6. (Optional) Run a udpserver on the computer running the compose container: java -jar udpserver.jar 0.0.0.0 <port>
    - port = The port on which messages are received, **cannot be** 11000 or 12000, suggest 12345  
7. Test communication between the containers
    - (Optional) From the Android app:
        1. Enter the IP of the computer running the compose container
            - Get the IP of the computer running the compose container with 'ipconfig'
        2. Enter the destination port, either 11000, 12000, or 12345 (if running a local instance)
        3. (Optionally) Enter the port from which the UDP packet is sent
        4. Enter the message to send
        5. Press send
        6. **VERIFY**: The message is received by the udpserver in a container
    - From command line on a computer: 'java -jar udpclient.jar <message> <host> <dest port> \[<src port>]'
        - message = The message to send to the udpserver
        - host = The host of the udpserver
            - The IP of the computer running the compose container or
            - The container name (when running inside a container, see below)
        - dest port = The port on which the udpserver is listening, either 11000, 12000, or 12345 (if running a local instance)
    - From command line inside a container
        - First exec into the container: 'podman exec -it <container name> bash'
            - container name = The name of the contain to exec into, i.e. udpserver-1 or udpserver-2
        - From the bash shell in the container: 'java -jar udpclient.jar <message> <host> <dest port> \[<src port>]'
            - message = The message to send to the udpserver
            - host = The host of the udpserver
                - The IP of the computer running the compose container or
                - The container name (when running inside a container, see below)
            - dest port = The port on which the udpserver is listening
                - If sending to the container name then the port is the unmapped 12345
                - If sending to the host ip then the port is either 11000, 12000, or 12345 (if running a local instance)

# Troubleshooting
1. If a udpserver within the containers are not receiving data then verify connections are working
    - Consider running wireshark on the network and localhost looking for the UDP traffic
    1. Verify the devices are all on the same network
    2. Try sending from udpclient to udpserver both running on the same computer, not in containers
        - Verifies the server is working correctly
    3. Try sending from udpclient on another computer / device to a udpserver running on a computer, not in a container
        - Verifies the firewall is not blocking incoming UDP traffic
    4. Try sending from udpclient inside a container to a udpserver inside a container
        - Verifies the udpserver within the container is running correctly
    5. Try sending from udpclient on another computer / device to a udpserver running inside a container
        - Verifies the end to end solution 

# Contribute
1. Make the App UI better
    1. Have the app remember the client Host & Destination Ports
    2. Make the layout better
    3. Add the ability to set the IP address of the device to the Server Address
    4. Track outgoing messages from the UDP Client
2. Make the Server and Client able to share a socket
    1. This would allow the UDP Client Source Port and the UDP Server Port be the same
3. Change the UDP Content to have a Version Header to process acknowledgements
    1. If the message is version 1 (or greater) AND the header has an ACK req flag then the UDP Server responds with an "ACK"
    2. Change the App so that if the UDP Client Source Port and the UDP Server Port are the same that the ACK req flag is sent
    3. Change the App UDP Client so that if an ACK is requested that a status is shown on the outgoing message
    4. Change the App UDP Client so that if an ACK is received that an updated status is shown on the outgoing message
    5. Change the App UDP Client so that if an ACK is not received (in some time period) that an updated status is shown on the outgoing message
    6. Make the acknowledgement time period configurable