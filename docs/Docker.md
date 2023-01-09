# Docker

This CodeDefenders version can be easily deployed and run using Docker!

## How to deploy
Follow these steps:
- clone this repository and move to its root
- run `cd docker`
- in the [.env](../docker/.env) file set `CODEDEFENDERS_VERSION` to `latest` to run the latest CodeDefenders version (alternatively you can find images for other versions on our [Docker Hub repository](https://hub.docker.com/r/codebenders/codedefenders))
- run `docker compose up -d`

You can now access and use the tournament application for CodeDefenders at [http://localhost:80/](http://localhost:8080/).

## Custom deployment
You can change database credentials, admin credentials and default port for CodeDefender server by editing the [.env](../docker/.env) file.

For environment variables which are available by default see  [the configuration documentation](./Configuration.md).

The docker container supports only a subset of the available configuration properties.  

**WARNING:**  
Changing the `CODEDEFENDERS_DB_*` variables after creating the container for the first time requires you to update the settings in the database container manually.  
Otherwise, you have to destroy the [volumes](#persistence).

## Generating admin token

This section contains instructions to create an admin account and retrieve its admin token. This token can be used to register this CodeDefendes instance in the [Tournament Application](https://github.com/POLIMI-FER-codebenders/tournament_app).

To create an admin account follow these steps:
- before running CodeDefenders, set `CODEDEFENDERS_ADMIN_USERNAME` in [.env](../docker/.env) to the username of the admin account you want to create
- run CodeDefenders
- using CodeDefenders user interface register a new account. The new account MUST have username equal to the value of `CODEDEFENDERS_ADMIN_USERNAME` variable

You have now registered your admin account.

To obtain the admin token of the account just created:
- Log into CodeDefenders using the admin credentials of the just created account
- On the top right of the page click on your username and select "profile" section from the dropdown menu
- At the bottom of the profile page you can see the admin token next to the entry "Your API token"

## Persistence

The `docker-compose` file uses named volumes to persist the database and codedefenders data directory.  
The names used in a normal setup should be `docker_datavolume` and `docker_dbvolume`.

Those are managed via the `docker volume` command.

As long as those volumes are there, the content of the mysql db and the data folder are persisted.
So if you have stopped, and even destroyed, the containers by using the same docker-compose file the data are preserved.

**WARNING:**  
You should only ever `rm` both volumes at the same time as otherwise the database will reference no longer existing files, or the database will try to write at places where already some files exist.

More information can be found in the [docker documentation](https://docs.docker.com/storage/volumes/)
