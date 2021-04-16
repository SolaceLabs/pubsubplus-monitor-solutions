#!/usr/bin/perl -w

use strict;
use FindBin;
use lib "$FindBin::Bin/common";
use Utilities;
use Configs;
use Data::Dumper qw(Dumper);
use File::Copy;
use Cwd;

use constant SERVICE_XML_PREFIX	=> "/usr/local/nagios/var/solace/serviceManager_";
use constant CONFIG_FILE_PREFIX	=> "/usr/local/nagios/etc/objects/solaceVpns/";
use constant SERVICE_FILTER	=> "\"svc/*\"";

main();

sub main {
 #this will contain all the input values we capture
 my $solaceVpn;
 
 print ("\n");

 #grab inputs
 $solaceVpn->{ $tokens->{VPN_NAME} }		= askQuestion("Enter the name of the message-vpn on the router:","",'AN',"","",128);
 $solaceVpn->{ $tokens->{HOST_NAME} }		= askQuestion("Enter the Nagios host name (display name) of the VPN:",$solaceVpn->{ $tokens->{VPN_NAME} } ,
								"AN","","",128);
 $solaceVpn->{ $tokens->{HOST_ALIAS} }	 	= "Solace VPN : ".$solaceVpn->{ $tokens->{HOST_NAME} }; 
 $solaceVpn->{ $tokens->{MGMT_ADDRESS} }	= askQuestion("Enter the management IP address in dotted decimal format (xx.xx.xx.xx):",
									"",'AN',"","",16);

 my $port					= askQuestion("Enter the port number for the management IP:","80",'N',1,55555,"");
 
 $solaceVpn->{ $tokens->{ADDRESS_PORT} }	= join(':',$solaceVpn->{ $tokens->{MGMT_ADDRESS} },$port);
 
 $solaceVpn->{ $tokens->{PARENT} }		= askQuestion("Enter the name of the router hosting this VPN:","",'AN',"","",128);

 $solaceVpn->{ $tokens->{ROUTER_NAME} }		= $solaceVpn->{ $tokens->{PARENT} };

 my $msgbAddr					= askQuestion("Enter the message-backbone IP in dotted decimal format (xx.xx.xx.xx):",
									"",'AN',"","",16);
 
 $port						= askQuestion("Enter the port number for the message-backbone IP:",55555,'N',1,100000,"");

 $solaceVpn->{ $tokens->{MSGB_ADDRESS} }	= join(':',$msgbAddr,$port);

 $solaceVpn->{ $tokens->{SERVICE_XML} }		= SERVICE_XML_PREFIX.$solaceVpn->{ $tokens->{VPN_NAME} }.".xml";
 $solaceVpn->{ $tokens->{CONFIG_FILE} }		= CONFIG_FILE_PREFIX.$solaceVpn->{ $tokens->{VPN_NAME} }.".cfg";
 $solaceVpn->{ $tokens->{SERVICE_FILTER} }	= SERVICE_FILTER; 

 if ( lc(askQuestion("Are there any queues to be monitored on this VPN?(Y/N)","Y",'AN',"","",1)) eq "y"){
	$solaceVpn->{ $tokens->{QUEUE} }	= askQuestion("Enter the name of the queue:","",'AN',"","",128);
 }					
 

 if ( lc(askQuestion("Are there any VPN bridges to be monitored on this VPN?(Y/N)","Y",'AN',"","",1)) eq "y"){                            
        $solaceVpn->{ $tokens->{BRIDGE} }        = askQuestion("Enter the name of the VPN Bridge:","",'AN',"","",128);
 }  
 
 #print Dumper($solaceVpn);

 my $outfileName 		= join('/',$dirs->{OUTPUTS},$solaceVpn->{ $tokens->{VPN_NAME} }.".cfg");

 my $templateFile		= $templates->{SOLACE_VPN};
 
 copyTemplate($templateFile,$outfileName);

 my $key;
 for $key ( keys %{$solaceVpn}) {
 	substitute($key,$solaceVpn->{$key},$outfileName);
 }

 
 print ("\nThe Object configuration file for the Solace router has been created at: $outfileName\n\n");

 print ("If the there are no queues or VPN bridges to be monitored, please delete the appropriate service checks from the file. For monitoring additional queues and VPN bridges, refer to the Nagios Operational Guide\n\n");

}
