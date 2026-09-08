#!/usr/bin/env bash
# 解压后在本目录执行: ./start.sh
cd "$(dirname "$0")"
exec java -jar app.jar
