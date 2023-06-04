toolforge-jobs delete mycronjob
toolforge-jobs run mycronjob --command ./startcmd.sh --image jdk17 --email all --schedule "0 4 * * *"
