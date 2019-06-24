#!/bin/sh
docker build . -t lambda-graal

./gradlew shadowJar

cat bootstrap_java > build/bootstrap
chmod 755 build/bootstrap
zip -j build/function.zip build/bootstrap ./build/libs/lambda-graal-1.0-SNAPSHOT-all.jar

aws lambda create-function --function-name lambda-graal-java \
--zip-file fileb://build/function.zip --handler function.handler --runtime provided \
--role arn:aws:iam::000000000000:role/service-role/FIX_ROLE \
--region ap-northeast-1 --timeout 5

aws lambda invoke --function-name lambda-graal-java --region ap-northeast-1 \
--payload '{"input": "Lambda Java" }' response.txt
