You need to manually set up file with credentials for you tool. 

    become toolname
    install -m 600 <(echo -e 'user=BotUsername\npassword=BotPassword') /data/project/toolname/credentials.properties
