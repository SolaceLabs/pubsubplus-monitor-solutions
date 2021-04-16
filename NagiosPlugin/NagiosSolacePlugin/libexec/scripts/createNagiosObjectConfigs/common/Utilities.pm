#!/usr/bin/perl -w
package Utilities;

use strict;
use warnings;
use Exporter;
use Config::Std;
use Data::Dumper;
use File::Copy qw(copy);

use constant ROUTER             => "ROUTER";
use constant VPN                => "VPN";
use constant NAGIOS_SERVER      => "NAGIOS_SERVER";
use constant TYPE               => "TYPE";

our @ISA= qw( Exporter );

# these functions are experted by default
our @EXPORT = qw( substitute
                  askQuestion
		  copyTemplate
                );

######################################
#				     #
# substitite ($1,$2,$3)              #
# substitutes  $1 with $2            #
# in place in file $3                #
#                                    #
######################################

sub substitute {

 my $find=$_[0];
 #$find = "<".$find.">";
 my $replace=$_[1];
 my $infilename=$_[2];
        #print "find:$find     ::replace:$replace\n";
 
 open(FILE, "<$infilename") || die "Could not open input file -  File not found"; my @lines = <FILE>; close(FILE);
 
 my @newlines;
 foreach(@lines) {
 	$_ =~ s/$find/$replace/g;
        push(@newlines,$_);
 }
 open(FILE, ">$infilename") || die "Could not open output file - File not found"; print FILE @newlines; close(FILE);
}

########################################
#
# asks a question , validates the input 
# (either a valid digit in a range or 
# an alphanumeric string within character limits
#
#
#  parameters
#  question                     - question to be asked
#  defaultAnswer                - default answer to be supplied 
#  input type                   - numeric or alphanumeric
#  lower and upper values       - for number inputs
#  length                       - for numeric inputs
#  optionality                  - defines if the answer to the question is optional or mandatory. 
#                                 if this is not specified the answer is assumed to be mandatory
#
##########################################

sub askQuestion {

 my $question           = shift or die ("unable to get question");
 my $defaultAnswer      = shift;
 my $inputType          = shift;                                ## N -> numeric AN -> alphanumeric
 my $lowerValue         = shift;                                ## applies only to digits
 my $upperValue         = shift;                                ## applies only to digits
 my $length             = shift;                                ## applies only to strings
 my $optionality        = shift;                                ## defines if the answer is optional or mandatory 

 my $errorMsg;
 if (!defined($optionality)){
        $optionality = 'M';
 }

 if ($inputType eq "N"){
        $errorMsg       = "\t*** Please enter a valid number between $lowerValue and $upperValue ***\n";
 }
 elsif ($inputType eq "AN"){
         $errorMsg      = "\t*** Please enter a valid string not containing wildcards(*,>) or whitespaces, with max length $length ***\n";
 }

 my $reply;
 my $answer="";

 if ( $inputType eq "N") {
        $answer         =  getAnswer($question,$defaultAnswer,$optionality);

        if ($answer ne ""){
                if( $answer =~ /[^0-9.]/ || ($answer =~ /[0-9]/ && $answer < $lowerValue || $answer > $upperValue)) {
                        print $errorMsg;
                        $answer = askQuestion ($question, $defaultAnswer, "N", $lowerValue, $upperValue, "",$optionality);
                }
        }
 }
 elsif ( $inputType eq "AN" ){
        $answer         =  getAnswer($question,$defaultAnswer,$optionality);
        if ($answer ne "" && $answer =~ /[^a-zA-Z0-9]/ && $answer =~ /[\*\s\>]/ || length($answer) > $length) {
                print $errorMsg;
                $answer = askQuestion ($question, $defaultAnswer,"AN", "", "", $length,$optionality);
        }
 }

 return ($answer);

}


#########################################
#
# asks a question, and returns the answer
# called by askQuestion function above
#
###########################################

sub getAnswer {

 my $question           = shift;
 my $defaultAnswer      = shift;
 my $optionality        = shift;

 my $reply;
 my $answer="";

 if ($optionality eq 'O'){
        print "$question ";
        chomp($reply=<STDIN>);
        $answer=$reply;
 }
 else{
        do {
                if ($defaultAnswer ne ""){
                        print "$question [$defaultAnswer] ";
                        $answer=$defaultAnswer;
                }
                else {
                        print "$question ";
                }
                chomp($reply=<STDIN>);
                if ($reply ne ""){
                        $answer=$reply;
                }

        } while ($answer eq "" );
 }
 return $answer;
}

##################################
#
# creates a copy of the file supplied
# first parameter - file to be copied
# second parameter - name of destination file
#
####################################

sub copyTemplate {

 my $source=$_[0];
 my $dest=$_[1];

 #print ("Source: $source ; Dest: $dest \n");
 copy($source,$dest) or die "Copy of $source failed in function copyTemplate: $!";
 chmod 0666,$dest;
}

1

