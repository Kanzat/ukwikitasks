toolforge-jobs delete mycronjob
toolforge-jobs run mycronjob --command ./startcmd.sh --image jdk17 --emails onfailure --schedule "0 4 * * *"
