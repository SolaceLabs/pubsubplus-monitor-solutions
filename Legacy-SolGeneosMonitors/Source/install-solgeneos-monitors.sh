#!/bin/bash
##########################################################################
# Run this script to update your custom solgeneos monitors.
# Just place your solgeneos-monitors_*.tar.gz in one of the directories
# listed under: source_dirs and run the script.
# The script will:
#  - backup your /usr/sw/solgeneos directory to a date stamped archive
#  - copy the contained jar files to your solgeneos install directory
#  - copy any Vpn* and App* properties to your solgeneos install directory
#  - restart the solgeneos and netprobe agent
# 
##########################################################################
# change to true, if your script needs to extract the tar.gz file first
extract_before_install="true"
##########################################################################
# Functions

function check_rc {
  rc=$?
  if [[ $rc != 0 ]]; then 
    echo "$1 returned non zero rc! Rc was: $rc"
    read -p "Do you want to continue? " -n 1 -r
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      exit $rc
    fi
  fi
}

function restart {
  echo "Restarting solgeneos and netprobe service..."
  service solgeneos stop
  service netprobe restart
  sleep 1
  service solgeneos start
}

function backup {
  echo "Backing up existing solgeneos directory..."
  cur_path=`pwd`
  sw=/usr/sw
  dat=`date +%Y-%m-%d`
  backup="solgeneos-${dat}.tar.gz"
  echo "Creating backup archive ${backup}..."
  if [ -d "${sw}" ]; then
    cd ${sw}
    tar -zcf ${backup} ${sw}/solgeneos/
    check_rc "tar -zcWf ${backup} ${sw}/solgeneos/"
    cd ${cur_path}
  else
    echo "Failed to change into ${sw}! Exiting..."
    exit 1
  fi
}

function install {
  install_done="false"
  files_copied="false"
  base_dir=`pwd`
  echo "Checking ${base_dir} exists..."
  if [ -d "${base_dir}" ]; then
    echo "Updating solgeneos from ${base_dir}..."
    cd ${base_dir}
    if ls README.txt 1>/dev/null 2>&1; then
      echo "Updating solgeneos installation in /usr/sw/solgeneos..."
      # take a backup first
      backup
      file_pattern=${base_dir}/lib/*.jar
      if ls ${file_pattern} 1> /dev/null 2>&1; then
        mv -f  ${file_pattern} /usr/sw/solgeneos/monitors/ && echo "Copied jar files." && files_copied="true"
      fi
      file_pattern=${base_dir}/config/Vpn*.properties
      if ls ${file_pattern} 1> /dev/null 2>&1; then
        mv -f  ${file_pattern} /usr/sw/solgeneos/config/ && echo "Copied Vpn* properties." && files_copied="true"
      fi
      file_pattern=${base_dir}/config/App*.properties
      if ls ${file_pattern} 1> /dev/null 2>&1; then
        mv -f  ${file_pattern} /usr/sw/solgeneos/config/ && echo "Copied App* properties." && files_copied="true"
      fi
      if test -e /usr/sw/solgeneos/config/_user_sample.properties; then
        echo "_user_sample.properties kept"
      else
        mv -n ${base_dir}/config/_user*.properties /usr/sw/solgeneos/config/ && echo "Copied _user* properties." && files_copied="true"
      fi
      if [ "$files_copied" = "true" ]; then
        install_done="true"
      else
        echo "No install files foundin ${base_dir}/ ..."
      fi
    else
        echo "No install files found in ${base_dir}/ ..."
    fi
  else
    echo "${base_dir} does not exist, skipping..."
  fi
  if [ "$install_done" = "true" ]; then
    restart
    echo "Installation finished!"
  else
    echo "No install files found!"
    echo "Installation failed!"
    exit 1
  fi
}

##########################################################################
# main
install
exit 0

