#!/usr/bin/perl -w

use strict;
use FindBin;
use lib "$FindBin::Bin/common";
use Utilities;
use Configs;
use Data::Dumper qw(Dumper);
use File::Copy;
use Cwd;
use POSIX qw(strftime);
use Switch;

use constant FIXFLYER			=> "FixFlyer Engine";
use constant REDLINE_BRIDGE		=> "Redline Bridge";
use constant SOLSENDER			=> "SolSender";
use constant SOLREPLAY			=> "SolReplay";

use constant FIXFLYER_CHECK		=> "fixFlyerCheck";
use constant REDLINE_BRIDGE_CHECK	=> "redlineBridgeCheck";
use constant SOLSENDER_CHECK		=> "solSenderCheck";
use constant SOLREPLAY_CHECK		=> "solReplayCheck";

main();

sub main {
 #this will contain all the input values we capture
 
 print ("\n");
 
 my $solaceAdapter;
 
 #grab inputs
 my $adapterName				= uc(askQuestion("Enter the adapter type(FIXFLYER/REDLINE_BRIDGE/SOLSENDER/SOLREPLAY):","",'AN',"","",128));

 switch ($adapterName) {
 		
		case "FIXFLYER"		{ 
					  	$solaceAdapter->{ $tokens->{ADAPTER_TYPE} }= FIXFLYER;
						$solaceAdapter->{ $tokens->{ADAPTER_CHECK_COMMAND} } = FIXFLYER_CHECK;
					}
 		
		case "REDLINE_BRIDGE"	{ 
						$solaceAdapter->{ $tokens->{ADAPTER_TYPE} } = REDLINE_BRIDGE;
                                                $solaceAdapter->{ $tokens->{ADAPTER_CHECK_COMMAND} } = REDLINE_BRIDGE_CHECK;
					 }

 		case "SOLSENDER"	{
						$solaceAdapter->{ $tokens->{ADAPTER_TYPE} } = SOLSENDER;
                                                $solaceAdapter->{ $tokens->{ADAPTER_CHECK_COMMAND} } = SOLSENDER_CHECK; 
					}

		case "SOLREPLAY"	{ 
						$solaceAdapter->{ $tokens->{ADAPTER_TYPE} } = SOLREPLAY;
                                                $solaceAdapter->{ $tokens->{ADAPTER_CHECK_COMMAND} } = SOLREPLAY_CHECK;	
					 }

 		else{
			print "Unknown adapter type..exiting\n";
			return 0;
 		}	
 }
 
 $solaceAdapter->{ $tokens->{HOST_NAME} }		= askQuestion("Enter the host name for the adapter:","",'AN',"","",128);
 
 $solaceAdapter->{ $tokens->{HOST_ALIAS} }	 	= "$adapterName : ".$solaceAdapter->{ $tokens->{HOST_NAME} }; 

 $solaceAdapter->{ $tokens->{PARENT} }			= askQuestion("Enter the hostname of the router that this adapter connects to:","",'AN',"","",128);

 $solaceAdapter->{ $tokens->{ROUTER_NAME} }		= $solaceAdapter->{ $tokens->{PARENT} };

 $solaceAdapter->{ $tokens->{SERVICE_DESCRIPTION} }	= askQuestion("Enter the name of the adapter instance:","",'AN',"","",128);

 $solaceAdapter->{ $tokens->{VPN_NAME} }                = askQuestion("Enter the name of the message-vpn on the router that this adapter connects to:","",'AN',"","",128);

 my $clientName						= askQuestion("Enter the client name for this adapter:","",'AN',"","",128);

 my $clientStatsTopic = join("","\"#P2P/v:",$solaceAdapter->{ $tokens->{ROUTER_NAME} },"/",$clientName,"/stats\"");

 #print "$clientStatsTopic\n";

 $solaceAdapter->{ $tokens->{CLIENT_STATS_TOPIC} }	= $clientStatsTopic;
 
 my $msgbAddr						= askQuestion("Enter the message-backbone IP in dotted decimal format (xx.xx.xx.xx):",
									"",'AN',"","",16);
 
 my $port						= askQuestion("Enter the port number for the message-backbone IP:",55555,'N',1,100000,"");

 $solaceAdapter->{ $tokens->{MSGB_ADDRESS} }		= $msgbAddr;	


 $solaceAdapter->{ $tokens->{ADDRESS_PORT} }        	= join(':',$msgbAddr,$port);


 my $outfileName 		= join('/',$dirs->{OUTPUTS},$solaceAdapter->{ $tokens->{HOST_NAME} }.".cfg");
	
 my $templateFile		= $templates->{SOLACE_ADAPTER};
 
 copyTemplate($templateFile,$outfileName);

 my $key;
 for $key ( keys %{$solaceAdapter}) {
 	substitute($key,$solaceAdapter->{$key},$outfileName);
 }

 
 print ("\nThe Object configuration file for the Solace adapter has been created at: $outfileName\n\n");

}
