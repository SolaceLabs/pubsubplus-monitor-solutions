#!/bin/bash

backup_dir=/root/solace_backup
soluser=backupuser
solpassword=ytbackup123
solhosts=(10.214.7.1 10.214.7.2)

EXPECT=/usr/bin/expect

for curhost in "${solhosts[@]}"
do
  mkdir -p ${backup_dir}/${curhost}

  # timestamp
  /usr/bin/date

  $EXPECT -c "
    spawn scp -p ${soluser}@${curhost}:/configs/*backup* $backup_dir/${curhost}/
    expect \"Password: \"
    send \"$solpassword\r\"
    expect eof
  "
done

# timestamp
if [ -x /usr/bin/date ]; then
  /usr/bin/date
else
  /bin/date
fi
