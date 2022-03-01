#!/bin/bash -e

release_repo_url="https://mdsol.jfrog.io/artifactory/PlatformLibraries-maven-prod-local"
artifact_id=mauth-client
group_id=com.mdsol.clients
group_path=${group_id//.//} # com.mdsol.clients => com/mdsol/clients
version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
module_name="the version $version of $artifact_id"

release_status=$(curl -s -o /dev/null -w "%{http_code}" --head -u $ARTIFACTORY_ACCOUNT_NAME:$ARTIFACTORY_ACCESS_TOKEN \
  "$release_repo_url/$group_path/$artifact_id/$version/")

if [ $release_status -eq 200 ]; then
  echo "Skipping deploy, $module_name already exists"
else
  echo "Deploying $module_name"
  mvn clean deploy \
    -Dmaven.javadoc.skip=true \
    -Dmaven.test.skip=true \
    -DaltDeploymentRepository=central::default::$release_repo_url \
    -B -V \
    --settings travis/settings.xml
fi
