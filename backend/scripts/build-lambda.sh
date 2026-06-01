#!/bin/bash
set -e
./gradlew bootJar --no-daemon
JAR=$(ls build/libs/*.jar | head -1)
aws s3 cp "$JAR" s3://food-app-assets-sj/lambda/foodgroup-backend.jar
aws lambda update-function-code \
  --function-name foodgroup-backend \
  --s3-bucket food-app-assets-sj \
  --s3-key lambda/foodgroup-backend.jar \
  --region ap-northeast-2
echo "Lambda 배포 완료"
