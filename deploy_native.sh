#!/bin/sh
docker build . -t lambda-graal

./gradlew shadowJar

docker run --rm -v "$(pwd):/home/application" lambda-graal
mv server build/

cat bootstrap_native > build/bootstrap
chmod 755 build/bootstrap build/server
zip -j build/function.zip build/bootstrap build/server

aws lambda create-function --function-name lambda-graal-native \
--zip-file fileb://build/function.zip --handler function.handler --runtime provided \
--role arn:aws:iam::000000000000:role/service-role/FIX_ROLE \
--region ap-northeast-1 --timeout 5

aws lambda invoke --function-name lambda-graal-native --region ap-northeast-1 \
--payload '{"input": "Lambda GraalVM" }' response.txt
