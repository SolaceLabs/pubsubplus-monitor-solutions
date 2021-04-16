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


main();

sub main {
 #this will contain all the input values we capture
 my $solaceRouterPair;
 
 print "\n------------------------------------------------------------------------------------------------------------------------------------\n";
 print "NOTE:Ensure that the members of this Solace router pair have been created prior to running this script. If not,exit and create them!\n";
 print "------------------------------------------------------------------------------------------------------------------------------------\n";

 if (lc (askQuestion("Do you want to continue? (Y/N):",'N','AN',"","",1)) eq 'n' ){
	print "\n";
	exit 0;
 }
 else{
 	#grab inputs
 	print ("\n");

 	$solaceRouterPair->{ $tokens->{HOST_NAME} }		= askQuestion("Enter a hostname for the router HA pair:","",'AN',"","",128);
 	$solaceRouterPair->{ $tokens->{HOST_ALIAS} }	 	= "Solace Router HA Pair: ".$solaceRouterPair->{ $tokens->{HOST_NAME} }; 

 	$solaceRouterPair->{ $tokens->{MSGB_ADDRESS} } 		= askQuestion("Enter the primary message-backbone IP address of the HA pair in dotted decimal form (xx.xx.xx.xx):","",'AN',"","",,16);
  
	$solaceRouterPair->{ $tokens->{PORT_NUMBER} }		= askQuestion("Enter the port number:","55555",'N',1,99999,"");

 	#print Dumper($solaceRouter);

 	my $outfileName 		= join('/',$dirs->{OUTPUTS},$solaceRouterPair->{ $tokens->{HOST_NAME} }.".cfg");

 	my $templateFile		= $templates->{SOLACE_ROUTER_PAIR};
 
 	copyTemplate($templateFile,$outfileName);

 	my $key;
 	for $key ( keys %{$solaceRouterPair}) {
 		substitute($key,$solaceRouterPair->{$key},$outfileName);
 	}

 
 		print ("\nThe Object configuration file for the Solace router pair has been created at: $outfileName\n\n");

    }

}
