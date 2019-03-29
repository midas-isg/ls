# Apollo Location Service (LS)

## Required DBMS
- Postgres with PostGIS EXTENSION
- Data in the DBMS

## Deployment
### Required tools for deployment
- git
- JDK version 8+
### Clone
`git clone https://github.com/midas-isg/ls`

### Configuration
Copy the application_overrides.conf.template file as application_overrides.conf:

`
cp conf/application_overrides.conf.template conf/application_overrides.conf
`

Edit the conf/application_overrides.conf to the proper settings.
Change the new secret key to the output from `./activator playGenerateSecret`

### Start the service
`./activator start`

### 