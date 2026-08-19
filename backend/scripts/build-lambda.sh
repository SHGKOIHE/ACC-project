#!/bin/bash
set -e
# lambdaJar (not bootJar!) — bootJar nests classes under BOOT-INF/, which AWS Lambda's
# classloader can't see (it expects the handler class at the jar root / lib/*.jar), so it
# fails every invocation with ClassNotFoundException. lambdaJar also strips Firebase/gRPC and
# other deps that don't belong in the Lambda runtime.
./gradlew lambdaJar --no-daemon
JAR=$(ls build/libs/*-lambda.jar | head -1)
aws s3 cp "$JAR" s3://food-app-assets-sj/lambda/foodgroup-backend.jar
aws lambda update-function-code \
  --function-name foodgroup-backend \
  --s3-bucket food-app-assets-sj \
  --s3-key lambda/foodgroup-backend.jar \
  --region ap-northeast-2
echo "Lambda 배포 완료"
