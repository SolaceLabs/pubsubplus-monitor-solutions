#!/bin/sh

echo "Checking if Perl and required modules are installed..."
echo

perl -v | grep 'This is perl' &> /dev/null
if [ $? == 0 ]; then
   PERL_VERSION=$(perl -v | awk '/This/ {print $4}' | sed -e 's/v//' | cut -d . -f1,2)
   echo "Perl Version $PERL_VERSION detected"
   echo
fi

MODULES="Config::Std Data::Dumper File::Basename File::Slurp File::Copy Cwd Getopt::Long LWP::UserAgent Nagios::Plugin Nagios::Plugin::DieNicely Pod::Usage Scalar::Util Switch Try::Tiny XML::LibXML"

FLAG=1
for i in $MODULES ; do
  if  $(perl -M$i -e '1;' >/dev/null 2>&1 ) ; then

	echo -e " $i  ... OK"
    else
    	echo -e " $i  ... Module not present!!!" 
	$FLAG=0
  fi
done

if [[ $FLAG -eq 0 ]];then
	echo 
	echo "One or more required Perl modules are missing."
else
	echo
	echo "All required Perl modules are present."
fi
